package de.uni_due.s3.jack3.beans;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.Hibernate;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultMenuItem.Builder;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.MenuElement;
import org.primefaces.model.menu.MenuModel;

import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;

@Named
@ViewScoped
public class PathComponent extends AbstractView implements Serializable {

	private static final long serialVersionUID = 1867440673455267346L;

	@Inject
	private AuthorizationBusiness authorizationBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	private static final String PARAM_LOCATIONID = "location";

	private static final String SEPARATOR = " \\ ";

	////////////////////////////	Result MenuModel	////////////////////////

	public MenuModel getUserSpecificPathAsModel(Folder target, boolean isPartOfTheYouAreHereModel, boolean suppressRoot, User user, boolean isInContentTab) {
		return getUserSpecificPathOfFolderAsModel(target, isInContentTab, isPartOfTheYouAreHereModel, suppressRoot, user);
	}

	public MenuModel getUserSpecificPathAsModel(CourseOffer target, boolean isPartOfTheYouAreHereModel, boolean suppressRoot, User user, boolean isToCourseMainMenu) {
		return getUserSpecificPathOfCourseOfferAsModel(target, isPartOfTheYouAreHereModel, isToCourseMainMenu, suppressRoot,user);
	}

	public MenuModel getUserSpecificPathAsModel(Course target, boolean isPartOfTheYouAreHereModel, boolean suppressRoot, User user) {
		return getUserSpecificPathOfCourseAsModel(target, isPartOfTheYouAreHereModel, suppressRoot, user);
	}

	public MenuModel getUserSpecificPathAsModel(Exercise target, boolean isPartOfTheYouAreHereModel, boolean suppressRoot, User user) {
		return getUserSpecificPathOfExerciseAsModel(target, isPartOfTheYouAreHereModel, suppressRoot, user);
	}

	@Override
	public MenuModel getPathAsModel(Folder target, boolean isPartOfTheYouAreHereModel, boolean suppressRoot, boolean isInContentTab) {
		return getPathOfFolderAsModel(target, isInContentTab, isPartOfTheYouAreHereModel, suppressRoot);
	}

	@Override
	public MenuModel getPathAsModel(CourseOffer target, boolean isPartOfTheYouAreHereModel, boolean suppressRoot, boolean isToCourseMainMenu) {
		return getPathOfCourseOfferAsModel(target, isPartOfTheYouAreHereModel, isToCourseMainMenu, suppressRoot);
	}

	@Override
	public MenuModel getPathAsModel(Course target, boolean isPartOfTheYouAreHereModel, boolean suppressRoot) {
		return getPathOfCourseAsModel(target, isPartOfTheYouAreHereModel, suppressRoot);
	}

	@Override
	public MenuModel getPathAsModel(Exercise target, boolean isPartOfTheYouAreHereModel, boolean suppressRoot) {
		return getPathOfExerciseAsModel(target, isPartOfTheYouAreHereModel, suppressRoot);
	}


	private MenuModel getPathOfFolderAsModel(Folder targetFolder, boolean isInContentTab, boolean isPartOfTheYouAreHereModel,
			boolean suppressRoot) {
		// In case of old revisions, Folders are not initialized, but then we dont need a Breadcrumb anyway. Furthermore
		// FrozenCourses don't have a Folder (set to null)
		if (!Hibernate.isInitialized(targetFolder) || targetFolder == null) {
			return null;
		}

		final List<Folder> folderHierarchy = targetFolder.getBreadcrumb();
		final Map<Folder, Boolean> folderAllowedToReadMap = authorizationBusiness
				.isAllowedToSeeFolders(getCurrentUser(), folderHierarchy);

		MenuModel pathAsModel = new DefaultMenuModel();

		for (final Folder currentFolder : folderHierarchy) {
			addFolderToPath(currentFolder, pathAsModel, isInContentTab, !folderAllowedToReadMap.get(currentFolder),
					suppressRoot);
		}
		addFolderToPath(targetFolder, pathAsModel, isInContentTab,
				!authorizationBusiness.isAllowedToSeeFolders(getCurrentUser(), Arrays.asList(targetFolder)).get(targetFolder) || isPartOfTheYouAreHereModel,
				suppressRoot);

		return pathAsModel;
	}

