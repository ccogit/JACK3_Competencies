package de.uni_due.s3.jack3.business;

import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import de.uni_due.s3.jack3.entities.tenant.LtiLaunch;
import de.uni_due.s3.jack3.services.LtiService;
import de.uni_due.s3.jack3.utils.StringGenerator;

@RequestScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class LtiBusiness extends AbstractBusiness {

	@Inject
	private LtiService ltiService;

	public LtiLaunch prepareLaunch(String userName, String consumerInstanceGuid, String resourceLinkId,
			String contextLabel, String returnUrl) {

		Optional<LtiLaunch> launch = ltiService.getLaunch(consumerInstanceGuid, resourceLinkId, userName);
		if (launch.isPresent()) {
			return launch.get();
		}

		return createNewLaunch(consumerInstanceGuid, resourceLinkId, userName, contextLabel, returnUrl);
	}

	private LtiLaunch createNewLaunch(String consumerInstanceGuid, String resourceLinkId, String userName,
			String contextLabel, String returnUrl) {
		final String token = StringGenerator.forPasswords().build().generate();
		LtiLaunch launch = new LtiLaunch(consumerInstanceGuid, resourceLinkId, userName, token, contextLabel, returnUrl);
		return ltiService.persist(launch);
	}
}
