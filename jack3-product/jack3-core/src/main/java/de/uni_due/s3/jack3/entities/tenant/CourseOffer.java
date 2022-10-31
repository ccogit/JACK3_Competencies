package de.uni_due.s3.jack3.entities.tenant;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.validation.constraints.Min;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.DeepCopyOmitField;
import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.enums.ECourseOfferReviewMode;
import de.uni_due.s3.jack3.entities.enums.ECourseResultDisplay;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;
import de.uni_due.s3.jack3.interfaces.Namable;

@Audited
@NamedQuery(
		name = CourseOffer.ALL_COURSEOFFERS, //
		query = "SELECT c FROM CourseOffer c ORDER BY c.name ASC") //
@NamedQuery(
		name = CourseOffer.COURSEOFFERS_REFERENCING_COURSE_REVISION, //
		query = "SELECT co FROM CourseOffer co WHERE co.course = :course") //
@NamedQuery(
	name = CourseOffer.ALL_COURSEOFFERS_FOR_FOLDER, //
	query = "SELECT co FROM CourseOffer co WHERE co.folder = :folder") //
@NamedQuery(
	name = CourseOffer.ALL_COURSEOFFERS_FOR_FOLDERS, //
	query = "SELECT co FROM CourseOffer co WHERE co.folder IN (:folders)") //
@Entity
public class CourseOffer extends AbstractEntity implements DeepCopyable<CourseOffer>, Namable {

	private static final long serialVersionUID = 1812208684854262994L;

	public static final String CONSUMER_KEY_PREFIX = "lti-";

	// #########################################################################
	// Queries
	// #########################################################################

	/** Query returns all course offers, ordered by name ascending. */
	public static final String ALL_COURSEOFFERS = "CourseOffer.allCourseOffers";

	/** Query returns all course offers by given course. */
	public static final String COURSEOFFERS_REFERENCING_COURSE_REVISION = "CourseOffer.courseOffersReferencingCourseRevision";

	/** Query returns all course offers that are children of a folder */
	public static final String ALL_COURSEOFFERS_FOR_FOLDER = "CourseOffer.allCourseOffersForFolder";

	/** Query returns all course offers that are children of a folder in a folder list */
	public static final String ALL_COURSEOFFERS_FOR_FOLDERS = "CourseOffer.allCourseOffersForFolders";

	// #########################################################################
	// Attributes
	// NOTE: IF YOU ADD NEW ATTRIBUTES HERE, MAKE SURE THAT YOU ADD THEM IN deepCopy() TOO !
	// #########################################################################

	@ToString
	@Column(nullable = false)
	@Type(type = "text")
	private String name;

	@ManyToOne(fetch = FetchType.EAGER)
	@DeepCopyOmitField(copyTheReference = true, reason = "Copying a courseOffer doesn't mean copying the entire folder")
	private PresentationFolder folder;

	@ManyToOne(optional = true)
	@DeepCopyOmitField(
		copyTheReference = true,
		reason = "Copying a courseOffer doesn't mean copying the corresponding course")
	private AbstractCourse course;

	@Column
	@Type(type = "text")
	private String publicDescription;

	@Column
	@Type(type = "text")
	private String internalDescription;

	@Column
	// REIVEW lg - unbenutzt
	private boolean isMultilingual;

	@Column
	@Type(type = "text")
	protected String language;

	// #########################################################################
	// Email settings
	// #########################################################################

	@Column(nullable = false)
	@ColumnDefault("false")
	private boolean enrollmentEmail = false;

	@Column
	@Type(type = "text")
	private String enrollmentEmailText;

	@Column(nullable = false)
	@ColumnDefault("false")
	private boolean waitingListEmail = false;

	@Column
	@Type(type = "text")
	private String waitingListEmailText;

	// #########################################################################
	// Access restriction
	// #########################################################################

	/**
	 * A list that filters specific users for Course Offers, according to {@link #toggleAllowlist} and the
	 * {@link #profileFieldFilter}. Strings in this list match the login name or profile field data of a user.
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	@Type(type = "text")
	private Set<String> userFilter = new HashSet<>();

	/**
	 * If {@code true}, only users who match the {@link #userFilter} will be allowed for the course. If {@code false},
	 * only users who <strong>don't match</strong> the {@link #userFilter} will be allowed. In the latter case, the
	 * filter excludes certain users from the course.
	 */
	@Column(nullable = false)
	private boolean toggleAllowlist = false;