	private MenuModel getUserSpecificPathOfFolderAsModel(Folder targetFolder, boolean isInContentTab, boolean isPartOfTheYouAreHereModel,
			boolean suppressRoot, User user) {
		// In case of old revisions, Folders are not initialized, but then we dont need a Breadcrumb anyway. Furthermore
		// FrozenCourses don't have a Folder (set to null)
		if (!Hibernate.isInitialized(targetFolder) || targetFolder == null) {
			return null;
		}

		final List<Folder> folderHierarchy = targetFolder.getBreadcrumb();
		final Map<Folder, Boolean> folderAllowedToReadMap = authorizationBusiness
				.isAllowedToSeeFolders(user, folderHierarchy);

		MenuModel pathAsModel = new DefaultMenuModel();

		for (final Folder currentFolder : folderHierarchy) {
			// the folder is displayed in the breadcrumb if:
			// it is the root folder
			// it is a personalfolder
			// the current user has read right on this folder
			if(currentFolder.isRoot() || folderBusiness.isPersonalFolder(currentFolder) || folderAllowedToReadMap.get(currentFolder)) {
				addFolderToPath(currentFolder, pathAsModel, isInContentTab, !folderAllowedToReadMap.get(currentFolder),
						suppressRoot);
			}
		}
		addFolderToPath(targetFolder, pathAsModel, isInContentTab,
				!authorizationBusiness.isAllowedToSeeFolders(getCurrentUser(), Arrays.asList(targetFolder)).get(targetFolder) || isPartOfTheYouAreHereModel,
				suppressRoot);

		return pathAsModel;
	}

	private void addFolderToPath(Folder folder, MenuModel pathAsModel, boolean isInContentTab, boolean isPartOfTheYouAreHereModel,
			boolean suppressRoot) {
		final String name;

		//Two special little Snowflakes to handle first
		if(folderBusiness.getContentRoot().getId() == folder.getId() || folderBusiness.getPresentationRoot().getId() == folder.getId()) {
			if(suppressRoot) {
				return;
			} else {
				name = folder.getName();
				Builder item = DefaultMenuItem.builder().value(name);
				if (folder instanceof ContentFolder) {
					item.outcome(viewId.getMyWorkspace().toOutcome());
				} else if (folder instanceof PresentationFolder) {
					item.outcome(viewId.getAvailableCourses().toOutcome());
				} else {
					item.outcome(viewId.getAvailableCourses().toOutcome());
				}
				pathAsModel.getElements().add(item.build());
				return;
			}

		}

		if (folderBusiness.isPersonalFolder(folder)) {
			// Replace the name of the personal folder
			name = folderBusiness.getOwnerOfContentFolder((ContentFolder) folder).getLoginName();
		} else {
			name = folder.getName();
		}
		Builder item = DefaultMenuItem.builder().value(name);
		if (!isInContentTab) {
			item.outcome(viewId.getAvailableCourses().withParam(PARAM_LOCATIONID, folder.getId()).toOutcome());
			if (isPartOfTheYouAreHereModel) {
				item.disabled(true);
			}
		} else {
			item.outcome(viewId.getMyWorkspace().withParam(PARAM_LOCATIONID, folder.getId()).toOutcome());
			if (isPartOfTheYouAreHereModel) {
				item.disabled(true);
			}
		}
		pathAsModel.getElements().add(item.build());
	}

	private MenuModel getPathOfCourseOfferAsModel(CourseOffer courseOffer, boolean isPartOfTheYouAreHereModel,
			boolean isToCourseMainMenu, boolean suppressRoot) {
		MenuModel pathAsModel = getPathOfFolderAsModel(courseOffer.getFolder(), false, false, suppressRoot);
		if (isToCourseMainMenu) {
			addCourseOfferToMenuModelToCourseMenu(courseOffer, pathAsModel, isPartOfTheYouAreHereModel);
		} else {
			addCourseOfferToMenuModelToEdit(courseOffer, pathAsModel, isPartOfTheYouAreHereModel);
		}
		return pathAsModel;
	}

	private MenuModel getUserSpecificPathOfCourseOfferAsModel(CourseOffer courseOffer, boolean isPartOfTheYouAreHereModel,
			boolean isToCourseMainMenu, boolean suppressRoot, User user) {
		MenuModel pathAsModel = getUserSpecificPathOfFolderAsModel(courseOffer.getFolder(), false, false, suppressRoot,user);
		if (isToCourseMainMenu) {
			addCourseOfferToMenuModelToCourseMenu(courseOffer, pathAsModel, isPartOfTheYouAreHereModel);
		} else {
			addCourseOfferToMenuModelToEdit(courseOffer, pathAsModel, isPartOfTheYouAreHereModel);
		}
		return pathAsModel;
	}

