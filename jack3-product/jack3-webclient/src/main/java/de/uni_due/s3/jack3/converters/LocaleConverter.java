package de.uni_due.s3.jack3.converters;

import java.util.Locale;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

@FacesConverter(value = "localeConverter")
public class LocaleConverter implements Converter<Locale> {

	@Override
	public Locale getAsObject(final FacesContext context, final UIComponent component, final String value) {
		return value == null ? null : Locale.forLanguageTag(value);
	}

	@Override
	public String getAsString(final FacesContext context, final UIComponent component, final Locale value) {
		return value == null ? "" : value.toLanguageTag();
	}
}
