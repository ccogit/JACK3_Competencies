package de.uni_due.s3.jack3.services;

import javax.ejb.Stateless;
import javax.inject.Inject;

import de.uni_due.s3.jack3.entities.tenant.Result;

/**
 * Service for managing {@link Result} entities.
 */
@Stateless
public class ResultService extends AbstractServiceBean {

	@Inject
	private BaseService baseService;

	public void persistResult(Result result) {
		baseService.persist(result);
	}

}
