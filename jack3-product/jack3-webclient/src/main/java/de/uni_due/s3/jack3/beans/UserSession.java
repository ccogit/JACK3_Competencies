package de.uni_due.s3.jack3.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.event.ToggleEvent;
import org.primefaces.model.TreeNode;
import org.primefaces.model.Visibility;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.MenuModel;

import de.uni_due.s3.jack3.beans.data.StatisticsStateHolder;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;

@SessionScoped
@Named
public class UserSession extends AbstractView implements Serializable {

	private static final long serialVersionUID = 6764199592572006189L;

	private static final Locale DEFAULT_LOCALE = Locale.GERMAN;

	@Inject
	private UserBusiness userBusiness;

	private Locale locale;

	/*
	 * It is saved session-wide which folders were marked last. This is helpful during a page reload to jump directly
	 * back to the place.
	 */
	private Long latestPresentationLocationId;
	private Long latestContentLocationId;

	/* Setting for start content tab view */
	private boolean showContentTags = false;
	private boolean showContentInternalDescription = false;


	/* Setting for ShowCourseRecordView */
	/**
	 * Stores personal passwords for course offers for which the user was authenticated.
	 */
	private Map<CourseOffer, String> courseOfferPasswords = new HashMap<>();

	private final List<Folder> expandedFolders = new ArrayList<>();
	private final List<Folder> expandedFoldersCourseEdit = new ArrayList<>();

	private MenuModel model;
	private Folder currentFolder;

	private static Map<String, Object> countries;

	private Map<AbstractExercise, List<String>> toggledComponentsInExercises = new HashMap<>();
	private final StatisticsStateHolder statisticsStateHolder = new StatisticsStateHolder();

	// Saves ID of panels and their toggle state (TRUE means hidden / collapsed)
	private final Map<String, Boolean> toggleState = new HashMap<>();

	public boolean isPanelCollapsed(String key, boolean defaultValue) {
		return toggleState.getOrDefault(key, defaultValue);
	}

	public void togglePanel(ToggleEvent event) {
		toggleState.put(event.getComponent().getClientId(), event.getVisibility() == Visibility.HIDDEN);
	}

	static {
		countries = new LinkedHashMap<>();
		countries.put(Locale.GERMAN.getDisplayName(Locale.GERMAN), Locale.GERMAN);
		countries.put(Locale.ENGLISH.getDisplayName(Locale.ENGLISH), Locale.ENGLISH);
	}

	// value change event listener
	public void countryLocaleCodeChanged(ValueChangeEvent e) {

		final String newLocaleValue = e.getNewValue().toString();

		// loop country map to compare the locale code
		for (final Map.Entry<String, Object> entry : countries.entrySet()) {

			if (entry.getValue().toString().equals(newLocaleValue)) {
				FacesContext.getCurrentInstance().getViewRoot().setLocale((Locale) entry.getValue());
				setLocale((Locale) entry.getValue());
				if (getCurrentUser() != null) {
					updateCurrentUserLanguage(newLocaleValue);
				}
			}
		}
	}

	public Map<String, Object> getCountriesInMap() {
		return countries;
	}

	public void createYouAreHereMenuForCourse(AbstractCourse abstractCourse, boolean testModus) {
		if (abstractCourse.isFromEnvers()) {
			/**
			 * revisions of the course could be in an deleted folder
			 * so don't show MenuModel
			 */
			model = null;
			currentFolder = null;
			return;
		}
		//Frozen Courses don't have a Path.
		if (abstractCourse.isFrozen()) {
			model = null;
			currentFolder = null;
			return;
		}
		currentFolder = ((Course) abstractCourse).getFolder();
		model = getPathAsModel((Course)abstractCourse, !testModus, false);
	}
	
	public void createUserSpecificYouAreHereMenuForCourse(AbstractCourse abstractCourse, boolean testModus) {
		if (abstractCourse.isFromEnvers()) {
			/**
			 * revisions of the course could be in an deleted folder
			 * so don't show MenuModel
			 */
			model = null;
			currentFolder = null;
			return;
		}
		//Frozen Courses don't have a Path.
		if (abstractCourse.isFrozen()) {
			model = null;
			currentFolder = null;
			return;
		}
		currentFolder = ((Course) abstractCourse).getFolder();
		model = getUserSpecificPathAsModel(((Course) abstractCourse), !testModus, false);
	}

	public void createYouAreHereMenuForCourseOffer(CourseOffer courseOffer) {
		currentFolder = courseOffer.getFolder();
		model = getPathAsModel(courseOffer, true, false, true);
	}

	public void createYouAreHereMenuForCourseOfferEdit(CourseOffer courseOffer) {
		currentFolder = courseOffer.getFolder();
		model = getPathAsModel(courseOffer, true, false, false);
	}

	public void createYouAreHereMenuForExercise(AbstractExercise abstractExercise) {
		//Frozen Exercises don't have a Path.
		if (abstractExercise.isFrozen()) {
			model = null;
			currentFolder = null;
			return;
		}
		// Prevent generating of YouAreHere-model for old revisions, which is never shown anyway, fixes #712
		if (abstractExercise.isFromEnvers()) {
			model = null;
			currentFolder = null;
			return;
		}
		currentFolder = ((Exercise)abstractExercise).getFolder();
		model = getPathAsModel((Exercise)abstractExercise, true, false);
	}
	
