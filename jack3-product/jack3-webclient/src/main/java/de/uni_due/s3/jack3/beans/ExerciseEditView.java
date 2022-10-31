package de.uni_due.s3.jack3.beans;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Unmanaged;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.FilenameUtils;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.ReorderEvent;
import org.primefaces.event.ToggleEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.diagram.Connection;
import org.primefaces.model.diagram.DefaultDiagramModel;
import org.primefaces.model.diagram.DiagramModel;
import org.primefaces.model.diagram.Element;
import org.primefaces.model.diagram.connector.Connector;
import org.primefaces.model.diagram.connector.FlowChartConnector;
import org.primefaces.model.diagram.endpoint.BlankEndPoint;
import org.primefaces.model.diagram.endpoint.EndPointAnchor;
import org.primefaces.model.diagram.overlay.ArrowOverlay;
import org.primefaces.model.file.UploadedFile;
import org.primefaces.model.menu.MenuModel;

import com.google.common.base.VerifyException;

import de.uni_due.s3.jack3.beans.ViewId.Builder;
import de.uni_due.s3.jack3.beans.lazymodels.LazyExerciseDataModel;
import de.uni_due.s3.jack3.beans.stagetypes.AbstractStageEditDialogView;
import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.ResourceBusiness;
import de.uni_due.s3.jack3.business.StatisticsBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.business.stagetypes.AbstractStageBusiness;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.enums.EStageHintMalus;
import de.uni_due.s3.jack3.entities.stagetypes.r.RStage;
import de.uni_due.s3.jack3.entities.stagetypes.r.TestCaseTuple;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.ExerciseResource;
import de.uni_due.s3.jack3.entities.tenant.FrozenExercise;
import de.uni_due.s3.jack3.entities.tenant.JSXGraph;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;
import de.uni_due.s3.jack3.exceptions.ViewBeanInstantiationFailedException;
import de.uni_due.s3.jack3.services.utils.RepeatStage;
import de.uni_due.s3.jack3.utils.ByteCount;
import de.uni_due.s3.jack3.utils.JackStringUtils;

@Named
@ViewScoped
public class ExerciseEditView extends AbstractView implements Serializable {

	private static final long serialVersionUID = -5168922975468359967L;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private ResourceBusiness resourceBusiness;

	@Inject
	private AuthorizationBusiness authorizationBusiness;

	@Inject
	private StatisticsBusiness statisticsBusiness;

	@Inject
	private UserSession userSession;

	@Inject
	private FolderBusiness folderBusiness;

	private AbstractExercise exercise;
	private AbstractEntity nextItemInFolder;
	private AbstractEntity previousItemInFolder;

	private List<FrozenExercise> availableFrozenExercises;
	private FrozenExercise selectedFrozenExercise;
	private long exerciseId;
	private boolean lazyDataLoaded = false;
	private LazyDataModel<Exercise> exerciseRevisionsLazyDataModel;
	private String newTagName;
	private List<Exercise> exercisesForThisTag;
	private long testingSubmissionCount;
	private long nonTestingsubmissionCount;
	private long numberOfUnreadComments;
	private String originalAuthor;

	private String hyperlinkToExercise;

	private transient DataModel<JSXGraph> jSXGraphModel;

	private List<AbstractStageEditDialogView> stageViewBeans;
	private String newStageType;
	private ExerciseResource currentFile;
	private boolean newestRevision = true;

	// Tags are cached due to performance issues
	private List<String> allAvailableTags;

	/**
	 * Indicates if our current user has write permission on the folder of the exercise (for Revisions or
	 * FrozenExercises that means of the corresponding exercise in the main DB).
	 */
	private boolean userAllowedToEdit = false;

	/**
	 * Indicates if user has the ability to view not only the exercise but additionally the user submissions to that
	 * exercise
	 */
	private boolean userAllowedToExtendedRead = false;

	private Integer currentRevisionId;
	private Integer revisionCount;
	private String originalExerciseName;

	private List<ExerciseResource> exerciseResourceToDelete = new ArrayList<>();
	// Because exercise recources are saved as a set, we store the resources as a list here for sorting
	private List<ExerciseResource> sortedResources;
	// Filtered values, only for PrimeFaces' datatable filter
	private List<ExerciseResource> filteredResources;
	// If a resource was Added the sortedResources need to be refreshed after saving the resource in the db
	private boolean resourceWasAdded = false;

	private ExerciseResource selectedFile;

	@Inject
	private ExercisePlayerBusiness exercisePlayerBusiness;

	@PostConstruct
	public void init() {
		// We use LinkedList here because we benefit from its fast add / remove operations
		allAvailableTags = new LinkedList<>(exerciseBusiness.getAllTagsAsStrings());
	}

