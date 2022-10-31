package de.uni_due.s3.jack3.validators;

import java.util.ResourceBundle;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

/**
 * This is a validator for required input. In some cases the "required" indicator is not suitable because we cannot
 * specify detail <u>and</u> summary message. To use this validator, just add:
 * 
 * <pre>
 * validator = "requiredValidator"
 * </pre>
 * 
 * There is no need to specify <code>required=true</code>.
 * 
 * @author lukas.glaser
 *
 */
@FacesValidator("requiredValidator")
public class RequiredValidator implements Validator<String> {

	@Override
	public void validate(FacesContext context, UIComponent component, String value) throws ValidatorException {
		final ResourceBundle msg = FacesContext.getCurrentInstance().getApplication()
				.getResourceBundle(FacesContext.getCurrentInstance(), "msg");

		if (value == null || value.isBlank()) {
			throw new ValidatorException(
					new FacesMessage(FacesMessage.SEVERITY_ERROR, msg.getString("global.invalidInput"),
							msg.getString("global.required")));
		}
	}

}
