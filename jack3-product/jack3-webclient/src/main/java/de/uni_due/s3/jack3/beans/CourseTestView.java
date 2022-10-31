package de.uni_due.s3.jack3.beans;

import java.io.IOException;
import java.io.Serializable;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;

@Named
@ViewScoped
public class CourseTestView extends AbstractView implements Serializable {

	private static final long serialVersionUID = -8202706339428501078L;

	private long courseId;
	private long courseRecordId;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private AuthorizationBusiness authorizationBusiness;

	@Inject
	private CoursePlayerView coursePlayer;

	@Inject
	private FolderBusiness folderBusiness;

	public void loadCourse() throws IOException {
		AbstractCourse course = courseBusiness.getCourseByCourseID(courseId);

		//only user with read right for the course can test the course
		ContentFolder folder = folderBusiness.getFolderForAbstractCourse(course);

		if (!authorizationBusiness.isAllowedToReadFromFolder(getCurrentUser(), folder)) {
			sendErrorResponse(403, "User is not allowed to test course");
			return;
		}

		CourseRecord courseRecord = null;
		if (courseRecordId == 0) {
			// Creating new courseRecord, when course is about to begin
			courseRecord = courseBusiness.createTestCourseRecord(getCurrentUser(), course);
			courseRecordId = courseRecord.getId();
		} else {
			// Loading the courseRecord from database, when it exists
			try {
				courseRecord = courseBusiness.getCourseRecordById(courseRecordId);
				getLogger().info("Successfully loaded course record " + courseRecord + " from database.");
			} catch (NoSuchJackEntityException e) {
				sendErrorResponse(400, "CourseRecord with given courseRecordId does not exist in database");
				return;
			}
		}
		coursePlayer.setCourseRecord(courseRecord);

		coursePlayer.initForTesting(courseId);

		// Adding breadcrumb with path to course
		createYouAreHereModelForCourse(course, true);
	}

	public long getCourseId() {
		return courseId;
	}

	public void setCourseId(long courseId) {
		this.courseId = courseId;
	}

	public long getCourseRecordId() {
		return courseRecordId;
	}

	public void setCourseRecordId(long courseRecordId) {
		this.courseRecordId = courseRecordId;
	}
}
