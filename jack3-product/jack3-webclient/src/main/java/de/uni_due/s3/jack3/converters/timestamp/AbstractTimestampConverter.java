package de.uni_due.s3.jack3.converters.timestamp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.ResourceBundle;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import de.uni_due.s3.jack3.utils.JackStringUtils;

/**
 * Abstract class for providing a localized one-way-converter for timestamps ({@link Temporal} objects). You can simply
 * add other converters that depend on a timestamp pattern by using the inherited constructor and pass the pattern.
 * 
 * @author lukas.glaser
 */
public abstract class AbstractTimestampConverter implements Converter<LocalDateTime> {

	private final ResourceBundle msg;
	private final DateTimeFormatter formatter;

	/**
	 * Creates a new converter using a specified pattern. Use this constructur for your inherited converter as in the
	 * following examples:
	 * 
	 * <pre>
	 * super("timestamp.formatter.short", true);
	 * super("yyyy-MM-dd", false);
	 * </pre>
	 * 
	 * @param pattern
	 *            The pattern for the underlying {@link DateTimeFormatter}.
	 * @param isLocalized
	 *            If {@code True}, the passed pattern must be a key for a localization entry. Otherwise the pattern must
	 *            be a valid pattern for a {@link DateTimeFormatter}.
	 */
	protected AbstractTimestampConverter(String pattern, boolean isLocalized) {
		final FacesContext context = FacesContext.getCurrentInstance();
		msg = context.getApplication().getResourceBundle(context, "msg");
		if (isLocalized) {
			formatter = DateTimeFormatter.ofPattern(getLocalizedText(pattern), context.getViewRoot().getLocale());
		} else {
			formatter = DateTimeFormatter.ofPattern(pattern, context.getViewRoot().getLocale());
		}
	}

	@Override
	public LocalDateTime getAsObject(FacesContext context, UIComponent component, String value) {
		if (JackStringUtils.isBlank(value)) {
			return null;
		}
		try {
			return LocalDateTime.parse(value, formatter);
		} catch (DateTimeParseException e) {
			throw new ConverterException("Conversion failed for input: " + value, e);
		}
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, LocalDateTime value) {
		if (value == null) {
			return null;
		}

		return getTimestampAsString(value);
	}

	/**
	 * Converts a date-time to a string representation. This method is attended to be overwritten by special converters
	 * e.g. to get a relative timestamp. By default the underlaying formatter is used.
	 * 
	 * @see DateTimeFormatter#ofPattern(String)
	 */
	protected String getTimestampAsString(Temporal value) {
		if (value == null) {
			return null;
		}
		return formatter.format(value);
	}

	/**
	 * Returns a localized string by the given key.
	 */
	protected final String getLocalizedText(String key) {
		return msg.getString(key);
	}

}
