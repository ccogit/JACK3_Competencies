package de.uni_due.s3.jack3.beans;

import java.io.Serializable;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.security.auth.login.CredentialException;

import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.tenant.ProfileField;
import de.uni_due.s3.jack3.entities.tenant.User;

@ViewScoped
@Named
public class MyAccountView extends AbstractView implements Serializable {

	@Inject
	private UserBusiness userBusiness;

	private static final long serialVersionUID = -4193486252955218142L;

	private String loginName;

	private String oldEmail;

	private String oldPassword;

	private String newPassword;

	private String newEmail;

	public void loadMyAccount() {
		loginName = getCurrentUser().getLoginName();
		newEmail = oldEmail = getCurrentUser().getEmail();
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getEmail() {
		return oldEmail;
	}

	public void setOldEmail(String oldEmail) {
		this.oldEmail = oldEmail;
	}

	public void setNewEmail(final String newEmail) {
		this.newEmail = newEmail;
	}

	public String getNewEmail() {
		return newEmail;
	}

	public Set<Entry<ProfileField, String>> getProfileFields() {
		return getCurrentUser().getProfileData().entrySet();
	}

	public String getNewPassword() {
		return "";
	}

	public void setNewPassword(final String newPassword) {
		this.newPassword = newPassword;
	}

	public String getOldPassword() {
		return "";
	}

	public void setOldPassword(final String oldPassword) {
		this.oldPassword = oldPassword;
	}

	public void changePassword() {
		try {
			userBusiness.changeUserPassword(getCurrentUser(), oldPassword, newPassword);
			addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "myaccount.passwordChanged", null);
		} catch (final CredentialException e) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "myaccount.wrongPassword", null);
		}
	}

	/**
	 * The @EmailValidator already checks that the Email is valid
	 */
	public void changeEmail() {
		User updatedUser = getCurrentUser();
		updatedUser.setEmail(newEmail);
		userBusiness.updateUser(updatedUser);
		setOldEmail(newEmail);
		addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "myaccount.emailChanged", null);
	}

}
