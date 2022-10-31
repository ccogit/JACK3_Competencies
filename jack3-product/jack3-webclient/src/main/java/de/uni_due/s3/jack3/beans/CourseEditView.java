package de.uni_due.s3.jack3.beans;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.model.ListDataModel;
import javax.faces.validator.ValidatorException;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import de.uni_due.s3.jack3.business.SubjectBusiness;
import de.uni_due.s3.jack3.entities.tenant.*;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;
import org.primefaces.model.menu.MenuModel;

import de.uni_due.s3.jack3.beans.courseedit.ChooseFolderView;
import de.uni_due.s3.jack3.beans.courseedit.ExerciseTreeView;
import de.uni_due.s3.jack3.beans.courseedit.FixedAllocationView;
import de.uni_due.s3.jack3.beans.lazymodels.LazyCourseDataModel;
import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.enums.ECourseExercisesOrder;
import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.providers.AbstractExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.enums.ECourseContentType;
import de.uni_due.s3.jack3.exceptions.DeepCloningException;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;

@ViewScoped @Named public class CourseEditView extends AbstractView implements Serializable {

	private static final String ID_OF_GROWL = "messages";

	private static final long serialVersionUID = 3803210557059695368L;

	@Inject private FixedAllocationView fixedAllocationView;

	@Inject private ChooseFolderView chooseFolderView;

	@Inject private CourseBusiness courseBusiness;

	@Inject private AuthorizationBusiness authorizationBusiness;

	@Inject private FolderBusiness folderBusiness;

	@Inject private ExerciseTreeView exerciseTreeView;

	@Inject private SubjectBusiness subjectBusiness;

	private LazyDataModel<Course> courseRevisionsLazyModel;

	private long courseRevision;

	private AbstractCourse course;

	private AbstractEntity previousItemInFolder;

	private AbstractEntity nextItemInFolder;

	private Integer revisionCount;

	private long courseId;

	private int revisionId;

	private String searchCourseOffers;

	private ECourseContentType contentType;

	private boolean newestRevision = true;

	private int currentRevisionId;

	// Because course recources are saved as a set, we store the resources as a list here for sorting
	private List<CourseResource> sortedResources;
	// Filtered values, only for PrimeFaces' datatable filter
	private List<CourseResource> filteredResources;
	// If a resource was Added the sortedResources need to be refreshed after saving the resource in the db
	private boolean resourceWasAdded = false;

	private List<FrozenCourse> availableFrozenCourses;

	private FrozenCourse selectedFrozenCourse;

	private String originalCourseName;

	private List<CourseEntry> courseEntrysMissingInMainDB = new ArrayList<>();
	private List<ContentFolder> foldersMissingInMainDB = new ArrayList<>();

	private List<Subject> subjectList = new ArrayList<>();

	private boolean readOnly;
	private boolean extended_read;

	public boolean entitysAreMissingInRevision() {
		return !courseEntrysMissingInMainDB.isEmpty() || !foldersMissingInMainDB.isEmpty();
	}

	public String getMissingEntitysAsString() {

		// process missing exercises
		StringBuilder resultString = new StringBuilder("<ul>");
		courseEntrysMissingInMainDB.forEach(entry -> {
			Exercise currentExercise = entry.getExercise();
			resultString.append("<li>");
			resultString.append(getLocalizedMessage("global.exercise")).append(" \"");
			resultString.append(currentExercise.getName());
			resultString.append("\" (ID: ").append(currentExercise.getId()).append(")");
			resultString.append("</li>");
		});
		resultString.append("</ul>");

		// process missing folders
		resultString.append("<ul>");
		foldersMissingInMainDB.forEach(folder -> {
			resultString.append("<li>");
			resultString.append(getLocalizedMessage("global.folder")).append(" \"");
			resultString.append(folder.getName());
			resultString.append("\" (ID: ").append(folder.getId()).append(")");
			resultString.append("</li>");
		});
		resultString.append("</ul>");

		return resultString.toString();
	}

	public LazyDataModel<Course> getCourseRevisionsLazyModel() {
		return courseRevisionsLazyModel;
	}

	public AbstractCourse getCourse() {
		return course;
	}

	public void setCourse(AbstractCourse course) {
		this.course = course;
	}

	public FixedAllocationView getFixedAllocationView() {
		return fixedAllocationView;
	}

