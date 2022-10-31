package de.uni_due.s3.jack3.uitests.utils.pages;

import static de.uni_due.s3.jack3.uitests.utils.Assert.assertNotVisible;
import static de.uni_due.s3.jack3.uitests.utils.Assert.assertVisible;
import static de.uni_due.s3.jack3.uitests.utils.Click.clickWithJs;
import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Find.findChildren;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitNotVisible;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitVisible;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.opentest4j.AssertionFailedError;

import de.uni_due.s3.jack3.entities.enums.ECourseOfferReviewMode;
import de.uni_due.s3.jack3.entities.enums.ECourseResultDisplay;
import de.uni_due.s3.jack3.uitests.utils.Driver;
import de.uni_due.s3.jack3.uitests.utils.Time;

public class CourseOfferEditPage {

	private static final String SAVE_BUTTON_ID = "courseOfferEdit:saveCourseOffer";
	private static final String DISCARD_BUTTON_ID = "courseOfferEdit:discardChanges";

	private static final String NAME_FIELD_ID = "courseOfferEdit:courseOfferName";
	private static final String NAME_INPUT_ID = "courseOfferEdit:courseOfferNameInput";
	private static final String NAME_EDITOR_ID = "courseOfferEdit:courseOfferName_editor";

	private static final String COURSE_LABEL_ID = "courseOfferEdit:courses_label";
	private static final String SELECT_COURSE_ITEMS_ID = "courseOfferEdit:courses_items";

	private static final String ACCESS_TIME_VISIBILITY_TOGGLE = "courseOfferEdit:accessTimeVisibilityToggle";
	private static final String HAS_VISIBILITY_START_TIME = "courseOfferEdit:hasVisibilityStartTime";
	private static final String VISIBILITY_START_TIME_INPUT = "courseOfferEdit:courseOfferVisibilityStartTime_input";
	private static final String HAS_VISIBILITY_END_TIME = "courseOfferEdit:hasVisibilityEndTime";
	private static final String VISIBILITY_END_TIME_INPUT = "courseOfferEdit:courseOfferVisibilityEndTime_input";

	private static final String ALLOW_STAGE_RESTART_ID = "courseOfferEdit:allowStageRestart";
	private static final String ALLOW_HINTS_ID = "courseOfferEdit:selectedAllowHints";
	private static final String ALLOW_PAUSE_ID = "courseOfferEdit:selectedAllowPauses";
	private static final String ALLOW_STUDENTS_COMMENTS_ID = "courseOfferEdit:selectedAllowStudentComments";
	private static final String LIMIT_EXERCISE_REPETITIONS_ID = "courseOfferEdit:limitExerciseRepetitions";
	private static final String MIN_MAX_REPETITIONS_INPUT_ID = "courseOfferEdit:minMaxRepetitions_input";

	private static final String HAS_EXPLICIT_ENROLLMENT_ID = "courseOfferEdit:hasExplicitEnrollment";
	private static final String HAS_ENROLLMENT_START_ID = "courseOfferEdit:hasEnrollmentStart";
	private static final String ENROLLMENT_START_INPUT = "courseOfferEdit:courseOfferEnrollmentStart_input";
	private static final String HAS_ENROLLMENT_DEADLINE_ID = "courseOfferEdit:hasEnrollmentDeadline";
	private static final String ENROLLMENT_DEADLINE_INPUT = "courseOfferEdit:courseOfferEnrollmentDeadline_input";
	private static final String HAS_DISENROLLMENT_DEADLINE_ID = "courseOfferEdit:hasDisenrollmentDeadline";
	private static final String DISENROLLMENT_DEADLINE_INPUT = "courseOfferEdit:courseOfferDisenrollmentDeadline_input";
	private static final String HAS_LINKED_COURSES = "courseOfferEdit:hasLinkedCoursesCheckbox";
	private static final String GLOBAL_PASSWORD_ID = "courseOfferEdit:globalPasswordInput";
	private static final String GLOBAL_PASSWORD_CLEAR_BUTTON_ID = "courseOfferEdit:globalPasswordClearButton";
	private static final String HAS_MAX_ALLOWED_PARTICIPANTS_ID = "courseOfferEdit:maxAllowedParticipantsCheckbox";
	private static final String MAX_ALLOWED_PARTICIPANTS_INPUT_ID = "courseOfferEdit:maxAllowedParticipants_input";
	private static final String HAS_WAITLIST_ENABLED = "courseOfferEdit:hasWaitlistEnabledCheckbox";

