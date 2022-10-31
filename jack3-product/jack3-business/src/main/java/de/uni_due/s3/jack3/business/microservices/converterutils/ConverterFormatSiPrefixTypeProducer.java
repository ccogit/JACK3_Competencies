package de.uni_due.s3.jack3.business.microservices.converterutils;

import de.uni_due.s3.evaluator_api.converter.properties.ConverterFormatSiPrefixType;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.Placeholder;

public class ConverterFormatSiPrefixTypeProducer {

	public static ConverterFormatSiPrefixType byPlaceholder(Placeholder placeholder) {
		return byInputString(placeholder.siPrefix);
	}

	private static ConverterFormatSiPrefixType byInputString(String siPrefix) {
		if ("base10".equals(siPrefix))
			return ConverterFormatSiPrefixType.BASE10;
		else if ("symbol".equals(siPrefix))
			return ConverterFormatSiPrefixType.SYMBOL;
		else
			return ConverterFormatSiPrefixType.NONE;
	}
}
