package de.uni_due.s3.jack3.beans;

import java.io.Serializable;
import java.util.Optional;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;

import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.tenant.User;

@ViewScoped
@Named
public class PasswordRetrievalView extends AbstractView implements Serializable {

	/* Generated serial version UID. */
	private static final long serialVersionUID = 2851347096211729565L;

	/* We need this business to lookup the user and change its password. */
	@Inject
	private UserBusiness userBusiness;

	/* The login name entered by the user. */
	private String loginName;

	/**
	 * Retrieves the user's password by first sending a mail with the new password and that succeeding
	 * updating the password also in the database.
	 * 
	 * @return The action's outcome.
	 */
	public String retrieve() {
		// We try to get the user who requested a new password.
		final Optional<User> user = userBusiness.getUserByName(loginName);

		// If no such user is known we just show an error message.
		if (!user.isPresent()) {
			return showErrorMessage("passwordRetrieval.userUnknownDetail");
		}

		// If the user does not have a password set it must be managed by an external system.
		// We just show an appropriate error message.
		if (user.get().getPassword() == null) {
			return showErrorMessage("passwordRetrieval.userNotManagedDetail");
		}

		// We attempt to send a new password to the given user.
		try {
			final String subject = getLocalizedMessage("passwordRetrieval.mailSubject");
			final String content = getLocalizedMessage("passwordRetrieval.mailContent");
			userBusiness.resetUserPassword(user.get(), subject, content);
		} catch (final MessagingException e) {
			getLogger().warn("Failed to send password retrieval mail to " + user.get().getEmail() + ".", e);
			return showErrorMessage("passwordRetrieval.mailNotSentDetail");
		}

		// In case of success we redirect to the login view and display a success message.
		addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "passwordRetrieval.succcessSummary",
				"passwordRetrieval.succcessDetail");
		return viewId.getLogin().toOutcome();
	}

	private String showErrorMessage(final String detailKey) {
		addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "passwordRetrieval.errorSummary", detailKey);
		return "";
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(final String loginName) {
		this.loginName = loginName.strip().toLowerCase();
	}
}
