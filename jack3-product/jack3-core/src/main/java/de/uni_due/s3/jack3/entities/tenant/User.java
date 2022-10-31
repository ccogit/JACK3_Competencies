
package de.uni_due.s3.jack3.entities.tenant;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Pattern;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;

// NOTE: For queries that consider frozen courses, we need a sub-query to retrieve the IDs of the frozen courses:
// "SELECT fc.id FROM FrozenCourse fc WHERE fc.proxiedCourseId = :courseId" (see QUERY_MATCH_COURSE_AND_FROZENREVISIONS)
@NamedQuery(
		name = User.ALL_USERS,
		query = "SELECT u FROM User u " //
		+ "ORDER BY u.loginName ASC")
@NamedQuery(
		name = User.ALL_USERS_WITH_EDIT_RIGHTS,
		query = "SELECT u FROM User u " //
		+ "WHERE u.hasEditRights = true " //
		+ "ORDER BY u.loginName ASC")
@NamedQuery(
	name = User.ALL_USERS_WITHOUT_EDIT_RIGHTS,
	query = "SELECT u FROM User u " //
			+ "WHERE u.hasEditRights = false " //
			+ "ORDER BY u.loginName ASC")
@NamedQuery(
		name = User.USER_BY_NAME,
		query = "SELECT u FROM User u " //
		+ "WHERE u.loginName = :loginName")
@NamedQuery(
		name = User.USER_BY_EMAIL,
		query = "SELECT u FROM User u " //
		+ "WHERE u.email = :email")
@NamedQuery(
		name = User.USER_OWNING_THIS_FOLDER,
		query = "SELECT u FROM User u " //
		+ "WHERE u.personalFolder = :folder")
@NamedQuery(
		name = User.QUERY_COUNT, //
		query = "SELECT COUNT (u) FROM User u")
@NamedQuery(
		name = User.EXISTS_PSEUDONYM,
		query = "SELECT COUNT(u) FROM User u WHERE pseudonym = :pseudonym")
@NamedQuery(
	name = User.ALL_NONTESTING_PARTICIPANTS_FOR_COURSE_INCLUDING_FROZENREVISIONS,
	query = "SELECT DISTINCT cr.user FROM CourseRecord cr " //
			+ "WHERE cr.isTestSubmission is FALSE " //
			+ "AND (cr.course.id = :courseId OR cr.course.id IN (SELECT fc.id FROM FrozenCourse fc WHERE fc.proxiedCourseId = :courseId))")
@NamedQuery(
	name = User.COUNT_NONTESTING_PARTICIPANTS_FOR_COURSE_INCLUDING_FROZENREVISIONS,
	query = "SELECT COUNT(DISTINCT cr.user) FROM CourseRecord cr " //
			+ "WHERE cr.isTestSubmission is FALSE " //
			+ "AND (cr.course.id = :courseId OR cr.course.id IN (SELECT fc.id FROM FrozenCourse fc WHERE fc.proxiedCourseId = :courseId))")

@Audited
@Entity
@Table(name = "Usertable")
public class User extends AbstractEntity implements Comparable<User> {

	private static final long serialVersionUID = 5425310294944885735L;

	/** This is the pattern the users' login names a validated against. */
	public static final String LOGIN_NAME_REGEX =
		"[" +
		"-_@\\." + // We allow the characters '-', '_', '@' and '.' for email addresses
		"\\p{IsAlphabetic}" + // as well as unicode letters
		"\\p{IsDigit}" + // and digits
		"]" +
		"{3,}"; // We require at least three characters.

	/** Name of the query that returns all users which are not deleted ordered by name ascending. */
	public static final String ALL_USERS = "User.allUsers";

	/** Name of the query that returns all users with edit rights. */
	public static final String ALL_USERS_WITH_EDIT_RIGHTS = "User.allUsersWithEditRights";

	public static final String ALL_USERS_WITHOUT_EDIT_RIGHTS = "User.allUsersWithoutEditRights";

	public static final String USER_BY_NAME = "User.userByName";

	public static final String USER_BY_EMAIL = "User.userByEmail";

	public static final String USER_OWNING_THIS_FOLDER = "User.userOwningThisFolder";

	public static final String QUERY_COUNT = "User.count";

	public static final String EXISTS_PSEUDONYM = "User.existsPseudonym";

	public static final String ALL_NONTESTING_PARTICIPANTS_FOR_COURSE_INCLUDING_FROZENREVISIONS = "User.allNonTestingParticipantsForCourseIncludingFrozenRevisions";