	private static final String EXPLICIT_SUBMISSION_REQUIRED_ID = "courseOfferEdit:selectedExplicitSubmissionRequired";
	private static final String HAS_SUBMISSION_START_ID = "courseOfferEdit:hasSubmissionStart";
	private static final String SUBMISSION_START_INPUT = "courseOfferEdit:courseOfferSubmissionStart_input";
	private static final String HAS_SUBMISSION_DEADLINE_ID = "courseOfferEdit:hasSubmissionDeadline";
	private static final String SUBMISSION_DEADLINE_INPUT = "courseOfferEdit:courseOfferSubmissionDeadline_input";
	private static final String ONLY_ALLOW_SINGULAR_PARTICIPATION = "courseOfferEdit:selectedOnlyAllowSingularParticipation";
	private static final String TIME_LIMIT_CHECK_ID = "courseOfferEdit:timelimitCheck";
	private static final String TIME_LIMIT_SPINNER_ID = "courseOfferEdit:timelimitSpinner_input";
	private static final String HAS_PERSONAL_PASSWORD = "courseOfferEdit:hasPersonalPassword";

	private static final String SHOW_DIFFICULTY_ID = "courseOfferEdit:showDifficulty";
	private static final String SHOW_RESULT_IMMEDIATELY = "courseOfferEdit:showResultImmediately";
	private static final String SHOW_FEEDBACK_IMMEDIATELY = "courseOfferEdit:showFeedbackImmediately";

	private static final String COURSE_RESULT_DISPLAY_ID = "courseOfferEdit:courseResultDisplay";
	private static final String REVIEW_MENU_ID = "courseOfferEdit:review";
	private static final String SHOW_EXERCISE_AND_SUBMISSION_IN_COURSE_RESULTS = "courseOfferEdit:showExerciseAndSubmissionInCourseResults";
	private static final String SHOW_RESULT_IN_COURSE_RESULTS = "courseOfferEdit:showResultInCourseResults";
	private static final String SHOW_FEEDBACK_IN_COURSE_RESULTS = "courseOfferEdit:showFeedbackInCourseResults";

	private static final String NUMBER_OF_PARTICIPANTS = "courseOfferEdit:participants";
	private static final String TO_MORE_COURSEOFFER_STATISTICS_ID = "courseOfferEdit:toMoreCourseOfferStatistics";

	public static void saveCourseOffer() {
		waitClickable(By.id(SAVE_BUTTON_ID));
		find(SAVE_BUTTON_ID).click();

		Time.waitNotClickable(By.id(SAVE_BUTTON_ID));
	}

	public static void discardChanges() {
		waitClickable(By.id(DISCARD_BUTTON_ID));
		find(DISCARD_BUTTON_ID).click();
	}

	public static void setStateOfCheckBox(boolean selected, String checkBoxId) {
		waitClickable(By.id(checkBoxId));
		if (find(checkBoxId + "_input").isSelected() != selected) {
			find(checkBoxId).click();
		}
	}

	public static String getNameOfCourseOffer() {
		waitClickable(By.id(NAME_FIELD_ID));
		return find(NAME_FIELD_ID).getText();
	}

