package de.uni_due.s3.jack3.business;

import de.uni_due.s3.jack3.business.exceptions.AuthorizationException;
import de.uni_due.s3.jack3.business.exceptions.SubjectException;
import de.uni_due.s3.jack3.entities.tenant.Config;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.Subject;
import de.uni_due.s3.jack3.services.BaseService;
import de.uni_due.s3.jack3.services.CourseService;
import de.uni_due.s3.jack3.services.ExerciseService;
import de.uni_due.s3.jack3.services.SubjectService;
import de.uni_due.s3.jack3.utils.JackStringUtils;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;

@RequestScoped @Transactional(value = Transactional.TxType.REQUIRED) public class SubjectBusiness
		extends AbstractBusiness {

	@Inject private SubjectService subjectService;

	@Inject private CourseService courseService;

	@Inject private ExerciseService exerciseService;

	@Inject private BaseService baseService;

	/**
	 * Lists all Subjects.
	 *
	 * @return Subject list
	 */
	public List<Subject> getAllSubjects() {
		return subjectService.getAllSubjects();
	}

	/**
	 * Removes a subject. Prerequisite: there are no references from courses or exercises.
	 * Iterates over courses and exercises and makes sure that there exists no reference to the subject to be deleted.
	 *
	 * @param subject
	 */
	public boolean removeSubject(Subject subject) {
		boolean hasBeenRemoved = false;
		List<Course> referencingCourses = courseService.getCoursesReferencingSubject(subject);
		List<Exercise> referencingExercises = exerciseService.getExercisesReferencingSubject(subject);

		if (referencingCourses.isEmpty() && referencingExercises.isEmpty()) {
			subjectService.deleteSubject(subject);
			hasBeenRemoved = true;
		}
		return hasBeenRemoved;
	}

	/**
	 * Adds a new subject to the database. Returns {@code true} if that was successful and {@code false} otherwise.
	 * <p>
	 * Prerequisites: there exists no subject with equal name and name is not empty.
	 * Iterates over subjects to make sure that there exists no subject with same name.
	 * <p>
	 * The method does NOT update any cached values.
	 * <p>
	 * * @param subject
	 */
	public boolean addNewSubject(Subject subject) throws SubjectException {
		if (subjectWithEqualNameExists(subject)) {
			throw new SubjectException(SubjectException.EType.SUBJECT_ALREADY_EXISTS);
		}
		if (JackStringUtils.isBlank(subject.getName())) {
			throw new IllegalArgumentException("You must specify a non-emtpy subject name.");
		}
		return subjectService.persistSubject(subject);
	}

	/**
	 * Updates a subject. Prerequisite: there exists no subject with equal name.
	 * Iterates over subjects to make sure that there exists no subject with same name.
	 *
	 * @param subject
	 */
	public boolean updateSubject(Subject subject) throws SubjectException {
		if (subjectWithEqualNameExists(subject)) {
			throw new SubjectException(SubjectException.EType.SUBJECT_ALREADY_EXISTS);
		}
		if (JackStringUtils.isBlank(subject.getName())) {
			throw new IllegalArgumentException("You must specify a non-emtpy subject name.");
		}
		return subjectService.mergeSubject(subject);
	}

	public boolean subjectWithEqualNameExists(Subject subject) {
		List<Subject> subjects = subjectService.getAllSubjects();
		boolean subjectWithEqualNameExists = false;
		for (Subject existingSubject : subjects) {
			if (existingSubject.getName().equals(subject.getName())) {
				subjectWithEqualNameExists = true;
			}
		}
		return subjectWithEqualNameExists;
	}

}