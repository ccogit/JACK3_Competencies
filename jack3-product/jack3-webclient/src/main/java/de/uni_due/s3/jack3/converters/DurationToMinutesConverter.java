package de.uni_due.s3.jack3.converters;

import java.time.Duration;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

/**
 * Performs conversions between a {@link Duration} object and the time in minutes.
 */
@FacesConverter(value = "durationToMinutesConverter")
public class DurationToMinutesConverter implements Converter<Duration> {

	@Override
	public Duration getAsObject(FacesContext context, UIComponent component, String value) {
		try {
			final long minutes = Long.parseLong(value);
			return Duration.ofMinutes(minutes);
		} catch (NumberFormatException e) {
			throw new ConverterException("Conversion failed for input: " + value, e);
		}
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Duration value) {
		if (value == null) {
			return "";
		}

		return String.valueOf(value.toMinutes());
	}

}