	public ChooseFolderView getChooseFolderView() {
		return chooseFolderView;
	}

	public long getCourseId() {
		return courseId;
	}

	public long getCourseRevision() {
		return courseRevision;
	}

	public ECourseExercisesOrder[] getExerciseOrder() {
		return ECourseExercisesOrder.values();
	}

	public ECourseScoring[] getScoringModes() {
		return ECourseScoring.values();
	}

	public String getSearchCourseOffers() {
		return searchCourseOffers;
	}

	public void saveFrozenRevision() {
		if (readOnly) {
			getLogger().warn(
					"User " + getCurrentUser().getLoginName() + " tried to save a frozen Revision from " + course + " while not having write permission! Users should not be able to even call this function without manipulation of the UI");
			return;
		}
		if (!course.isFrozen()) {
			throw new IllegalStateException("Tried to save a FrozenCourse, but " + course + " was given!");
		}

		courseBusiness.updateCourse(course);
		addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.save", "global.success");
		loadAvailableFrozenCourses();
	}

	public void resetToRevision() {
		if (course.isFrozen()) {
			FrozenCourse frozenCourze = (FrozenCourse) course;
			currentRevisionId = frozenCourze.getProxiedCourseRevisionId();
			course = courseBusiness.getNewestRevisionOfFrozenCourse(frozenCourze)
					.orElseThrow(NoSuchJackEntityException::new);
		}
		course = courseBusiness.resetToRevision(course, currentRevisionId, getCurrentUser());

		loadAdditionalCourseData();
		loadAvailableFrozenCourses();

		selectedFrozenCourse = null;
		newestRevision = true;
		revisionCount = courseBusiness.getNumberOfRevisions(course);
	}

	private void loadAvailableFrozenCourses() {
		List<FrozenCourse> frozenRevisionsForCourse = courseBusiness.getFrozenRevisionsForCourse(course);
		Collections.sort(frozenRevisionsForCourse);
		setAvailableFrozenCourses(frozenRevisionsForCourse);
	}

	public void loadFrozenCourse() {
		if (getSelectedFrozenCourse() == null) {
			jumpToNewestRevision();
			return;
		}

		int proxiedRevisionIdOfSelectFrozenCourse = courseBusiness.getProxiedOrLastPersistedRevisionId(
				getSelectedFrozenCourse());

		loadFrozenCourse(proxiedRevisionIdOfSelectFrozenCourse);
	}

	private void loadFrozenCourse(int revisionId) {
		course = getSelectedFrozenCourse();
		currentRevisionId = revisionId;

		course = courseBusiness.getFrozenCourseByProxiedIdsWithLazyData(course.getRealCourseId(), currentRevisionId);

		newestRevision = false;

		loadAdditionalCourseData();
	}

	/**
	 * This gets called when a user clicks on the magnifing glass in the revisons overlay. We then show the old revision
	 * directly from envers to the user while editing is disabled.
	 *
	 * @param revisionIndex Index of the revision we shall show the user
	 */
	public void loadRevision(int revisionIndex) {
		if (course.isFrozen()) {
			throw new IllegalArgumentException("This shouldn't be called on a frozen course, got: " + course);
		}

		courseEntrysMissingInMainDB = new ArrayList<>();
		foldersMissingInMainDB = new ArrayList<>();

		List<Integer> revisions = courseBusiness.getRevisionNumbersFor(course);
		currentRevisionId = revisions.get(revisionIndex);

		AbstractExerciseProvider abstractExerciseProvider = courseBusiness.getContentProviderAtRevision(course,
				currentRevisionId);
		if (abstractExerciseProvider instanceof FixedListExerciseProvider) {
			FixedListExerciseProvider fixedListExerciseProvider = (FixedListExerciseProvider) abstractExerciseProvider;
			courseEntrysMissingInMainDB = courseBusiness.getCourseEntrysMissingInMainDb(fixedListExerciseProvider);
		} else if (abstractExerciseProvider instanceof FolderExerciseProvider) {
			FolderExerciseProvider folderExerciseProvider = (FolderExerciseProvider) abstractExerciseProvider;
			foldersMissingInMainDB = courseBusiness.getFoldersMissingInMainDb(folderExerciseProvider);
		}

		course = courseBusiness.getRevisionOfCourseWithLazyData(course, currentRevisionId)
				.orElseThrow(NoSuchJackEntityException::new);
		newestRevision = false;
		loadAdditionalCourseData();
	}

