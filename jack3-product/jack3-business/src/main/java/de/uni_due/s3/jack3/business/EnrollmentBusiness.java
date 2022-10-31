package de.uni_due.s3.jack3.business;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.annotation.Nonnull;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.Application;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.transaction.Transactional;

import org.hibernate.Hibernate;

import de.uni_due.s3.jack3.business.exceptions.EnrollmentException;
import de.uni_due.s3.jack3.business.exceptions.LinkedCourseException;
import de.uni_due.s3.jack3.business.exceptions.NotInteractableException;
import de.uni_due.s3.jack3.business.exceptions.PasswordRequiredException;
import de.uni_due.s3.jack3.business.exceptions.SubmissionException;
import de.uni_due.s3.jack3.business.helpers.CourseParticipation;
import de.uni_due.s3.jack3.business.helpers.EnrollmentLogEntry;
import de.uni_due.s3.jack3.entities.enums.ECourseOfferReviewMode;
import de.uni_due.s3.jack3.entities.enums.EEnrollmentStatus;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.Enrollment;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;
import de.uni_due.s3.jack3.services.CourseOfferService;
import de.uni_due.s3.jack3.services.CourseRecordService;
import de.uni_due.s3.jack3.services.EmailService;
import de.uni_due.s3.jack3.services.EnrollmentService;
import de.uni_due.s3.jack3.services.FolderService;
import de.uni_due.s3.jack3.services.RevisionService;
import de.uni_due.s3.jack3.utils.JackStringUtils;

/**
 * Contains all actions and checks related to the course registration feature.
 */
@RequestScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class EnrollmentBusiness extends AbstractBusiness {

	@Inject
	private EnrollmentService enrollmentService;

	@Inject
	private CourseRecordService courseRecordService;

	@Inject
	private CourseOfferService courseOfferService;

	@Inject
	private FolderService folderService;

	@Inject
	private RevisionService revisionService;

	@Inject
	private EmailService emailService;

	@Inject
	private AuthorizationBusiness authBusiness;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	// #########################################################################
	// Enrollment - Actions
	// #########################################################################

	/**
	 * Enrolls a user in a course.
	 * 
	 * @param user
	 *            The user who will be enrolled
	 * @param offer
	 *            The linked course offer
	 * @return The newly created {@link CourseRecord} if submission was implicitly started with the enrollment,
	 *         otherwise Empty {@link Optional} if the user was only enrolled.
	 * @throws EnrollmentException
	 *             If the course either does not allow enrollment in general or the specific user is not allowed to
	 *             interact with the course.
	 * @throws PasswordRequiredException
	 *             When a (global) password is required from the user to enrol. In this case, use
	 *             {@link #enrollUser(User, CourseOffer, String)} and pass the password.
	 * @throws NotInteractableException
	 *             If the course offer is not visible
	 * @throws MessagingException
	 *             If the email cannot be sent
	 * @see #checkEnrollmentPermission(User, CourseOffer)
	 * @see #enrollUser(User, CourseOffer, String)
	 */
	public Optional<CourseRecord> enrollUser(User user, CourseOffer offer)
			throws EnrollmentException, PasswordRequiredException, NotInteractableException, MessagingException {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);

		// Check conditions
		ensureNoGlobalPassword(offer);
		checkEnrollmentPermission(user, offer);

		// All conditions are checked here, so we can enroll in the user.
		Optional<CourseRecord> courseRecord = enroll(user, offer, user, null);
		if (offer.isEnrollmentEmail()) {
			sendEnrollmentEmail(user, offer);
		}
		return courseRecord;
	}

	/**
	 * Enrolls a user in a course with a passed password.
	 * 
	 * @param user
	 *            The user who will be enrolled
	 * @param offer
	 *            The linked course offer
	 * @param password
	 *            Plaintext password entered by the user
	 * @return The newly created {@link CourseRecord} if submission was implicitly started with the enrollment,
	 *         otherwise Empty {@link Optional} if the user was only enrolled.
	 * @throws EnrollmentException
	 *             If the course either does not allow enrollment in general, the specific user is not allowed to
	 *             interact with the course or the password is wrong.
	 * @throws NotInteractableException
	 *             If the course offer is not visible
	 * @throws MessagingException
	 *             If there was an exception while sending the mail for the user
	 * @see #checkEnrollmentPermission(User, CourseOffer)
	 */
	public Optional<CourseRecord> enrollUser(User user, CourseOffer offer, String password)
			throws EnrollmentException, NotInteractableException, MessagingException {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);

		// Check conditions
		ensureCorrectGlobalPassword(offer, password);
		checkEnrollmentPermission(user, offer);

		// All conditions are checked here, so we can enroll in the user.
		Optional<CourseRecord> courseRecord = enroll(user, offer, user, null);
		if (offer.isEnrollmentEmail()) {
			sendEnrollmentEmail(user, offer);
		}
		return courseRecord;
	}

	/**
	 * Enrolls a user in a course. The action is performed by a user that <strong>must</strong> have extended read
	 * rights, usually a lecturer. The performing user may violate the enrollment period and the participants limit.
	 * However he/she is not allowed to enroll a user who is already enrolled in a linked course. Other permissions are
	 * not checked.
	 * 
	 * @param user
	 *            The user who will be enrolled
	 * @param offer
	 *            The linked course offer
	 * @param performedBy
	 *            The user who performs the action
	 * @param explanation
	 *            An optional explanation for the manual enrollment
	 * @throws EnrollmentException
	 *             If the performing user does not has the rights to manually enroll a user or if enrollment was not
	 *             allowed.
	 * @throws MessagingException
	 *             If there was an exception while sending the mail for the user
	 */
	public void enrollUserManually(User user, CourseOffer offer, User performedBy, String explanation)
			throws EnrollmentException, MessagingException {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		Objects.requireNonNull(performedBy);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);

		ensureLecturerRights(performedBy, offer);
		ensureNotEnrolled(user, offer);

		ensureNotEnrolledInLinkedCourse(user, offer);
		// Lecturers can ignore the other restrictions

		enroll(user, offer, performedBy, explanation);
		sendManuallyEnrollmentEmail(user, offer, explanation);
	}

	/**
	 * Disenrolls a user from a course.
	 * 
	 * @throws EnrollmentException
	 *             If disenrollment is not allowed.
	 * @see #checkDisenrollmentPermission(CourseOffer)
	 */
	public void disenrollUser(User user, CourseOffer offer) throws EnrollmentException {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);

		checkDisenrollmentPermission(offer);

		disenroll(user, offer, user, null);
	}

	/**
	 * Disenrolls a user from a course. The action is performed by a user that <strong>must</strong> have extended read
	 * rights, usually a lecturer. The performing user may violate the disenrollment deadline.
	 * 
	 * @param user
	 *            The user who will be disenrolled
	 * @param offer
	 *            The linked course offer
	 * @param performedBy
	 *            The user who performs the action
	 * @param explanation
	 *            An optional explanation for manual disenrollment
	 * @throws EnrollmentException
	 *             If the performing user does not has the required rights to manually disenroll the user.
	 * @throws MessagingException
	 *             If there was an exception while sending the mail for the user
	 */
	public void disenrollUserManually(User user, CourseOffer offer, User performedBy, String explanation)
			throws EnrollmentException, MessagingException {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		Objects.requireNonNull(performedBy);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);
		ensureLecturerRights(performedBy, offer);

		disenroll(user, offer, performedBy, explanation);
		sendManuallyDisenrollmentEmail(user, offer, explanation);
	}

	/**
	 * Puts a user on the waiting list.
	 * 
	 * @param user
	 *            The user who will join the waiting list
	 * @param offer
	 *            The linked course offer
	 * @throws EnrollmentException
	 *             If the course either does not provide a waiting list or the specific user is not allowed to interact
	 *             with the course.
	 * @throws PasswordRequiredException
	 *             When a password is required from the user to enroll. In this case, use
	 *             {@link #joinWaitingList(User, CourseOffer, String)} and pass the password.
	 * @throws NotInteractableException
	 *             If the course offer is not visible
	 * @throws MessagingException
	 *             If there was an exception while sending the mail for the user
	 * @see #checkWaitingListPermission(User, CourseOffer)
	 * @see #joinWaitingList(User, CourseOffer, String)
	 */
	public void joinWaitingList(User user, CourseOffer offer)
			throws EnrollmentException, PasswordRequiredException, NotInteractableException, MessagingException {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);

		// Check conditions
		ensureNoGlobalPassword(offer);
		checkWaitingListPermission(user, offer);

		// All conditions are checked here, so we can put the user on the waiting list.
		setStatus(user, offer, EEnrollmentStatus.ON_WAITINGLIST, user, null);

		if (offer.isWaitingListEmail()) {
			sendJoinedWaitingListEmail(user, offer);
		}
	}

	/**
	 * Puts a user on the waiting list with a passed password.
	 * 
	 * @param user
	 *            The user who will join the waiting list
	 * @param offer
	 *            The linked course offer
	 * @param password
	 *            Plaintext password entered by the user
	 * @throws EnrollmentException
	 *             If the course either does not provide a waiting list, the specific user is not allowed to interact
	 *             with the course or the password is wrong.
	 * @throws NotInteractableException
	 *             If the course offer is not visible
	 * @throws MessagingException
	 *             If there was an exception while sending the mail for the user
	 * @see #checkWaitingListPermission(User, CourseOffer)
	 */
	public void joinWaitingList(User user, CourseOffer offer, String password)
			throws EnrollmentException, NotInteractableException, MessagingException {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);

		// Check conditions
		ensureCorrectGlobalPassword(offer, password);
		checkWaitingListPermission(user, offer);

		// All conditions are checked here, so we can put the user on the waiting list.
		setStatus(user, offer, EEnrollmentStatus.ON_WAITINGLIST, user, null);

		if (offer.isWaitingListEmail()) {
			sendJoinedWaitingListEmail(user, offer);
		}
	}

	/**
	 * Sets the status of an {@link Enrollment} for the user and the course offer to {@link EEnrollmentStatus#ENROLLED}
	 * and returns a newly created {@link CourseRecord} if submission have started immediately.
	 */
	private Optional<CourseRecord> enroll(User user, CourseOffer offer, User performedBy, String explanation) {
		setStatus(user, offer, EEnrollmentStatus.ENROLLED, performedBy, explanation);
		return startCourseAfterEnrollment(user, offer);
	}

	/**
	 * Ensures that a user has the status {@link EEnrollmentStatus#DISENROLLED} for a course enrollment.
	 */
	private void disenroll(User user, CourseOffer offer, User performedBy, String explanation) {
		final Optional<Enrollment> found = enrollmentService.getEnrollment(user, offer);
		if (!found.isPresent()) {
			throw new IllegalStateException(
					user + " is to be disenrolled from " + offer + " but has never interacted with the course!");
		}

		final Enrollment enrollment = found.get();
		if (enrollment.getStatus() == EEnrollmentStatus.DISENROLLED) {
			// We don't have to do anything.
			return;
		}

		enrollment.updateStatus(EEnrollmentStatus.DISENROLLED, performedBy, explanation);
		enrollmentService.mergeEnrollment(enrollment);

		exitCourseAfterDisenrollment(user, offer);
		try {
			moveUpOneUser(offer);
		} catch (MessagingException e) {
			// This was already logged 
		}
	}

	/**
	 * Ensures that a course record is closed after a user was disenrolled from a course.
	 */
	private void exitCourseAfterDisenrollment(User user, CourseOffer offer) {
		final var courseRecord = courseRecordService.getOpenCourseRecordFor(user, offer);
		// We don't close the CourseRecord twice
		if (courseRecord.isPresent()) {
			courseRecord.get().closeAutomatically();
			courseRecordService.mergeCourseRecord(courseRecord.get());
		}
	}

	/**
	 * Moves a user from the waiting list to the {@link EEnrollmentStatus#ENROLLED} state if all of the following
	 * conditions apply:
	 * <ul>
	 * <li>Explicit enrollment is enabled and the course offer allows enrollment in general.</li>
	 * <li>The course offer has a limited number of participants <strong>and</strong> the waiting list is enabled.</li>
	 * <li>The course offer is not full.</li>
	 * </ul>
	 * In all other cases this method does nothing.
	 * 
	 * @param offer
	 *            Any course offer. It is safe to call this method even with course offers that do not meet the
	 *            conditions.
	 * @throws MessagingException
	 *             If at least one mail could not be sent. The Exception may be ignored by the caller.
	 */
	public void moveUpOneUser(CourseOffer offer) throws MessagingException {
		try {
			ensureWaitingListEnabled(offer);
			ensureWithinEnrollmentPeriod(offer);
			// If the course is still full, no user moves up. This could happen if lecturers manually have enrolled more
			// students than there were places available.
			ensureNotFull(offer);
		} catch (EnrollmentException e) {
			// No user moves up
			return;
		}

		// "ensureWaitingListEnabled" also ensured that the number of free places is limited.
		moveUpUsers(offer, 1);
	}

	/**
	 * Lets a certain number of users move up, but not more than are on the waiting list.
	 */
	private void moveUpUsers(final CourseOffer offer, final long moveUpCount) throws MessagingException {
		final List<Enrollment> waitingList = getWaitingList(offer);
		long i = 0;
		Enrollment toMoveUp;
		int failedEmails = 0;
		while (i < moveUpCount && !waitingList.isEmpty()) {

			// The first user on waiting list is enrolled
			toMoveUp = waitingList.remove(0);
			toMoveUp.updateStatus(EEnrollmentStatus.ENROLLED, null, null);
			enrollmentService.mergeEnrollment(toMoveUp);
			// Start submission if necessary
			startCourseAfterEnrollment(toMoveUp.getUser(), offer);

			// Send a mail to the user who moved up
			try {
				sendMoveUpEMail(toMoveUp.getUser(), offer);
			} catch (MessagingException e) {
				failedEmails++;
				getLogger()
						.warn("It was not possible to send an email to the user '" + toMoveUp.getUser().getLoginName()
								+ "' to inform him that he has moved up on the waiting list.");
			}
			i++;
		}

		if (failedEmails > 0) {
			throw new MessagingException(failedEmails + " emails could not be sent.");
		}
	}

	/**
	 * Changes the status for an existing {@link Enrollment} or creates a new one for the passed user and course.
	 */
	private void setStatus(User user, CourseOffer offer, EEnrollmentStatus status, User performedBy,
			String explanation) {
		final Optional<Enrollment> found = enrollmentService.getEnrollment(user, offer);
		if (found.isPresent()) {
			final Enrollment enrollment = found.get();
			enrollment.updateStatus(status, performedBy, explanation);
			enrollmentService.mergeEnrollment(enrollment);
		} else {
			final Enrollment enrollment = new Enrollment(user, offer, status, performedBy, explanation);
			enrollmentService.persistEnrollment(enrollment);
		}
	}

	/**
	 * Ensures that a course record is created after a user was enrolled in a course (only if submission starts
	 * implicitly).
	 */
	private Optional<CourseRecord> startCourseAfterEnrollment(User user, CourseOffer offer) {
		if (offer.getCourse() != null && !offer.isExplicitSubmission()) {
			// Implicit creation of a course submission
			return Optional.of(courseBusiness.createCourseRecord(user, offer));
		}
		return Optional.empty();
	}

	/**
	 * Enrolls users from the waiting list of a Course Offer if the participants limit was relaxed (users are enrolled
	 * until the Course Offer is full) or removed (all users are enrolled). Different to other Enrollment actions, the
	 * performing user must have <strong>WRITE</strong> rights. Disenrollment may violate the disenrollment deadline.
	 * 
	 * @param offer
	 *            The linked course offer
	 * @param performedBy
	 *            The user who performs the action
	 * @throws EnrollmentException
	 *             If the performing user does not have write rights on the Course Offer.
	 * @throws MessagingException
	 *             If at least one mail could not be sent. The Exception may be ignored by the caller.
	 * @see #disenrollUsersAfterSaving(CourseOffer, User)
	 */
	public void moveUpUsersAfterSaving(CourseOffer offer, User performedBy)
			throws EnrollmentException, MessagingException {
		Objects.requireNonNull(offer);
		Objects.requireNonNull(performedBy);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);
		ensureWriteRights(performedBy, offer);

		if (offer.isExplicitEnrollment()) {
			// Fill the free places or let all users move up if the limit was removed
			moveUpUsers(offer, getFreePlaces(offer).orElse(Long.MAX_VALUE));
		}
	}

	/**
	 * Disenrolls all users from the waiting list of a Course Offer if the waiting list is disabled but a participant
	 * limit is set. Different to other Enrollment actions, the performing user must have <strong>WRITE</strong> rights.
	 * Disenrollment may violate the disenrollment deadline.
	 * 
	 * @param offer
	 *            The linked course offer
	 * @param performedBy
	 *            The user who performs the action
	 * @throws EnrollmentException
	 *             If the performing user does not have write rights on the Course Offer.
	 * @throws MessagingException
	 *             If there was an exception while sending the mail for the user
	 * @see #moveUpUsersAfterSaving(CourseOffer, User)
	 */
	public void disenrollUsersAfterSaving(CourseOffer offer, User performedBy)
			throws EnrollmentException, MessagingException {
		Objects.requireNonNull(offer);
		Objects.requireNonNull(performedBy);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);
		ensureWriteRights(performedBy, offer);

		if (offer.isExplicitEnrollment() && offer.getMaxAllowedParticipants() > 0 && !offer.isEnableWaitingList()) {
			// All waiting users must be disenrolled
			final var waitingListEntries = enrollmentService.getEnrollments(offer, EEnrollmentStatus.ON_WAITINGLIST);
			int failedEmails = 0;
			for (Enrollment waitingListEntry : waitingListEntries) {
				disenroll(waitingListEntry.getUser(), offer, null, null);
				try {
					sendDisenrollmentEmailOnDisabledWaitingList(waitingListEntry.getUser(), offer);
				} catch (MessagingException e) {
					failedEmails++;
					getLogger().warnf(
							"It was not possible to send an email to the user '%s' to inform him that he was disenrolled because waiting list was disabled.",
							waitingListEntry.getUser().getLoginName());
				}
			}

			if (failedEmails > 0) {
				throw new MessagingException(failedEmails + " emails could not be sent.");
			}
		}
	}

	// #########################################################################
	// Enrollment - E-Mails
	// #########################################################################

	private void sendEnrollmentEmail(User user, CourseOffer courseOffer)
			throws MessagingException {

		if (!emailService.isReady()) {
			throw new MessagingException("No mail session available");
		}
		if (user.getEmail() == null) {
			throw new MessagingException(
					"The user does not have an email address. Because of this we can't send the email.");
		}
		
		final String language = getLanguageForEmail(courseOffer);

		final FacesContext facesContext = FacesContext.getCurrentInstance();
		final Application application = facesContext.getApplication();
		final String bundleName = application.getResourceBundle(facesContext, "msg").getBaseBundleName();
		final ResourceBundle bundle = ResourceBundle.getBundle(bundleName, Locale.forLanguageTag(language));
		final String courseOfferName = "'" + courseOffer.getName() + "'";

		//create a link to the CourseOffer
		String link = getEmailLinkToCourseOffer(courseOffer);

		//get localized subject and content of the mail
		String subject = bundle.getString("courseOffer.enrollmentEmail.subject");
		String content = bundle.getString("courseOffer.enrollmentEmail.content");
		
		//if there is an additional message for the mail add it to the content
		if (JackStringUtils.isNotBlank(courseOffer.getEnrollmentEmailText())) {
			content = MessageFormat.format(content, courseOfferName, link,
					bundle.getString("courseOffer.enrollmentEmail.lecturerMessage")
							+ courseOffer.getEnrollmentEmailText());
		}else {
			content = MessageFormat.format(content, courseOfferName, link, "");
		}

		//send the mail
		emailService.createMail().withRecipients(user.getEmail())
				.withSubject(MessageFormat.format(subject, courseOfferName))
				.withHtml(content)
				.send();
	}

	private void sendManuallyEnrollmentEmail(User user, CourseOffer courseOffer, String explanation)
			throws MessagingException {

		if (!emailService.isReady()) {
			throw new MessagingException("No mail session available");
		}
		if(user.getEmail() == null) {
			throw new MessagingException(
					"The user does not have an email address. Because of this we can't send the email.");
		}

		final String language = getLanguageForEmail(courseOffer);

		final FacesContext facesContext = FacesContext.getCurrentInstance();
		final Application application = facesContext.getApplication();
		final String bundleName = application.getResourceBundle(facesContext, "msg").getBaseBundleName();
		final ResourceBundle bundle = ResourceBundle.getBundle(bundleName, Locale.forLanguageTag(language));
		final String courseOfferName = "'" + courseOffer.getName() + "'";

		//create a link to the CourseOffer
		String link = getEmailLinkToCourseOffer(courseOffer);

		//get localized subject and content of the mail
		String subject = bundle.getString("courseOffer.manuallyEnrollmentEmail.subject");
		String content = bundle.getString("courseOffer.manuallyEnrollmentEmail.content");

		//Check if there is an enrollmentMessage
		String enrollmentMessage = "";
		if (courseOffer.isEnrollmentEmail() && JackStringUtils.isNotBlank(courseOffer.getEnrollmentEmailText())) {
			enrollmentMessage = bundle.getString("courseOffer.enrollmentEmail.lecturerMessage")
					+ courseOffer.getEnrollmentEmailText();
		}
		
		//Check if there is an explanation
		String explanationMessage = "";
		if (JackStringUtils.isNotBlank(explanation)) {
			explanationMessage = bundle.getString("courseOffer.manuallyEnrollmentEmail.enrollmentMessage")
					+ explanation;
			explanationMessage = explanationMessage.replace("\n", "<br/>");
		}

		content = MessageFormat.format(content, courseOfferName, link, enrollmentMessage, explanationMessage);

		//send the mail
		emailService.createMail() //
			.withRecipients(user.getEmail()) //
				.withSubject(MessageFormat.format(subject, courseOfferName))//
			.withHtml(content).send(); //
	}

	private void sendJoinedWaitingListEmail(User user, CourseOffer courseOffer) throws MessagingException {

		if (!emailService.isReady()) {
			throw new MessagingException("No mail session available");
		}
		if (user.getEmail() == null) {
			throw new MessagingException(
					"The user does not have an email address. Because of this we can't send the email.");
		}

		final String language = getLanguageForEmail(courseOffer);

		final FacesContext facesContext = FacesContext.getCurrentInstance();
		final Application application = facesContext.getApplication();
		final String bundleName = application.getResourceBundle(facesContext, "msg").getBaseBundleName();
		final ResourceBundle bundle = ResourceBundle.getBundle(bundleName, Locale.forLanguageTag(language));
		final String courseOfferName = "'" + courseOffer.getName() + "'";

		//create a link to the CourseOffer
		String link = getEmailLinkToCourseOffer(courseOffer);

		//get localized subject and content of the mail
		String subject = bundle.getString("courseOffer.waitingListEmail.subject");
		String content = bundle.getString("courseOffer.waitingListEmail.content");

		//Check if there is an waitingListMessage
		String waitingListMessage = "";
		if (courseOffer.isWaitingListEmail() && JackStringUtils.isNotBlank(courseOffer.getWaitingListEmailText())) {
			waitingListMessage = bundle.getString("courseOffer.enrollmentEmail.lecturerMessage")
					+ courseOffer.getWaitingListEmailText();
		}

		content = MessageFormat.format(content, courseOfferName, link, waitingListMessage);

		//send the mail
		emailService.createMail() //
				.withRecipients(user.getEmail()) //
				.withSubject(MessageFormat.format(subject, courseOfferName))//
				.withHtml(content).send(); //
	}

	private void sendManuallyDisenrollmentEmail(User user, CourseOffer courseOffer, String explanation)
			throws MessagingException {

		if (!emailService.isReady()) {
			throw new MessagingException("No mail session available");
		}
		if (user.getEmail() == null) {
			throw new MessagingException(
					"The user does not have an email address. Because of this we can't send the email.");
		}

		final String language = getLanguageForEmail(courseOffer);

		final FacesContext facesContext = FacesContext.getCurrentInstance();
		final Application application = facesContext.getApplication();
		final String bundleName = application.getResourceBundle(facesContext, "msg").getBaseBundleName();
		final ResourceBundle bundle = ResourceBundle.getBundle(bundleName, Locale.forLanguageTag(language));
		final String courseOfferName = "'" + courseOffer.getName() + "'";

		//create a link to the CourseOffer
		String link = getEmailLinkToCourseOffer(courseOffer);

		//get localized subject and content of the mail
		String subject = bundle.getString("courseOffer.manuallyDisenrollmentEmail.subject");
		String content = bundle.getString("courseOffer.manuallyDisenrollmentEmail.content");
		
		//Check if there is an explanation
		String explanationMessage = "";
		if (JackStringUtils.isNotBlank(explanation)) {
			explanationMessage = bundle.getString("courseOffer.manuallyDisenrollmentEmail.lecturerMessage")
					+ explanation;
			explanationMessage = explanationMessage.replace("\n", "<br/>");
		}

		content = MessageFormat.format(content, courseOfferName, link, explanationMessage);

		//send the mail
		emailService.createMail() //
				.withRecipients(user.getEmail()) //
				.withSubject(MessageFormat.format(subject, courseOfferName))//
				.withHtml(content).send(); //
	}

	private void sendDisenrollmentEmailOnDisabledWaitingList(User user, CourseOffer courseOffer)
			throws MessagingException {

		if (!emailService.isReady()) {
			throw new MessagingException("No mail session available");
		}
		if (user.getEmail() == null) {
			throw new MessagingException(
					"The user does not have an email address. Because of this we can't send the email.");
		}

		final String language = getLanguageForEmail(courseOffer);

		final FacesContext facesContext = FacesContext.getCurrentInstance();
		final Application application = facesContext.getApplication();
		final String bundleName = application.getResourceBundle(facesContext, "msg").getBaseBundleName();
		final ResourceBundle bundle = ResourceBundle.getBundle(bundleName, Locale.forLanguageTag(language));
		final String courseOfferName = "'" + courseOffer.getName() + "'";

		//create a link to the CourseOffer
		String link = getEmailLinkToCourseOffer(courseOffer);

		//get localized subject and content of the mail
		String subject = bundle.getString("courseOffer.manuallyDisenrollmentEmail.subject");
		String content = bundle.getString("courseOffer.manuallyDisenrollmentEmail.contentDisabledWaitlist");

		content = MessageFormat.format(content, courseOfferName, link);

		//send the mail
		emailService.createMail() //
				.withRecipients(user.getEmail()) //
				.withSubject(MessageFormat.format(subject, courseOfferName))//
				.withHtml(content).send(); //
	}

	private void sendMoveUpEMail(User user, CourseOffer courseOffer) throws MessagingException {

		if (!emailService.isReady()) {
			throw new MessagingException("No mail session available");
		}
		if (user.getEmail() == null) {
			throw new MessagingException(
					"The user does not have an email address. Because of this we can't send the email.");
		}

		final String language = getLanguageForEmail(courseOffer);

		final FacesContext facesContext = FacesContext.getCurrentInstance();
		final Application application = facesContext.getApplication();
		final String bundleName = application.getResourceBundle(facesContext, "msg").getBaseBundleName();
		final ResourceBundle bundle = ResourceBundle.getBundle(bundleName, Locale.forLanguageTag(language));
		final String courseOfferName = "'" + courseOffer.getName() + "'";

		//create a link to the CourseOffer
		String link = getEmailLinkToCourseOffer(courseOffer);

		//get localized subject and content of the mail
		String subject = bundle.getString("courseOffer.movedUpEmail.subject");
		String content = bundle.getString("courseOffer.movedUpEmail.content");

		//if there is an additional message for the mail add it to the content
		if (JackStringUtils.isNotBlank(courseOffer.getEnrollmentEmailText())) {
			content = MessageFormat.format(content, courseOfferName, link,
					bundle.getString("courseOffer.enrollmentEmail.lecturerMessage")
							+ courseOffer.getEnrollmentEmailText());
		} else {
			content = MessageFormat.format(content, courseOfferName, link, "");
		}

		//send the mail
		emailService.createMail() //
				.withRecipients(user.getEmail()) //
				.withSubject(MessageFormat.format(subject, courseOfferName)) //
				.withHtml(MessageFormat.format(content, courseOfferName, link)).send(); //
	}

	private String getEmailLinkToCourseOffer(CourseOffer courseOffer) {
		final FacesContext facesContext = FacesContext.getCurrentInstance();
		final ExternalContext externalContext = facesContext.getExternalContext();
		final Application application = facesContext.getApplication();

		String link = new StringBuilder() //
				.append(externalContext.getRequestScheme() + "://") //
				.append((JackStringUtils.isNotBlank(externalContext.getRequestServerName()))
								? externalContext.getRequestServerName() + ":"
								: "")
				.append(externalContext.getRequestServerPort()) //
				.append(application.getViewHandler().getActionURL(facesContext, "/user/courseMainMenu")) //
				.append("?courseOffer=" + courseOffer.getId()) //
				.toString();

		//display the link as a href so it is clickable
		return "<a href='" + link + "'>" + link + "</a>";
	}

	private String getLanguageForEmail(CourseOffer offer) {
		if (offer.getLanguage() != null) {
			return offer.getLanguage();
		}
		if (offer.getCourse() != null && offer.getCourse().getLanguage() != null) {
			return offer.getCourse().getLanguage();
		}
		return "de";
	}

	// #########################################################################
	// Enrollment - Preconditions
	// #########################################################################

	private void ensureVisible(CourseOffer offer) throws NotInteractableException {
		final LocalDateTime now = LocalDateTime.now();
		if (offer.getVisibilityStartTime() != null && now.isBefore(offer.getVisibilityStartTime())) {
			throw new NotInteractableException();
		}
		if (offer.getVisibilityEndTime() != null && now.isAfter(offer.getVisibilityEndTime())) {
			throw new NotInteractableException();
		}
		if (!offer.isCanBeVisible()) {
			throw new NotInteractableException();
		}
	}

	private void ensureInteractable(User user, CourseOffer offer) throws NotInteractableException {
		// Course offer is visible if user is on allowlist in allowlist mode
		if (offer.isToggleAllowlist() && !offer.matchFilter(user)) {
			throw new NotInteractableException();
		}
		// Course offer is not visible if user is on blocklist in blocklist mode
		if (!offer.isToggleAllowlist() && offer.matchFilter(user)) {
			throw new NotInteractableException();
		}
	}

	private void ensureWithinEnrollmentPeriod(CourseOffer offer) throws EnrollmentException {
		final LocalDateTime now = LocalDateTime.now();
		if (offer.isExplicitEnrollment() && offer.getEnrollmentStart() != null
				&& now.isBefore(offer.getEnrollmentStart())) {
			// Enrollment has not started yet
			throw new EnrollmentException(EnrollmentException.EType.ENROLLMENT_NOT_STARTED);
		}
		if (offer.isExplicitEnrollment() && offer.getEnrollmentDeadline() != null
				&& now.isAfter(offer.getEnrollmentDeadline())) {
			// Enrollment deadline is already over
			throw new EnrollmentException(EnrollmentException.EType.ENROLLMENT_DEADLINE_ELAPSED);
		}
	}

	private void ensureNoGlobalPassword(CourseOffer offer) throws PasswordRequiredException {
		if (offer.isExplicitEnrollment() && JackStringUtils.isNotBlank(offer.getGlobalPassword())) {
			// A global password was defined for the course offer but was not requested by the caller
			throw new PasswordRequiredException();
		}
	}

	private void ensureCorrectGlobalPassword(CourseOffer offer, String password) throws EnrollmentException {
		if (offer.isExplicitEnrollment() && JackStringUtils.isNotBlank(offer.getGlobalPassword())
				&& !offer.getGlobalPassword().equals(password)) {
			// The entered password is wrong
			throw new EnrollmentException(EnrollmentException.EType.PASSWORD_WRONG);
		}
	}

	private void ensureNotEnrolled(User user, CourseOffer offer) throws EnrollmentException {
		if (isEnrolled(user, offer)) {
			throw new EnrollmentException(EnrollmentException.EType.ALREADY_ENROLLED);
		}
	}

	private void ensureNotFull(CourseOffer offer) throws EnrollmentException {
		if (offer.isExplicitEnrollment() && !hasFreePlaces(offer)) {
			throw new EnrollmentException(EnrollmentException.EType.COURSE_IS_FULL);
		}
	}

	private void ensureFull(CourseOffer offer) throws EnrollmentException {
		if (offer.isExplicitEnrollment() && hasFreePlaces(offer)) {
			throw new EnrollmentException(EnrollmentException.EType.COURSE_NOT_FULL);
		}
	}

	private void ensureWaitingListEnabled(CourseOffer offer) throws EnrollmentException {
		if (!offer.isExplicitEnrollment() || offer.getMaxAllowedParticipants() == 0
				|| !offer.isEnableWaitingList()) {
			throw new EnrollmentException(EnrollmentException.EType.WAITINGLIST_DISABLED);
		}
	}

	private void ensureWithinDisenrollmentPeriod(CourseOffer offer) throws EnrollmentException {
		final LocalDateTime now = LocalDateTime.now();
		if (offer.isExplicitEnrollment() && offer.getDisenrollmentDeadline() != null
				&& now.isAfter(offer.getDisenrollmentDeadline())) {
			// Disenrollment deadline is already over
			throw new EnrollmentException(EnrollmentException.EType.DISENROLLMENT_DEADLINE_ELAPSED);
		}
	}

	private void ensureLecturerRights(User user, CourseOffer offer) throws EnrollmentException {
		if (!authBusiness.hasExtendedReadOnFolder(user, offer.getFolder())) {
			throw new EnrollmentException(EnrollmentException.EType.MISSING_RIGHT);
		}
	}

	private void ensureWriteRights(User user, CourseOffer offer) throws EnrollmentException {
		if (!authBusiness.isAllowedToEditFolder(user, offer.getFolder())) {
			throw new EnrollmentException(EnrollmentException.EType.MISSING_RIGHT);
		}
	}

	private void ensureNotEnrolledInLinkedCourse(User user, CourseOffer offer) throws LinkedCourseException {
		final PresentationFolder folder = folderService.getPresentationFolderById(offer.getFolder().getId())
				.orElseThrow(NoSuchJackEntityException::new);

		if (!offer.isExplicitEnrollment()) {
			// If explicit enrollment is disabled, the linked courses feature is not available.
			return;
		}

		final var highestLinkedFolder = folderBusiness.getHighestLinkedCourseFolder(folder);
		if (highestLinkedFolder.isEmpty()) {
			// There are no linked courses
			return;
		}

		final var foldersToSearch = folderBusiness.getAllChildPresentationFolders(highestLinkedFolder.get(), true);
		final var enrollments = enrollmentService.getEnrollmentsForUserAndFoldersUnordered(user, foldersToSearch);
		final Optional<CourseOffer> enrolledIn = enrollments.stream()
				.filter(enr -> !enr.getCourseOffer().equals(offer)) // Only courseoffers not equal to the parameter
				.filter(enr -> enr.getStatus() != EEnrollmentStatus.DISENROLLED) // Only ENROLLED and ON_WAITINGLIST
				.map(Enrollment::getCourseOffer)
				.findAny();
		if (enrolledIn.isPresent()) {
			// The user is already enrolled in a linked course or is on the waiting list
			throw new LinkedCourseException(enrolledIn.get());
		}
		// No active enrollment in a parallel course was found
	}

	// #########################################################################
	// Enrollment - Rights & Checks
	// #########################################################################

	/**
	 * Returns wether a course offer is visible by time constraints and the user is allowed to interact with the course
	 * offer. This is the case if his/her username is not filtered by allowlist/blocklist mode.
	 */
	public boolean isCourseOfferVisibleForStudent(User user, CourseOffer offer) {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);

		try {
			// Course offer is not visible at all
			ensureVisible(offer);
		} catch (NotInteractableException e) {
			return false;
		}

		try {
			ensureInteractable(user, offer);
		} catch (NotInteractableException e) {
			// An enrolled student should see the course even if he/she is filtered. In this case, the student was
			// enrolled manually.
			return isEnrolled(user, offer);
		}

		return true;
	}

	/**
	 * Checks that
	 * <ul>
	 * <li>a course offer is visible in general</li>
	 * <li>the passed student student can interact with the course offer so his/her username is not filtered by
	 * allowlist/blocklist mode</li>
	 * <li>the passed student is not already enrolled in this course or a linked one</li>
	 * <li>the current date is within the enrollment period</li>
	 * <li>the course still has free places</li>
	 * </ul>
	 * 
	 * @throws EnrollmentException
	 *             with the reason if the conditions are not met
	 * @throws NotInteractableException
	 *             If the course offer is not visible to the user
	 */
	public void checkEnrollmentPermission(User user, CourseOffer offer)
			throws EnrollmentException, NotInteractableException {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);

		ensureVisible(offer);
		ensureInteractable(user, offer);
		ensureNotEnrolled(user, offer);
		ensureNotEnrolledInLinkedCourse(user, offer);
		ensureWithinEnrollmentPeriod(offer);
		ensureNotFull(offer);
	}

	/**
	 * Checks that
	 * <ul>
	 * <li>a course offer is visible in general</li>
	 * <li>the passed student student can interact with the course offer so his/her username is not filtered by
	 * allowlist/blocklist mode</li>
	 * <li>the passed student is not already enrolled in this course or a linked one</li>
	 * <li>the current date is within the enrollment period</li>
	 * <li>the course is full</li>
	 * </ul>
	 * 
	 * @throws EnrollmentException
	 *             with the reason if the conditions are not met
	 * @throws NotInteractableException
	 *             If the course offer is not visible to the user
	 */
	public void checkWaitingListPermission(User user, CourseOffer offer)
			throws EnrollmentException, NotInteractableException {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);

		ensureVisible(offer);
		ensureInteractable(user, offer);
		ensureNotEnrolled(user, offer);
		ensureWithinEnrollmentPeriod(offer);
		ensureNotEnrolledInLinkedCourse(user, offer);
		ensureWaitingListEnabled(offer);
		ensureFull(offer); // A course must be full to activate the waiting list!
	}

	/**
	 * Checks that the disenrollment deadline is not over.
	 * 
	 * @throws EnrollmentException
	 *             If this is not the case
	 */
	public void checkDisenrollmentPermission(CourseOffer offer) throws EnrollmentException {
		Objects.requireNonNull(offer);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);

		ensureWithinDisenrollmentPeriod(offer);
	}

	/**
	 * The user is redirected and skips the main menu if all of the following conditions apply:
	 * <ul>
	 * <li>No explicit enrollment or submission is required. In this case, the main menu does not show any information.
	 * Whenever a user enters the course, he or she is automatically enrolled and a new {@link CourseRecord} is
	 * automatically generated.</li>
	 * <li>The course offer is linked with a course. Otherwise the course is only for enrollment.</li>
	 * <li>The user has not already participated in the course. Otherwise he or she has closed course records and the
	 * main menu displays the old participations for reviewing.</li>
	 * </ul>
	 * Special case: If the user has an open submission , redirecting is performed immediately.
	 * 
	 * @return The CourseRecord to which the user should be forwarded. It may be automatically created. Empty optional
	 *         if the user should not be forwarded.
	 * @throws NotInteractableException
	 *             If the course is not visible
	 */
	public Optional<CourseRecord> performAutomaticRedirect(User user, CourseOffer offer)
			throws NotInteractableException {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);

		Optional<CourseRecord> openCourseRecord = getOpenCourseRecord(user, offer);
		if (openCourseRecord.isPresent()) {
			// Open submission
			// In case of a required personal password, the showCourseRecord page takes over the password prompt
			return openCourseRecord;
		}

		if (offer.isExplicitEnrollment() || offer.isExplicitSubmission() || offer.getCourse() == null) {
			// The main menu is required
			return Optional.empty();
		}

		if (hasAlreadyParticipated(user, offer)) {
			// The main menu is shown in case of old submissions, so that the the user can review his/her submissions
			return Optional.empty();
		}

		// No open course record exists, but redirecting is enabled: The course is implicitely started
		try {
			return Optional.of(startSubmission(user, offer));
		} catch (SubmissionException | PasswordRequiredException e) {
			// If an error occurred, redirecting is not possible
			return Optional.empty();
		}
	}

	/**
	 * A global password is required if explicit enrollment is enabled and a global password was set.
	 */
	public boolean isGlobalPasswordRequired(CourseOffer offer) {
		Objects.requireNonNull(offer);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);
		try {
			ensureNoGlobalPassword(offer);
		} catch (PasswordRequiredException e) {
			return true;
		}
		return false;
	}

	// #########################################################################
	// Enrollment - Queries
	// #########################################################################

	/**
	 * @return {@code TRUE} if the passed user is enrolled in the course.
	 */
	public boolean isEnrolled(User user, CourseOffer offer) {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		return enrollmentService.getEnrollment(user, offer)
				.map(enrollment -> enrollment.getStatus() == EEnrollmentStatus.ENROLLED)
				.orElse(false);
	}

	/**
	 * @return {@code TRUE} if the passed user is not enrolled in the course and not on the waiting list.
	 */
	public boolean isDisenrolled(User user, CourseOffer offer) {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		return enrollmentService.getEnrollment(user, offer)
				.map(enrollment -> enrollment.getStatus() == EEnrollmentStatus.DISENROLLED)
				.orElse(true);
	}

	/**
	 * @return {@code TRUE} if the passed user is on the waiting list of the course.
	 */
	public boolean isOnWaitingList(User user, CourseOffer offer) {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		return enrollmentService.getEnrollment(user, offer)
				.map(enrollment -> enrollment.getStatus() == EEnrollmentStatus.ON_WAITINGLIST)
				.orElse(false);
	}

	/**
	 * @return Number of free places or empty optional if course offer has no participant limit.
	 */
	public Optional<Long> getFreePlaces(CourseOffer offer) {
		Objects.requireNonNull(offer);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);
		int max = offer.getMaxAllowedParticipants();
		if (!offer.isExplicitEnrollment() || max == 0) {
			// No limit
			return Optional.empty();
		}

		long enrolled = enrollmentService.countEnrollments(offer, EEnrollmentStatus.ENROLLED);
		// We have to ensure that the value is non-negative because lecturers can override the restriction so that more
		// users can be enrolled than allowed.
		return Optional.of(Math.max(0, max - enrolled));
	}

	/**
	 * @return {@code TRUE} if the course offer has free places.
	 * 
	 * @see #getFreePlaces(CourseOffer)
	 */
	public boolean hasFreePlaces(CourseOffer offer) {
		Objects.requireNonNull(offer);
		return getFreePlaces(offer).orElse(1L) > 0;
	}

	/**
	 * @return Current waiting list of the passed course offer. The list is sorted in ascending order by entry date,
	 *         i.e. users at the top of the list move up first.
	 */
	public List<Enrollment> getWaitingList(CourseOffer offer) {
		Objects.requireNonNull(offer);
		List<Enrollment> result = enrollmentService.getEnrollments(offer, EEnrollmentStatus.ON_WAITINGLIST);
		result.sort(Comparator.comparing(Enrollment::getLastChange));
		return result;
	}

	/**
	 * @return All enrollments that exist for the course offer with all users who have interacted with the course at
	 *         some point.
	 */
	public List<Enrollment> getAllParticipations(CourseOffer offer) {
		Objects.requireNonNull(offer);
		return enrollmentService.getEnrollments(offer);
	}

	/**
	 * @return All enrollments that exist for the course offers with all users who have interacted with at least one of
	 *         the course at some point.
	 */
	public List<Enrollment> getAllParticipations(List<CourseOffer> offers) {
		Objects.requireNonNull(offers);
		return enrollmentService.getEnrollments(offers);
	}

	/**
	 * @return Number of users who are currently enrolled.
	 */
	public long countCurrentEnrollments(CourseOffer offer) {
		Objects.requireNonNull(offer);
		return enrollmentService.countEnrollments(offer, EEnrollmentStatus.ENROLLED);
	}

	/**
	 * @return Number of users who are currently on waitinglist.
	 */
	public long countCurrentWaitinglist(CourseOffer offer) {
		Objects.requireNonNull(offer);
		return enrollmentService.countEnrollments(offer, EEnrollmentStatus.ON_WAITINGLIST);
	}

	/**
	 * @return All users who are currently enrolled.
	 */
	public List<Enrollment> getCurrentEnrollments(CourseOffer offer) {
		Objects.requireNonNull(offer);
		return enrollmentService.getEnrollments(offer, EEnrollmentStatus.ENROLLED);
	}

	/**
	 * @return All users who are currently disenrolled.
	 */
	public List<Enrollment> getDisenrolledEnrollments(CourseOffer offer) {
		Objects.requireNonNull(offer);
		return enrollmentService.getEnrollments(offer, EEnrollmentStatus.DISENROLLED);
	}

	/**
	 * @return All participations for a specific user, containing all enrollments for the user and an optional
	 *         submission if the user has an open one.
	 */
	public List<CourseParticipation> getVisibleParticipationsForUser(User user) {
		Objects.requireNonNull(user);
		
		final List<Enrollment> allEnrollments = enrollmentService.getEnrollments(user);
		final List<CourseParticipation> result = new ArrayList<>(allEnrollments.size());
		for (Enrollment enrollment : allEnrollments) {

			// Filter invisible course offers
			if (!isCourseOfferVisibleForStudent(user, enrollment.getCourseOffer())) {
				continue;
			}

			Optional<CourseRecord> openCourseRecord = getOpenCourseRecord(user, enrollment.getCourseOffer());
			if (openCourseRecord.isPresent()) {
				result.add(new CourseParticipation(enrollment, openCourseRecord.get()));
			} else {
				result.add(new CourseParticipation(enrollment));
			}
		}
		return result;
	}

	/**
	 * @return An enrollment for the passed user and the passed course offer (if existing) or empty {@link Optional} if
	 *         the user has had never interacted with the course before.
	 */
	public Optional<Enrollment> getEnrollment(User user, CourseOffer offer) {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		return enrollmentService.getEnrollment(user, offer);
	}

	/**
	 * @return The status the passed user has regarding the course. {@link EEnrollmentStatus#DISENROLLED} if the user
	 *         has never interacted with the course before.
	 */
	@Nonnull
	public EEnrollmentStatus getStatus(User user, CourseOffer offer) {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		return enrollmentService
				.getEnrollment(user, offer)
				.map(Enrollment::getStatus)
				.orElse(EEnrollmentStatus.DISENROLLED);
	}

	/**
	 * Returns a log of all enrollments a user has had over time.
	 * 
	 * @return List of all enrollment states for a user and a course offer or empty list if the user has never
	 *         interacted with the course offer.
	 */
	public List<EnrollmentLogEntry> getEnrollmentLog(User user, CourseOffer offer) {
		final Optional<Enrollment> enrollment = getEnrollment(user, offer);
		if (!enrollment.isPresent()) {
			return Collections.emptyList();
		}
		final List<Enrollment> logFromEnvers = revisionService.getAllRevisionsForEntity(enrollment.get());
		final List<EnrollmentLogEntry> result = new ArrayList<>(logFromEnvers.size());
		for (final Enrollment logEntry : logFromEnvers) {
			final User changedBy = (User) Hibernate.unproxy(logEntry.getLastChangedBy());
			result.add(new EnrollmentLogEntry(user, logEntry.getStatus(), logEntry.getLastChange(), changedBy,
					logEntry.getExplanation()));
		}

		return result;
	}

	// #########################################################################
	// Submission - Actions
	// #########################################################################

	/**
	 * A user starts a new submission on a course. If the user is not enrolled yet and the course does not force an
	 * explicit enrollment, the user additionally will be enrolled automatically.
	 * 
	 * @param user
	 *            The user who starts the submission
	 * @param offer
	 *            The linked course offer
	 * @return The newly created course record
	 * @throws PasswordRequiredException
	 *             If a personal password is required from the user to start the course. In this case, use
	 *             {@link #startSubmission(User, CourseOffer, String)} instead and pass the password.
	 * @throws SubmissionException
	 *             If the course either does not allow starting in general or the specific user is not allowed to start
	 *             the course.
	 * @throws NotInteractableException
	 *             If the course offer is not visible
	 * @see #checkStartSubmissionPermission(User, CourseOffer)
	 * @see #startSubmission(User, CourseOffer, String)
	 */
	public CourseRecord startSubmission(User user, CourseOffer offer)
			throws PasswordRequiredException, SubmissionException, NotInteractableException {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);

		// Check conditions
		ensureNoPersonalPassword(offer);
		checkStartSubmissionPermission(user, offer);

		enrollOnStartSubmission(user, offer);
		return courseBusiness.createCourseRecord(user, offer);
	}

	/**
	 * A user starts a new submission on a course and creates a new course record with an entered personal password. If
	 * the user is not enrolled yet and the course does not force an explicit enrollment, the user additionally will be
	 * enrolled automatically.
	 * 
	 * @param user
	 *            The user who starts the submission
	 * @param offer
	 *            The linked course offer
	 * @param password
	 *            Plaintext password entered by the user
	 * @return The newly created course record
	 * @throws SubmissionException
	 *             If the course either does not allow starting in general, the specific user is not allowed to start
	 *             the course or the password is wrong.
	 * @throws NotInteractableException
	 *             If the course offer is not visible
	 * @see #checkStartSubmissionPermission(User, CourseOffer)
	 */
	public CourseRecord startSubmission(User user, CourseOffer offer, String password)
			throws SubmissionException, NotInteractableException {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);

		// Check conditions
		ensureCorrectPersonalPassword(user, offer, password);
		checkStartSubmissionPermission(user, offer);

		enrollOnStartSubmission(user, offer);
		return courseBusiness.createCourseRecord(user, offer);
	}

	/**
	 * Restarts a course submission. The existing course record will be closed and a new one will be created. The
	 * enrollment state is not changed.
	 * 
	 * @param user
	 *            The user who restarts the course
	 * @param offerParam
	 *            The linked course offer
	 * @return The newly created course record
	 * @throws SubmissionException
	 *             If the course either does not allow restarting in general, the specific user is not allowed to
	 *             restart the course.
	 * @throws NotInteractableException
	 *             If the course offer is not visible
	 * @throws PasswordRequiredException
	 *             When a (personal) password is required from the user to restart. In this case, use
	 *             {@link #restartCourse(User, CourseOffer, String)} and pass the password.
	 * @see #checkRestartCoursePermission(User, CourseOffer)
	 * @see #restartCourse(User, CourseOffer, String)
	 */
	public CourseRecord restartCourse(User user, CourseOffer offerParam)
			throws SubmissionException, NotInteractableException, PasswordRequiredException {

		Objects.requireNonNull(user);
		Objects.requireNonNull(offerParam);
		// We need a new effectively final local variable because of the lambda expression below
		final CourseOffer offer = courseOfferService.getCourseOfferById(offerParam.getId())
				.orElseThrow(NoSuchJackEntityException::new);

		// Check conditions
		ensureNoPersonalPassword(offer);
		checkRestartCoursePermission(user, offer);

		// "checkRestartCoursePermission" ensured that there is an open course record!
		final CourseRecord record = courseRecordService.getOpenCourseRecordFor(user, offer).orElseThrow(
				() -> new IllegalStateException(user + " restarts " + offer + " but there is no open course record."));
		record.closeManually();
		courseRecordService.mergeCourseRecord(record);
		return courseBusiness.createCourseRecord(user, offer);
	}

	/**
	 * Restarts a course submission. The existing course record will be closed and a new one will be created. The
	 * enrollment state is not changed.
	 * 
	 * @param user
	 *            The user who restarts the course
	 * @param offerParam
	 *            The linked course offer
	 * @param password
	 *            Plaintext password entered by the user
	 * @return The newly created course record
	 * @throws SubmissionException
	 *             If the course either does not allow restarting in general, the specific user is not allowed to
	 *             restart the course or the password is wrong.
	 * @throws NotInteractableException
	 *             If the course offer is not visible
	 * @see #restartCourse(User, CourseOffer)
	 */
	public CourseRecord restartCourse(User user, CourseOffer offerParam, String password)
			throws SubmissionException, NotInteractableException {

		Objects.requireNonNull(user);
		Objects.requireNonNull(offerParam);
		// We need a new effectively final local variable because of the lambda expression below
		final CourseOffer offer = courseOfferService.getCourseOfferById(offerParam.getId())
				.orElseThrow(NoSuchJackEntityException::new);

		// Check conditions
		ensureCorrectPersonalPassword(user, offer, password);
		checkRestartCoursePermission(user, offer);

		// "checkRestartCoursePermission" ensured that there is an open course record!
		final CourseRecord record = courseRecordService.getOpenCourseRecordFor(user, offer).orElseThrow(
				() -> new IllegalStateException(user + " restarts " + offer + " but there is no open course record."));
		record.closeManually();
		courseRecordService.mergeCourseRecord(record);
		return courseBusiness.createCourseRecord(user, offer);
	}

	/**
	 * Continues a current course submission.
	 * 
	 * @param user
	 *            The participating user
	 * @param offerParam
	 *            The linked course offer
	 * @return The existing course record
	 * @throws SubmissionException
	 *             If the user is not allowed to visit the current submission
	 * @throws NotInteractableException
	 *             If the course offer is not visible
	 * @throws PasswordRequiredException
	 *             When a (personal) password is required from the user. In this case, use
	 *             {@link #continueCourse(User, CourseOffer, String)} and pass the password.
	 * @see #checkContinueCoursePermission(User, CourseOffer)
	 * @see #continueCourse(User, CourseOffer, String)
	 */
	public CourseRecord continueCourse(User user, CourseOffer offerParam)
			throws SubmissionException, NotInteractableException, PasswordRequiredException {

		Objects.requireNonNull(user);
		Objects.requireNonNull(offerParam);
		// We need a new effectively final local variable because of the lambda expression below
		final CourseOffer offer = courseOfferService.getCourseOfferById(offerParam.getId())
				.orElseThrow(NoSuchJackEntityException::new);

		// Check conditions
		ensureNoPersonalPassword(offer);
		checkContinueCoursePermission(user, offer);

		// "checkRestartCoursePermission" ensured that there is an open course record!
		return courseRecordService.getOpenCourseRecordFor(user, offer).orElseThrow(
				() -> new IllegalStateException(user + " continues " + offer + " but there is no open course record."));
	}

	/**
	 * Continues a current course submission.
	 * 
	 * @param user
	 *            The participating user
	 * @param offerParam
	 *            The linked course offer
	 * @param password
	 *            Plaintext password entered by the user
	 * @return The existing course record
	 * @throws SubmissionException
	 *             If the user is not allowed to visit the current submission
	 * @throws NotInteractableException
	 *             If the course offer is not visible
	 * @see #continueCourse(User, CourseOffer)
	 */
	public CourseRecord continueCourse(User user, CourseOffer offerParam, String password)
			throws SubmissionException, NotInteractableException {

		Objects.requireNonNull(user);
		Objects.requireNonNull(offerParam);
		// We need a new effectively final local variable because of the lambda expression below
		final CourseOffer offer = courseOfferService.getCourseOfferById(offerParam.getId())
				.orElseThrow(NoSuchJackEntityException::new);

		// Check conditions
		ensureCorrectPersonalPassword(user, offer, password);
		checkContinueCoursePermission(user, offer);

		// "checkRestartCoursePermission" ensured that there is an open course record!
		return courseRecordService.getOpenCourseRecordFor(user, offer).orElseThrow(
				() -> new IllegalStateException(user + " continues " + offer + " but there is no open course record."));
	}

	/**
	 * Closes the passed course record manually. If the course offer does not force an explicit enrollment, the user
	 * will be disenrolled automatically.
	 * 
	 * @return The updated course record
	 */
	public CourseRecord closeSubmissionManually(CourseRecord record) {
		Objects.requireNonNull(record);
		record = courseRecordService.getCourseRecordById(record.getId()).orElseThrow(NoSuchJackEntityException::new);
		if (record.isClosed()) {
			// We don't close the CourseRecord twice
			return record;
		}
		final Optional<CourseOffer> offer = record.getCourseOffer();
		if (offer.isPresent()) {
			disenrollOnCloseSubmission(record.getUser(), offer.get());
		}

		record.closeManually();
		return courseRecordService.mergeCourseRecord(record);
	}

	/**
	 * Closes the passed course record manually. The action is performed by a user that <strong>must</strong> have
	 * extended read rights, usually a lecturer. If the course offer does not force an explicit enrollment, the user
	 * will be disenrolled automatically.
	 * 
	 * @return The updated course record
	 */
	public CourseRecord closeSubmissionManually(CourseRecord record, User performedBy, String explanation) {
		Objects.requireNonNull(record);
		Objects.requireNonNull(performedBy);
		record = courseRecordService.getCourseRecordById(record.getId()).orElseThrow(NoSuchJackEntityException::new);
		if (record.isClosed()) {
			// We don't close the CourseRecord twice
			return record;
		}
		final Optional<CourseOffer> offer = record.getCourseOffer();
		if (offer.isPresent()) {
			disenrollOnCloseSubmission(record.getUser(), offer.get());
		}

		record.closeManually(performedBy, explanation);
		return courseRecordService.mergeCourseRecord(record);
	}

	/**
	 * Ensures that the user is enrolled while starting a course if the course does not force an explicit enrollment.
	 */
	private void enrollOnStartSubmission(User user, CourseOffer offer) {
		if (!offer.isExplicitEnrollment()) {
			setStatus(user, offer, EEnrollmentStatus.ENROLLED, null, null);
		}
	}

	/**
	 * Ensures that the user is disenrolled while closing an existing course record if the course does not force an
	 * explicit enrollment.
	 */
	private void disenrollOnCloseSubmission(User user, CourseOffer offer) {
		if (!offer.isExplicitEnrollment()) {
			final Enrollment enrollment = enrollmentService.getEnrollment(user, offer)
					.orElseThrow(() -> new IllegalStateException(user + " should be disenrolled after exiting " + offer
							+ " but no enrollment was present! All users who participate in a course must have an Enrollment!"));
			// No user moves up because implicit enrollment implies that no waiting list is available.
			enrollment.updateStatus(EEnrollmentStatus.DISENROLLED, null, null);
			enrollmentService.mergeEnrollment(enrollment);
		}
	}

	// #########################################################################
	// Submission - Preconditions
	// #########################################################################

	private void ensureWithinSubmissionPeriod(CourseOffer offer) throws SubmissionException {
		final LocalDateTime now = LocalDateTime.now();
		if (offer.isExplicitSubmission() && offer.getSubmissionStart() != null
				&& now.isBefore(offer.getSubmissionStart())) {
			// Submission not started yet
			throw new SubmissionException(SubmissionException.EType.SUBMISSION_NOT_STARTED);
		}
		if (offer.isExplicitSubmission() && offer.getSubmissionDeadline() != null
				&& now.isAfter(offer.getSubmissionDeadline())) {
			// Submission deadline is already over
			throw new SubmissionException(SubmissionException.EType.SUBMISSION_DEADLINE_ELAPSED);
		}
	}

	private void ensureNoPersonalPassword(CourseOffer offer) throws PasswordRequiredException {
		if (offer.isExplicitSubmission() && offer.isEnablePersonalPasswords()) {
			throw new PasswordRequiredException();
		}
	}

	private void ensureCorrectPersonalPassword(User user, CourseOffer offer, String password)
			throws SubmissionException {
		if (offer.isExplicitSubmission() && offer.isEnablePersonalPasswords()
				&& (!offer.getPersonalPasswords().containsKey(user)
						|| !offer.getPersonalPasswords().get(user).equals(password))) {
			throw new SubmissionException(SubmissionException.EType.PASSWORD_WRONG);
		}
	}

	private void ensureOfferHasCourse(CourseOffer offer) throws SubmissionException {
		if (offer.getCourse() == null) {
			throw new SubmissionException(SubmissionException.EType.NO_COURSE);
		}
	}

	private void ensureOpenCourseRecord(User user, CourseOffer offer) throws SubmissionException {
		if (!courseRecordService.getOpenCourseRecordFor(user, offer).isPresent()) {
			throw new SubmissionException(SubmissionException.EType.NO_OPEN_COURSE_RECORD);
		}
	}

	private void ensureNoOpenCourseRecord(User user, CourseOffer offer) throws SubmissionException {
		if (courseRecordService.getOpenCourseRecordFor(user, offer).isPresent()) {
			throw new SubmissionException(SubmissionException.EType.OPEN_COURSE_RECORD);
		}
	}

	/**
	 * Ensures that a course is restartable or if the user has not already participated.
	 */
	private void ensureRestartable(User user, CourseOffer offer) throws SubmissionException {
		if (offer.isExplicitSubmission() && offer.isOnlyOneParticipation()
				&& hasAlreadyParticipated(user, offer)) {
			throw new SubmissionException(SubmissionException.EType.ALREADY_PARTICIPATED);
		}
	}

	/**
	 * Ensures that a student is enrolled
	 */
	private void ensureEnrolled(User user, CourseOffer offer) throws SubmissionException {
		if (!isEnrolled(user, offer)) {
			throw new SubmissionException(SubmissionException.EType.NOT_ENROLLED);
		}
	}

	/**
	 * Ensures that a student is enrolled if explicit enrollment is forced.
	 */
	private void ensureEnrolledForNewSubmission(User user, CourseOffer offer) throws SubmissionException {
		if (offer.isExplicitEnrollment() && !isEnrolled(user, offer)) {
			throw new SubmissionException(SubmissionException.EType.NOT_ENROLLED);
		}
	}

	// #########################################################################
	// Submission - Rights & Checks
	// #########################################################################

	/**
	 * A student is allowed to start a course if
	 * <ul>
	 * <li>the student has no open submission in the course</li>
	 * <li>the course is visible in general</li>
	 * <li>the student is enrolled or the course does not force an explicit enrollment</li>
	 * <li>the course offer is linked to a course</li>
	 * <li>the actual time is within the submission period of the course offer</li>
	 * <li>the student has not already participated in the course or the course allows a restart</li>
	 * </ul>
	 * 
	 * @throws SubmissionException
	 *             In all other error cases
	 * @throws NotInteractableException
	 *             If the course offer is not visible
	 */
	public void checkStartSubmissionPermission(User user, CourseOffer offer)
			throws SubmissionException, NotInteractableException {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);

		// Check conditions
		ensureNoOpenCourseRecord(user, offer);
		ensureVisible(offer);
		ensureEnrolledForNewSubmission(user, offer);
		ensureOfferHasCourse(offer);
		ensureWithinSubmissionPeriod(offer);
		ensureRestartable(user, offer);
	}

	/**
	 * A student is allowed to restart a course if
	 * <ul>
	 * <li>the student has an open submission in the course</li>
	 * <li>the course is visible in general</li>
	 * <li>the student is enrolled</li>
	 * <li>the course offer is linked to a course</li>
	 * <li>the actual time is within the submission period of the course offer</li>
	 * <li>the student has not already participated in the course or the course allows a restart</li>
	 * </ul>
	 * 
	 * @throws SubmissionException
	 *             In all other error cases
	 * @throws NotInteractableException
	 *             If the course offer is not visible
	 */
	public void checkRestartCoursePermission(User user, CourseOffer offer)
			throws SubmissionException, NotInteractableException {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);

		// Check conditions
		ensureVisible(offer);
		ensureOpenCourseRecord(user, offer);
		ensureEnrolled(user, offer);
		ensureOfferHasCourse(offer);
		ensureWithinSubmissionPeriod(offer);
		ensureRestartable(user, offer);
	}

	/**
	 * A student is allowed to continue his/her course submission if
	 * <ul>
	 * <li>the student has an open submission in the course</li>
	 * <li>the course is visible in general</li>
	 * <li>the student is enrolled or the course does not force an explicit enrollment</li>
	 * <li>the course offer is linked to a course</li>
	 * <li>the actual time is within the submission period of the course offer</li>
	 * </ul>
	 * 
	 * @throws SubmissionException
	 *             In all other error cases
	 * @throws NotInteractableException
	 *             If the course offer is not visible
	 */
	public void checkContinueCoursePermission(User user, CourseOffer offer)
			throws SubmissionException, NotInteractableException {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);

		// Check conditions
		ensureOpenCourseRecord(user, offer);
		ensureVisible(offer);
		ensureEnrolledForNewSubmission(user, offer);
		ensureOfferHasCourse(offer);
		ensureWithinSubmissionPeriod(offer);
	}
	
	/**
	 * A personal password is required if explicit submission is enabled and personal passwords are enabled.
	 */
	public boolean isPersonalPasswordRequired(CourseOffer offer) {
		Objects.requireNonNull(offer);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);
		try {
			ensureNoPersonalPassword(offer);
		} catch (PasswordRequiredException e) {
			return true;
		}
		return false;
	}

	/**
	 * Returns <code>true</code> if the user is allowed to access the passed course offer with the passed personal
	 * password. Also returns <code>true</code> if no personal password is required. In any other cases
	 * <code>false</code> is returned. The passed password may be null.
	 */
	public boolean isPersonalPasswordCorrect(User user, CourseOffer courseOffer, String password) {
		Objects.requireNonNull(user);
		Objects.requireNonNull(courseOffer);
		courseOffer = courseOfferService.getCourseOfferById(courseOffer.getId())
				.orElseThrow(NoSuchJackEntityException::new);

		try {
			ensureCorrectPersonalPassword(user, courseOffer, password);
		} catch (SubmissionException e) {
			return false;
		}
		return true;
	}

	// #########################################################################
	// Submission - Queries
	// #########################################################################

	/**
	 * @return Wether the user has already started the course before. This is exactly the case if there is at least one
	 *         course record.
	 */
	public boolean hasAlreadyParticipated(User user, CourseOffer offer) {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		// TODO If we have a noticeable loss of speed here, the condition could be swapped out to a database query
		return courseRecordService.countAllCourseRecordsForCourseOfferAndUser(offer, user) > 0;
	}

	/**
	 * @return A course record that is open for a user and a course offer.
	 */
	public Optional<CourseRecord> getOpenCourseRecord(User user, CourseOffer offer) {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		return courseRecordService.getOpenCourseRecordFor(user, offer);
	}

	/**
	 * @return All open non-testing course records for a specific user.
	 */
	public List<CourseRecord> getOpenCourseRecords(User user) {
		Objects.requireNonNull(user);
		return courseRecordService.getOpenCourseRecords(user);
	}

	/**
	 * @return All course records for a user and a course offer that are visible for review, depending on the
	 *         {@link ECourseOfferReviewMode}.
	 */
	public List<CourseRecord> getVisibleOldCourseRecords(User user, CourseOffer offer) {
		Objects.requireNonNull(user);
		Objects.requireNonNull(offer);
		offer = courseOfferService.getCourseOfferById(offer.getId()).orElseThrow(NoSuchJackEntityException::new);


		switch (offer.getReviewMode()) {
		case AFTER_END:
			LocalDateTime submissionDeadline = offer.getSubmissionDeadline();
			if (submissionDeadline == null || submissionDeadline.isAfter(LocalDateTime.now())) {
				return Collections.emptyList();
			}
			return courseRecordService.getClosedCourseRecords(user, offer);
		case AFTER_EXIT:
		case ALWAYS:
			return courseRecordService.getClosedCourseRecords(user, offer);
		default:
			return Collections.emptyList();
		}
	}

}
