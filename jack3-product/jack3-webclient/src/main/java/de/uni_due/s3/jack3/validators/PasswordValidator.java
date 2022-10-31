package de.uni_due.s3.jack3.validators;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

@FacesValidator("passwordValidator")
public class PasswordValidator implements Validator<String> {

	@Override
	public void validate(FacesContext context, UIComponent component, String password) throws ValidatorException {

		UIInput uiInputConfirmPassword = (UIInput) component.getAttributes().get("confirmPassword");
		String confirmPassword = uiInputConfirmPassword.getSubmittedValue().toString();

		String errorCategory = FacesContext.getCurrentInstance().getApplication()
				.getResourceBundle(FacesContext.getCurrentInstance(), "msg").getString("myaccount.passwordError");

		// Let required="true" do its job.
		if (password == null || password.isEmpty() || confirmPassword == null || confirmPassword.isEmpty()) {
			return;
		}

		if (!password.equals(confirmPassword)) {
			uiInputConfirmPassword.setValid(false);
			String errorMessage = FacesContext.getCurrentInstance().getApplication()
					.getResourceBundle(FacesContext.getCurrentInstance(), "msg").getString("myaccount.noMatch");

			throw new ValidatorException(
					new FacesMessage(FacesMessage.SEVERITY_ERROR, errorCategory, errorMessage));
		}

	}
}