	public void jumpToNewestRevision() {
		// This gives us the courseId for Courses or the proxied courseId for FrozenCourses
		long id = course.getRealCourseId();

		course = courseBusiness.getCourseWithLazyDataByCourseID(id);
		newestRevision = true;

		courseEntrysMissingInMainDB = new ArrayList<>();
		foldersMissingInMainDB = new ArrayList<>();

		setSelectedFrozenCourse(null);
		loadAdditionalCourseData();
		loadAvailableFrozenCourses();
		revisionCount = courseBusiness.getNumberOfRevisions(course);
	}

	public int getCurrentRevisionId() {
		return currentRevisionId;
	}

	public boolean isCourseEntryMissing(CourseEntry courseEntry) {
		return courseEntrysMissingInMainDB.contains(courseEntry);
	}

	public boolean isFolderMissing(ContentFolder contentFolder) {
		return foldersMissingInMainDB.contains(contentFolder);
	}

	public int getCurrentProxiedExerciseRevisionIndex() {
		if (!course.isFrozen()) {
			throw new IllegalStateException("This can only be called on frozen exercises! Was: " + course);
		}
		return getRevisionIndexForRevisionId(currentRevisionId);
	}

	public void loadCourse() throws IOException {
		try {
			if (isFrozen()) {
				course = courseBusiness.getFrozenCourseByProxiedIdsWithLazyData(courseId, revisionId);
			} else {
				course = courseBusiness.getCourseWithLazyDataByCourseID(courseId);
				revisionCount = courseBusiness.getNumberOfRevisions(course);
				loadAvailableFrozenCourses();
			}
		} catch (NoSuchJackEntityException e) {
			if (isFrozen()) {
				sendErrorResponse(400,
						"FrozenCourse with the realCourseId: " + courseId + " and proxiedCourseRevisionId: " + revisionId + " not found!");
			} else {
				sendErrorResponse(400, "Course with ID " + courseId + " not found!");
			}
			return;
		}

		ContentFolder folder = folderBusiness.getFolderForAbstractCourse(course);

		if (!authorizationBusiness.isAllowedToReadFromFolder(getCurrentUser(), folder)) {
			sendErrorResponse(403, getLocalizedMessage("courseEdit.forbiddenCourse"));
			return;
		}
		setReadOnly(!authorizationBusiness.isAllowedToEditFolder(getCurrentUser(), folder));
		setExtended_read(authorizationBusiness.hasExtendedReadOnFolder(getCurrentUser(), folder));
		loadAdditionalCourseData();

		this.previousItemInFolder = folderBusiness.getNeighbor(course, -1);
		this.nextItemInFolder = folderBusiness.getNeighbor(course, +1);
	}

	private void loadAdditionalCourseData() {
		loadExerciseProvider();
		refreshResources();

		Course nonFrozenCourse = courseBusiness.getNonFrozenCourse(course);
		courseRevisionsLazyModel = new LazyCourseDataModel(nonFrozenCourse, courseBusiness);

		originalCourseName = course.getName();
	}

	public void updateBreadCrumb() {
		createUserSpecificYouAreHereModelForCourse(course, false);
	}

	private void loadExerciseProvider() {
		AbstractExerciseProvider exerciseProvider = course.getContentProvider();
		if (exerciseProvider == null) {
			contentType = null;
			return;
		}

		//If we are showing an old revision, we get an Hibernate-Proxy. To make the instanceOf-Checks down below
		// work, we get the implementation here.
		exerciseProvider = (AbstractExerciseProvider) Hibernate.unproxy(exerciseProvider);

		if (!course.isFrozen()) {
			course.setContentProvider(exerciseProvider);
		}

		// Load view with the current provider
		if (exerciseProvider instanceof FixedListExerciseProvider) {
			contentType = ECourseContentType.FIXED_ALLOCATION;
			final FixedListExerciseProvider flep = (FixedListExerciseProvider) exerciseProvider;
			fixedAllocationView.loadView(flep, course);
		} else if (exerciseProvider instanceof FolderExerciseProvider) {
			contentType = ECourseContentType.CHOOSE_FOLDER;
			final FolderExerciseProvider fep = (FolderExerciseProvider) exerciseProvider;
			chooseFolderView.loadView(fep, course);
		} else {
			throw new UnsupportedOperationException("Unknown exercise provider: " + exerciseProvider);
		}
		exerciseTreeView.updateSelectableProperty();
	}

