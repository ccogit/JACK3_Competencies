package de.uni_due.s3.jack3.converters.timestamp;

import javax.faces.convert.FacesConverter;

@FacesConverter(value = "longTimestampConverter")
public class LongTimestampConverter extends AbstractTimestampConverter {

	public LongTimestampConverter() {
		super("timestamp.formatter.long", true);
	}

}
