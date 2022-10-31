package de.uni_due.s3.jack3.services;

import de.uni_due.s3.jack3.entities.tenant.Subject;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.List;

/**
 * Service for managing {@link Subject} entities.
 */

@Stateless
public class SubjectService extends AbstractServiceBean {

	@Inject BaseService baseService;

	public List<Subject> getAllSubjects() {
		return baseService.findAll(Subject.class);
	}

	public void persistSubject(Subject subject) {
		baseService.persist(subject);
	}

	public Subject mergeSubject(Subject subject) {
		return baseService.merge(subject);
	}

	public void deleteSubject(Subject subject) {
		baseService.deleteEntity(subject);
	}

}
