package de.uni_due.s3.jack3.services;

import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.CourseResource;
import de.uni_due.s3.jack3.services.utils.DBHelper;

/**
 * Service for managing {@link CourseResource} entities.
 */
@Stateless
public class CourseResourceService extends AbstractServiceBean {

	public List<CourseResource> getAllCourseResourcesForCourse(AbstractCourse course) {
		final EntityManager em = getEntityManager();
		final TypedQuery<CourseResource> q = em.createNamedQuery(CourseResource.ALL_COURSE_RESOURCES_FOR_COURSE,
				CourseResource.class);
		q.setParameter("course", course);
		return q.getResultList();
	}

	public Optional<CourseResource> getCourseResourceForCourseByFilename(String filename, long courseId) {
		final var query = getEntityManager()
				.createNamedQuery(CourseResource.COURSE_RESOURCE_WITH_FILENAME, CourseResource.class)
				.setParameter("filename", filename) //
				.setParameter("courseId", courseId);
		return DBHelper.getOneOrZero(query);
	}

}
