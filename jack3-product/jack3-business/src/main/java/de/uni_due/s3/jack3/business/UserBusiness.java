package de.uni_due.s3.jack3.business;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javax.annotation.Nullable;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.security.auth.login.CredentialException;
import javax.transaction.Transactional;

import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.authz.Attributes;

import de.uni_due.s3.jack3.business.helpers.PublicUserName;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.Password;
import de.uni_due.s3.jack3.entities.tenant.ProfileField;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;
import de.uni_due.s3.jack3.multitenancy.TenantIdentifier;
import de.uni_due.s3.jack3.services.EmailService;
import de.uni_due.s3.jack3.services.ProfileFieldService;
import de.uni_due.s3.jack3.services.UserGroupService;
import de.uni_due.s3.jack3.services.UserService;
import de.uni_due.s3.jack3.utils.JackStringUtils;
import de.uni_due.s3.jack3.utils.StringGenerator;

@RequestScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class UserBusiness extends AbstractBusiness {

	@Inject
	private UserService userService;

	@Inject
	private UserGroupService userGroupService;

	@Inject
	private ProfileFieldService profileFieldService;

	@Inject
	private EmailService emailService;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private BcryptBusiness bcryptBusiness;

	@Inject
	private AuthorizationBusiness authorizationBusiness;

	@Inject
	private ConfigurationBusiness configurationBusiness;

	public List<UserGroup> getAllUserGroups() {
		return userGroupService.getAllUserGroups();
	}

	public List<UserGroup> getUserGroupsForUser(User user) {
		final List<UserGroup> resultGroups = new LinkedList<>();

		final List<UserGroup> allGroups = getAllUserGroups();

		for (final UserGroup u : allGroups) {
			if (u.containsUserAsAnyMember(user)) {
				resultGroups.add(u);
			}
		}

		return resultGroups;
	}

	// REVIEW bo: sollte das nach AuthorizationBusiness verschoben werden?
	public List<User> getAllUsersWithEditRights() {
		return userService.getAllUsersWithEditRights();
	}

	// REVIEW bo: sollte das nach AuthorizationBusiness verschoben werden?
	public List<User> getAllUsersWithoutEditRights() {
		return userService.getAllUsersWithoutEditRights();
	}

	public Optional<User> getUserOwningThisFolder(Folder folder) {
		return userService.getUserOwningThisFolder(folder);
	}
	
	public Optional<User> getUserOwningFolderFromEnvers(Folder folder){
		return userService.getUserOwningFolderFromEnvers(folder);
	}

	public Optional<User> getUserByName(String name) {
		return userService.getUserByName(name);
	}

	public Optional<User> getUserById(long id) {
		return userService.getUserById(id);
	}

	public Optional<User> getUserByEmail(final String email) {
		return userService.getUserByEmail(email);
	}

	public User registerUser(String email, final Map<ProfileField,String> profileFields, ResourceBundle bundle, String tenantUrl)
			throws MessagingException {
		final User user = createUser(null, email, email, false, false, bundle, tenantUrl);
		for (final Entry<ProfileField,String> entry : profileFields.entrySet()) {
			user.putProfileField(entry.getKey(), entry.getValue());
		}
		return userService.mergeUser(user);
	}

	public User createUser(User creator,String loginName, String email, boolean hasAdminRights, boolean hasEditRights,ResourceBundle bundle,String tenantUrl)
			throws MessagingException {
		final String plainPassword = StringGenerator.forPasswords().build().generate();
		final User user = createUser(loginName, plainPassword, email, hasAdminRights, hasEditRights);
		sendCredentials(user, plainPassword, bundle, tenantUrl);
		return user;
	}

	public User createUser(String loginName, String plaintextPassword, String email, boolean hasAdminRights,
			boolean hasEditRights) {
		if (plaintextPassword.isBlank()) {
			throw new IllegalArgumentException("plaintextPassword must not be empty.");
		}
		final String pseudonym = generatePseudonym();
		final Password password = bcryptBusiness.createPassword(plaintextPassword);
		final User user = new User(loginName, pseudonym, password, email, hasAdminRights, hasEditRights);
		userService.persistUser(user);
		getLogger().info("Created user " + loginName + ".");
		return createPersonalFolderIfRequired(user);
	}

	private String generatePseudonym() {
		final StringGenerator sg = StringGenerator.forPseudonyms().build();
		String pseudonym;
		do {
			pseudonym = sg.generate();
		} while (userService.hasPseudonym(pseudonym));
		return pseudonym;
	}

	public void sendCredentials(User user, String plaintextPassword, ResourceBundle bundle,String tenantUrl)
			throws MessagingException {
		final String subject = bundle.getString("tenantadmin.credentialsMail.subject");
		final String content = bundle.getString("tenantadmin.credentialsMail.content");
		emailService.createMail()
				.withRecipients(user.getEmail())
				.withSubject(subject)
				.withHtml(content,tenantUrl,user.getLoginName(),plaintextPassword)
				.send();
	}

	public User performPostLoginOperations() {
		final SecurityIdentity identity = SecurityDomain.getCurrent().getCurrentSecurityIdentity();
		final String userName = TenantIdentifier.dequalify(identity.getPrincipal().getName());
		User user = getUserByName(userName).orElseGet(() -> createExternalUser(userName));

		user.setLastLogin(LocalDateTime.now());
		updateProfileFields(user, identity.getAttributes());
		user = createPersonalFolderIfRequired(user);

		return userService.mergeUser(user);
	}

	private User createExternalUser(final String userName) {
		final String pseudonym = StringGenerator.forPseudonyms().build().generate();
		final User user = new User(userName, pseudonym, null, null, false, false);
		userService.persistUser(user);
		getLogger().info("Created external user " + userName + ".");
		return user;
	}

	private void updateProfileFields(final User user, final Attributes attributes) {
		for (final Attributes.Entry entry : attributes.entries()) {

			// We explicitly look for the email attribute to update the user's email field.
			if ("email".equals(entry.getKey())) {
				user.setEmail(entry.get(0));
			} else {
				// Other attributes are wrapped into profile fields.
				final String attributeName = entry.getKey();
				ProfileField field = profileFieldService.getOrCreateIdentityField(attributeName);
				user.putProfileField(field, entry.get(0));
			}
		}
	}

	public User changeUserPassword(final User user, final String oldPassword, final String newPassword)
			throws CredentialException, IllegalArgumentException {

		if (!bcryptBusiness.matches(oldPassword, user.getPassword())) {
			throw new CredentialException("Old password is incorrect.");
		}

		if (JackStringUtils.isBlank(newPassword)) {
			throw new IllegalArgumentException("Password must not be empty!");
		}

		user.setPassword(bcryptBusiness.createPassword(newPassword));
		return updateUser(user);
	}

	public void resetUserPassword(final User user, final String subject, final String contentFormat)
			throws MessagingException {
		final String password = StringGenerator.forPasswords().build().generate();
		emailService.createMail()
				.withSubject(subject)
				.withRecipients(user.getEmail())
				.withHtml(contentFormat, password)
				.send();

		user.setPassword(bcryptBusiness.createPassword(password));
		updateUser(user);
	}

	/**
	 * Ensures that the given user has a personal folder if the user has edit rights.
	 */
	private User createPersonalFolderIfRequired(User user) {
		if (user.isHasEditRights() && (user.getPersonalFolder() == null)) {
			getLogger().debug("Creating personal folder for user " + user.getLoginName());
			user.setPersonalFolder(folderBusiness.createPersonalFolder(user));
			user = userService.mergeUser(user);
		}
		return user;
	}

	public void removeUserFromAllUserGroups(User user) {
		List<UserGroup> groupsOfUser = getUserGroupsForUser(user);
		for (UserGroup group : groupsOfUser) {
			removeUserFromUserGroup(user, group);
		}
	}

	public User updateUser(User user) {
		user = createPersonalFolderIfRequired(user);

		if (!user.isHasEditRights()) {
			removeUserFromAllUserGroups(user);
			folderBusiness.removeUserRightsOnNonPersonalContentFolders(user);
		}

		return userService.mergeUser(user);
	}

	public Optional<UserGroup> getUserGroup(String name) {
		return userGroupService.getUserGroupByName(name);
	}
	
	public UserGroup getUserGroupWithLazyData (UserGroup userGroup) {
		return userGroupService.getUserGroupWithLazyData(userGroup);
	}

	public Optional<UserGroup> getUserGroupWithLazyDataById(long id) {
		return userGroupService.getUserGroupWithLazyData(id);
	}

	public UserGroup createUserGroup(String name, String description) {
		final UserGroup group = new UserGroup(name, description);
		userGroupService.persistUserGroup(group);
		return group;
	}

	public void deleteUserGroup(UserGroup userGroup) {
		userGroupService.removeUserGroup(userGroup);
	}

	public void switchMembershipOfUserGroup(UserGroup group, final UserGroup member) {
		// The caller may didn't receive the changed UserGroup object back during an previous update. For this reason,
		// we have to fetch a fresh copy of the object here
		group = userGroupService.getUserGroupWithMemberGroups(group);
		if (group.containsGroupAsDirectMember(member)) {
			removeUserGroupFromUserGroup(member, group);
		} else {
			addUserGroupToUserGroup(member, group);
		}
	}

	public void switchMembershipOfUser(UserGroup group, final User member) {
		// The caller may didn't receive the changed UserGroup object back during an previous update. For this reason,
		// we have to fetch a fresh copy of the object here
		group = userGroupService.getUserGroupWithMemberUsers(group);
		if (group.containsUserAsDirectMember(member)) {
			removeUserFromUserGroup(member, group);
		} else {
			addUserToUserGroup(member, group);
		}
	}

	/**
	 * Receives two user group entities and ensures that the first user group is no direct member of the second group.
	 */
	public void removeUserGroupFromUserGroup(UserGroup memberGroup, UserGroup parentGroup) {
		parentGroup = userGroupService.getUserGroupWithMemberGroups(parentGroup);
		parentGroup.removeMemberGroup(memberGroup);
		userGroupService.mergeUserGroup(parentGroup);
	}

	/**
	 * Receives two user group entities and makes the first user group a direct member of the second group.
	 */
	public void addUserGroupToUserGroup(UserGroup memberGroup, UserGroup parentGroup) {
		parentGroup = userGroupService.getUserGroupWithMemberGroups(parentGroup);
		parentGroup.addMemberGroup(memberGroup);
		userGroupService.mergeUserGroup(parentGroup);
	}

	/**
	 * Removes a user entity from a user group entity.
	 */
	public void removeUserFromUserGroup(User user, UserGroup group) {
		group = userGroupService.getUserGroupWithMemberUsers(group);
		group.removeMemberUser(user);
		userGroupService.mergeUserGroup(group);
	}

	/**
	 * Adds a user entity to a user group entity.
	 */
	public void addUserToUserGroup(User user, UserGroup group) {
		group = userGroupService.getUserGroupWithMemberUsers(group);
		group.addMemberUser(user);
		userGroupService.mergeUserGroup(group);
	}

	public UserGroup updateUserGroup(UserGroup userGroup) {
		return userGroupService.mergeUserGroup(userGroup);
	}

	public void updateUserGroupInformation(UserGroup userGroup, String newName, String newDescription) {
		userGroup = userGroupService.getUserGroupById(userGroup.getId()).orElseThrow(NoSuchJackEntityException::new);
		userGroup.setName(newName.strip());
		userGroup.setDescription(newDescription);
		userGroupService.mergeUserGroup(userGroup);
	}

	public void updateUserInformation(User user, String newEmail, boolean newAdminRights, boolean newEditRights) {
		user = userService.getUserById(user.getId()).orElseThrow(NoSuchJackEntityException::new);
		user.setEmail(newEmail);
		user.setHasAdminRights(newAdminRights);
		user.setHasEditRights(newEditRights);
		userService.mergeUser(user);
	}

	/**
	 * Returns all users that have participated in the passed course including frozen versions. Test submissions will be
	 * ignored.
	 */
	public List<User> getParticipantsForCourseIgnoringTestSubmissions(Course course) {
		return userService.getAllParticipantsForCourse(course);
	}

	public Set<User> getAllUsersForUserGroup(UserGroup userGroup) {
		return userGroupService.getUserGroupWithMemberUsers(userGroup).getMemberUsers();
	}

	public List<User> getAllUsers() {
		return userService.getAllUsers();
	}

	public boolean hasNoUser() {
		return userService.hasNoUser();
	}

	public List<ProfileField> getAllProfileFields() {
		return profileFieldService.getAllFields();
	}

	public List<ProfileField> getMandatoryProfileFields() {
		return profileFieldService.getAllMandatoryFields();
	}

	/**
	 * Returns a list of all public profile fields if the given user has extended read rights on the given folder.
	 * Returns an empty list otherwise.
	 *
	 * @param user
	 *            The user who wants to see a list of public profile fields.
	 * @param folder
	 *            The folder from which the user triggered the request.
	 * @return A (possibly empty) list of profile fields.
	 */
	public List<ProfileField> getAllPublicProfileFields(User user, Folder folder) {
		if (!authorizationBusiness.hasExtendedReadOnFolder(user, folder)) {
			return new LinkedList<ProfileField>();
		}

		return profileFieldService.getAllPublicFields();
	}

	/**
	 * Constructs a string that can be used to display a user name according to the following rules:
	 *
	 * <ol>
	 * <li>If the asking user has no extended read rights on the given folder, a pseudonym is returned.</li>
	 * <li>If the asking user has extended read rights on the given folder and no pattern for display name construction
	 * has been defined by the administrator, the loginname is returned.</li>
	 * <li>If the asking user has extended read rights on the given folder and a pattern for display name construction
	 * has been defined by the administrator, a string following that pattern is returned.</li>
	 * </ol>
	 *
	 * @param forUser
	 *            The user entity who's name should be displayed.
	 * @param askingUser
	 *            The asking user who wants to see the name.
	 * @param folder
	 *            The folder from which the asking user triggered the request.
	 * @return A string that conforms to all configured patterns and rights.
	 */
	@Nullable
	public PublicUserName getPublicUserName(User forUser, User askingUser, Folder folder) {
		// If there is no user, there also is no name
		// (That case happens due to JSF implementation details when name is used for sorting lists.)
		if (forUser == null) {
			return null;
		}

		// Users are always allowed to see their own name
		if (forUser.equals(askingUser)) {
			return PublicUserName.of(forUser.getLoginName());
		}

		// If we don't know the folder or the asking user, or the asking user has not enough rights on a folder, we
		// return the pseudonym
		if (folder == null || askingUser == null
				|| !authorizationBusiness.hasExtendedReadOnFolder(askingUser, folder)) {
			return PublicUserName.ofPseudonym(forUser.getPseudonym());
		}

		// If we are here, a user asks for the name of another user and has enough rights, so we construct the display
		// name based on the configuration settings.
		List<String> nameConfig = configurationBusiness.getValueList("publicUserName");
		String publicName = "";

		// If there is a primary display name pattern, we apply that
		if (nameConfig.size() > 0) {
			publicName = replaceProfileFields(nameConfig.get(0), profileFieldService.getAllFields(), forUser);
		}

		// If the first try resulted in an empty string and there is another pattern, we apply that
		if (publicName.isEmpty() && nameConfig.size() > 1) {
			publicName = replaceProfileFields(nameConfig.get(1), profileFieldService.getAllFields(), forUser);
		}

		// If we still have an empty string, we use the default fallback
		if (publicName.isEmpty()) {
			publicName = forUser.getLoginName();
		}

		return PublicUserName.of(publicName);
	}

	private String replaceProfileFields(String pattern, List<ProfileField> fields, User user) {
		user = userService.getUserById(user.getId()).orElseThrow(IllegalStateException::new);

		String displayName = new String(pattern);

		for (ProfileField publicProfileField : fields) {
			displayName = displayName.replace("[" + publicProfileField.getName() + "]",
					user.getProfileData().get(publicProfileField));
		}

		displayName = displayName.replace("[loginname]", user.getLoginName());

		if (user.getEmail() != null) {
			displayName = displayName.replace("[email]", user.getEmail());
		} else {
			displayName = displayName.replace("[email]", "");
		}

		return displayName.strip();
	}

	public ProfileField updateProfileField(ProfileField profileField) {
		return profileFieldService.updateProfileField(profileField);
	}
}
