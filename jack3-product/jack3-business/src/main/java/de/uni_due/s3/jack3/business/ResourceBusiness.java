package de.uni_due.s3.jack3.business;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.CourseResource;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.ExerciseResource;
import de.uni_due.s3.jack3.entities.tenant.SubmissionResource;
import de.uni_due.s3.jack3.services.BaseService;
import de.uni_due.s3.jack3.services.CourseResourceService;
import de.uni_due.s3.jack3.services.ExerciseService;

@RequestScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class ResourceBusiness extends AbstractBusiness {

	@Inject
	private CourseResourceService courseResourceService;

	@Inject
	private BaseService baseService;

	@Inject
	private ExerciseService exerciseService;

	public boolean removeResourceIfIsOrphaned(ExerciseResource exerciseResource) {
		boolean hasBeenRemoved = false;

		List<Exercise> referencingExericses = exerciseService.getExercisesReferencingExerciseResource(exerciseResource);
		
		if (referencingExericses.isEmpty()) {
			baseService.deleteEntity(exerciseResource);
			hasBeenRemoved = true;
		}

		return hasBeenRemoved;
	}

	public Optional<ExerciseResource> getExerciseResourceById(final long id) {
		return baseService.findById(ExerciseResource.class, id, false);
	}

	public Optional<CourseResource> getCourseResourceByFileName(final String fileName, final AbstractCourse course) {
		return courseResourceService.getCourseResourceForCourseByFilename(fileName, course.getId());
	}

	public Optional<SubmissionResource> getSubmissionResourceById(final long id) {
		return baseService.findById(SubmissionResource.class, id, false);
	}

	public void persistSubmissionResource(SubmissionResource submissionResource) {
		baseService.persist(submissionResource);
	}
}