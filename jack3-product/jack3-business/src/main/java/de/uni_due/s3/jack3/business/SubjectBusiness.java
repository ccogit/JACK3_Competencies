package de.uni_due.s3.jack3.business;

import de.uni_due.s3.jack3.entities.tenant.Subject;
import de.uni_due.s3.jack3.services.BaseService;
import de.uni_due.s3.jack3.services.SubjectService;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

@RequestScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class SubjectBusiness extends AbstractBusiness {

	@Inject
	private SubjectService subjectService;

	@Inject
	private BaseService baseService;

	/**
	 * Removes a subject. Prerequisite: there are no references from courses or exercises.
	 * Iterates over courses and exercises and makes sure that there exists no reference to the subject to be deleted.
	 *
	 * @param subject
	 */
	public boolean removeSubject(Subject subject) {
		boolean hasBeenRemoved = false;



		return hasBeenRemoved;
	}

}