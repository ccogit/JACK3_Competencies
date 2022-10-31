package de.uni_due.s3.jack3.converters.timestamp;

import javax.faces.convert.FacesConverter;

@FacesConverter(value = "fullTimestampConverter")
public class FullTimestampConverter extends AbstractTimestampConverter {

	public FullTimestampConverter() {
		super("timestamp.formatter.full", true);
	}

}
