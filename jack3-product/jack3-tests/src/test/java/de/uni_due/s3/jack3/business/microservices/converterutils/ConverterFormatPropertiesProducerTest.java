package de.uni_due.s3.jack3.business.microservices.converterutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.evaluator_api.converter.properties.ConverterFormatDecimalsType;
import de.uni_due.s3.evaluator_api.converter.properties.ConverterFormatProperties;
import de.uni_due.s3.evaluator_api.converter.properties.ConverterFormatSiPrefixType;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.Placeholder;

public class ConverterFormatPropertiesProducerTest {

	@Test
	public void test1() {
		ConverterFormatProperties format = ConverterFormatPropertiesProducer
				.byPlaceholder(new Placeholder("", "", "", false, "12", "symbol"));

		assertEquals(ConverterFormatDecimalsType.PLUS_12, format.decimals);
		assertEquals(ConverterFormatSiPrefixType.SYMBOL, format.siPrefix);
	}

	@Test
	public void test2() {
		ConverterFormatProperties format = ConverterFormatPropertiesProducer
				.byPlaceholder(new Placeholder("", "", "", false, "-3", "base10"));

		assertEquals(ConverterFormatDecimalsType.MINUS_3, format.decimals);
		assertEquals(ConverterFormatSiPrefixType.BASE10, format.siPrefix);
	}

}