	public void setCourseId(long courseId) {
		this.courseId = courseId;
	}

	public void setSearchCourseOffers(String searchCourseOffers) {
		this.searchCourseOffers = searchCourseOffers;
	}

	public ECourseContentType[] getContentTypes() {
		return ECourseContentType.values();
	}

	public void onContentChange() {
		// Load view with a new empty provider
		if (isFixedAllocation()) {
			final FixedListExerciseProvider flep = new FixedListExerciseProvider();
			course.setContentProvider(flep);
			fixedAllocationView.loadView(flep, course);
		}
		if (isChooseFolder()) {
			final FolderExerciseProvider fep = new FolderExerciseProvider();
			course.setContentProvider(fep);
			chooseFolderView.loadView(fep, course);
		}
		if (course.getExerciseOrder() == null || !course.getContentProvider()
				.isExerciseOrderSupported(course.getExerciseOrder())) {
			course.setExerciseOrder(ECourseExercisesOrder.ALPHABETIC_ASCENDING);
		}

		exerciseTreeView.clearSelectionState();
		exerciseTreeView.updateSelectableProperty();
	}

	public ECourseContentType getContentType() {
		return contentType;
	}

	public void setContentType(ECourseContentType contentType) {
		this.contentType = contentType;
	}

	public void saveCourse() {
		if (readOnly) {
			getLogger().warn(
					"User " + getCurrentUser().getLoginName() + " tried to save " + course + " while not having write permission! Users should not be able to even call this function without manipulation of the UI");
			return;

		}
		if (course.getContentProvider() instanceof FixedListExerciseProvider) {
			course.setContentProvider(fixedAllocationView.getContentProvider());
		} else if (course.getContentProvider() instanceof FolderExerciseProvider) {
			course.setContentProvider(chooseFolderView.getContentProvider());
		} else if (course.getContentProvider() instanceof HibernateProxy) {
			throw new AssertionError();
		}

		course = courseBusiness.updateCourse(course);

		// This resolves erroneous behaviour when the order is changed
		// (EntityNotFoundException, EntityExistsException)
		if (course.getContentProvider() instanceof FixedListExerciseProvider) {
			fixedAllocationView.updateFields((FixedListExerciseProvider) course.getContentProvider(), course);
		} else if (course.getContentProvider() instanceof FolderExerciseProvider) {
			chooseFolderView.updateFields((FolderExerciseProvider) course.getContentProvider(), course);
		}

		if (resourceWasAdded) {
			refreshResources();
			resourceWasAdded = false;
		}
		revisionCount = courseBusiness.getNumberOfRevisions(course);
	}

	public void reloadCourseByRedirect() throws IOException {
		redirect(viewId.getCurrent().withParam(Course.class, courseId));
	}

	public void redirectToTestCourse() throws IOException {
		redirect(viewId.getCourseTest().withParam(Course.class, courseId));
	}

	public SubjectBusiness getSubjectBusiness(){
		return this.subjectBusiness;
	}

	// -------------------- Course resources --------------------

	public void handleFileUpload(FileUploadEvent event) {
		final UploadedFile file = event.getFile();
		final byte[] content = file.getContent();
		if (courseBusiness.isCourseResourceFilenameAlreadyExisting(file.getFileName(), course)) {
			// Tell user that we didn't save
			addFacesMessage(ID_OF_GROWL, FacesMessage.SEVERITY_ERROR, "courseEditView.saveOverlayHeaderFail",
					"courseEditView.saveOverlayFooterFail", file.getFileName());
			return;
		}

		final CourseResource courseResource = new CourseResource(file.getFileName(), content, course, getCurrentUser());

		// Not logging this here, since the DB write happens only if the user clicks on "save"
		course.addCourseResource(courseResource);
		resourceWasAdded = true;
		refreshResources();
	}

	public StreamedContent getCourseResource(CourseResource courseResource) {
		return DefaultStreamedContent.builder().stream(() -> new ByteArrayInputStream(courseResource.getContent()))
				.contentType(courseResource.getMimeType()).name(courseResource.getFilename())
				.contentLength(courseResource.getSize()).build();
	}