	public static final String COUNT_NONTESTING_PARTICIPANTS_FOR_COURSE_INCLUDING_FROZENREVISIONS = "User.countNonTestingParticipantsForCourseIncludingFrozenRevisions";

	@ToString
	@Column(nullable = false, unique = true)
	@Type(type = "text")
	@Pattern(regexp = User.LOGIN_NAME_REGEX)
	private String loginName;

	@Column(nullable = false, unique = true)
	@Type(type = "text")
	private String pseudonym;

	@Embedded
	private Password password;

	@Column(nullable = true)
	@Type(type = "text")
	private String email;

	@Column
	@Type(type = "text")
	private String language;

	// REVIEW lg - Sollte man dieses Attribut in "isLecturer", "hasLecturerRights" o.ä. umbenennen? "editRights"
	// suggerriert, dass der Benutzer Schreibrechte hat, was aber nur auf seinem persönlichen Ordner der Fall ist.
	@Column(nullable = false, columnDefinition = "boolean default false")
	private boolean hasEditRights;

	@OneToOne
	private ContentFolder personalFolder;

	@Column(nullable = false, columnDefinition = "boolean default false")
	private boolean hasAdminRights;

	@ElementCollection(targetClass = String.class,fetch=FetchType.EAGER)
	@MapKeyJoinColumn(name = "id")
	@Type(type = "text")
	private Map<ProfileField, String> profileData = new HashMap<>();

	// REVIEW lg - Ungenutzt
	@OneToMany
	private Map<CourseOffer, UserExerciseFilter> exerciseFilters = new HashMap<>();

	@Column
	private LocalDateTime lastLogin;

	public User() {
	}

	public User(String name, String pseudonym, Password password, String email, boolean hasAdminRights, boolean hasEditRights) {
		loginName = requireLoginName(name).toLowerCase();
		this.pseudonym = requireIdentifier(pseudonym,"The user's pseudonym must not be emtpy.");
		setPassword(password);
		this.email = email;
		this.hasAdminRights = hasAdminRights;
		this.hasEditRights = hasEditRights;
	}

	private String requireLoginName(final String loginName) {
		requireIdentifier(loginName, "The user's name must be a non-empty string.");

		if (!loginName.matches(LOGIN_NAME_REGEX)) {
			throw new IllegalArgumentException("loginName does not meet the loginname restrictions.");
		}

		return loginName;
	}

	public String getEmail() {
		return email;
	}

	/*
	 * @return unmodifiableMap of exerciseFilters
	 */
	public Map<CourseOffer, UserExerciseFilter> getExerciseFilters() {
		return Collections.unmodifiableMap(exerciseFilters);
	}

	public LocalDateTime getLastLogin() {
		return lastLogin;
	}

	public Password getPassword() {
		return password;
	}

	public boolean isExternal() {
		return password == null;
	}

	public Locale getLanguage() {
		return language != null ? Locale.forLanguageTag(language) : null;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public ContentFolder getPersonalFolder() {
		return personalFolder;
	}

	public Map<ProfileField, String> getProfileData() {
		return Collections.unmodifiableMap(profileData);
	}

	public String getPseudonym() {
		return pseudonym;
	}

	public String getLoginName() {
		return loginName;
	}

	public boolean isHasAdminRights() {
		return hasAdminRights;
	}

	public boolean isHasEditRights() {
		return hasEditRights;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void addExerciseFilters(Map<CourseOffer, UserExerciseFilter> exerciseFilters) {
		this.exerciseFilters.putAll(exerciseFilters);
	}

	public void setHasAdminRights(boolean hasAdminRights) {
		this.hasAdminRights = hasAdminRights;
	}

	public void setHasEditRights(boolean hasEditRights) {
		this.hasEditRights = hasEditRights;
	}

	public void setLastLogin(LocalDateTime lastLogin) {
		this.lastLogin = lastLogin;
	}

	public void setPassword(final Password password) {
		this.password = password;
	}

	public void setPersonalFolder(ContentFolder personalFolder) {
		this.personalFolder = personalFolder;
	}

	public void addProfileData(Map<ProfileField, String> profileData) {
		this.profileData.putAll(profileData);
	}

	public void putProfileField(final ProfileField field,final String value) {
		profileData.put(field,value);
	}

	@Override
	public int compareTo(User other) {
		if (other == null) {
			return -1;
		}
		return getLoginName().compareTo(other.getLoginName());
	}
}
