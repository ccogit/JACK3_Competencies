package de.uni_due.s3.jack3.business;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.business.exceptions.AuthorizationException;
import de.uni_due.s3.jack3.business.helpers.EFolderChildType;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;
import de.uni_due.s3.jack3.exceptions.PreconditionException;
import de.uni_due.s3.jack3.services.BaseService;
import de.uni_due.s3.jack3.services.FolderService;
import de.uni_due.s3.jack3.services.UserService;
import de.uni_due.s3.jack3.utils.JackStringUtils;


@RequestScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class FolderBusiness extends AbstractBusiness {

	@Inject
	private FolderService folderService;

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private BaseService baseService;

	@Inject
	private AuthorizationBusiness authorizationBusiness;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private UserService usersevice;

	@Inject
	private CourseBusiness courseBusiness;

	public PresentationFolder createPresentationFolder(String name, PresentationFolder parentFolder) {

		Objects.requireNonNull(parentFolder, "You must specifiy a parent folder.");
		if (JackStringUtils.isBlank(name)) {
			throw new IllegalArgumentException("You must specify a non-empty name.");
		}
		parentFolder = folderService.getPresentationFolderWithLazyData(parentFolder);
		PresentationFolder newFolder = new PresentationFolder(name.strip());

		parentFolder.addChildFolder(newFolder);
		newFolder.addInheritedUserRights(authorizationBusiness.getAllManagingUsers(parentFolder));
		newFolder.addInheritedUserGroupRights(authorizationBusiness.getAllManagingUserGroups(parentFolder));

		folderService.mergePresentationFolder(parentFolder);
		folderService.persistFolder(newFolder);
		return newFolder;
	}

	public PresentationFolder createTopLevelPresentationFolder(final User acting, final String name)
			throws ActionNotAllowedException {
		Objects.requireNonNull(acting, "Acting user must be given.");
		requireIdentifier(name, "Folder name must not be empty.");

		if (!authorizationBusiness.hasAdminRights(acting)) {
			throw new ActionNotAllowedException(
					"Only administrators are allowed to add top-level Presentation Folders");
		}

		PresentationFolder parent = folderService.getPresentationRoot();
		PresentationFolder newFolder = new PresentationFolder(name.strip());

		parent.addChildFolder(newFolder);
		newFolder.addUserRight(acting, AccessRight.getFull());

		folderService.mergePresentationFolder(parent);
		folderService.persistFolder(newFolder);
		getLogger().infof("Top-level Presentation Folder \"%s\" (id=%s) was created by %s.", name, newFolder.getId(),
				acting.getLoginName());
		return newFolder;
	}

	public void deleteTopLevelPresentationFolder(final User acting, final PresentationFolder folder)
			throws ActionNotAllowedException {
		Objects.requireNonNull(acting, "Acting user must be given.");
		Objects.requireNonNull(folder, "Folder to delete must be given.");

		if (!authorizationBusiness.hasAdminRights(acting)) {
			throw new ActionNotAllowedException(
					"Only administrators are allowed to delete top-level Presentation Folders");
		}
		if (!authorizationBusiness.isAllowedToEditFolder(acting, folder)) {
			throw new AuthorizationException(AuthorizationException.EType.INSUFFICIENT_RIGHT);
		}

		deletePresentationfolder(folder);
		getLogger().infof("Top-level Presentation Folder \"%s\" (id=%s) was deleted by %s.", folder.getName(),
				folder.getId(), acting.getLoginName());
	}

	public void deleteFolder(User user, Folder folder) throws ActionNotAllowedException {
		if (folder instanceof ContentFolder) {
			if (!authorizationBusiness.isAllowedToEditFolder(user, folder)) {
				throw new AuthorizationException(AuthorizationException.EType.INSUFFICIENT_RIGHT);
			}
			deleteContentFolder(folder, user);
		} else if (folder instanceof PresentationFolder) {
			deletePresentationfolder(folder);
		} else {
			throw new UnsupportedOperationException("Folder Type not (yet?) supported! " + folder);
		}
	}

	private void deletePresentationfolder(Folder folder) {
		PresentationFolder presentationFolder = getPresentationFolderById(folder.getId())
				.orElseThrow(() -> new PreconditionException("The Folder doesn't exist anymore: " + folder));

		// Is the Folder empty? If not it can't be deleted
		if (presentationFolder.getChildrenCourseOffer().isEmpty() && presentationFolder.getChildrenFolder().isEmpty()) {
			folderService.deleteFolder(folder);
		} else {
			throw new PreconditionException(
					"The Folder is not empty and therefore can't be deleted: " + presentationFolder);
		}
	}

	/**
	 * Deletes the content folder. The folder must be empty or contain only deletable data.
	 * 
	 * @param folder
	 *            which should be deleted
	 * @param user
	 *            which deletes the folder
	 * @throws ActionNotAllowedException
	 *             if the deletable folder is not empty and contains data which can't be deleted
	 */
	private void deleteContentFolder(Folder folder, User user) throws ActionNotAllowedException {
		ContentFolder contentFolder = getContentFolderById(folder.getId())
				.orElseThrow(() -> new PreconditionException("The Folder doesn't exist anymore: " + folder));

		// Is the Folder empty or all contained data deletable ? If not it can't be deleted
		if (this.isContentFolderDeletable(contentFolder, user)) {
			this.deleteContentFolderWithDeletableData(contentFolder, user);
		} else {
			throw new PreconditionException(
					"The Folder contains data, which is not deletable, and therefore can't be deleted: "
							+ contentFolder);
		}
	}

	/**
	 * Deletes the content folder and all containing data(child exercises/courses/folder) recursively.
	 * 
	 * @param folder
	 *            which is empty or contains only deletable data
	 * @param user
	 *            which wants to delete the folder
	 * @throws ActionNotAllowedException
	 *             if a exercise or course can't be deleted
	 */
	private void deleteContentFolderWithDeletableData(ContentFolder folder, User user)
			throws ActionNotAllowedException {
		folder = folderService.getContentFolderWithLazyData(folder);
		//delete exercises
		for (AbstractExercise exercise : folder.getChildrenExercises()) {
			exerciseBusiness.deleteExercise((Exercise) exercise, user);
		}

		//delete courses
		for (AbstractCourse course : folder.getChildrenCourses()) {
			courseBusiness.deleteCourse((Course) course, user);
		}

		//delete folders
		for (Folder childfolder : folder.getChildrenFolder()) {
			this.deleteContentFolderWithDeletableData((ContentFolder) childfolder, user);
		}
		//delete folder
		folderService.deleteFolder(folder);

	}

	public Optional<Folder> getFolderWithManagingRights(Folder folder) {
		return folderService.getFolderWithManagingRights(folder, Folder.class);
	}

	//REVIEW SW: hier könnte man ggf. noch optimieren
	public List<Folder> getFoldersWithManagingRights(List<Folder> folderList) {
		List<Folder> listOfOptionalFolders = new ArrayList<>();
		folderList.forEach((folder) -> listOfOptionalFolders.add(getFolderWithManagingRights(folder)
				.orElseThrow(() -> new PreconditionException("The Folder doesn't exist anymore: " + folder))));
		return listOfOptionalFolders;
	}

	public Folder updateFolder(Folder folder) {
		return folderService.mergeFolder(folder);
	}

	/**
	 * Removes all inherited rights on the given folder and sets new inherited
	 * rights according to rights granted on the parent folder.
	 *
	 * @param folder
	 */
	public void resetFolderRights(Folder folder) {
		folder = getFolderWithManagingRights(folder).orElseThrow(AssertionError::new);

		final Folder parentFolder = getFolderWithManagingRights(folder.getParentFolder())
				.orElseThrow(AssertionError::new);

		folder.deleteAllInheritedRights();

		folder.addInheritedUserRights(authorizationBusiness.getAllManagingUsers(parentFolder));
		folder.addInheritedUserGroupRights(authorizationBusiness.getAllManagingUserGroups(parentFolder));

		folder = folderService.mergeFolder(folder);

		for (Folder f : folder.getChildrenFolder()) {
			resetFolderRights(f);
		}
	}

	public List<PresentationFolder> getAllPresentationFoldersForUser(User user) {
		return folderService.getAllPresentationFoldersForUser(user);
	}

	public List<PresentationFolder> getAllPresentationFoldersForUserGroup(UserGroup userGroup) {
		return folderService.getAllPresentationFoldersForUserGroup(userGroup);
	}

	public PresentationFolder getPresentationRoot() {
		return folderService.getPresentationRoot();
	}

	/**
	 * Grants or removes user rights for a particular folder. Performs no change if newRights is not different from the
	 * current rights. If changes are performed, they are also propagated to all child folders as inherited rights.
	 *
	 * All parameters must <strong>not</strong> be null. This method does <strong>not</strong> perform a right-check.
	 */
	public void updateFolderRightsForUser(Folder folder, final User user, final AccessRight newRights) {
		Objects.requireNonNull(folder);
		Objects.requireNonNull(user);
		Objects.requireNonNull(newRights);

		folder = getFolderWithManagingRights(folder).orElseThrow(NoSuchJackEntityException::new);
		final AccessRight oldRights = folder.getManagingUsers().getOrDefault(user, AccessRight.getNone());

		// Check if we have no changes
		if (Objects.equals(oldRights, newRights)) {
			return;
		}

		// ... otherwise update this folder ...
		// This also handles NONE rights correctly
		folder.addUserRight(user, newRights);
		folder = updateFolder(folder);

		final AccessRight totalRights = folder.getInheritedManagingUsers().getOrDefault(user, AccessRight.getNone())
				.add(newRights);

		for (final Folder child : folderService.getChildrenFolder(folder)) {
			propagateFolderRightsChangeForUser(child, user, totalRights);
		}
	}

	/**
	 * Grants all users with edit rights full access to their personal folder.
	 *
	 * @return The return value indicates for how many users the rights have been reset.
	 */
	public int propagateFullRightsForAllPersonalFolders() {
		int numberOfChanges = 0;
		final AccessRight fullRight = AccessRight.getFull();
		for (final User user : userBusiness.getAllUsersWithEditRights()) {
			Folder folder = getFolderWithManagingRights(user.getPersonalFolder())
					.orElseThrow(NoSuchJackEntityException::new);

			final AccessRight actualRight = folder.getManagingUsers().getOrDefault(user, AccessRight.getNone());
			if (actualRight.equals(fullRight))
				continue;

			// The actual rights of the user on the personal folder must be set to FULL
			// This is similar to the method above
			folder.addUserRight(user, fullRight);
			folder = updateFolder(folder);
			for (final Folder child : folderService.getChildrenFolder(folder)) {
				propagateFolderRightsChangeForUser(child, user, fullRight);
			}
			numberOfChanges++;
		}

		return numberOfChanges;
	}

	/**
	 * Grants or removes user rights for a particular folder. Performs no change if newRights is not different from the
	 * current rights. If changes are performed, they are also propagated to all child folders as inherited rights.
	 *
	 * All parameters must <strong>not</strong> be null. This method does <strong>not</strong> perform a right-check.
	 */
	public void updateFolderRightsForUserGroup(Folder folder, final UserGroup userGroup, final AccessRight newRights) {
		Objects.requireNonNull(folder);
		Objects.requireNonNull(userGroup);
		Objects.requireNonNull(newRights);

		folder = getFolderWithManagingRights(folder).orElseThrow(NoSuchJackEntityException::new);
		final AccessRight oldRights = folder.getManagingUserGroups().getOrDefault(userGroup, AccessRight.getNone());

		// Check if we have no changes
		if (Objects.equals(oldRights, newRights)) {
			return;
		}

		// ... otherwise update this folder ...
		// This also handles NONE rights correctly
		folder.addUserGroupRight(userGroup, newRights);
		folder = updateFolder(folder);

		final AccessRight totalRights = folder.getInheritedManagingUserGroups()
				.getOrDefault(userGroup, AccessRight.getNone()).add(newRights);

		for (final Folder child : folderService.getChildrenFolder(folder)) {
			propagateFolderRightsChangeForUserGroup(child, userGroup, totalRights);
		}
	}

	/**
	 * Sets the given rights as inherited rights for the given user to the given
	 * folder and all of its children.
	 *
	 * @param folder
	 * @param user
	 * @param rights
	 */
	private void propagateFolderRightsChangeForUser(Folder folder, User user, AccessRight rights) {
		if (user == null) {
			throw new NullPointerException("User group must not be null.");
		}

		folder = getFolderWithManagingRights(folder).orElseThrow(AssertionError::new);

		folder.addInheritedUserRight(user, rights);

		folder = updateFolder(folder);

		for (final Folder childFolder : folderService.getChildrenFolder(folder)) {
			propagateFolderRightsChangeForUser(childFolder, user, rights);
		}
	}

	/**
	 * Sets the given rights as inherited rights for the given user group to the
	 * given folder and all of its children.
	 *
	 * @param folder
	 * @param userGroup
	 * @param rights
	 */
	private void propagateFolderRightsChangeForUserGroup(Folder folder, UserGroup userGroup, AccessRight rights) {
		if (userGroup == null) {
			throw new NullPointerException("User group must not be null.");
		}

		folder = getFolderWithManagingRights(folder).orElseThrow(AssertionError::new);

		folder.addInheritedUserGroupRight(userGroup, rights);

		folder = updateFolder(folder);

		for (final Folder childFolder : folderService.getChildrenFolder(folder)) {
			propagateFolderRightsChangeForUserGroup(childFolder, userGroup, rights);
		}
	}

	/**
	 * Revokes all rights granted to the given users on content folders outside
	 * the personal folder tree of that user.
	 */
	public void removeUserRightsOnNonPersonalContentFolders(User user) {
		final List<ContentFolder> folders = folderService.getAllContentFoldersForUser(user);
		for (Folder folder : folders) {
			if (!folder.isChildOf(user.getPersonalFolder())) {
				folder = folderService.getFolderWithManagingRights(folder).orElseThrow(AssertionError::new);
				folder.deleteAllUserRights(user);
				folderService.mergeFolder(folder);
			}
		}
	}

	/**
	 * Revokes all rights granted to the given user on presentation folders.
	 */
	public void removeUserRightsOnPresentationFolders(User user) {
		final List<PresentationFolder> folders = folderService.getAllPresentationFoldersForUser(user);
		for (Folder folder : folders) {
			folder = folderService.getFolderWithManagingRights(folder).orElseThrow(AssertionError::new);
			folder.deleteAllUserRights(user);
			folderService.mergeFolder(folder);
		}
	}

	public Map<String, ContentFolder> createContentFolderHierarchy(String folderNameHierarchy,
			ContentFolder parentFolder, User currentUser) throws ActionNotAllowedException {
		Map<String, ContentFolder> result = new HashMap<>();

		String[] folderHierarchy = folderNameHierarchy.split("\\\\");
		StringJoiner folderHierarchyStringJoiner = new StringJoiner("\\");
		ContentFolder currentParent = getContentFolderWithLazyData(parentFolder);
		for (String folderInHierarchy : folderHierarchy) {
			folderHierarchyStringJoiner.add(folderInHierarchy);
			if (JackStringUtils.isNotBlank(folderNameHierarchy)) {
				Optional<Folder> someFolder = currentParent.getChildrenFolder().stream() //
						.filter(f -> f.getName().equals(folderInHierarchy)) //
						.findFirst();
				if (someFolder.isPresent()) {
					currentParent = (ContentFolder) someFolder.get();
				} else {
					currentParent = createContentFolder(currentUser, folderInHierarchy, currentParent);
				}
				result.put(folderHierarchyStringJoiner.toString(), currentParent);
			}
		}
		return result;
	}

	public ContentFolder createContentFolder(User currentUser, String name, Folder parentFolder)
			throws ActionNotAllowedException {

		Objects.requireNonNull(parentFolder, "You must specify a parent folder.");
		if (JackStringUtils.isBlank(name)) {
			throw new IllegalArgumentException("You must specify a non-empty name");
		}
		ContentFolder folder = new ContentFolder(name.strip());
		if (!authorizationBusiness.isAllowedToCreateFolder(currentUser, parentFolder)) {
			throw new AuthorizationException(AuthorizationException.EType.INSUFFICIENT_RIGHT);
		}

		parentFolder = folderService.getContentFolderWithLazyData(parentFolder);
		parentFolder.addChildFolder(folder);
		folder.addInheritedUserRights(authorizationBusiness.getAllManagingUsers(parentFolder));
		folder.addInheritedUserGroupRights(authorizationBusiness.getAllManagingUserGroups(parentFolder));
		folderService.persistFolder(folder);

		folderService.mergeFolder(parentFolder);
		return folder;
	}

	/**
	 * Creates a personal folder for the specified user.
	 *
	 * @param owner
	 * @return The user's new personal folder
	 */
	public ContentFolder createPersonalFolder(User owner) {
		Objects.requireNonNull(owner, "You must specify an owner.");

		if (!owner.isHasEditRights()) {
			throw new IllegalArgumentException("Users without edit rights must not have a personal folder.");
		}

		if (owner.getPersonalFolder() != null) {
			throw new IllegalArgumentException("The user already has a personal folder.");
		}

		ContentFolder folder = new ContentFolder(ContentFolder.PERSONAL_FOLDER_NAME);
		folder.addUserRight(owner, AccessRight.getFull());
		ContentFolder parent = folderService.getContentFolderWithLazyData(folderService.getContentRoot());
		parent.addChildFolder(folder);

		folderService.persistFolder(folder);
		folderService.mergeContentFolder(parent);
		return folder;
	}

	public List<ContentFolder> getAllContentFoldersForUser(User user) {
		return folderService.getAllContentFoldersForUser(user);
	}

	public List<ContentFolder> getAllContentFoldersForUserGroup(UserGroup userGroup) {
		return folderService.getAllContentFoldersForUserGroup(userGroup);
	}

	public Map<ContentFolder, AccessRight> getContentFoldersWithAtLeastReadRightForUser(User user) {
		//Folders with access through userGroups have to be handled separately
		//Get all Folders for User with direct rights
		List<ContentFolder> contentFolderList = getAllContentFoldersForUser(user);
		Map<ContentFolder, AccessRight> contentFolderAccessRightMap = new HashMap<>();

		//Add all Folders for User with userGroups
		//procedure is necessary, because recursive sql requests are currently not available
		final List<UserGroup> groups = userBusiness.getUserGroupsForUser(user);
		for (final UserGroup userGroup : groups) {
			contentFolderList.addAll(getAllContentFoldersForUserGroup(userGroup));
		}

		contentFolderList = contentFolderList.stream().distinct().collect(Collectors.toList());

		List<Folder> folderList = contentFolderList.stream().map((contentFolder) -> (Folder) contentFolder)
				.collect(Collectors.toList());
		Map<Folder, AccessRight> folderRightsMap = authorizationBusiness.getMaximumRightForUser(user, folderList,
				groups);
		contentFolderList.forEach(
				(contentFolder) -> contentFolderAccessRightMap.put(contentFolder, folderRightsMap.get(contentFolder)));

		return contentFolderAccessRightMap;
	}

	public ContentFolder getContentRoot() {
		return folderService.getContentFolderWithLazyData(folderService.getContentRoot());
	}

	public List<PresentationFolder> getAllPresentationFolders() {
		return folderService.getAllPresentationFolders();
	}

	public Optional<PresentationFolder> getPresentationFolderById(long id) {
		return folderService.getPresentationFolderById(id);
	}

	public Optional<ContentFolder> getContentFolderById(long id) {
		return folderService.getContentFolderById(id);
	}

	public Optional<ContentFolder> getContentFolderWithLazyDataByIdFromEnvers(long id) {
		return folderService.getContentFolderWithLazyDataByIdFromEnvers(id);
	}

	/**
	 * Moves a content folder to a new parent folder. This updates the folder pointer and triggers an update.
	 *
	 * @throws ActionNotAllowedException
	 *             If the user is not allowed to move the folder.
	 */
	public void moveContentFolder(User acting, Folder folderToMove, Folder newParentFolder)
			throws ActionNotAllowedException {
		final Folder oldParent = folderService.getContentFolderWithLazyData(folderToMove.getParentFolder());
		newParentFolder = folderService.getContentFolderWithLazyData(newParentFolder);
		moveFolder(acting, folderToMove, oldParent, newParentFolder);
	}

	/**
	 * Moves a presentation folder to a new parent folder. This updates the folder pointer and triggers an update.
	 *
	 * @throws ActionNotAllowedException
	 *             If the user is not allowed to move the folder.
	 */
	public void movePresentationFolder(User acting, Folder folderToMove, Folder newParentFolder)
			throws ActionNotAllowedException {
		final Folder oldParent = folderService.getPresentationFolderWithLazyData(folderToMove.getParentFolder());
		final Folder newParent = folderService.getPresentationFolderWithLazyData(newParentFolder);
		moveFolder(acting, folderToMove, oldParent, newParent);
	}

	private void moveFolder(User acting, Folder folderToMove, Folder oldParent, Folder newParent)
			throws ActionNotAllowedException {
		authorizationBusiness.ensureIsAllowedToMoveFolder(acting, folderToMove, newParent);

		// REVIEW lg - To ensure that the rights are updated, it would be better to call "resetFolderRights" here
		//             instead of letting the caller do the cal

		oldParent.removeChildFolder(folderToMove);
		newParent.addChildFolder(folderToMove);
		folderService.mergeFolder(oldParent);
		folderService.mergeFolder(newParent);
		folderService.mergeFolder(folderToMove);
	}

	public ContentFolder getContentFolderWithLazyData(Folder folder) {
		return folderService.getContentFolderWithLazyData(folder);
	}

	public PresentationFolder getPresentationFolderWithLazyData(Folder folder) {
		return folderService.getPresentationFolderWithLazyData(folder);
	}

	/**
	 * If courses in the passed folder are linked, unlink the courses. Otherwise, link the courses.
	 *
	 * @return the new state of linked courses property
	 * @throws ActionNotAllowedException
	 *             If one of the parent folders is already linked.
	 */
	public boolean switchLinkedCourses(PresentationFolder folder) throws ActionNotAllowedException {
		folder = getPresentationFolderById(folder.getId()).orElseThrow(NoSuchJackEntityException::new);
		if (hasInheritedLinkedCourses(folder)) {
			// The user wants to unlink, but the property is inherited from a parent folder
			throw new ActionNotAllowedException();
		}

		boolean newState = !folder.isContainsLinkedCourses();
		folder.setContainsLinkedCourses(newState);
		folderService.mergePresentationFolder(folder);
		return newState;
	}

	/**
	 * Returns the highest parent folder of the passed folder that has the
	 * {@link PresentationFolder#containsLinkedCourses} property set. If no folder has this property, an empty Optional
	 * is returned.
	 *
	 * @see #hasLinkedCourses(PresentationFolder)
	 */
	public Optional<PresentationFolder> getHighestLinkedCourseFolder(final PresentationFolder folder) {
		final List<Folder> foldersToCheck = new ArrayList<>(folder.getBreadcrumb());
		foldersToCheck.add(folder);
		return foldersToCheck.stream().map(PresentationFolder.class::cast) // Safe, because we are in a PresentationFolder hierarchy
				.filter(PresentationFolder::isContainsLinkedCourses).findFirst();
	}

	/**
	 * Returns {@code true} if either the passed folder contains linked courses or one of the parent folders has this
	 * setting.
	 *
	 * @see #hasInheritedLinkedCourses(PresentationFolder)
	 */
	public boolean hasLinkedCourses(PresentationFolder folder) {
		final List<Folder> foldersToCheck = new ArrayList<>(folder.getBreadcrumb());
		foldersToCheck.add(folder);
		return foldersToCheck.stream().map(PresentationFolder.class::cast) // Safe, because we are in a PresentationFolder hierarchy
				.anyMatch(PresentationFolder::isContainsLinkedCourses);
	}

	/**
	 * Returns {@code true} only if one of the parent folders has linked courses set.
	 *
	 * @see #hasLinkedCourses(PresentationFolder)
	 */
	public boolean hasInheritedLinkedCourses(PresentationFolder folder) {
		final List<Folder> foldersToCheck = new ArrayList<>(folder.getBreadcrumb());
		return foldersToCheck.stream().map(PresentationFolder.class::cast) // Safe, because we are in a PresentationFolder hierarchy
				.anyMatch(PresentationFolder::isContainsLinkedCourses);
	}

	/**
	 * Returns all direct and indirect child folders.
	 */
	public List<ContentFolder> getAllChildContentFolders(ContentFolder root, boolean includeRoot) {
		final List<ContentFolder> directChildren = folderService.getChildrenContentFolder(root);
		final List<ContentFolder> children = new LinkedList<>(directChildren);

		for (ContentFolder directChild : directChildren) {
			children.addAll(getAllChildContentFolders(directChild, false));
		}
		if (includeRoot)
			children.add(root);
		return children;
	}

	/**
	 * Returns all direct and indirect child folders.
	 */
	public List<PresentationFolder> getAllChildPresentationFolders(PresentationFolder root, boolean includeRoot) {
		final List<PresentationFolder> directChildren = folderService.getChildrenPresentationFolder(root);
		final List<PresentationFolder> children = new LinkedList<>(directChildren);

		for (PresentationFolder directChild : directChildren) {
			children.addAll(getAllChildPresentationFolders(directChild, false));
		}
		if (includeRoot)
			children.add(root);
		return children;
	}

	/**
	 * Returns all direct and indirect child folders.
	 */
	public List<Folder> getAllChildFolders(Folder root, boolean includeRoot) {
		final List<Folder> directChildren = folderService.getChildrenFolder(root);
		final List<Folder> children = new LinkedList<>(directChildren);

		for (Folder directChild : directChildren) {
			children.addAll(getAllChildFolders(directChild, false));
		}
		if (includeRoot)
			children.add(root);
		return children;
	}

	/**
	 * Lookups the owner of a given {@link ContentFolder}.
	 *
	 * @param folder
	 *            A {@link ContentFolder}. The folder must <strong>not</strong> be the root folder!
	 * @return The user who owns the personal folder that contains the folder. The return-value is not nullable.
	 */
	@Nonnull
	public User getOwnerOfContentFolder(final ContentFolder folder) {
		Objects.requireNonNull(folder, "Folder must exist!");
		Objects.requireNonNull(folder.getParentFolder(), "Folder must not be the root folder!");

		ContentFolder tmpParent = getContentFolderById(folder.getId()).orElseThrow(NoSuchJackEntityException::new);
		while (tmpParent.getParentFolder().getParentFolder() != null) {
			tmpParent = (ContentFolder) tmpParent.getParentFolder();
		}
		// "folder" is now a personal folder of a user (Personal folders are one level below the root folder)
		return userBusiness.getUserOwningThisFolder(tmpParent).orElseThrow(() -> new IllegalStateException(folder
				+ " has no owning user. All content folders should have a user owning the parent personal folder!"));
	}

	/**
	 * Lookups the owner of a given {@link ContentFolder}.
	 *
	 * @param folder
	 *            A {@link ContentFolder}. The folder must <strong>not</strong> be the root folder!
	 * @return The user who owns the personal folder that contains the folder. The return-value is not nullable.
	 */
	@Nonnull
	public User getOwnerOfContentFolderFromEnvers(final Folder folder) {
		Objects.requireNonNull(folder, "Folder must not be null!");
		Objects.requireNonNull(folder.getParentFolder(), "Folder must not be the root folder!");

		Folder tmpParent = folder;
		while (tmpParent.getParentFolder().getParentFolder() != null) {
			tmpParent = tmpParent.getParentFolder();
		}
		// "folder" is now a personal folder of a user (Personal folders are one level below the root folder)
		return userBusiness.getUserOwningFolderFromEnvers(tmpParent).orElseThrow(() -> new IllegalStateException(folder
				+ " has no owning user. All content folders should have a user owning the parent personal folder!"));
	}

	/**
	 * Checks if a given {@link Folder} is a personal folder. A folder is a personal folder if
	 * <ul>
	 * <li>the folder is a {@link ContentFolder}</li>
	 * <li>the folder's name is "personalFolder"</li>
	 * <li>the folder is one level below the root folder</li>
	 * <li>the folder has an owner</li>
	 * </ul>
	 *
	 * @param folder
	 * @return {@code TRUE} if the given folder meets the requirements above.
	 */
	public boolean isPersonalFolder(Folder folder) {
		if (!(folder instanceof ContentFolder)) {
			return false;
		}
		// Prevent LazyInitializationException while trying to access the parent, fixes #622. Since we know what we are
		// doing here, prevent SonarQube from complaining about reasignment of a parameter.
		folder = baseService.findById(ContentFolder.class, folder.getId(), false)
				.orElseThrow(() -> new NoSuchJackEntityException("ContentFolder does not exist (anymore),"
						+ " this method must not be called for exercises from the audit table!")); //NOSONAR
		Folder parent = folder.getParentFolder();

		return folder.getName().equals(ContentFolder.PERSONAL_FOLDER_NAME) //
				&& parent != null //
				&& parent.getParentFolder() == null //
				&& userBusiness.getUserOwningThisFolder(folder).isPresent();
	}

	public boolean isPersonalFolderFromEnvers(Folder folder) {
		if (!(folder instanceof ContentFolder)) {
			return false;
		}
		Folder parent = folder.getParentFolder();

		return folder.getName().equals(ContentFolder.PERSONAL_FOLDER_NAME) //
				&& parent != null //
				&& parent.getParentFolder() == null;
	}

	/**
	 * Checks if a given {@link Folder} is owned by the given {@link User}. This is the case when the user's personal
	 * folder is a parent folder of the given folder.
	 *
	 * @param folder
	 * @param user
	 * @return
	 */
	public boolean isOwnedBy(final Folder folder, final User user) {
		final Folder personalFolder = user.getPersonalFolder();
		if (personalFolder == null) {
			return false;
		}
		return folder instanceof ContentFolder && folder.isChildOf(personalFolder);
	}

	// #########################################################################
	// Check for duplicate names
	// #########################################################################

	/**
	 * @return true if given folder already contains a object of the given type with the given name
	 */
	public boolean checkForDuplicateName(ContentFolder parent, String name, EFolderChildType childType) {
		parent = getContentFolderWithLazyData(parent);

		switch (childType) {
		case EXERCISE:
			return parent.getChildrenExercises().stream().anyMatch(c -> c.getName().equals(name.strip()));
		case COURSE:
			return parent.getChildrenCourses().stream().anyMatch(c -> c.getName().equals(name.strip()));
		case CONTENT_FOLDER:
			return parent.getChildrenFolder().stream().anyMatch(c -> c.getName().equals(name.strip()));
		default:
			throw new IllegalArgumentException("Wrong content type for content folder (" + childType + ")");
		}
	}

	/**
	 * @return true if given folder already contains a object of the given type with the given name
	 */
	public boolean checkForDuplicateName(PresentationFolder parent, String name, EFolderChildType childType) {
		parent = getPresentationFolderWithLazyData(parent);

		switch (childType) {
		case COURSEOFFER:
			return parent.getChildrenCourseOffer().stream().anyMatch(c -> c.getName().equals(name.strip()));
		case PRESENTATION_FOLDER:
			return parent.getChildrenFolder().stream().anyMatch(c -> c.getName().equals(name.strip()));
		default:
			throw new IllegalArgumentException("Wrong content type for presentation folder (" + childType + ")");
		}
	}

	public boolean isFolderEmpty(Folder folder) {
		if (folder != null) {
			ContentFolder contentFolder = getContentFolderById(folder.getId())
					.orElseThrow(() -> new PreconditionException("The Folder doesn't exist anymore: " + folder));
			return contentFolder.getChildrenFolder().isEmpty() && contentFolder.getChildrenCourses().isEmpty()
					&& contentFolder.getChildrenExercises().isEmpty();
		}
		return true;
	}

	public ContentFolder getContentFolderFor(Exercise exercise) {
		return folderService.getContentFolderFor(exercise).orElseThrow();
	}

	public ContentFolder getFolderForAbstractExercise(AbstractExercise exercise) {
		if (exercise.isFrozen()) {
			return exerciseBusiness.getExerciseById(exercise.getProxiedOrRegularExerciseId()).orElseThrow().getFolder();
		}
		return ((Exercise) exercise).getFolder();
	}

	public ContentFolder getFolderForAbstractCourse(AbstractCourse course) {
		if (course.isFrozen()) {
			return exerciseBusiness.getExerciseById(course.getRealCourseId()).orElseThrow().getFolder();
		}
		return ((Course) course).getFolder();
	}

	public boolean foldersHaveTheSameOwner(ContentFolder f1, ContentFolder f2) {
		final User owner1 = getOwnerOfContentFolder(f1);
		final User owner2 = getOwnerOfContentFolder(f2);
		return owner1.equals(owner2);
	}

	/**
	 * Returns all parent folders for this folder and this folder.
	 * All parents on the path to the root are returned.
	 * The root is not included.
	 * <br>
	 * Example: <br>
	 * root <br>
	 * --parent <br>
	 * ---parent1 <br>
	 * ----folder<br>
	 *
	 * Returns parent, parent1 and folder.
	 *
	 * @param folder
	 * @return
	 */
	public List<ContentFolder> getAllParentFoldersAndContentFolder(ContentFolder folder) {
		List<ContentFolder> parentFolders = new ArrayList<>();

		while (folder.getParentFolder() != null) {
			parentFolders.add(folder);
			folder = (ContentFolder) folder.getParentFolder();
		}

		return parentFolders;

	}

	/**
	 * 
	 * Deletes the personal Folder of the user, if one exists.
	 * A personal Folder can only be deleted, if it is empty or contains only deletable data.
	 * 
	 * @param user
	 *            from which the personal Folder should be deleted
	 * @throws ActionNotAllowedException
	 *             if the Folder is not deletable
	 */
	public void deletePersonalFolder(User user) throws ActionNotAllowedException {
		ContentFolder personalFolder = user.getPersonalFolder();
		//check if folder is deletable
		if (personalFolder != null && this.isContentFolderDeletable(personalFolder, user)) {
			//remove folder from user
			user.setPersonalFolder(null);
			usersevice.mergeUser(user);
			//delete folder
			this.deleteContentFolder(personalFolder, user);
		} else {
			getLogger().error("Personal Folder of user: " + user + " could not be deleted.");
			throw new ActionNotAllowedException();
		}
	}

	/**
	 * Folder is deletable if:
	 * <ul>
	 * <li>the folder is empty</li>
	 * <li>the user has edit rights</li>
	 * <li>all children-data (exercises, courses, folders) are deletable</li>
	 * </ul>
	 * 
	 * 
	 * @param folder
	 *            for which should be checked if it is deletable
	 * @param user
	 *            the current user
	 * @return true if the folder is deletable
	 */
	public boolean isContentFolderDeletable(ContentFolder folder, User user) {

		folder = folderService.getContentFolderWithLazyData(folder);

		//folder is empty
		if (this.isFolderEmpty(folder)) {
			return true;
		}

		//user has edit rights on folder
		if (authorizationBusiness.isAllowedToEditFolder(user, folder)) {

			//all exercises are deletable
			if (!folder.getChildrenExercises().stream()
					.allMatch(ex -> exerciseBusiness.isExerciseDeletableByUser((Exercise) ex, user))) {
				return false;
			}

			//all courses are deletable
			if (!folder.getChildrenCourses().stream()
					.allMatch(c -> courseBusiness.isCourseDeletableByUser((Course) c, user))) {
				return false;
			}

			//all folders are deletable
			return folder.getChildrenFolder().stream()
					.allMatch(fold -> this.isContentFolderDeletable((ContentFolder) fold, user));
		}
		return false;

	}

	/*
	 * Returns the next or previous course or exercise in the list relative to the given exercise.
	 * 
	 * @param exercise The exercise whose neighbor is requested.
	 * 
	 * @param direction The direction to look for the neighbor.
	 * 
	 * @return The neighbor or {@code null} in case there is no neighbor in that direction.
	 */
	public AbstractEntity getNeighbor(final AbstractExercise exercise, final int direction) {
		return (getNeighbor(getFolderForAbstractExercise(exercise), exercise, direction));
	}

	/**
	 * Returns the next or previous course or exercise in the list relative to the given course.
	 * 
	 * @param course
	 *            The course whose neighbor is requested.
	 * @param direction
	 *            The direction to look for the neighbor.
	 * @return The neighbor or {@code null} in case there is no neighbor in that direction.
	 */
	public AbstractEntity getNeighbor(final AbstractCourse course, final int direction) {
		return (getNeighbor(getFolderForAbstractCourse(course), course, direction));
	}

	private AbstractEntity getNeighbor(final ContentFolder folder, final AbstractEntity item, final int direction) {
		if (direction == 0) {
			throw new IllegalArgumentException("You must provide a non-zero direction.");
		}
		final List<AbstractEntity> items = getAllExercisesAndCoursesIn(folder);
		final int index = items.indexOf(item) + Integer.signum(direction);
		if (0 <= index && index < items.size()) {
			return items.get(index);
		}
		return null;
	}

	/**
	 * Returns a list of all exercises and courses in the given folder. The list is is sorted
	 * so that courses are before exercises and then alphabetically.
	 * 
	 * @param folder
	 *            The folder whose courses and exercises are requested.
	 * @return A list of all exercises and courses in the given folder.
	 */
	private List<AbstractEntity> getAllExercisesAndCoursesIn(ContentFolder folder) {
		folder = folderService.getContentFolderWithLazyData(folder);

		final List<AbstractEntity> items = folder.getChildrenCourses().stream()
				.sorted(Comparator.comparing(AbstractCourse::getName)).collect(Collectors.toCollection(ArrayList::new));

		folder.getChildrenExercises().stream().sorted(Comparator.comparing(AbstractExercise::getName))
				.forEach(items::add);

		return items;
	}
}
