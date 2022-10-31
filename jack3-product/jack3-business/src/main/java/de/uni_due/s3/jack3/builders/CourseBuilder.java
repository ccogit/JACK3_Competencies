package de.uni_due.s3.jack3.builders;

import java.util.Collection;

import de.uni_due.s3.jack3.entities.enums.ECourseExercisesOrder;
import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.entities.tenant.ResultFeedbackMapping;

/**
 * Builder for courses
 */
public class CourseBuilder {

	private final Course course;

	/**
	 * Creates a new instance of the builder from an existing course.
	 * 
	 * @param course
	 *            Existing course to be modified
	 */
	public CourseBuilder(Course course) {
		this.course = course;
	}

	/**
	 * Creates a new instance of the builder.
	 * 
	 * @param name
	 *            Name of the course to build
	 */
	public CourseBuilder(String name) {
		course = new Course(name);
	}

	/**
	 * Builds the course.
	 * 
	 * @return Course object, ready to persist
	 */
	public Course build() {
		return course;
	}

	/**
	 * Sets the external (public) course description.
	 */
	public CourseBuilder withExternalDescription(String externalDescription) {
		course.setExternalDescription(externalDescription);
		return this;
	}

	/**
	 * Sets the internal course description (only visible from course edit view).
	 */
	public CourseBuilder withInternalDescription(String internalDescription) {
		course.setInternalDescription(internalDescription);
		return this;
	}

	/**
	 * Sets the order in which exercises are presented in a course record.
	 */
	public CourseBuilder withExerciseOrder(ECourseExercisesOrder exerciseOrder) {
		course.setExerciseOrder(exerciseOrder);
		return this;
	}

	/**
	 * Sets the scoring mode (how a result will be calculated).
	 */
	public CourseBuilder withScoringMode(ECourseScoring scoringMode) {
		course.setScoringMode(scoringMode);
		return this;
	}

	/**
	 * Adds a folder exercise provider to the course.
	 * 
	 * @param folders
	 *            All folders to be searched for exercises
	 */
	public CourseBuilder withFolderExerciseProvider(Collection<ContentFolder> folders) {
		if (course.getContentProvider() != null) {
			throw new IllegalArgumentException("Content provider has already been defined.");
		}
		FolderExerciseProvider fep = new FolderExerciseProvider();
		folders.forEach(fep::addFolder);
		course.setContentProvider(fep);
		return this;
	}

	/**
	 * Adds a fixed list exercise provider to the course.
	 * 
	 * @param entries
	 *            All exercises with defined points
	 */
	public CourseBuilder withFixedListExerciseProvider(Collection<CourseEntry> entries) {
		if (course.getContentProvider() != null) {
			throw new IllegalArgumentException("Content provider has already been defined.");
		}
		FixedListExerciseProvider flep = new FixedListExerciseProvider();
		entries.forEach(flep::addCourseEntry);
		course.setContentProvider(flep);
		return this;
	}

	/**
	 * Adds a feedback to the course that appears if the expression is true.
	 * 
	 * @param expression
	 *            An evaluator expression. You can e.g. use the meta variable {@code currentResult}.
	 * @param title
	 *            The feedback title
	 * @param text
	 *            The feedback text
	 */
	public CourseBuilder withResultFeedbackMapping(String expression, String title, String text) {
		course.addResultFeedbackMapping(new ResultFeedbackMapping(expression, title, text));
		return this;
	}

	/**
	 * Adds a feedback to the course that appears if the course record has earned the specified points.
	 * 
	 * @param points
	 *            The score of the course record to show the feedback
	 * @param title
	 *            The feedback title
	 * @param text
	 */
	public CourseBuilder withResultFeedbackMappingExactPoints(int points, String title, String text) {
		course.addResultFeedbackMapping(new ResultFeedbackMapping("[meta=currentResult]==" + points, title, text));
		return this;
	}

}
