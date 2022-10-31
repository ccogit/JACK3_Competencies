package de.uni_due.s3.jack3.services;

import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.hibernate.Hibernate;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

import de.uni_due.s3.jack3.entities.stagetypes.r.RSubmission;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.services.utils.DBHelper;
import de.uni_due.s3.jack3.utils.EntityReflectionHelper;

/**
 * Service for managing entities derived from {@link StageSubmission}.
 */
@Stateless
public class StageSubmissionService extends AbstractServiceBean {
	@Inject
	private BaseService baseService;

	public void persistStageSubmission(StageSubmission stageSubmission) {
		baseService.persist(stageSubmission);
	}

	public StageSubmission mergeStageSubmission(StageSubmission stageSubmission) {
		return baseService.merge(stageSubmission);
	}

	/**
	 * Loads a submission with lazy data by ID regularly from database.
	 *
	 * @return Stage submission with all lazy data
	 * @see #getStageSubmissionWithLazyDataByStageSubmissionIDFromEnvers(long)
	 */
	public Optional<StageSubmission> getStageSubmissionWithLazyData(long stageSubmissionID) {
		final EntityManager em = getEntityManager();
		final TypedQuery<StageSubmission> query = em.createNamedQuery(
				StageSubmission.STAGESUBMISSION_WITH_LAZY_DATA_BY_STAGESUBMISSION_ID, StageSubmission.class);
		query.setParameter("id", stageSubmissionID);

		Optional<StageSubmission> stagesubmission = DBHelper.getOneOrZeroRemovingDuplicates(query);

		stagesubmission.ifPresent(sub -> Hibernate.initialize(sub.getVariableValues().keySet()));
		stagesubmission.ifPresent(sub -> Hibernate.initialize(sub.getVariableValues().values()));

		return stagesubmission;
	}

	/**
	 * Loads a submission with lazy data by ID from Envers.
	 *
	 * @return Stage submission with all lazy data from Envers
	 * @see #getStageSubmissionWithLazyData(long)
	 */
	public Optional<StageSubmission> getStageSubmissionWithLazyDataByStageSubmissionIDFromEnvers(
			long stageSubmissionID) {
		AuditReader auditReader = AuditReaderFactory.get(getEntityManager());
		List<Number> revisionNumbers = auditReader.getRevisions(StageSubmission.class, stageSubmissionID);
		StageSubmission result = auditReader.find(StageSubmission.class, stageSubmissionID,
				revisionNumbers.get(revisionNumbers.size() - 1));

		if (result == null) {
			return Optional.empty();
		}

		EntityReflectionHelper.hibernateInitializeObjectGraph(result);

		return Optional.of(result);
	}

	public Optional<RSubmission> getRSubmissionWithLazyDataById(long id) {
		final EntityManager em = getEntityManager();
		final TypedQuery<RSubmission> query = em.createNamedQuery(RSubmission.RSUBMISSION_WITH_LAZY_DATA_BY_ID,
				RSubmission.class);
		query.setParameter("id", id);
		return DBHelper.getOneOrZero(query);
	}

}
