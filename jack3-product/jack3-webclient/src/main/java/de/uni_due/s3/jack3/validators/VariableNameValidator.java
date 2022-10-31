package de.uni_due.s3.jack3.validators;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

@FacesValidator("variableNameValidator")
public class VariableNameValidator implements Validator<String> {

	private static final String INVALID_INPUT = "global.invalidInput";

	@Override
	public void validate(FacesContext context, UIComponent component, String newVariableName) throws ValidatorException {
		final String oldVariableName = ((UIInput) component).getValue().toString();

		
		// Check if string is empty
		if ((newVariableName == null) || newVariableName.isBlank()) {
			throw new ValidatorException(
					new FacesMessage(FacesMessage.SEVERITY_ERROR, getLocalizedMessage(INVALID_INPUT),
							getLocalizedMessage("global.required")));
		}	

		// The new name is always valid if old and new values are equal. We check this to prevent validation failure on
		// AJAX update.
		if (oldVariableName.equals(newVariableName)) {
			return;
		}

		//First character must be letter
		if(!newVariableName.subSequence(0, 1).toString().matches("[a-zA-Z]+")) {
			throw new ValidatorException(
					new FacesMessage(FacesMessage.SEVERITY_ERROR, getLocalizedMessage(INVALID_INPUT),
							getLocalizedMessage("exerciseEdit.firstCharacterLetter")));
		}

		// Other characters can be letters, digits and underscore
		if(!newVariableName.matches("[a-zA-Z]+[a-zA-Z0-9_]*")) {
			throw new ValidatorException(
					new FacesMessage(FacesMessage.SEVERITY_ERROR, getLocalizedMessage(INVALID_INPUT),
							getLocalizedMessage("global.onlyLetterNumberUnderscore")));
		}
	}
	
	private String getLocalizedMessage(String i18nUri) {
		return FacesContext.getCurrentInstance().getApplication()
				.getResourceBundle(FacesContext.getCurrentInstance(), "msg").getString(i18nUri);
	}
	

}