	public void removeCourseResource(CourseResource courseResource) {
		// Not logging this here, since the removal happens only if the user
		// clicks on "save"
		course.removeCourseResource(courseResource);
		refreshResources();
	}

	/**
	 * Loads all course resources.
	 */
	private void refreshResources() {
		List<CourseResource> courseResourcesAsList = new ArrayList<>(course.getCourseResources());
		// Default sorting: by filename ascending
		Collections.sort(courseResourcesAsList);
		sortedResources = courseResourcesAsList;
		// We have to filter datatable again:
		// https://stackoverflow.com/questions/14339855/ajax-update-doesnt-work-when-using-filter-on-pdatatable
		PrimeFaces.current().executeScript("PF('courseResources').filter()");
	}

	public List<CourseResource> getFilteredResources() {
		return filteredResources;
	}

	public void setFilteredResources(List<CourseResource> filteredResources) {
		this.filteredResources = filteredResources;
	}

	public List<CourseResource> getSortedResources() {
		return sortedResources;
	}

	public void setSortedResources(List<CourseResource> sortedResources) {
		this.sortedResources = sortedResources;
	}

	public boolean isFixedAllocation() {
		return contentType == ECourseContentType.FIXED_ALLOCATION;
	}

	public boolean isChooseFolder() {
		return contentType == ECourseContentType.CHOOSE_FOLDER;
	}

	public void freezeRevision(int revisionIndex) {
		try {
			List<Integer> revisions = courseBusiness.getRevisionNumbersFor(course);

			courseBusiness.createFrozenCourse(course, revisions.get(revisionIndex));
			addFacesMessage(ID_OF_GROWL, FacesMessage.SEVERITY_INFO, "global.save", "global.success");

		} catch (DeepCloningException deepCloningException) {
			switch (deepCloningException.getErrorcode()) {
			case ONLY_FIXEDLIST_EXERCISEPROVIDER_ALLOWED:
				getLogger().warn(deepCloningException.getMessage());
				addFacesMessage(ID_OF_GROWL, FacesMessage.SEVERITY_ERROR,
						"CourseEditView.deepCloningNeedFixedListProvider", null);
				break;
			case ONLY_FROZEN_EXERCISES_IN_FROZENCOURSES_ALLOWED:
				getLogger().warn(deepCloningException.getMessage());
				addFacesMessage(ID_OF_GROWL, FacesMessage.SEVERITY_ERROR,
						"CourseEditView.deepCloningOnlyFrozenExercisesAllowed", null);
				break;
			default:
				throw new UnsupportedOperationException(deepCloningException);
			}
		}
		loadAvailableFrozenCourses();
	}

	public boolean isNewestRevision() {
		return newestRevision;
	}

	public boolean isRevisionCurrentRevision(int index) {
		return index + 1 == courseRevisionsLazyModel.getRowCount();
	}

	public void setNewestRevision(boolean newestRevision) {
		this.newestRevision = newestRevision;
	}

	public long getRevisionId() {
		return revisionId;
	}

	public void setRevisionId(int revisionId) {
		this.revisionId = revisionId;
	}

	public boolean isFrozen() {
		if (course == null) {
			return false;
		}
		return course.isFrozen();
	}

	public boolean revisionIsFrozen(int revisionIndex) {
		List<Integer> revisions = courseBusiness.getRevisionNumbersFor(course);
		return courseBusiness.frozenCourseExists(course.getId(), revisions.get(revisionIndex));
	}

	public List<FrozenCourse> getAvailableFrozenCourses() {
		return availableFrozenCourses;
	}

	public void setAvailableFrozenCourses(List<FrozenCourse> availableFrozenCourses) {
		this.availableFrozenCourses = availableFrozenCourses;
	}

	public FrozenCourse getSelectedFrozenCourse() {
		return selectedFrozenCourse;
	}

	public void setSelectedFrozenCourse(FrozenCourse selectedFrozenCourse) {
		this.selectedFrozenCourse = selectedFrozenCourse;
	}

	public Integer getRevisionCount() {
		return revisionCount;
	}

	public int getRevisionIndexForRevisionId(int revisionId) {
		AbstractCourse tmpCourse = course;
		if (tmpCourse.isFrozen()) {
			tmpCourse = courseBusiness.getCourseByCourseID(tmpCourse.getRealCourseId());
		}

		return courseBusiness.getRevisionIndexForRevisionId(tmpCourse, revisionId);
	}