	/**
	 * Loads contents of exercise from database.
	 */
	public void loadExercise() throws IOException {

		// First we always try to load the exercise with the exerciseId set in as viewParam in ExerciseEdit.xhtml
		// from the database even if we later want to load another revision of this exercise.
		Exercise exerciseToLoad;
		try {
			exerciseToLoad = exerciseBusiness.getExerciseWithLazyDataByExerciseId(exerciseId);
		} catch (NoSuchJackEntityException e) {
			sendErrorResponse(400, "Exercise with given exerciseId '" + exerciseId + "' does not exist in database");
			return;
		}

		// We check if the user has the permission to view this exercise and send a "403 - Forbidden" if not
		if (!authorizationBusiness.isAllowedToReadFromFolder(getCurrentUser(), exerciseToLoad.getFolder())) {
			sendErrorResponse(403, getLocalizedMessage("exerciseEdit.forbiddenExercise"));
			return;
		}

		if (authorizationBusiness.hasExtendedReadOnFolder(getCurrentUser(), exerciseToLoad.getFolder())) {
			userAllowedToExtendedRead = true;
		} else {
			userAllowedToExtendedRead = false;
		}

		if (authorizationBusiness.isAllowedToEditFolder(getCurrentUser(), exerciseToLoad.getFolder())) {
			userAllowedToEdit = true;
		} else {
			userAllowedToEdit = false;
		}

		if (currentRevisionId == null) {
			exercise = exerciseToLoad;
		} else {
			// Load revision if currentRevisionId is present
			try {
				// Load revision if requested lastPersistedRevisionId is not the newest.
				exercise = exerciseBusiness.getRevisionOfExerciseWithLazyData(exerciseToLoad, currentRevisionId);
				newestRevision = false;
			} catch (NoSuchJackEntityException e) {
				sendErrorResponse(400, "Exercise with given exerciseId '" + exerciseId + "' and currentRevisionId '"
						+ currentRevisionId + "' does not exist in database");
				return;
			}
		}

		originalAuthor = exerciseBusiness.getAuthorNameFromExericse(exercise);
		previousItemInFolder = folderBusiness.getNeighbor(exercise, -1);
		nextItemInFolder = folderBusiness.getNeighbor(exercise, +1);
		loadAdditionalExerciseData();
		updateHyperlinkToExercise();
	}

	public void saveFrozenRevision() {
		if (!userAllowedToEdit) {
			// We should not be here since the user should't even be able to press save if userAllowedToEdit is not set
			// true. But since that is clientside logic, we check it here to be save.
			getLogger().warn("User " + getCurrentUser().getLoginName() + " tried to save a frozen revision of " + exercise
					+ " while not having write permission! Users should not be able to even call this function without"
					+ " manipulation of the UI!");
			return;
		}

		if (!exercise.isFrozen()) {
			throw new IllegalStateException("Tried to save a FrozenCourse, but " + exercise + " was given!");
		}

		try {
			exerciseBusiness.updateExercise(exercise, getCurrentUser());
		} catch (ActionNotAllowedException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "exception.actionNotAllowed",
					"exception.actionNotAllowed.noRightsDiscardChanges");
			// AJAX update will handle the changed property and make the views readonly.
			userAllowedToEdit = false;
			return;
		}