	private void addCourseOfferToMenuModelToEdit(CourseOffer courseOffer, MenuModel pathAsModel,
			boolean isPartOfTheYouAreHereModel) {
		final String name = courseOffer.getName();
		Builder item = DefaultMenuItem.builder().value(name);
		item.outcome(viewId.getCourseOfferEditor().withParam(courseOffer).withParam("redirect", false).toOutcome());
		if (!authorizationBusiness.isAllowedToReadFromFolder(getCurrentUser(), courseOffer.getFolder())
				|| isPartOfTheYouAreHereModel) {
			item.disabled(true);
		}
		pathAsModel.getElements().add(item.build());
	}

	private void addCourseOfferToMenuModelToCourseMenu(CourseOffer courseOffer, MenuModel pathAsModel,
			boolean isPartOfTheYouAreHereModel) {
		final String name = courseOffer.getName();
		Builder item = DefaultMenuItem.builder().value(name);
		item.outcome(viewId.getCourseMainMenu().withParam(courseOffer).withParam("redirect", false).toOutcome());
		if (!authorizationBusiness.isAllowedToReadFromFolder(getCurrentUser(), courseOffer.getFolder())
				|| isPartOfTheYouAreHereModel) {
			item.disabled(true);
		}
		pathAsModel.getElements().add(item.build());
	}

	private MenuModel getPathOfCourseAsModel(Course course, boolean isPartOfTheYouAreHereModel, boolean suppressRoot) {
		MenuModel pathAsModel = getPathOfFolderAsModel(course.getFolder(), true, false, suppressRoot);
		addCourseToMenuModel(course, pathAsModel, isPartOfTheYouAreHereModel);
		return pathAsModel;
	}

	private MenuModel getUserSpecificPathOfCourseAsModel (Course course, boolean isPartOfTheYouAreHereModel, boolean suppressRoot, User user) {
		MenuModel pathAsModel = getUserSpecificPathOfFolderAsModel(course.getFolder(), true, false, suppressRoot, user);
		addCourseToMenuModel(course, pathAsModel, isPartOfTheYouAreHereModel);
		return pathAsModel;
	}

	private void addCourseToMenuModel(Course course, MenuModel pathAsModel, boolean isPartOfTheYouAreHereModel) {
		final String name = course.getName();
		Builder item = DefaultMenuItem.builder().value(name);
		item.outcome(viewId.getCourseEditor().withParam(course).toString());
		if (!authorizationBusiness.isAllowedToReadFromFolder(getCurrentUser(), course.getFolder()) || isPartOfTheYouAreHereModel) {
			item.disabled(true);
		}
		pathAsModel.getElements().add(item.build());
	}

	private MenuModel getPathOfExerciseAsModel(Exercise exercise, boolean isPartOfTheYouAreHereModel, boolean suppressRoot) {
		MenuModel pathAsModel = getPathOfFolderAsModel(exercise.getFolder(), true, false, suppressRoot);
		addExerciseToMenuModel(exercise, pathAsModel, isPartOfTheYouAreHereModel);
		return pathAsModel;
	}

	private MenuModel getUserSpecificPathOfExerciseAsModel (Exercise exercise, boolean isPartOfTheYouAreHereModel, boolean suppressRoot, User user) {
		MenuModel pathAsModel = getUserSpecificPathOfFolderAsModel(exercise.getFolder(), true, false, suppressRoot, user);
		addExerciseToMenuModel(exercise, pathAsModel, isPartOfTheYouAreHereModel);
		return pathAsModel;
	}

	private void addExerciseToMenuModel(Exercise exercise, MenuModel pathAsModel, boolean isPartOfTheYouAreHereModel) {
		final String name = exercise.getName();
		Builder item = DefaultMenuItem.builder().value(name);
		item.outcome(viewId.getExerciseEditor().withParam(exercise).toOutcome());
		if (!authorizationBusiness.isAllowedToReadFromFolder(getCurrentUser(), exercise.getFolder()) || isPartOfTheYouAreHereModel) {
			item.disabled(true);
		}
		pathAsModel.getElements().add(item.build());
	}

	@Override
	public MenuModel addDefaultMenuModelToMenuModel(MenuModel oldModel, DefaultMenuItem item) {
		final MenuModel newModel = new DefaultMenuModel();
		for (final MenuElement me : oldModel.getElements()) {
			final DefaultMenuItem existingItem = (DefaultMenuItem) me;
			if (existingItem.getOutcome() == null || !existingItem.getOutcome().equals(item.getOutcome())) {
				newModel.getElements().add(existingItem);
			} else {
				break;
			}
		}

		//it is always possible to visit the previous menuItem
		((DefaultMenuItem) newModel.getElements().get(newModel.getElements().size()-1)).setDisabled(false);

		item.setDisabled(true);
		newModel.getElements().add(item);
		return newModel;
	}

