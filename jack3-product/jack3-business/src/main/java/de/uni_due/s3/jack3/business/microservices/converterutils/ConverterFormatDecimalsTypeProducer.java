package de.uni_due.s3.jack3.business.microservices.converterutils;

import de.uni_due.s3.evaluator_api.converter.properties.ConverterFormatDecimalsType;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.Placeholder;

public class ConverterFormatDecimalsTypeProducer {

	public static ConverterFormatDecimalsType byPlaceholder(Placeholder placeholder) {
		return byInputString(placeholder.decimals);
	}

	private static ConverterFormatDecimalsType byInputString(String decimals) {
		if ("0".equals(decimals))
			return ConverterFormatDecimalsType.PLUS_0;
		else if ("1".equals(decimals))
			return ConverterFormatDecimalsType.PLUS_1;
		else if ("2".equals(decimals))
			return ConverterFormatDecimalsType.PLUS_2;
		else if ("3".equals(decimals))
			return ConverterFormatDecimalsType.PLUS_3;
		else if ("4".equals(decimals))
			return ConverterFormatDecimalsType.PLUS_4;
		else if ("5".equals(decimals))
			return ConverterFormatDecimalsType.PLUS_5;
		else if ("6".equals(decimals))
			return ConverterFormatDecimalsType.PLUS_6;
		else if ("7".equals(decimals))
			return ConverterFormatDecimalsType.PLUS_7;
		else if ("8".equals(decimals))
			return ConverterFormatDecimalsType.PLUS_8;
		else if ("9".equals(decimals))
			return ConverterFormatDecimalsType.PLUS_9;
		else if ("10".equals(decimals))
			return ConverterFormatDecimalsType.PLUS_10;
		else if ("11".equals(decimals))
			return ConverterFormatDecimalsType.PLUS_11;
		else if ("12".equals(decimals))
			return ConverterFormatDecimalsType.PLUS_12;
		else if ("-1".equals(decimals))
			return ConverterFormatDecimalsType.MINUS_1;
		else if ("-2".equals(decimals))
			return ConverterFormatDecimalsType.MINUS_2;
		else if ("-3".equals(decimals))
			return ConverterFormatDecimalsType.MINUS_3;
		else if ("-4".equals(decimals))
			return ConverterFormatDecimalsType.MINUS_4;
		else if ("-5".equals(decimals))
			return ConverterFormatDecimalsType.MINUS_5;
		else if ("-6".equals(decimals))
			return ConverterFormatDecimalsType.MINUS_6;
		else if ("-7".equals(decimals))
			return ConverterFormatDecimalsType.MINUS_7;
		else if ("-8".equals(decimals))
			return ConverterFormatDecimalsType.MINUS_8;
		else if ("-9".equals(decimals))
			return ConverterFormatDecimalsType.MINUS_9;
		else if ("-10".equals(decimals))
			return ConverterFormatDecimalsType.MINUS_10;
		else if ("-11".equals(decimals))
			return ConverterFormatDecimalsType.MINUS_11;
		else if ("-12".equals(decimals))
			return ConverterFormatDecimalsType.MINUS_12;
		else
			return ConverterFormatDecimalsType.NONE;
	}

}
