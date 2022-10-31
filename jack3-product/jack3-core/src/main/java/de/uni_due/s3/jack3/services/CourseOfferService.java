package de.uni_due.s3.jack3.services;

import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;

/**
 * Service for managing {@link CourseOffer} entities.
 */
@Stateless
public class CourseOfferService extends AbstractServiceBean {
	@Inject
	private BaseService baseService;

	@Inject
	private CourseRecordService courseRecordService;

	public List<CourseOffer> getAllCourseOffers() {
		final EntityManager em = getEntityManager();
		final TypedQuery<CourseOffer> q = em.createNamedQuery(CourseOffer.ALL_COURSEOFFERS, CourseOffer.class);
		return q.getResultList();
	}

	public void persistCourseOffer(CourseOffer courseOffer) {
		baseService.persist(courseOffer);
	}

	public Optional<CourseOffer> getCourseOfferById(long id) {
		return baseService.findById(CourseOffer.class, id, false);
	}

	public List<CourseOffer> getCourseOffersReferencingCourse(AbstractCourse course) {
		final EntityManager em = getEntityManager();
		final TypedQuery<CourseOffer> query = em.createNamedQuery(CourseOffer.COURSEOFFERS_REFERENCING_COURSE_REVISION,
				CourseOffer.class);
		query.setParameter("course", course);
		return query.getResultList();
	}
	
	public List<CourseOffer> getCourseOffersByFolder(PresentationFolder folder) {
		return getEntityManager()
				.createNamedQuery(CourseOffer.ALL_COURSEOFFERS_FOR_FOLDER, CourseOffer.class)
				.setParameter("folder", folder)
				.getResultList();
	}

	public List<CourseOffer> getCourseOffersByFolders(List<PresentationFolder> folders) {
		return getEntityManager()
				.createNamedQuery(CourseOffer.ALL_COURSEOFFERS_FOR_FOLDERS, CourseOffer.class)
				.setParameter("folders", folders)
				.getResultList();
	}

	public CourseOffer mergeCourseOffer(CourseOffer courseOffer) {
		return baseService.merge(courseOffer);
	}

	public void deleteCourseOffer(CourseOffer courseOffer) {
		final List<CourseRecord> courseRecordList = courseRecordService.getAllCourseRecordsForCourseOfferOrderedByStarttime(courseOffer);
		for(CourseRecord courseRecord : courseRecordList) {
			courseRecordService.removeCourseRecordAndAttachedSubmissions(courseRecord);
		}
		baseService.deleteEntity(courseOffer);
	}

}