	public static void setNameOfCourseOffer(String newName) {
		waitClickable(By.id(NAME_FIELD_ID));
		find(NAME_FIELD_ID).click();
		waitClickable(By.id(NAME_INPUT_ID));
		find(NAME_INPUT_ID).sendKeys(newName);
		findChildren(find(NAME_EDITOR_ID)).get(0).click();
		waitClickable(By.id(NAME_FIELD_ID));

		Time.wait(ExpectedConditions.textToBe(By.id(NAME_FIELD_ID), newName),
				"The Name of the CourseOffer couldn't be changed");
	}

	public static String getNameOfSelectedCourse() {
		waitClickable(By.id(COURSE_LABEL_ID));
		return find(COURSE_LABEL_ID).getText();
	}

	/**
	 * Sets the Course which shall be used for this CourseOffer.
	 * 
	 * @param courseName
	 *            The name of the course. If courseName is null or empty, 'No Course' will be selected
	 */
	public static void setCourse(String courseName) {
		waitClickable(By.id(COURSE_LABEL_ID));
		find(COURSE_LABEL_ID).click();
		waitClickable(By.id(SELECT_COURSE_ITEMS_ID));

		if (courseName == null || courseName.isEmpty()) {
			find(By.id("courseOfferEdit:courses_0")).click();
			return;
		}

		for (WebElement element : findChildren(find(SELECT_COURSE_ITEMS_ID))) {
			if (courseName.equals(element.getText())) {
				element.click();
				return;
			}
		}

		throw new AssertionFailedError("The Course '" + courseName + "' could not be found");
	}

	public static void setAllowStageRestart(boolean allowRestart) {
		setStateOfCheckBox(allowRestart, ALLOW_STAGE_RESTART_ID);
	}

	public static void setAllowHints(boolean allowHints) {
		setStateOfCheckBox(allowHints, ALLOW_HINTS_ID);
	}

	public static void setAllowPause(boolean allowPause) {
		setStateOfCheckBox(allowPause, ALLOW_PAUSE_ID);
	}

	public static void setAllowStudentsComments(boolean allowComments) {
		setStateOfCheckBox(allowComments, ALLOW_STUDENTS_COMMENTS_ID);
	}

	/**
	 * If numberOfMaxRepetitions is <1, there won't be a limit for exercise repetitions
	 */
	public static void limitExericseRepetitions(int numberOfMaxRepetitions) {
		waitClickable(By.id(LIMIT_EXERCISE_REPETITIONS_ID));
		if (numberOfMaxRepetitions < 1 && find(LIMIT_EXERCISE_REPETITIONS_ID + "_input").isSelected()) {
			find(LIMIT_EXERCISE_REPETITIONS_ID).click();
			return;
		}
		if (numberOfMaxRepetitions >= 1 && !find(LIMIT_EXERCISE_REPETITIONS_ID + "_input").isSelected()) {
			find(LIMIT_EXERCISE_REPETITIONS_ID).click();
		}

		waitClickable(By.id(MIN_MAX_REPETITIONS_INPUT_ID));
		find(MIN_MAX_REPETITIONS_INPUT_ID).clear();
		find(MIN_MAX_REPETITIONS_INPUT_ID).sendKeys(numberOfMaxRepetitions + "");
	}

	public static void setExplicitEnrollment(boolean hasExplicitEnrollment) {
		waitClickable(By.id(HAS_EXPLICIT_ENROLLMENT_ID));

		if (find(HAS_EXPLICIT_ENROLLMENT_ID + "_input").isSelected() != hasExplicitEnrollment) {
			find(HAS_EXPLICIT_ENROLLMENT_ID).click();
		}

		if (hasExplicitEnrollment) {
			assertVisible(By.id(HAS_ENROLLMENT_START_ID),
					"Enrollment panel was shown");
		} else {
			assertNotVisible(By.id(HAS_ENROLLMENT_START_ID),
					"Enrollment panel was shown");
		}
	}

	public static void setHasEnrollmentStart(boolean hasEnrollmentStart) {
		setStateOfCheckBox(hasEnrollmentStart, HAS_ENROLLMENT_START_ID);
	}