		loadAvailableFrozenExercises();
		addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.save", "global.success");
	}

	private void loadAdditionalExerciseData() {
		loadAvailableFrozenExercises();
		refreshResources();

		if (!exercise.isFrozen()) {
			revisionCount = exerciseBusiness.getNumberOfRevisions(exercise);
		}

		Exercise nonFrozenExercise = exerciseBusiness.getNonFrozenExercise(exercise);
		exerciseRevisionsLazyDataModel = new LazyExerciseDataModel(nonFrozenExercise, exerciseBusiness);
		originalExerciseName = exercise.getName();
	}

	public void updateBreadCrumb() {
		createUserSpecificYouAreHereModelForExercise(exercise);
	}

	public void loadFrozenExercise() throws IOException {
		if (getSelectedFrozenExercise() == null) {
			jumpToNewestRevision();
			return;
		}

		exercise = exerciseBusiness.getFrozenExerciseWithLazyDataById(getSelectedFrozenExercise().getId());

		currentRevisionId = exerciseBusiness.getProxiedOrLastPersistedRevisionId(exercise);
		exerciseId = exercise.getProxiedOrRegularExerciseId();
		newestRevision = false;
		loadAdditionalExerciseData();

		getLogger().debug("Successfully loaded FrozenExercise: " + exercise);
		// Force the getter to actually load the current stages
		stageViewBeans = null;
	}

	public void freezeRevision(int revisionIndex) throws IOException {
		if (!userAllowedToEdit) {
			// We should not be here since the user should't even be able to press save if userAllowedToEdit is not set
			// true. But since that is clientside logic, we check it here to be save.
			getLogger().warn("User " + getCurrentUser().getLoginName() + " tried to freeze " + exercise
					+ " while not having write permission! Users should not be able to even call this function without"
					+ " manipulation of the UI!");
			return;
		}

		List<Integer> revisions = exerciseBusiness.getRevisionNumbersFor(exercise);

		exerciseBusiness.createFrozenExercise(exercise, revisions.get(revisionIndex));
		reloadExerciseByRedirect();
	}

	private void loadAvailableFrozenExercises() {
		List<FrozenExercise> frozenRevisionsForExercise = exerciseBusiness.getFrozenRevisionsForExercise(exercise);
		Collections.sort(frozenRevisionsForExercise);
		setAvailableFrozenExercises(frozenRevisionsForExercise);
	}

	public boolean revisionIsFrozen(int revisionIndex) {
		if (exercise.isFrozen()) {
			throw new AssertionError("This method must not be called on FrozenExercises, got " + exercise);
		}

		List<Integer> revisions = exerciseBusiness.getRevisionNumbersFor(exercise);

		return exerciseBusiness.frozenExerciseExists(exercise.getId(), revisions.get(revisionIndex));
	}

	private void reloadExerciseByRedirect() throws IOException {
		redirect(viewId.getCurrent().withParam(Exercise.class, exercise.getProxiedOrRegularExerciseId()));
	}

	private void reloadExerciseByRedirect(final int revisionIndex) throws IOException {
		final int revisionId = exerciseBusiness.getRevisionNumbersFor(exercise).get(revisionIndex);
		redirect(viewId.getCurrent().withParam(Exercise.class, exerciseId).withParam("revision", revisionId));
	}

	public void redirectToTestExercise() throws IOException {
		redirect(viewId.getExerciseTest().withParam(Exercise.class, exerciseId));
	}

	/**
	 * Loads revisions and comments (lazy data) for this exercise.
	 */
	public void loadLazyExerciseData() {
		Exercise nonFrozenExercise = exerciseBusiness.getNonFrozenExercise(exercise);

		// Get submission count
		nonTestingsubmissionCount = statisticsBusiness.countSubmissions(nonFrozenExercise);
		testingSubmissionCount = statisticsBusiness.countAllSubmissions(nonFrozenExercise) - nonTestingsubmissionCount;

		// Get comment count
		numberOfUnreadComments = statisticsBusiness.countUnreadComments(nonFrozenExercise);

		// Set flag
		lazyDataLoaded = true;
	}

	public boolean isLazyDataLoaded() {
		return lazyDataLoaded;
	}

	/**
	 * Saves all changes in the database.
	 */
	public void saveExercise() throws IOException {

		if (!userAllowedToEdit) {
			// We should not be here since the user should't even be able to press save if userAllowedToEdit is not set
			// true. But since that is clientside logic, we check it here to be save.
			getLogger().warn("User " + getCurrentUser().getLoginName() + " tried to save " + exercise
					+ " while not having write permission! Users should not be able to even call this function without"
					+ " manipulation of the UI!");
			return;
		}

		areRStagesValid(exercise);

		exercise.generateSuffixWeights();

		// Save to database and retrieve changed exercise instance
		try {
			exercise = exerciseBusiness.updateExercise(exercise, getCurrentUser());
		} catch (ActionNotAllowedException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "exception.actionNotAllowed",
					"exception.actionNotAllowed.noRightsDiscardChanges");
			// AJAX update will handle the changed property and make the views readonly.
			userAllowedToEdit = false;
			return;
		}

		for (Iterator<ExerciseResource> iterator = exerciseResourceToDelete.iterator(); iterator.hasNext();) {
			ExerciseResource exerciseResource = iterator.next();
			resourceBusiness.removeResourceIfIsOrphaned(exerciseResource);
			iterator.remove();
		}
		if (resourceWasAdded) {
			refreshResources();
			resourceWasAdded = false;
		}

		stageViewBeans = null; //Reinitialize stageViewBeans from new exercise Object
		getStageViewBeans();

		if (!exercise.isFrozen()) {
			revisionCount = exerciseBusiness.getNumberOfRevisions(exercise);
		}

		// Update cached tags
		allAvailableTags = new LinkedList<>(exerciseBusiness.getAllTagsAsStrings());
	}

	// REVIEW: Maybe alle validate methods should be moved to business?
	private boolean areRStagesValid(AbstractExercise exercise) {

		for (Stage stage : exercise.getStagesAsList()) {
			if (stage instanceof RStage) {
				RStage rStage = (RStage) stage;
				List<TestCaseTuple> testCasetuple = rStage.getTestCasetuples();
				if (testCasetuple.isEmpty()) {
					addGlobalFacesMessage(FacesMessage.SEVERITY_WARN, "global.save", "exerciseEdit.rStageCheckerMissing");
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Adds a new tag to the list of tags for this exercise.
	 */
	public void addNewTag() {
		if (JackStringUtils.isBlank(newTagName)) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_WARN, "global.information", "exerciseEdit.emptyTag");
			return;
		}

		newTagName = newTagName.strip();

		// If exercise already has this tag give a warning.
		if (exercise.getTagsAsStrings().contains(newTagName)) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_WARN, "global.information", "exerciseEdit.tagExists",
					newTagName);
			return;
		}

		exerciseBusiness.addTagToExercise(exercise, newTagName);
		newTagName = null;
	}

	/**
	 * Auto completes input string with already existing tag names.
	 *
	 * @param query
	 * @return
	 */
	public List<String> completeTags(String query) {
		query = query.toLowerCase();
		// We use LinkedList here because we benefit from its fast add operation
		final List<String> filteredTags = new LinkedList<>();
		for (final String tag : allAvailableTags) {
			if (tag.toLowerCase().contains(query)) {
				filteredTags.add(tag);
			}
		}
		return filteredTags;
	}

	/**
	 * Removes Tag from tag list.
	 *
	 * @param tag
	 */
	public void removeTag(String tag) {
		exerciseBusiness.removeTagFromExercise(exercise, tag);
	}

	public void loadExercisesWithThisTag(String tagName) {
		exercisesForThisTag = exerciseBusiness.getAllExercisesForTagNameWhereUserCanRead(tagName, getCurrentUser());
	}

	public String getExercisebreadcrumbAsString(AbstractExercise abstractExercise) {
		if (abstractExercise.isFrozen()) {
			return "";
		}
		Exercise currentExercise = (Exercise) abstractExercise;
		return getPathAsString(currentExercise.getFolder());
	}

	public List<SelectItem> getAvailableStageTypes() {
		return exercisePlayerBusiness.getRegisteredStagetypes().stream()
			.map(type -> new SelectItem(type.getCanonicalName(),getStageTypeName(type)))
			.sorted(Comparator.comparing(SelectItem::getLabel))
			.collect(Collectors.toList());
	}

	public void addNewStage() {
		if ((newStageType == null) || newStageType.isEmpty()) {
			throw new IllegalStateException("Cannot create new stage without stage type.");
		}

		for (final Class<? extends Stage> registeredStageType : exercisePlayerBusiness.getRegisteredStagetypes()) {
			if (registeredStageType.getCanonicalName().equals(newStageType)) {
				// Create the instance
				final Stage newStage = instantiateStageType(registeredStageType);

				// Place it at the end of the list of stages
				final int currentSize = exercise.getStages().size();
				newStage.setOrderIndex(currentSize);

				// Assign it the next available internal name
				newStage.setInternalName(exercise.getNextDefaultInternalNameForStages());

				// Make it the start stage if it is the only one
				if (currentSize == 0) {
					exercise.setStartStage(newStage);
				}

				// Add it to the exercise
				exercise.addStage(newStage);

				// Set view defaults if needed
				getStageBusiness(newStage).setViewDefaults(newStage);

				// destroy existing stageViewBeans
				stageViewBeans = null;
			}
		}
	}

	private Stage instantiateStageType(Class<? extends Stage> registeredStageType) {
		try {
			return registeredStageType.getDeclaredConstructor().newInstance();
		} catch (final ReflectiveOperationException  e) {
			getLogger().error("Could not create instance of stage type", e);
			throw new AssertionError("Cannot create new stage for selected stage type.", e);
		}
	}

	public Stage getRepeatStage() {
		return new RepeatStage();
	}

	private AbstractStageBusiness getStageBusiness(Stage stage) {
		final String stageBusinessName = "de.uni_due.s3.jack3.business.stagetypes." + stage.getClass().getSimpleName()
				+ "Business";

		// Load stage specific business bean
		try {
			final Class<?> stageBusinessClass = getClass().getClassLoader().loadClass(stageBusinessName);
			return (AbstractStageBusiness) (CDI.current().select(stageBusinessClass).get());
		} catch (ClassNotFoundException e) {
			throw new VerifyException(e);
		}
	}

	public List<EStageHintMalus> getAvailableHintMali() {
		return Arrays.asList(EStageHintMalus.values());
	}

	/*
	 * Ajax Event Handlers
	 */
	// TODO ms/rs: Re-implement this feature, as reorder on drag&drop is not supported by the UI anymore
	public void onStageReorder(ReorderEvent event) {
		final int from = event.getFromIndex();
		final int to = event.getToIndex();

		for (final Stage stage : exercise.getStages()) {
			if (stage.getOrderIndex() == from) {
				stage.setOrderIndex(to);
			} else if ((from < to) && (stage.getOrderIndex() > from) && (stage.getOrderIndex() <= to)) {
				stage.setOrderIndex(stage.getOrderIndex() - 1);
			} else if ((from > to) && (stage.getOrderIndex() < from) && (stage.getOrderIndex() >= to)) {
				stage.setOrderIndex(stage.getOrderIndex() + 1);
			}
		}
	}

	public void removeStage(Stage stage) {
		// remove stage from exercise
		exercise.removeStage(stage);

		// destroy existing stageViewBeans
		stageViewBeans = null;
	}

	// -------------------- Exercise resources --------------------

	/**
	 * Adds uploaded file to exercise resource list.
	 *
	 * @param event
	 */
	public void handleFileUpload(FileUploadEvent event) {
		final UploadedFile file = event.getFile();
		final byte[] content = file.getContent();
		final ExerciseResource exerciseResource = new ExerciseResource(file.getFileName(), content, getCurrentUser(),
				"", false);

		// Not logging this here, since the DB write happens only if the user clicks on "save"
		exercise.addExerciseResource(exerciseResource);
		resourceWasAdded = true;
		refreshResources();
	}

	/**
	 * Removes file from exercise resource list.
	 *
	 * @param exerciseResource
	 */
	public void removeExerciseResource(ExerciseResource exerciseResource) {

		// Remove resource from all stages
		for (Stage stage : exercise.getStages()) {
			stage.getStageResources()
			.removeIf(stageResource -> stageResource.getExerciseResource().equals(exerciseResource));
		}

		// Remove resource from exercise
		if (exercise.getExerciseResources().contains(exerciseResource)) {
			exercise.removeExerciseResource(exerciseResource);
		}

		// Only delete the resource from the db if the resource exists in the db (see also #538)
		if (resourceBusiness.getExerciseResourceById(exerciseResource.getId()).isPresent()) {
			exerciseResourceToDelete.add(exerciseResource);
		}
		refreshResources();
	}

	/**
	 * Handles file download of an exercise resource.
	 */
	public StreamedContent getExerciseResource(ExerciseResource exerciseResource) {
		return DefaultStreamedContent.builder()
				.stream(() -> new ByteArrayInputStream(exerciseResource.getContent()))
				.name(exerciseResource.getFilename())
				.contentLength(exerciseResource.getSize())
				.build();
	}

	/**
	 * Loads all exercise resources.
	 */
	private void refreshResources() {
		List<ExerciseResource> exerciseResourcesAsList = new ArrayList<>(exercise.getExerciseResources());
		// Default sorting: by filename ascending
		Collections.sort(exerciseResourcesAsList);
		sortedResources = exerciseResourcesAsList;
		// We have to filter datatable again:
		// https://stackoverflow.com/questions/14339855/ajax-update-doesnt-work-when-using-filter-on-pdatatable
		PrimeFaces.current().executeScript("PF('exerciseResources').filter()");
	}

	public List<ExerciseResource> getFilteredResources() {
		return filteredResources;
	}

	public void setFilteredResources(List<ExerciseResource> filteredResources) {
		this.filteredResources = filteredResources;
	}

	public List<ExerciseResource> getSortedResources() {
		return sortedResources;
	}

	public void setSortedResources(List<ExerciseResource> sortedResources) {
		this.sortedResources = sortedResources;
	}

	/**
	 * Gets the size of an exercise resource in Bytes with SI prefixes.
	 */
	public String getResourceSize(ExerciseResource exerciseResource) {
		return ByteCount.toSIString(exerciseResource.getSize());
	}

	public void addNewVariable() {
		// Create the instance
		final VariableDeclaration newVariable = new VariableDeclaration(exercise.getNextDefaultNameForVariables());

		// Add it to the exercise
		exercise.addVariable(newVariable);
	}

	/**
	 * Checks that a new variable name is not empty and no other variable with this name exists in this exercise.
	 */
	public void validateVariableName(FacesContext context, UIComponent component, Object value) {

		final String newVariableName = (String) value;
		String oldVariableName = ((UIInput) component).getValue().toString();

		if ((value == null) || oldVariableName.equals(newVariableName)) {
			return;
		}

		// Check if any existing variable declaration has already this name
		if (exercise.getVariableDeclarations().stream()
				.anyMatch(varDecl -> varDecl.getName().equals(newVariableName))) {
			throw new ValidatorException(
					new FacesMessage(FacesMessage.SEVERITY_ERROR, getLocalizedMessage("global.invalidInput"),
							formatLocalizedMessage("exerciseEditView.varExists", new Object[] { newVariableName })));
		}
	}

	public void removeVariable(VariableDeclaration variable) {
		exercise.removeVariable(variable);
	}

	public LazyDataModel<Exercise> getExerciseRevisionsLazyDataModel() {
		return exerciseRevisionsLazyDataModel;
	}

	public ExerciseResource getCurrentFile() {
		return currentFile;
	}

	public void setCurrentFile(ExerciseResource currentFile) {
		this.currentFile = currentFile;
	}

	public List<AbstractStageEditDialogView> getStageViewBeans() {
		if (stageViewBeans == null) {
			stageViewBeans = new ArrayList<>();

			AbstractStageEditDialogView currentStageViewBean;
			for (Stage stage : exercise.getStagesAsList()) {
				try {
					@SuppressWarnings("unchecked")
					final Class<AbstractStageEditDialogView> viewBeanClass = (Class<AbstractStageEditDialogView>) this
					.getClass().getClassLoader()
					.loadClass("de.uni_due.s3.jack3.beans.stagetypes." + stage.getClass().getSimpleName()
							+ "EditDialogView");
					currentStageViewBean = new Unmanaged<>(viewBeanClass).newInstance().produce().inject()
							.postConstruct().get();
					currentStageViewBean.setParentView(this);
					currentStageViewBean.setStage(stage);
					stageViewBeans.add(currentStageViewBean);
				} catch (final ClassNotFoundException e) {
					throw new ViewBeanInstantiationFailedException(
							"View bean for stage type " + stage.getClass().getSimpleName() + " cannot be loaded.", e);
				}

			}
		}

		return stageViewBeans;
	}

	public AbstractExercise getExercise() {
		return exercise;
	}

	public void setExercise(AbstractExercise exercise) {
		this.exercise = exercise;
	}

	public long getExerciseId() {
		return exerciseId;
	}

	public void setExerciseId(long exerciseId) {
		this.exerciseId = exerciseId;
	}

	public String getOriginalAuthor() {
		return originalAuthor;
	}

	public List<Exercise> getExercisesForThisTag() {
		return exercisesForThisTag;
	}

	public void setExercisesForThisTag(List<Exercise> exercisesForThisTag) {
		this.exercisesForThisTag = exercisesForThisTag;
	}

	public String getNewStageType() {
		return newStageType;
	}

	public void setNewStageType(String stageType) {
		newStageType = stageType;
	}

	public String getStageTypeName(final Class<? extends Stage> stageClass) {
		return getLocalizedMessage("global.stageType." + stageClass.getSimpleName());
	}

	public String getNewTagName() {
		return newTagName;
	}

	public void setNewTagName(String newTagName) {
		this.newTagName = newTagName;
	}

	public Integer getCurrentRevisionId() {
		return currentRevisionId;
	}

	public void setCurrentRevisionId(Integer currentRevisionId) {
		this.currentRevisionId = currentRevisionId;
	}

	public long getTestingSubmissionCount() {
		return testingSubmissionCount;
	}

	public long getNonTestingsubmissionCount() {
		return nonTestingsubmissionCount;
	}

	// REVIEW bo: das sollte obsolet sein, oder?
	public void exportStage(Stage stage) {
		// TODO Implement this feature
		// Is this method placed here correctly or should it be moved to AbstractStageEditDialogView?
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public void showRevision(int index) throws IOException {
		reloadExerciseByRedirect(index);
	}

	public boolean isRevisionCurrentRevision(int index) {
		return index + 1 == exerciseRevisionsLazyDataModel.getRowCount();
	}

	public boolean isNewestRevision() {
		return newestRevision;
	}

	public void jumpToNewestRevision() throws IOException {
		if (!newestRevision) {
			reloadExerciseByRedirect();
		}
	}

	public boolean isFrozen() {
		if (exercise == null) {
			return false;
		}
		return exercise.isFrozen();
	}

	public void resetToRevision() throws IOException {
		if (!userAllowedToEdit) {
			// We should not be here since the user should't even be able to press save if userAllowedToEdit is not set
			// true. But since that is clientside logic, we check it here to be save.
			getLogger().warn("User " + getCurrentUser().getLoginName() + " tried to set to an old revision of " + exercise
					+ " while not having write permission! Users should not be able to even call this function without"
					+ " manipulation of the UI!");
			return;
		}

		Exercise exerciseNewestRev;
		int revisionID;
		if (exercise.isFrozen()) {
			revisionID = ((FrozenExercise) exercise).getProxiedExerciseRevisionId();

			long realExerciseId = ((FrozenExercise) exercise).getProxiedOrRegularExerciseId();
			exerciseNewestRev = exerciseBusiness.getExerciseById(realExerciseId)
					.orElseThrow(NoSuchJackEntityException::new);
		} else {
			// We need to get the exercise again to get the correct current folder.
			exerciseNewestRev = exerciseBusiness.getExerciseWithLazyDataByExerciseId(exercise.getId());
			revisionID = currentRevisionId;
		}
		try {
			exercise = exerciseBusiness.resetToRevision(exerciseNewestRev, revisionID, getCurrentUser());
		} catch (ActionNotAllowedException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "exception.actionNotAllowed",
					"exception.actionNotAllowed.noRightsDiscardChanges");
			// AJAX update will handle the changed property and make the views readonly.
			userAllowedToEdit = false;
			return;
		}

		reloadExerciseByRedirect();
	}

	public void addResourceAsImageInEditor(final ExerciseResource exerciseResource, String editorWidgetVarId,
			int orderIndex) {
		String resourceURL = getExerciseResourceURL(exerciseResource);
		StringBuilder imgHTML = new StringBuilder(100);
		imgHTML.append("&nbsp;<img ");
		imgHTML.append("src=");
		imgHTML.append(resourceURL);
		imgHTML.append(" />&nbsp;");
		String output = imgHTML.toString();

		String stageNumber = Integer.toString(orderIndex);
		PrimeFaces.current().executeScript(
				"PF('" + editorWidgetVarId + "_" + stageNumber + "').instance.insertHtml('" + output + "')");
	}

	public void addResourceAsLinkInEditor(final ExerciseResource exerciseResource, String editorWidgetVarId,
			int orderIndex) {
		String resourceURL = getExerciseResourceURL(exerciseResource);
		StringBuilder imgHTML = new StringBuilder(100);
		imgHTML.append("&nbsp;<a ");
		imgHTML.append("href=");
		imgHTML.append(resourceURL);
		imgHTML.append(" download>");
		imgHTML.append(exerciseResource.getFilename());
		imgHTML.append("</a>&nbsp;");
		String output = imgHTML.toString();

		String stageNumber = Integer.toString(orderIndex);
		PrimeFaces.current().executeScript(
				"PF('" + editorWidgetVarId + "_" + stageNumber + "').instance.insertHtml('" + output + "')");
	}

	public boolean isStageExisting() {
		return !stageViewBeans.isEmpty();
	}

	public List<ExerciseResource> getImageList() {
		Set<ExerciseResource> allResources = exercise.getExerciseResources();
		List<ExerciseResource> imageList = new ArrayList<>();
		for (ExerciseResource exResource : allResources) {
			if ("image".equals(exResource.getMediaType()) && (exResource.getId() != 0)) {
				// Only saved image resources are allowed
				imageList.add(exResource);
			}
		}
		return imageList;
	}

	public List<FrozenExercise> getAvailableFrozenExercises() {
		return availableFrozenExercises;
	}

	public void setAvailableFrozenExercises(List<FrozenExercise> availableFrozenExercises) {
		this.availableFrozenExercises = availableFrozenExercises;
	}

	public FrozenExercise getSelectedFrozenExercise() {
		return selectedFrozenExercise;
	}

	public void setSelectedFrozenExercise(FrozenExercise selectedFrozenExercise) {
		this.selectedFrozenExercise = selectedFrozenExercise;
	}

	public Integer getRevisionCount() {
		return revisionCount;
	}

	public int getRevisionIndexForRevisionId(int revisionId) {
		AbstractExercise tmpExercise = exercise;
		if (tmpExercise.isFrozen()) {
			tmpExercise = exerciseBusiness
					.getExerciseWithLazyDataByExerciseId(tmpExercise.getProxiedOrRegularExerciseId());
		}

		return exerciseBusiness.getRevisionIndexForRevisionId(tmpExercise, revisionId);
	}

	public int getCurrentProxiedExerciseRevisionIndex() {
		if (!exercise.isFrozen()) {
			throw new IllegalStateException("This can only be called on frozen exercises! Was: " + exercise);
		}

		return getRevisionIndexForRevisionId(currentRevisionId);
	}

	public long getNumberOfUnreadComments() {
		return numberOfUnreadComments;
	}

	public void variableDeclarationReorder(ReorderEvent event) {
		exercise.reorderVariableDeclarations(event.getFromIndex(), event.getToIndex());
	}

	public MenuModel updateMenuModel(UserSession currentUserSession) {
		createUserSpecificYouAreHereModelForExercise(exercise);
		return currentUserSession.getModel();
	}

	public DiagramModel getStageGraphModel() {
		DefaultDiagramModel model = new DefaultDiagramModel();
		model.setMaxConnections(-1);
		model.setConnectionsDetachable(false);

		FlowChartConnector defaultTransitionConnector = new FlowChartConnector();
		defaultTransitionConnector.setPaintStyle("{strokeStyle:'#5D5443',lineWidth:4}");
		FlowChartConnector stageTransitionConnector = new FlowChartConnector();
		stageTransitionConnector.setPaintStyle("{strokeStyle:'#7D7463',lineWidth:3}");
		FlowChartConnector skipTransitionConnector = new FlowChartConnector();
		skipTransitionConnector.setPaintStyle("{strokeStyle:'#7D7463 dashed',lineWidth:3}");
		// model.setDefaultConnector(defaultTransitionConnector);

		Map<Stage, Element> stageNodeMap = new HashMap<>();
		Map<Stage, Integer> stageLayerMap = new HashMap<>();
		Map<Integer, List<Element>> layerNodeListMap = new HashMap<>();

		for (Stage stage : exercise.getStages()) {
			Element stageNode = new Element(stage.getInternalName());
			stageNode.setId(Long.toString(stage.getId()));
			stageNode.addEndPoint(new BlankEndPoint(EndPointAnchor.TOP));
			stageNode.addEndPoint(new BlankEndPoint(EndPointAnchor.BOTTOM));
			stageNode.addEndPoint(new BlankEndPoint(EndPointAnchor.RIGHT));
			model.addElement(stageNode);
			stageNodeMap.put(stage, stageNode);
		}

		Queue<Stage> stageQueue = new LinkedList<>();
		Stage start = exercise.getStartStage();
		stageQueue.add(start);
		stageLayerMap.put(start, 0);
		List<Element> layerNodeList = new LinkedList<>();
		layerNodeList.add(stageNodeMap.get(start));
		layerNodeListMap.put(0, layerNodeList);
		int maxLayer = 0;

		while (!stageQueue.isEmpty()) {
			Stage stage = stageQueue.poll();
			int currentLayer = stageLayerMap.get(stage);
			if (currentLayer > maxLayer) {
				maxLayer = currentLayer;
			}

			if (stage.getDefaultTransition() != null) {
				Stage target = stage.getDefaultTransition().getTarget();
				if (target != null) {
					addStageGraphEdge(model, stageNodeMap, stage, target, defaultTransitionConnector);
					if (!(target instanceof RepeatStage) && !stageLayerMap.containsKey(target)) {
						stageQueue.add(target);
						stageLayerMap.put(target, currentLayer + 1);
						layerNodeList = layerNodeListMap.get(currentLayer + 1);
						if (layerNodeList == null) {
							layerNodeList = new LinkedList<>();
						}
						layerNodeList.add(stageNodeMap.get(target));
						layerNodeListMap.put(currentLayer + 1, layerNodeList);
					}
				}
			}
			for (StageTransition stageTransition : stage.getStageTransitions()) {
				Stage target = stageTransition.getTarget();
				if (target != null) {
					addStageGraphEdge(model, stageNodeMap, stage, target, stageTransitionConnector);
					if (!(target instanceof RepeatStage) && !stageLayerMap.containsKey(target)) {
						stageQueue.add(target);
						stageLayerMap.put(target, currentLayer + 1);
						layerNodeList = layerNodeListMap.get(currentLayer + 1);
						if (layerNodeList == null) {
							layerNodeList = new LinkedList<>();
						}
						layerNodeList.add(stageNodeMap.get(target));
						layerNodeListMap.put(currentLayer + 1, layerNodeList);
					}
				}
			}
			for (StageTransition skipTransition : stage.getSkipTransitions()) {
				Stage target = skipTransition.getTarget();
				if (target != null) {
					addStageGraphEdge(model, stageNodeMap, stage, target, skipTransitionConnector);
					if (!(target instanceof RepeatStage) && !stageLayerMap.containsKey(target)) {
						stageQueue.add(target);
						stageLayerMap.put(target, currentLayer + 1);
						layerNodeList = layerNodeListMap.get(currentLayer + 1);
						if (layerNodeList == null) {
							layerNodeList = new LinkedList<>();
						}
						layerNodeList.add(stageNodeMap.get(target));
						layerNodeListMap.put(currentLayer + 1, layerNodeList);
					}
				}
			}
		}

		computeStageGraphLayout(stageNodeMap, stageLayerMap, layerNodeListMap, maxLayer);

		return model;
	}

	private void computeStageGraphLayout(Map<Stage, Element> stageNodeMap, Map<Stage, Integer> stageLayerMap,
			Map<Integer, List<Element>> layerNodeListMap, int maxLayer) {
		int spaceY = 400 / (maxLayer + 2);
		for (Entry<Stage, Integer> layer : stageLayerMap.entrySet()) {
			Element node = stageNodeMap.get(layer.getKey());
			if (node != null) {
				node.setY((spaceY + (spaceY * layer.getValue())) + "px");
			}
		}

		for (Entry<Integer, List<Element>> layer : layerNodeListMap.entrySet()) {
			int spaceX = 924 / (layer.getValue().size() + 1);

			int i = 1;
			for (Element node : layer.getValue()) {
				if (node != null) {
					node.setX((((spaceX * i)) - 96) + "px");
					i++;
				}
			}
		}
	}

	private void addStageGraphEdge(DefaultDiagramModel model, Map<Stage, Element> stageNodeMap, Stage source,
			Stage target, Connector connector) {
		Connection connection = null;

		if (target instanceof RepeatStage) {
			connection = new Connection(stageNodeMap.get(source).getEndPoints().get(2),
					stageNodeMap.get(source).getEndPoints().get(0));
		} else if ((target != null) && (stageNodeMap.get(target) != null)) {
			connection = new Connection(stageNodeMap.get(source).getEndPoints().get(1),
					stageNodeMap.get(target).getEndPoints().get(0));
		}

		if (connection != null) {
			connection.setConnector(connector);
			connection.getOverlays().add(new ArrowOverlay(20, 20, 1, 1));
			model.connect(connection);
		}
	}

	public void validateResourceName(FacesContext context, UIComponent component, Object value) {

		final String newValue = (String) value;

		// Name must not be empty
		if ((value == null) || newValue.strip().isEmpty()) {
			throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
					getLocalizedMessage("global.invalidName"), getLocalizedMessage("global.invalidName.empty")));
		}
	}

	public void validateExerciseName(FacesContext context, UIComponent component, Object value) {

		final String newValue = (String) value;
		final String oldValue = (String) component.getAttributes().get("oldValue");

		// Name must not be empty
		if ((value == null) || newValue.strip().isEmpty()) {
			throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
					getLocalizedMessage("global.invalidName"), getLocalizedMessage("global.invalidName.empty")));
		}

		// Name is always valid if user did not change input.
		if (newValue.equals(oldValue) || newValue.equals(originalExerciseName)) {
		}
	}

	public boolean isUserAllowedToEdit() {
		return userAllowedToEdit;
	}

	public boolean isUserAllowedToExtendedRead() {
		return userAllowedToExtendedRead;
	}

	public void addNewJSXGraph(String name) {
		int countJSXGraphs = exercise.getJSXGraphs().size();
		countJSXGraphs = getNextFreeJSXGraphNumber(name, countJSXGraphs);
		name += countJSXGraphs;
		JSXGraph jSXGraph = new JSXGraph(name, countJSXGraphs);
		exercise.addJSXGraph(jSXGraph);
		reintializeJSXGraphDataModel();
	}

	private static Comparator<JSXGraph> getAbstractJSXGraphComparator() {
		return Comparator.comparing(JSXGraph::getOrderIndex);
	}

	public DataModel<JSXGraph> getJSXGraphsDataModel() {
		if(jSXGraphModel == null) {
			reintializeJSXGraphDataModel();
		}
		return jSXGraphModel;
	}

	private int getNextFreeJSXGraphNumber(String graphName, int startNumber) {
		ArrayList<JSXGraph> allExerciseGraphs = new ArrayList<>(exercise.getJSXGraphs());
		boolean graphNameUnique = false;
		while(!graphNameUnique) {
			startNumber += 1;
			graphNameUnique = true;
			for(JSXGraph graph : allExerciseGraphs) {
				if((graphName + Integer.toString(startNumber)).equals(graph.getName())) {
					graphNameUnique = false;
				}
			}
		}
		return startNumber;
	}

	private void reintializeJSXGraphDataModel() {
		List<JSXGraph> listJSXGraphs = new ArrayList<>(exercise.getJSXGraphs());
		Collections.sort(listJSXGraphs, getAbstractJSXGraphComparator());
		jSXGraphModel = new ListDataModel<>(listJSXGraphs);
	}

	public void validateJSXGraphName(FacesContext fcontext, UIComponent component, Object value) {
		String newJSXGraphName = (String) value;
		String oldJSXGraphName = ((UIInput) component).getValue().toString();

		if((value == null) || oldJSXGraphName.contentEquals(newJSXGraphName)) {
			return;
		}

		if(exercise.getJSXGraphs().stream().anyMatch(jsxGraph -> jsxGraph.getName().equals(newJSXGraphName))) {
			throw new ValidatorException(
					new FacesMessage(FacesMessage.SEVERITY_ERROR, getLocalizedMessage("global.invalidInput"),
							formatLocalizedMessage("exerciseEditView.jsxGraphExists", new Object[] { newJSXGraphName })));
		}
	}

	public void removeJSXGraph(JSXGraph jSXGraph) {
		exercise.removeJSXGraph(jSXGraph);
		reintializeJSXGraphDataModel();
	}

	public void onToggle(ToggleEvent event) {
		String panelId = event.getComponent().getId();
		if (org.primefaces.model.Visibility.VISIBLE.equals(event.getVisibility())) {
			userSession.addToggledComponent(exercise, panelId);
		} else {
			userSession.removeToggledComponent(exercise, panelId);
		}
	}

	public boolean panelCollapsed(UIComponent panel) {
		String id = panel.getId();
		return userSession.isComponentCollapsed(id, exercise);
	}

	public void duplicateStage(Stage stage) {

		Stage duplicate = stage.deepCopy();

		// Place it at the end of the list of stages
		final int currentSize = exercise.getStages().size();
		duplicate.setOrderIndex(currentSize);

		// Assign it the next available internal name
		duplicate.setInternalName(exercise.getNextDefaultInternalNameForStages());

		// Add it to the exercise
		exercise.addStage(duplicate);

		// Set view defaults if needed
		getStageBusiness(duplicate).setViewDefaults(duplicate);

		// destroy existing stageViewBeans
		stageViewBeans = null;

	}

	/**
	 * Replace the content of the selected file with the content of the uploaded file.
	 * The content is only replaced, if both files have the same file-extension.
	 *
	 * @param event
	 */
	public void handleFileReplacement(FileUploadEvent event) {

		final UploadedFile file = event.getFile();

		String newFileType = FilenameUtils.getExtension(file.getFileName());
		String originalType = FilenameUtils.getExtension(selectedFile.getFilename());

		if (!newFileType.equals(originalType)) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "exerciseEdit.wrongFileExtension",
					null);
			return;
		}

		selectedFile.setContent(file.getContent());

		// Not logging this here, since the DB write happens only if the user clicks on "save"
		resourceWasAdded = true;
		refreshResources();

	}

	public void setSelectedFile(ExerciseResource selectedFile) {
		this.selectedFile = selectedFile;
	}

	public void updateHyperlinkToExercise() {
		Builder viewIdBuilder = viewId.getQuickId().withParam("exerciseId", exerciseId);
		setHyperlinkToExercise(getServerUrl() + viewIdBuilder.toActionUrl());
	}

	public String getHyperlinkToExercise() {
		return hyperlinkToExercise;
	}

	public void setHyperlinkToExercise(String hyperlinkToExercise) {
		this.hyperlinkToExercise = hyperlinkToExercise;
	}

	public AbstractEntity getNextItemInFolder() {
		return nextItemInFolder;
	}

	public AbstractEntity getPreviousItemInFolder() {
		return previousItemInFolder;
	}
}