	/**
	 * For which profile fields the {@link #userFilter} is applied. By default (empty set) the filter only applies to
	 * the login name.
	 */
	@ManyToMany(fetch = FetchType.EAGER)
	@DeepCopyOmitField(
		copyTheReference = true,
		reason = "We don't want to deepCopy ProfileFields. Just take references to the profile fields")
	private Set<ProfileField> profileFieldFilter = new HashSet<>();

	/**
	 * Time until which the course offer is not yet visible.
	 */
	@Column
	private LocalDateTime visibilityStartTime;

	/**
	 * Time from which the Course Offer is no longer visible.
	 */
	@Column
	private LocalDateTime visibilityEndTime;

	/**
	 * Defines whether the Course Offer is potentially visible, according to the time limits and restrictions set. If
	 * {@code false}, the Course Offer is globally invisible, no matter what other restrictions have been set.
	 */
	@Column(nullable = false)
	@ColumnDefault("false")
	private boolean canBeVisible = false;

	// #########################################################################
	// LTI Data
	// #########################################################################

	/**
	 * Defines if the Course Offer is accessible via LTI.
	 */
	@Column(nullable = false)
	@ColumnDefault("false")
	private boolean ltiEnabled = false;

	@Column
	@Type(type = "text")
	@DeepCopyOmitField(reason = "A new secret ist generated by the business when duplicating.")
	private String ltiConsumerSecret;

	// #########################################################################
	// Enrollment restrictions
	// #########################################################################

	// Implicit enrollment and lack of a course are mutually exclusive! By default, no course is set, so an explicit
	// enrollment is forced.
	@Column(nullable = false)
	private boolean explicitEnrollment = true;

	@Column
	@Type(type = "text")
	private String globalPassword;

	@Column
	private LocalDateTime enrollmentStart;

	@Column
	private LocalDateTime enrollmentDeadline;

	@Column
	private LocalDateTime disenrollmentDeadline;

	@Column(nullable = false)
	@Min(0)
	private int maxAllowedParticipants; // 0 means no limit

	@Column(nullable = false)
	private boolean enableWaitingList;

	// #########################################################################
	// Submission restrictions
	// #########################################################################

	@Column(nullable = false)
	private boolean explicitSubmission;

	@Column
	private LocalDateTime submissionStart;

	/**
	 * After submission deadline, students are not allowed to submit an exercise or to restart the course offer.
	 */
	@Column
	private LocalDateTime submissionDeadline;

	@Column
	private Duration timeLimit = Duration.ZERO;

	@Column(nullable = false)
	private boolean enablePersonalPasswords;

	@ElementCollection(fetch = FetchType.EAGER)
	@Type(type = "text")
	private Map<User, String> personalPasswords = new HashMap<>();

	@Column(nullable = false)
	private boolean onlyOneParticipation;

	@Column(nullable = false)
	private boolean allowExerciseRestart = true;

	@Column(nullable = false)
	private boolean allowPauses = true;

	/** If the student is allowed to restart from a stage ("Start over from here") */
	@Column(nullable = false)
	private boolean allowStageRestart = true;

	@Column(nullable = false)
	private boolean allowHints = true;

	@Column(nullable = false)
	@Min(0)
	private int maxSubmissionsPerExercise;

	@Column(nullable = false)
	private boolean allowStudentComments = true;

	// #########################################################################
	// Display in running course
	// #########################################################################

	@Column(nullable = false)
	private boolean showDifficulty = true;

	@Column(nullable = false)
	private boolean showResultImmediately = true;

	@Column(nullable = false)
	private boolean showFeedbackImmediately = true;

	// #########################################################################
	// Display after finishing the course
	// #########################################################################

	@Enumerated(EnumType.STRING)
	private ECourseResultDisplay courseResultDisplay = ECourseResultDisplay.BOTH;

	@Enumerated(EnumType.STRING)
	private ECourseOfferReviewMode reviewMode = ECourseOfferReviewMode.ALWAYS;

	@Column(nullable = false)
	private boolean showExerciseAndSubmissionInCourseResults = true;

	@Column(nullable = false)
	private boolean showResultInCourseResults = true;

	@Column(nullable = false)
	private boolean showFeedbackInCourseResults = true;

	// #########################################################################
	// Constructors
	// #########################################################################

	public CourseOffer() {
		// Empty constructor for Hibernate
	}

	public CourseOffer(String courseOfferName, AbstractCourse course) {
		name = requireIdentifier(courseOfferName, "You must specify a non-empty name.");
		this.course = course;
	}

	// #########################################################################
	// Getters & Setters
	// #########################################################################

	@Override
	@Nonnull
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public PresentationFolder getFolder() {
		return folder;
	}