	/**
	 * If date is null there won't be a visibility start time
	 */
	public static void setEnrollmentStart(LocalDateTime localDateTime) {
		boolean hasStartTime = (localDateTime != null);
		setHasEnrollmentStart(hasStartTime);

		if (hasStartTime) {
			waitClickable(By.id(ENROLLMENT_START_INPUT));

			//find(ENROLLMENT_START_INPUT).click();

			final JavascriptExecutor js = (JavascriptExecutor) Driver.get();
			final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
			final String INPUT = formatter.format(localDateTime);

			js.executeScript("arguments[0].value = '" + INPUT + "';", find(ENROLLMENT_START_INPUT));

			Time.wait(ExpectedConditions.textToBePresentInElementValue(find(ENROLLMENT_START_INPUT), INPUT),
					"It was not possible to set the date intot he input field");
		}
	}

	public static void setHasEnrollmentDeadline(boolean hasEnrollmentDeadline) {
		setStateOfCheckBox(hasEnrollmentDeadline, HAS_ENROLLMENT_DEADLINE_ID);
	}

	/**
	 * If date is null there won't be a visibility start time
	 */
	public static void setEnrollmentDeadline(LocalDateTime localDateTime) {
		boolean hasEnrollmentDeadline = (localDateTime != null);
		setHasEnrollmentDeadline(hasEnrollmentDeadline);

		if (hasEnrollmentDeadline) {
			waitClickable(By.id(ENROLLMENT_DEADLINE_INPUT));

			//find(ENROLLMENT_DEADLINE_INPUT).click();

			final JavascriptExecutor js = (JavascriptExecutor) Driver.get();
			final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
			final String INPUT = formatter.format(localDateTime);

			js.executeScript("arguments[0].value = '" + INPUT + "';", find(ENROLLMENT_DEADLINE_INPUT));

			Time.wait(ExpectedConditions.textToBePresentInElementValue(find(ENROLLMENT_DEADLINE_INPUT), INPUT),
					"It was not possible to set the date intot he input field");
		}
	}

	public static void setHasDisenrollmentDeadline(boolean hasDisenrollmentDeadline) {
		setStateOfCheckBox(hasDisenrollmentDeadline, HAS_DISENROLLMENT_DEADLINE_ID);
	}

	/**
	 * If date is null there won't be a visibility start time
	 */
	public static void setDisenrollmentDeadline(LocalDateTime localDateTime) {
		boolean hasDisenrollmentDeadline = (localDateTime != null);
		setHasDisenrollmentDeadline(hasDisenrollmentDeadline);

		if (hasDisenrollmentDeadline) {
			waitClickable(By.id(DISENROLLMENT_DEADLINE_INPUT));

			//find(DISENROLLMENT_DEADLINE_INPUT).click();

			final JavascriptExecutor js = (JavascriptExecutor) Driver.get();
			final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
			final String INPUT = formatter.format(localDateTime);

			js.executeScript("arguments[0].value = '" + INPUT + "';", find(DISENROLLMENT_DEADLINE_INPUT));

			Time.wait(ExpectedConditions.textToBePresentInElementValue(find(DISENROLLMENT_DEADLINE_INPUT), INPUT),
					"It was not possible to set the date intot he input field");
		}
	}

	public static void setHasLinkedCourses(boolean hasLinkedCourses) {
		setStateOfCheckBox(hasLinkedCourses, HAS_LINKED_COURSES);
	}

	/**
	 * If the password is null or empty there won't be a global password
	 */
	public static void setGlobalPassword(String password) {
		waitClickable(By.id(GLOBAL_PASSWORD_ID));
		
		if(password == null || password.isEmpty()) {
			find(GLOBAL_PASSWORD_CLEAR_BUTTON_ID).click();
		}else {
			find(GLOBAL_PASSWORD_ID).clear();
			find(GLOBAL_PASSWORD_ID).sendKeys(password);
		}
	}