	public ListDataModel<ResultFeedbackMapping> getResultFeedbackMappings() {
		return new ListDataModel<>(new ArrayList<>(course.getResultFeedbackMappings()));
	}

	public void addNewResultFeedbackMapping() {
		course.addResultFeedbackMapping(new ResultFeedbackMapping());
	}

	public MenuModel updateMenuModel(UserSession currentUserSession) {
		createUserSpecificYouAreHereModelForCourse(course, false);
		return currentUserSession.getModel();
	}

	public void validateCourseName(FacesContext context, UIComponent component, Object value) {
		final String newValue = (String) value;
		final String oldValue = (String) component.getAttributes().get("oldValue");

		// Name must not be empty
		if ((value == null) || newValue.strip().isEmpty()) {
			throw new ValidatorException(
					new FacesMessage(FacesMessage.SEVERITY_ERROR, getLocalizedMessage("global.invalidName"),
							getLocalizedMessage("global.invalidName.empty")));
		}

		// Name is always valid if user did not change input.
		if (newValue.equals(oldValue) || newValue.equals(originalCourseName)) {
			return;
		}
	}

	public void validateCourseResourceName(FacesContext context, UIComponent component, Object value) {
		final String newValue = (String) value;
		final String oldValue = (String) component.getAttributes().get("oldValue");

		// Name must not be empty
		if ((value == null) || newValue.strip().isEmpty()) {
			throw new ValidatorException(
					new FacesMessage(FacesMessage.SEVERITY_ERROR, getLocalizedMessage("global.invalidName"),
							getLocalizedMessage("global.invalidName.empty")));
		}

		if (newValue.equals(oldValue) || newValue.equals(originalCourseName)) {
			return;
		}

		// Check for a duplicate name
		for (CourseResource resource : course.getCourseResources()) {
			if (newValue.equals(resource.getFilename())) {
				throw new ValidatorException(
						new FacesMessage(FacesMessage.SEVERITY_ERROR, getLocalizedMessage("global.invalidInput"),
								getLocalizedMessage("global.invalidName.duplicateResource")));

			}
		}
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public boolean isExtended_read() {
		return extended_read;
	}

	public void setExtended_read(boolean extended_read) {
		this.extended_read = extended_read;
	}

	// NOTE that the following two methods cannot be moved into the specific provider class
	//      because access is needed for "is...Missing" method.

	/**
	 * Returns the visible (trimmed by non-accessible folders) path for a Course Entry. This works only for
	 * {@link FixedListExerciseProvider}.
	 */
	public String getUserSpecificPathForFixedAllocation(CourseEntry courseEntry) {
		if (!isFixedAllocation())
			throw new UnsupportedOperationException();

		Exercise targetExercise = courseEntry.getExercise();
		// Exercise.folder property is lazy
		if (!Hibernate.isInitialized(targetExercise.getFolder())) {
			targetExercise.setFolder(folderBusiness.getContentFolderFor(targetExercise));
		}
		return getUserSpecificPathForFolder(targetExercise.getFolder());
	}

	/**
	 * Returns the visible (trimmed by non-accessible folders) path for a folder. This works only for
	 * {@link FolderExerciseProvider}.
	 */
	public String getUserSpecificPathForChooseFolder(Exercise exercise) {
		if (!isChooseFolder())
			throw new UnsupportedOperationException();

		ContentFolder folder = chooseFolderView.getFolderFromProviderContainingExercise(exercise);
		return getUserSpecificPathForFolder(folder);
	}

	private String getUserSpecificPathForFolder(Folder folder) {
		boolean folderWasDeleted = folderBusiness.getContentFolderById(folder.getId()).isEmpty();
		return getPathComponent().getUserSpecificPathOfFolderAsString(folder, getCurrentUser(), folderWasDeleted);
	}

	public boolean isShowOrderHint() {
		return (course.getExerciseOrder() != null && getCourse().getContentProvider() != null) && (course.getExerciseOrder() == ECourseExercisesOrder.NUMBER_OF_SUBMISSIONS || course.getExerciseOrder() == ECourseExercisesOrder.MANUAL);
	}

	public AbstractEntity getPreviousItemInFolder() {
		return previousItemInFolder;
	}

	public AbstractEntity getNextItemInFolder() {
		return nextItemInFolder;
	}

}