	public void setFolder(PresentationFolder folder) {
		this.folder = folder;
	}

	public AbstractCourse getCourse() {
		return course;
	}

	public void setCourse(AbstractCourse course) {
		this.course = course;
	}

	public String getPublicDescription() {
		return publicDescription;
	}

	public void setPublicDescription(String publicDescription) {
		this.publicDescription = publicDescription;
	}

	public String getInternalDescription() {
		return internalDescription;
	}

	public void setInternalDescription(String internalDescription) {
		this.internalDescription = internalDescription;
	}

	public boolean isMultilingual() {
		return isMultilingual;
	}

	public void setMultilingual(boolean isMultilingual) {
		this.isMultilingual = isMultilingual;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public boolean isEnrollmentEmail() {
		return enrollmentEmail;
	}

	public void setEnrollmentEmail(boolean enrollmentEmail) {
		this.enrollmentEmail = enrollmentEmail;
	}

	public String getEnrollmentEmailText() {
		return enrollmentEmailText;
	}

	public void setEnrollmentEmailText(String enrollmentEmailText) {
		this.enrollmentEmailText = enrollmentEmailText;
	}

	public boolean isWaitingListEmail() {
		return waitingListEmail;
	}

	public void setWaitingListEmail(boolean waitingListEmail) {
		this.waitingListEmail = waitingListEmail;
	}

	public String getWaitingListEmailText() {
		return waitingListEmailText;
	}

	public void setWaitingListEmailText(String waitingListEmailText) {
		this.waitingListEmailText = waitingListEmailText;
	}

	public Set<String> getUserFilter() {
		return userFilter;
	}

	public void setUserFilter(Set<String> userFilter) {
		this.userFilter.clear();
		this.userFilter.addAll(userFilter);
	}

	public boolean isToggleAllowlist() {
		return toggleAllowlist;
	}

	public void setToggleAllowlist(boolean toggleAllowlist) {
		this.toggleAllowlist = toggleAllowlist;
	}

	public Set<ProfileField> getProfileFieldFilter() {
		return profileFieldFilter;
	}

	public void setProfileFieldFilter(Set<ProfileField> profileFieldFilter) {
		this.profileFieldFilter.clear();
		this.profileFieldFilter.addAll(profileFieldFilter);
	}

	@CheckForNull
	public LocalDateTime getVisibilityStartTime() {
		return visibilityStartTime;
	}

	public void setVisibilityStartTime(LocalDateTime visibilityStartTime) {
		this.visibilityStartTime = visibilityStartTime;
	}

	@CheckForNull
	public LocalDateTime getVisibilityEndTime() {
		return visibilityEndTime;
	}

	public void setVisibilityEndTime(LocalDateTime visibilityEndTime) {
		this.visibilityEndTime = visibilityEndTime;
	}

	public boolean isCanBeVisible() {
		return canBeVisible;
	}

	public void setCanBeVisible(boolean canBeVisible) {
		this.canBeVisible = canBeVisible;
	}

	public boolean isLtiEnabled() {
		return ltiEnabled;
	}

	public void setLtiEnabled(final boolean ltiEnabled) {
		this.ltiEnabled = ltiEnabled;
	}

	public String getLtiConsumerKey() {
		return CONSUMER_KEY_PREFIX + getId();
	}

	public String getLtiConsumerSecret() {
		return ltiConsumerSecret;
	}

	public void setLtiConsumerSecret(final String ltiConsumerSecret) {
		this.ltiConsumerSecret = ltiConsumerSecret;
	}

	public boolean isExplicitEnrollment() {
		return explicitEnrollment;
	}

	public void setExplicitEnrollment(boolean explicitEnrollment) {
		this.explicitEnrollment = explicitEnrollment;
	}

	public String getGlobalPassword() {
		return globalPassword;
	}

	public void setGlobalPassword(String globalPassword) {
		this.globalPassword = globalPassword;
	}

	@CheckForNull
	public LocalDateTime getEnrollmentStart() {
		return enrollmentStart;
	}

	public void setEnrollmentStart(LocalDateTime enrollmentStart) {
		this.enrollmentStart = enrollmentStart;
	}

	@CheckForNull
	public LocalDateTime getEnrollmentDeadline() {
		return enrollmentDeadline;
	}

	public void setEnrollmentDeadline(LocalDateTime enrollmentDeadline) {
		this.enrollmentDeadline = enrollmentDeadline;
	}

	@CheckForNull
	public LocalDateTime getDisenrollmentDeadline() {
		return disenrollmentDeadline;
	}

	public void setDisenrollmentDeadline(LocalDateTime disenrollmentDeadline) {
		this.disenrollmentDeadline = disenrollmentDeadline;
	}

	public int getMaxAllowedParticipants() {
		return maxAllowedParticipants;
	}

	public void setMaxAllowedParticipants(int maxAllowedParticipants) {
		this.maxAllowedParticipants = maxAllowedParticipants;
	}

	public boolean isEnableWaitingList() {
		return enableWaitingList;
	}

	public void setEnableWaitingList(boolean enableWaitingList) {
		this.enableWaitingList = enableWaitingList;
	}

	public boolean isExplicitSubmission() {
		return explicitSubmission;
	}

	public void setExplicitSubmission(boolean explicitSubmission) {
		this.explicitSubmission = explicitSubmission;
	}

	@CheckForNull
	public LocalDateTime getSubmissionStart() {
		return submissionStart;
	}

	public void setSubmissionStart(LocalDateTime submissionStart) {
		this.submissionStart = submissionStart;
	}

	@CheckForNull
	public LocalDateTime getSubmissionDeadline() {
		return submissionDeadline;
	}

	public void setSubmissionDeadline(LocalDateTime submissionDeadline) {
		this.submissionDeadline = submissionDeadline;
	}

	public Duration getTimeLimit() {
		if (timeLimit == null) {
			return Duration.ZERO;
		}
		return timeLimit;
	}

	public long getTimeLimitInMinutes() {
		return timeLimit.toMinutes();
	}

	public void setTimeLimit(Duration timeLimit) {
		this.timeLimit = timeLimit;
	}

	public boolean isEnablePersonalPasswords() {
		return enablePersonalPasswords;
	}

	public void setEnablePersonalPasswords(boolean enablePersonalPasswords) {
		this.enablePersonalPasswords = enablePersonalPasswords;
	}

	public Map<User, String> getPersonalPasswords() {
		return Collections.unmodifiableMap(personalPasswords);
	}

	public void addPersonalPassword(User key, String password) {
		personalPasswords.put(key, password);
	}

	public void removePersonalPassword(User key) {
		personalPasswords.remove(key);
	}

	public void clearAllPersonalPasswords() {
		personalPasswords.clear();
	}

	public boolean isOnlyOneParticipation() {
		return onlyOneParticipation;
	}

	public void setOnlyOneParticipation(boolean onlyOneParticipation) {
		this.onlyOneParticipation = onlyOneParticipation;
	}

	public boolean isAllowExerciseRestart() {
		return allowExerciseRestart;
	}

	public void setAllowExerciseRestart(boolean allowExerciseRestart) {
		this.allowExerciseRestart = allowExerciseRestart;
	}

	public boolean isAllowPauses() {
		return allowPauses;
	}

	public void setAllowPauses(boolean allowPauses) {
		this.allowPauses = allowPauses;
	}

	public boolean isAllowStageRestart() {
		return allowStageRestart;
	}

	public void setAllowStageRestart(boolean allowStageRestart) {
		this.allowStageRestart = allowStageRestart;
	}

	public boolean isAllowHints() {
		return allowHints;
	}

	public void setAllowHints(boolean allowHints) {
		this.allowHints = allowHints;
	}

	public int getMaxSubmissionsPerExercise() {
		return maxSubmissionsPerExercise;
	}

	public void setMaxSubmissionsPerExercise(int maxSubmissionsPerExercise) {
		this.maxSubmissionsPerExercise = maxSubmissionsPerExercise;
	}

	public boolean isAllowStudentComments() {
		return allowStudentComments;
	}

	public void setAllowStudentComments(boolean allowStudentComments) {
		this.allowStudentComments = allowStudentComments;
	}

	public boolean isShowDifficulty() {
		return showDifficulty;
	}

	public void setShowDifficulty(boolean showDifficulty) {
		this.showDifficulty = showDifficulty;
	}

	public boolean isShowResultImmediately() {
		return showResultImmediately;
	}

	public void setShowResultImmediately(boolean showResultImmediately) {
		this.showResultImmediately = showResultImmediately;
	}

	public boolean isShowFeedbackImmediately() {
		return showFeedbackImmediately;
	}

	public void setShowFeedbackImmediately(boolean showFeedbackImmediately) {
		this.showFeedbackImmediately = showFeedbackImmediately;
	}

	public ECourseResultDisplay getCourseResultDisplay() {
		return courseResultDisplay;
	}

	public void setCourseResultDisplay(ECourseResultDisplay courseResultDisplay) {
		this.courseResultDisplay = courseResultDisplay;
	}

	public ECourseOfferReviewMode getReviewMode() {
		return reviewMode;
	}

	public void setReviewMode(ECourseOfferReviewMode reviewMode) {
		this.reviewMode = reviewMode;
	}

	public boolean isShowExerciseAndSubmissionInCourseResults() {
		return showExerciseAndSubmissionInCourseResults;
	}

	public void setShowExerciseAndSubmissionInCourseResults(boolean showExerciseAndSubmissionInCourseResults) {
		this.showExerciseAndSubmissionInCourseResults = showExerciseAndSubmissionInCourseResults;
	}

	public boolean isShowResultInCourseResults() {
		return showResultInCourseResults;
	}

	public void setShowResultInCourseResults(boolean showResultInCourseResults) {
		this.showResultInCourseResults = showResultInCourseResults;
	}

	public boolean isShowFeedbackInCourseResults() {
		return showFeedbackInCourseResults;
	}

	public void setShowFeedbackInCourseResults(boolean showFeedbackInCourseResults) {
		this.showFeedbackInCourseResults = showFeedbackInCourseResults;
	}

	// #########################################################################
	// Methods & computed values
	// #########################################################################


	public boolean hasFrozenCourse() {
		return course.isFrozen();
	}

	public List<Folder> getBreadcrumb() {
		final List<Folder> b = folder.getBreadcrumb();
		b.add(folder);
		return b;
	}

	/**
	 * Checks if a user's login or a profile field of him/her matches filter.
	 */
	public boolean matchFilter(User user) {
		return userFilter.contains(user.getLoginName()) || profileFieldFilter.stream()
				.anyMatch(field -> userFilter.contains(user.getProfileData().get(field)));
	}

	@Override
	public CourseOffer deepCopy() {
		CourseOffer clone = new CourseOffer();

		// General
		clone.name = name;
		clone.folder = folder; // not deepcopied
		clone.course = course; // not deepcopied
		clone.publicDescription = publicDescription;
		clone.internalDescription = internalDescription;
		clone.language = language;

		// Email settings
		clone.enrollmentEmail = enrollmentEmail;
		clone.enrollmentEmailText = enrollmentEmailText;
		clone.waitingListEmail = waitingListEmail;
		clone.waitingListEmailText = waitingListEmailText;

		// Access restriction
		clone.userFilter.addAll(userFilter);
		clone.toggleAllowlist = toggleAllowlist;
		clone.profileFieldFilter.addAll(profileFieldFilter); // not deepcopied, only references to profile fields
		clone.visibilityStartTime = visibilityStartTime;
		clone.visibilityEndTime = visibilityEndTime;
		clone.canBeVisible = canBeVisible;

		// LTI
		clone.ltiEnabled = ltiEnabled;

		// Enrollment restrictions
		clone.explicitEnrollment = explicitEnrollment;
		clone.globalPassword = globalPassword;
		clone.enrollmentStart = enrollmentStart;
		clone.enrollmentDeadline = enrollmentDeadline;
		clone.disenrollmentDeadline = disenrollmentDeadline;
		clone.maxAllowedParticipants = maxAllowedParticipants;
		clone.enableWaitingList = enableWaitingList;

		// Submission restrictions
		clone.explicitSubmission = explicitSubmission;
		clone.submissionStart = submissionStart;
		clone.submissionDeadline = submissionDeadline;
		clone.timeLimit = timeLimit;
		clone.enablePersonalPasswords = enablePersonalPasswords;
		clone.personalPasswords.putAll(personalPasswords);
		clone.onlyOneParticipation = onlyOneParticipation;
		clone.allowExerciseRestart = allowExerciseRestart;
		clone.allowPauses = allowPauses;
		clone.allowStageRestart = allowStageRestart;
		clone.allowHints = allowHints;
		clone.maxSubmissionsPerExercise = maxSubmissionsPerExercise;
		clone.allowStudentComments = allowStudentComments;

		// Display settings for running courses
		clone.showDifficulty = showDifficulty;
		clone.showResultImmediately = showResultImmediately;
		clone.showFeedbackImmediately = showFeedbackImmediately;

		// Display settings for closed course records
		clone.courseResultDisplay = courseResultDisplay;
		clone.reviewMode = reviewMode;
		clone.showExerciseAndSubmissionInCourseResults = showExerciseAndSubmissionInCourseResults;
		clone.showResultInCourseResults = showResultInCourseResults;
		clone.showFeedbackInCourseResults = showFeedbackInCourseResults;

		// TODO Not implemented / used yet
		clone.isMultilingual = isMultilingual;

		return clone;
	}
}