	/**
	 * If the maxAllowedParticipants is <1, there won't be a maximum of allowed participants
	 */
	public static void setMaxAllowedParticpants (int maxAllowedParticipants) {
		waitClickable(By.id(HAS_MAX_ALLOWED_PARTICIPANTS_ID));
		
		if (maxAllowedParticipants < 1 && find(HAS_MAX_ALLOWED_PARTICIPANTS_ID + "_input").isSelected()) {
			find(HAS_MAX_ALLOWED_PARTICIPANTS_ID).click();
			return;
		}
		if (maxAllowedParticipants >= 1 && !find(HAS_MAX_ALLOWED_PARTICIPANTS_ID + "_input").isSelected()) {
			find(HAS_MAX_ALLOWED_PARTICIPANTS_ID).click();
		}

		waitClickable(By.id(MAX_ALLOWED_PARTICIPANTS_INPUT_ID));
		//calling clear() doesn't work. We are pressing many times back space to manually clear the input field
		find(MAX_ALLOWED_PARTICIPANTS_INPUT_ID).sendKeys("" + Keys.BACK_SPACE + Keys.BACK_SPACE + Keys.BACK_SPACE
				+ Keys.BACK_SPACE + Keys.BACK_SPACE);
		find(MAX_ALLOWED_PARTICIPANTS_INPUT_ID).sendKeys(maxAllowedParticipants + "");
	}

	public static void setHasWaitingList(boolean hasWaitingList) {
		setStateOfCheckBox(hasWaitingList, HAS_WAITLIST_ENABLED);
	}

	public static void setExplicitSubmissionRequired(boolean explicitSubmissionRequired) {
		waitClickable(By.id(EXPLICIT_SUBMISSION_REQUIRED_ID));

		if (find(EXPLICIT_SUBMISSION_REQUIRED_ID + "_input").isSelected() != explicitSubmissionRequired) {
			find(EXPLICIT_SUBMISSION_REQUIRED_ID).click();
		}

		if (explicitSubmissionRequired) {
			waitVisible(By.id(HAS_ENROLLMENT_START_ID), "Submission panel wasn't shown");
		} else {
			waitNotVisible(By.id(HAS_ENROLLMENT_START_ID), "Submission panel was shown");
		}
	}

	public static void setHasSubmissionStart(boolean submissionStart) {
		setStateOfCheckBox(submissionStart, HAS_SUBMISSION_START_ID);
	}

	/**
	 * If date is null there won't be a visibility start time
	 */
	public static void setSubmissionStart(LocalDateTime localDateTime) {
		boolean hasSubmissionStart = (localDateTime != null);
		setHasSubmissionStart(hasSubmissionStart);

		if (hasSubmissionStart) {
			waitClickable(By.id(SUBMISSION_START_INPUT));

			final JavascriptExecutor js = (JavascriptExecutor) Driver.get();
			final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
			final String INPUT = formatter.format(localDateTime);

			js.executeScript("arguments[0].value = '" + INPUT + "';", find(SUBMISSION_START_INPUT));

			Time.wait(ExpectedConditions.textToBePresentInElementValue(find(SUBMISSION_START_INPUT), INPUT),
					"It was not possible to set the date intot he input field");
		}
	}

	public static void setHasSubmissionDeadline(boolean hasSubmissionDeadline) {
		setStateOfCheckBox(hasSubmissionDeadline, HAS_SUBMISSION_DEADLINE_ID);
	}

	/**
	 * If date is null there won't be a visibility start time
	 */
	public static void setSubmissionDeadline(LocalDateTime localDateTime) {
		boolean hasSubmissionDeadline = (localDateTime != null);
		setHasSubmissionDeadline(hasSubmissionDeadline);

		if (hasSubmissionDeadline) {
			waitClickable(By.id(SUBMISSION_DEADLINE_INPUT));

			final JavascriptExecutor js = (JavascriptExecutor) Driver.get();
			final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
			final String INPUT = formatter.format(localDateTime);

			js.executeScript("arguments[0].value = '" + INPUT + "';", find(SUBMISSION_DEADLINE_INPUT));

			Time.wait(ExpectedConditions.textToBePresentInElementValue(find(SUBMISSION_DEADLINE_INPUT), INPUT),
					"It was not possible to set the date intot he input field");
		}
	}

