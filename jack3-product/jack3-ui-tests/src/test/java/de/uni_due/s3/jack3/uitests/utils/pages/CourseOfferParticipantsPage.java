package de.uni_due.s3.jack3.uitests.utils.pages;


import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;

import org.openqa.selenium.By;

public class CourseOfferParticipantsPage {

	private static final String CourseRecordActionButton = "courseOfferParticipantsMainForm:participantsDetails:courseRecordTable:0:actions";
	private static final String SHOW_COURSE_RECORD_DETAILS = "courseOfferParticipantsMainForm:participantsDetails:courseRecordTable:0:showAction";

	public static void openFirstCourseRecordDetails() {
		waitClickable(By.id(CourseRecordActionButton));
		find(CourseRecordActionButton).click();
		waitClickable(By.id(SHOW_COURSE_RECORD_DETAILS));
		find(SHOW_COURSE_RECORD_DETAILS).click();

		//wait until the page has been loaded
		CourseRecordSubmissionPage.getLastVisitTime();
	}

	public static void openCourseRecordDetailAtIndex(int index) {
		waitClickable(By.id(CourseRecordActionButton.replace("0", index + "")));
		find(CourseRecordActionButton.replace("0", index + "")).click();
		waitClickable(By.id(SHOW_COURSE_RECORD_DETAILS));
		find(SHOW_COURSE_RECORD_DETAILS).click();

		//wait until the page has been loaded
		CourseRecordSubmissionPage.getLastVisitTime();
	}
}
