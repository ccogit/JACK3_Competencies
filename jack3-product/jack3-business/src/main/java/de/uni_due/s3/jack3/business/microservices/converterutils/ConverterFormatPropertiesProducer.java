package de.uni_due.s3.jack3.business.microservices.converterutils;

import de.uni_due.s3.evaluator_api.converter.properties.ConverterFormatDecimalsType;
import de.uni_due.s3.evaluator_api.converter.properties.ConverterFormatProperties;
import de.uni_due.s3.evaluator_api.converter.properties.ConverterFormatSiPrefixType;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.Placeholder;

public class ConverterFormatPropertiesProducer {

	public static final ConverterFormatProperties NONE = new ConverterFormatProperties(ConverterFormatSiPrefixType.NONE,
			ConverterFormatDecimalsType.NONE);

	public static ConverterFormatProperties byPlaceholder(Placeholder placeholder) {
		ConverterFormatDecimalsType decimalsType = ConverterFormatDecimalsTypeProducer.byPlaceholder(placeholder);
		ConverterFormatSiPrefixType siprefixesType = ConverterFormatSiPrefixTypeProducer.byPlaceholder(placeholder);
		return new ConverterFormatProperties(siprefixesType, decimalsType);
	}

}