	public static void setOnlyAllowAllowSingularParticipation(boolean singularParticipation) {
		setStateOfCheckBox(singularParticipation, ONLY_ALLOW_SINGULAR_PARTICIPATION);
	}

	/**
	 * If timeLimit is 0, there won't be a timeLimit set
	 */
	public static void setTimeLimit(int timeLimit) {
		waitClickable(By.id(TIME_LIMIT_CHECK_ID));

		if (timeLimit == 0 && find(TIME_LIMIT_CHECK_ID + "_input").isSelected()) {
			find(TIME_LIMIT_CHECK_ID).click();
			return;
		}
		if (timeLimit != 0 && !find(TIME_LIMIT_CHECK_ID + "_input").isSelected()) {
			find(TIME_LIMIT_CHECK_ID).click();
		}

		waitClickable(By.id(TIME_LIMIT_SPINNER_ID));
		//calling clear() doesn't work. We are pressing many times back space to manually clear the input field
		find(TIME_LIMIT_SPINNER_ID).sendKeys("" + Keys.BACK_SPACE + Keys.BACK_SPACE + Keys.BACK_SPACE + Keys.BACK_SPACE
				+ Keys.BACK_SPACE + Keys.BACK_SPACE + Keys.BACK_SPACE + Keys.BACK_SPACE + Keys.BACK_SPACE
				+ Keys.BACK_SPACE + Keys.BACK_SPACE + Keys.BACK_SPACE + Keys.BACK_SPACE);

		find(TIME_LIMIT_SPINNER_ID).sendKeys(timeLimit + "");
	}

	public static void setHasPersonalPasswords(boolean hasPersonalPasswords) {
		setStateOfCheckBox(hasPersonalPasswords, HAS_PERSONAL_PASSWORD);
	}

	public static void setShowDifficulty(boolean showDifficulty) {
		setStateOfCheckBox(showDifficulty, SHOW_DIFFICULTY_ID);
	}

	public static void setShowResultImmediately(boolean showResultImmediately) {
		setStateOfCheckBox(showResultImmediately, SHOW_RESULT_IMMEDIATELY);
	}

	public static void setShowFeedbackImmediately(boolean showFeedbackImmediately) {
		setStateOfCheckBox(showFeedbackImmediately, SHOW_FEEDBACK_IMMEDIATELY);
	}

	public static void setCourseResultDisplay(ECourseResultDisplay courseResultDisplay) {
		waitClickable(By.id(COURSE_RESULT_DISPLAY_ID));

		find(COURSE_RESULT_DISPLAY_ID).click();
		waitClickable(By.id(COURSE_RESULT_DISPLAY_ID + "_0"));

		switch (courseResultDisplay) {
		case NONE:
			find(COURSE_RESULT_DISPLAY_ID + "_0").click();
			break;
		case POINTS:
			find(COURSE_RESULT_DISPLAY_ID + "_1").click();
			break;
		case TEXT:
			find(COURSE_RESULT_DISPLAY_ID + "_2").click();
			break;
		case BOTH:
			find(COURSE_RESULT_DISPLAY_ID + "_3").click();
			break;
		}
	}

	public static void setReviewMode(ECourseOfferReviewMode reviedMode) {
		waitClickable(By.id(REVIEW_MENU_ID));

		find(REVIEW_MENU_ID).click();
		waitClickable(By.id(REVIEW_MENU_ID + "_0"));

		switch (reviedMode) {
		case ALWAYS:
			find(REVIEW_MENU_ID + "_0").click();
			break;
		case AFTER_EXIT:
			find(REVIEW_MENU_ID + "_1").click();
			break;
		case AFTER_END:
			find(REVIEW_MENU_ID + "_2").click();
			break;
		case AFTER_REVIEW:
			find(REVIEW_MENU_ID + "_3").click();
			break;
		case NEVER:
			find(REVIEW_MENU_ID + "_4").click();
			break;
		}
	}

