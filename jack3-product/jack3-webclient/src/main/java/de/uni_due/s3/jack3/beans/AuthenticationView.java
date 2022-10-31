package de.uni_due.s3.jack3.beans;

import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import de.uni_due.s3.jack3.business.ConfigurationBusiness;
import de.uni_due.s3.jack3.entities.tenant.User;

abstract class AuthenticationView extends AbstractView {

	@Inject
	private ConfigurationBusiness configurationBusiness;

	protected Optional<String> emailPattern;

	@PostConstruct
	public void initEmailPattern() {
		this.emailPattern = configurationBusiness.getSingleValue("RegistrationEmailPattern");
	}

	public boolean isRegistrationEnabled() {
		return emailPattern.isPresent();
	}

	public String getLoginNameRegex() {
		return User.LOGIN_NAME_REGEX;
	}
}
