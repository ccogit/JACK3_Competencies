package de.uni_due.s3.jack3.validators;

import java.io.Serializable;
import java.text.MessageFormat;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

@FacesValidator("custom.acceptanceValidator")
public class AcceptanceValidator implements Validator<Boolean>, Serializable {

	private static final long serialVersionUID = 2400761932713573741L;

	private final String createDefaultMessage(final FacesContext context, final UIComponent component) {
		Object label = component.getAttributes().get("label");

		if (label == null || label.toString().isEmpty()) {
			label = component.getValueExpression("label");
		}
		if (label == null) {
			label = component.getClientId(context);
		}

		return MessageFormat.format(UIInput.REQUIRED_MESSAGE_ID, label);
	}

	@Override
	public void validate(final FacesContext context,final UIComponent component,final Boolean value) {

		if (!value.booleanValue()) {
			String requiredMessage = ((UIInput)component).getRequiredMessage();

			if (requiredMessage == null) {
				requiredMessage = createDefaultMessage(context,component);
			}

			throw new ValidatorException(
				new FacesMessage(FacesMessage.SEVERITY_ERROR,requiredMessage,null));
		}
	}
}
