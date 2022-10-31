package de.uni_due.s3.jack3.validators;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.primefaces.validate.ClientValidator;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.uni_due.s3.jack3.beans.AbstractView;

@FacesValidator("custom.jsonValidator")
public class JsonValidator extends AbstractView implements Validator<String>, ClientValidator {

	@Override
	public void validate(FacesContext context, UIComponent component, String value) throws ValidatorException {

		try {
			Type type = new TypeToken<Collection<String>>() {
			}.getType();
			new Gson().fromJson(value, type);
		} catch (Exception e) {
			String detail = "\"" + value + "\" " + getLocalizedMessage("myaccount.validationError");
			String summary = getLocalizedMessage("tenantadmin.general.jsonValidationError");
			throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, detail));
		}
	}

	@Override
	public String getValidatorId() {
		return "custom.jsonValidator";
	}

	@Override
	public Map<String, Object> getMetadata() {
		return new HashMap<>();
	}

}