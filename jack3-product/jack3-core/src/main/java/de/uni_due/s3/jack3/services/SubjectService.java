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

	public boolean persistSubject(Subject subject) {
		try {
			baseService.persist(subject);
			return true;
		} catch (Exception e) {
			getLogger().error("Error while trying to persist " + subject, e);
			return false;
		}
	}

	public boolean mergeSubject(Subject subject) {
		try {
			baseService.persist(subject);
			return true;
		} catch (Exception e) {
			getLogger().error("Error while trying to update " + subject, e);
			return false;
		}
	}

	public void deleteSubject(Subject subject) {
		baseService.deleteEntity(subject);
	}

}
