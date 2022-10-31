package de.uni_due.s3.jack3.services;

import java.util.List;

import javax.ejb.Stateless;

import de.uni_due.s3.jack3.entities.tenant.Job;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.Submission;

@Stateless
public class JobService extends AbstractServiceBean {

	public List<Job> getAllJobsForSubmission(final Submission submission) {
		return getEntityManager()
				.createNamedQuery(Job.ALL_JOBS_FOR_SUBMISSION, Job.class)
				.setParameter("submission", submission)
				.getResultList();
	}

	public long countPendingJobsForStageSubmission(final StageSubmission stageSubmission) {
		return getEntityManager().createNamedQuery(Job.COUNT_PENDING_JOBS_FOR_STAGE_SUBMISSION, Long.class)
				.setParameter("stageSubmission", stageSubmission).getSingleResult();
	}
}
