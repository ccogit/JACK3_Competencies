package de.uni_due.s3.jack3.services;

import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;

import de.uni_due.s3.jack3.entities.tenant.LtiLaunch;
import de.uni_due.s3.jack3.services.utils.DBHelper;

@Stateless
public class LtiService extends AbstractServiceBean {

	@Inject
	private BaseService baseService;

	public Optional<LtiLaunch> getLaunch(String consumerInstanceGuid, String resourceLinkId, String userName) {
		return DBHelper.getOneOrZero(
			getEntityManager()
				.createNamedQuery(LtiLaunch.GET_BY_LOGINNAME_AND_CONSUMER, LtiLaunch.class)
				.setParameter("userName", userName)
				.setParameter("consumerInstanceGuid", consumerInstanceGuid)
				.setParameter("resourceLinkId",resourceLinkId));
	}

	public LtiLaunch persist(LtiLaunch ltiLaunch) {
		baseService.persist(ltiLaunch);
		return ltiLaunch;
	}
}