	public static void setShowExerciseAndSubmissionInCourseResults(boolean showExerciseAndSubmissionInCourseResults) {
		setStateOfCheckBox(showExerciseAndSubmissionInCourseResults, SHOW_EXERCISE_AND_SUBMISSION_IN_COURSE_RESULTS);
	}

	public static void setShowResultInCourseResults(boolean showResultInCourseResults) {
		setStateOfCheckBox(showResultInCourseResults, SHOW_RESULT_IN_COURSE_RESULTS);
	}

	public static void setShowFeedbackInCourseResults(boolean showFeedbackInCourseResults) {
		setStateOfCheckBox(showFeedbackInCourseResults, SHOW_FEEDBACK_IN_COURSE_RESULTS);
	}

	public static void setVisibilityOfCourseOffer(boolean visibility) {
		waitClickable(By.id(ACCESS_TIME_VISIBILITY_TOGGLE));

		List<WebElement> webElements = findChildren(find(ACCESS_TIME_VISIBILITY_TOGGLE));

		webElements = webElements.stream()
				.filter(ele -> "label".equals(ele.getTagName())).collect(Collectors.toList());

		if (visibility) {
			clickWithJs(webElements.get(1));
		} else {
			clickWithJs(webElements.get(0));
		}
	}

	/**
	 * If date is null there won't be a visibility start time
	 */
	public static void setVisibilityStart(LocalDateTime localDateTime) {
		waitClickable(By.id(HAS_VISIBILITY_START_TIME));
		boolean hasStartTime = (localDateTime != null);
		setStateOfCheckBox(hasStartTime, HAS_VISIBILITY_START_TIME);

		if (hasStartTime) {
			waitClickable(By.id(VISIBILITY_START_TIME_INPUT));

			final JavascriptExecutor js = (JavascriptExecutor) Driver.get();
			final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
			final String INPUT = formatter.format(localDateTime);

			js.executeScript("arguments[0].value = '" + INPUT + "';",
					find(VISIBILITY_START_TIME_INPUT));

			Time.wait(ExpectedConditions.textToBePresentInElementValue(find(VISIBILITY_START_TIME_INPUT), INPUT),
					"It was not possible to set the date intot he input field");
		}
	}

	/**
	 * If date is null there won't be a visibility start time
	 */
	public static void setVisibilityEnd(LocalDateTime localDateTime) {
		boolean hasVisibilityEnd = (localDateTime != null);
		setStateOfCheckBox(hasVisibilityEnd, HAS_VISIBILITY_END_TIME);

		if (hasVisibilityEnd) {
			waitClickable(By.id(VISIBILITY_END_TIME_INPUT));

			final JavascriptExecutor js = (JavascriptExecutor) Driver.get();
			final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
			final String INPUT = formatter.format(localDateTime);

			js.executeScript("arguments[0].value = '" + INPUT + "';", find(VISIBILITY_END_TIME_INPUT));

			Time.wait(ExpectedConditions.textToBePresentInElementValue(find(VISIBILITY_END_TIME_INPUT), INPUT),
					"It was not possible to set the date intot he input field");
		}
	}

	public static int getNumberOfParticipants() {
		waitClickable(By.id(NUMBER_OF_PARTICIPANTS));

		return Integer.parseInt(find(NUMBER_OF_PARTICIPANTS).getText());
	}

	public static void openCourseOfferStatistics() {
		waitClickable(By.id(TO_MORE_COURSEOFFER_STATISTICS_ID));
		find(TO_MORE_COURSEOFFER_STATISTICS_ID).click();

		waitVisible(By.id("courseOfferParticipantsMainForm:submissions"));
	}

}