	public void createUserSpecificYouAreHereMenuForExercise(AbstractExercise abstractExercise) {
		//Frozen Exercises don't have a Path.
		if (abstractExercise.isFrozen()) {
			model = null;
			currentFolder = null;
			return;
		}
		// Prevent generating of YouAreHere-model for old revisions, which is never shown anyway, fixes #712
		if (abstractExercise.isFromEnvers()) {
			model = null;
			currentFolder = null;
			return;
		}
		currentFolder = ((Exercise)abstractExercise).getFolder();
		model = getUserSpecificPathAsModel((Exercise) abstractExercise, true, false);
	}

	public void createYouAreHereMenuForPresentationFolder(PresentationFolder folder) {
		currentFolder = folder;
		model = getPathAsModel(folder, true, true, false);
	}

	@Override
	public void addYouAreHereModelMenuEntry(DefaultMenuItem item) {
		model = addDefaultMenuModelToMenuModel(model, item);
	}

	public Locale getLocale() {
		final User currentUser = getCurrentUser();
		if (currentUser == null || currentUser.getLanguage() == null) {
			if (locale != null) {
				return locale;
			} else {
				return DEFAULT_LOCALE;
			}
		}
		return currentUser.getLanguage();
	}

	public void updateCurrentUserLanguage(String newLanguage) {
		final User user = getCurrentUser();
		user.setLanguage(newLanguage);
		userBusiness.updateUser(user);
	}

	@CheckForNull
	public Long getLatestPresentationLocationId() {
		return latestPresentationLocationId;
	}

	public void setLatestPresentationLocationId(Long latestLocationId) {
		latestPresentationLocationId = latestLocationId;
	}

	@CheckForNull
	public Long getLatestContentLocationId() {
		return latestContentLocationId;
	}

	public void setLatestContentLocationId(Long latestContentLocationId) {
		this.latestContentLocationId = latestContentLocationId;
	}

	public MenuModel getModel() {
		return model;
	}

	public void setModel(MenuModel model) {
		this.model = model;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public boolean isShowContentTags() {
		return showContentTags;
	}

	public void setShowContentTags(boolean showContentTags) {
		this.showContentTags = showContentTags;
	}

	public boolean isShowContentInternalDescriptions() {
		return showContentInternalDescription;
	}

	public void setShowContentInternalDescriptions(boolean showContentInternalDescription) {
		this.showContentInternalDescription = showContentInternalDescription;
	}


	public Folder getCurrentFolder() {
		return currentFolder;
	}

	public void setCurrentFolder(Folder currentFolder) {
		this.currentFolder = currentFolder;
	}

	public List<Folder> getExpandedFolders() {
		return expandedFolders;
	}

	public void addExpandedFolder(TreeNode folderNode) {
		Folder collapsedFolder = (Folder) folderNode.getData();
		if (!expandedFolders.contains(collapsedFolder)) {
			expandedFolders.add(collapsedFolder);
		}
	}

	public void removeExpandedFolder(TreeNode folderNode) {
		Folder expandedFolder = (Folder) folderNode.getData();
		if (expandedFolders.contains(expandedFolder)) {
			expandedFolders.remove(expandedFolder);
		}
		for (TreeNode childNode : folderNode.getChildren()) {
			if (childNode.getData() instanceof Folder) {
				childNode.setExpanded(false);
				removeExpandedFolder(childNode);
			}
		}
	}

	public List<Folder> getExpandedFoldersCourseEdit() {
		return expandedFoldersCourseEdit;
	}

	public void addExpandedFolderCourseEdit(TreeNode folderNode) {
		Folder collapsedFolder = (Folder) folderNode.getData();
		if (!expandedFoldersCourseEdit.contains(collapsedFolder)) {
			expandedFoldersCourseEdit.add(collapsedFolder);
		}
	}

	public void removeExpandedFolderCourseEdit(TreeNode folderNode) {
		Folder expandedFolder = (Folder) folderNode.getData();
		if (expandedFoldersCourseEdit.contains(expandedFolder)) {
			expandedFoldersCourseEdit.remove(expandedFolder);
		}
		for (TreeNode childNode : folderNode.getChildren()) {
			if (childNode.getData() instanceof Folder) {
				childNode.setExpanded(false);
				removeExpandedFolderCourseEdit(childNode);
			}
		}
	}

	public void addToggledComponent(AbstractExercise exercise, String componentId) {
		if (!toggledComponentsInExercises.containsKey(exercise)) {
			List<String> stages = new ArrayList<>();
			stages.add(componentId);
			toggledComponentsInExercises.put(exercise, stages);
		} else {
			toggledComponentsInExercises.get(exercise).add(componentId);
		}
	}

	public void removeToggledComponent(AbstractExercise exercise, String componentId) {
		if (toggledComponentsInExercises.containsKey(exercise)) {
			List<String> stages = toggledComponentsInExercises.get(exercise);
			if (stages.contains(componentId)) {
				toggledComponentsInExercises.get(exercise).remove(componentId);
			}
		}
	}

	public boolean isComponentCollapsed(String componentId, AbstractExercise exercise) {
		if (toggledComponentsInExercises.containsKey(exercise)) {
			List<String> stages = toggledComponentsInExercises.get(exercise);
			return !stages.contains(componentId);
		}
		return true;
	}

	public void addPasswordForCourseOffer(final CourseOffer courseOffer, final String password) {
		courseOfferPasswords.put(courseOffer, password);
	}

	@CheckForNull
	public String getPasswordForCourseOffer(final CourseOffer courseOffer) {
		return courseOfferPasswords.get(courseOffer);
	}

	public StatisticsStateHolder getStatisticsStateHolder() {
		return statisticsStateHolder;
	}

}