	////////////////////////////	Result String	////////////////////////////

	public String getPathOfFolderAsString(Folder targetFolder, boolean fromEnvers) {
		StringBuilder path = new StringBuilder();

		// TODO Research, if we still need the lazy data - look also into "Folder.getBreadcrumb()" method.
		if(targetFolder instanceof PresentationFolder) {
			targetFolder = folderBusiness.getPresentationFolderWithLazyData(targetFolder);
		}else {
			//targetFolder is a ContentFolder. Check if we have to load it from envers
			if(fromEnvers) {
				targetFolder = folderBusiness.getContentFolderWithLazyDataByIdFromEnvers(targetFolder.getId()).orElseThrow();
			}else {
				targetFolder = folderBusiness.getContentFolderWithLazyData(targetFolder);
			}
		}

		final List<Folder> folderHierarchy = targetFolder.getBreadcrumb();

		for (final Folder currentFolder : folderHierarchy) {
			path.append(getDisplayedNameOfFolderAsString(currentFolder, fromEnvers));
			path.append(SEPARATOR);
		}
		path.append(getDisplayedNameOfFolderAsString(targetFolder, fromEnvers));
		path.append(SEPARATOR);

		return path.toString();
	}

	public String getUserSpecificPathOfFolderAsString(Folder targetFolder, User user, boolean fromEnvers) {
		StringBuilder path = new StringBuilder();

		// TODO Research, if we still need the lazy data - look also into "Folder.getBreadcrumb()" method.
		if(targetFolder instanceof PresentationFolder) {
			targetFolder = folderBusiness.getPresentationFolderWithLazyData(targetFolder);
		}else {
			//targetFolder is a ContentFolder. Check if we have to load it from envers
			if(fromEnvers) {
				targetFolder = folderBusiness.getContentFolderWithLazyDataByIdFromEnvers(targetFolder.getId()).orElseThrow();
			}else {
				targetFolder = folderBusiness.getContentFolderWithLazyData(targetFolder);
			}
		}

		final List<Folder> folderHierarchy = targetFolder.getBreadcrumb();

		for (final Folder currentFolder : folderHierarchy) {
			if(currentFolder.isTransient() || authorizationBusiness.isAllowedToReadFromFolder(user, currentFolder) || folderBusiness.isPersonalFolder(currentFolder)) {
				path.append(getDisplayedNameOfFolderAsString(currentFolder, fromEnvers));
				path.append(SEPARATOR);
			}
		}
		path.append(getDisplayedNameOfFolderAsString(targetFolder, fromEnvers));
		path.append(SEPARATOR);
		return path.toString();
	}

	private String getDisplayedNameOfFolderAsString(final Folder targetFolder, final boolean fromEnvers) {
		//suppress RootFolders
		if(folderBusiness.getContentRoot().getId() == targetFolder.getId() || folderBusiness.getPresentationRoot().getId() == targetFolder.getId()) {
			return "";
		}

		// TODO Research, if we still need the lazy data - look also into "Folder.getBreadcrumb()" method.
		if(fromEnvers) {
			if (!targetFolder.isTransient() && folderBusiness.isPersonalFolderFromEnvers(targetFolder)) {
				//targetFolder is a personal folder
				return folderBusiness.getOwnerOfContentFolderFromEnvers(targetFolder).getLoginName();
			}else {
				return targetFolder.getName();
			}
		}else {
			if (!targetFolder.isTransient() && folderBusiness.isPersonalFolder(targetFolder)) {
				//targetFolder is a personal folder
				return folderBusiness.getOwnerOfContentFolder((ContentFolder) targetFolder).getLoginName();
			}else {
				return targetFolder.getName();
			}
		}
	}

	public String getPathOfCourseOfferAsString(final CourseOffer targetCourseOffer) {
		String path = "";
		path += getPathOfFolderAsString(targetCourseOffer.getFolder(),false);
		path += targetCourseOffer.getName();
		return path;
	}

	public String getPathOfCourseAsString(final Course targetCourse) {
		String path = "";
		path += getPathOfFolderAsString(targetCourse.getFolder(),targetCourse.isFromEnvers());
		path += targetCourse.getName();
		return path;
	}

	public String getPathOfExerciseAsString(final Exercise targetExercise) {
		String path = "";
		if (!Hibernate.isInitialized(targetExercise.getFolder())) {
			targetExercise.setFolder(folderBusiness.getContentFolderFor(targetExercise));
		}

		path += getPathOfFolderAsString(targetExercise.getFolder(),targetExercise.isFromEnvers());
		path += targetExercise.getName();
		return path;
	}

}
