package de.uni_due.s3.jack3.validators;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.primefaces.validate.ClientValidator;

@FacesValidator(EmailValidator.VALIDATOR_ID)
public class EmailValidator implements Validator<String>, ClientValidator {

	static final String VALIDATOR_ID = "custom.emailValidator";

	/**
	 * A regular expression to check email addresses taken from a W3C recommendation. See
	 * <a href="http://www.w3.org/TR/html5/forms.html#valid-e-mail-address">
	 * http://www.w3.org/TR/html5/forms.html#valid-e-mail-address</a> for more details.
	 */
	private static final String EMAIL_REGEX = "[äüöa-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+" + "@"
			+ "[äüöa-zA-Z0-9](?:[äüöa-zA-Z0-9-]{0,61}[äüöa-zA-Z0-9])?"
			+ "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*";

	/** A pattern of the regex {@link #EMAIL_REGEX} for shared usage. */
	private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

	public static final boolean isValidEmailAddress(final String address) {
		// Check 1: A simple regular expression test.
		if (!EMAIL_PATTERN.matcher(address).matches()) {
			return false;
		}

		// Check 2: We try to parse the email as an internet address.
		try {
			InternetAddress.parse(address);
			return true;
		}
		catch (final AddressException e) {
			return false;
		}
	}

	@Override
	public void validate(FacesContext context, UIComponent component, String value) throws ValidatorException {
		if (value == null || isValidEmailAddress(value)) {
			return;
		}

		final ResourceBundle bundle = FacesContext.getCurrentInstance()
			.getApplication()
			.getResourceBundle(FacesContext.getCurrentInstance(), "msg");
		throw new ValidatorException(new FacesMessage(
			FacesMessage.SEVERITY_ERROR,
			bundle.getString("myaccount.validationError"),
			"\"" + value + "\" " + bundle.getString("myaccount.invalidMail")));
	}

	@Override
	public Map<String, Object> getMetadata() {
		return null;
	}

	@Override
	public String getValidatorId() {
		return EmailValidator.VALIDATOR_ID;
	}
}