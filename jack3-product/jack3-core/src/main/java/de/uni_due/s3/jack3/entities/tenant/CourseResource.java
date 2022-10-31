package de.uni_due.s3.jack3.entities.tenant;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.DeepCopyOmitField;
/*
 * Subclass for representing on Courses attached Files.
 */
@Audited
@Entity
@NamedQuery( //
		name = CourseResource.COURSE_RESOURCE_WITH_FILENAME, //
		query = "SELECT cr FROM CourseResource cr WHERE cr.course.id=:courseId AND cr.filename = :filename")
@NamedQuery( //
		name = CourseResource.ALL_COURSE_RESOURCES_FOR_COURSE, //
		query = "SELECT cr FROM CourseResource cr WHERE cr.course = :course ORDER BY cr.filename ASC")
@Table( //
		name = "CourseResource", // Make filename only unique per course
		uniqueConstraints = @UniqueConstraint(columnNames = { "filename", "course_id" }))

public class CourseResource extends Resource {

	private static final long serialVersionUID = 8569522292393606303L;

	/**
	 * Name of the query that returns the course resource with the given filename
	 * and for the given course id.
	 */
	public static final String COURSE_RESOURCE_WITH_FILENAME = "CourseResource.courseResourceWithFilename";

	/**
	 * Name of the query that returns all course resources for a given course
	 * ordered alphabetically by file name.
	 */
	public static final String ALL_COURSE_RESOURCES_FOR_COURSE = "CourseResource.allCourseResourcesForCourse";

	/**
	 * This field is used to ensure that filenames of resources are unique per course.
	 */
	@ManyToOne(optional = false)
	@DeepCopyOmitField(reason = "The course reference must be set by caller of the deepCopy method")
	private AbstractCourse course;

	public CourseResource() {
		super();
	}

	public CourseResource(String filename, byte[] content, AbstractCourse course, User lastEditor) {
		super(filename, content, lastEditor, null);
		this.course = Objects.requireNonNull(course);
	}

	public AbstractCourse getCourse() {
		return course;
	}

	public void setCourse(AbstractCourse course) {
		this.course = Objects.requireNonNull(course);
	}

	/**
	 * The course reference must be set by caller!
	 */
	@Override
	public CourseResource deepCopy() {
		CourseResource courseResourceDeepCopy = new CourseResource();
		courseResourceDeepCopy.deepCopyResourceVars(this);

		return courseResourceDeepCopy;
	}

}