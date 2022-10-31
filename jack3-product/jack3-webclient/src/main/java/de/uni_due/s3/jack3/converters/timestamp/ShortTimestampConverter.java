package de.uni_due.s3.jack3.converters.timestamp;

import javax.faces.convert.FacesConverter;

@FacesConverter(value = "shortTimestampConverter")
public class ShortTimestampConverter extends AbstractTimestampConverter {

	public ShortTimestampConverter() {
		super("timestamp.formatter.short", true);
	}

}
