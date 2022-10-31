package de.uni_due.s3.jack3.beans;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.map.HashedMap;

import de.uni_due.s3.jack3.business.ConfigurationBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.tenant.ProfileField;

@ViewScoped
@Named
public class RegisterView extends AuthenticationView implements Serializable {

	/** Generated serial version UID. */
	private static final long serialVersionUID = -733972297217065347L;

	@Inject
	UserBusiness userBusiness;

	@Inject
	ConfigurationBusiness configurationBusiness;

	private String email;

	private List<ProfileField> mandatoryProfileFields;

	private Map<ProfileField,String> profileFieldValues;

	private boolean privacyPolicyAccepted;

	@PostConstruct
	void init() {
		this.mandatoryProfileFields = userBusiness.getMandatoryProfileFields();
		profileFieldValues = new HashedMap<>();
		for (final ProfileField field : mandatoryProfileFields) {
			final String defaultValue = field.getDefaultContent();
			if (defaultValue != null && !defaultValue.isBlank()) {
				profileFieldValues.put(field,defaultValue);
			}
		}
	}

	public void ensureRegistrationIsEnabled() throws IOException {
		if (!isRegistrationEnabled()) {
			sendErrorResponse(HttpServletResponse.SC_FORBIDDEN,"Registration disabled.");
		}
	}

	public String doRegister() {
		if (userBusiness.getUserByEmail(email).isPresent()) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR,
				"registrationView.emailTakenSummary",
				"registrationView.emailTakenDetails");
			return "";
		}

		try {
			userBusiness.registerUser(email,profileFieldValues,getResourceBundle(),getTenantUrl());
			addGlobalFacesMessage(FacesMessage.SEVERITY_INFO,"registrationView.successSummary","registrationView.successDetails");
		} catch (MessagingException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR,
				"registrationView.mailNotSentSummary",
				"registrationView.mailNotSentDetails");
		}
		return viewId.getLogin().toOutcome();
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(final String email) {
		this.email = email;
	}

	public List<ProfileField> getMandatoryProfileFields() {
		return mandatoryProfileFields;
	}

	public Map<ProfileField, String> getProfileFieldValues() {
		return profileFieldValues;
	}

	public void setProfileFieldValues(Map<ProfileField, String> profileFieldValues) {
		this.profileFieldValues = profileFieldValues;
	}

	public boolean isPrivacyPolicyAccepted() {
		return privacyPolicyAccepted;
	}

	public void setPrivacyPolicyAccepted(boolean privacyPolicyAccepted) {
		this.privacyPolicyAccepted = privacyPolicyAccepted;
	}

	public String getEmailPattern() {
		return emailPattern.get();
	}
}
