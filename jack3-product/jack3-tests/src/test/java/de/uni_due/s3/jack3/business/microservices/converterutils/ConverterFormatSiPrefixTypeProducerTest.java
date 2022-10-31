package de.uni_due.s3.jack3.business.microservices.converterutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.evaluator_api.converter.properties.ConverterFormatSiPrefixType;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.Placeholder;

public class ConverterFormatSiPrefixTypeProducerTest {

	@Test
	public void test() {
		assertEquals(ConverterFormatSiPrefixType.BASE10,
				ConverterFormatSiPrefixTypeProducer.byPlaceholder(new Placeholder("", "", "", false, null, "base10")));

		assertEquals(ConverterFormatSiPrefixType.SYMBOL,
				ConverterFormatSiPrefixTypeProducer.byPlaceholder(new Placeholder("", "", "", false, null, "symbol")));

		assertEquals(ConverterFormatSiPrefixType.NONE,
				ConverterFormatSiPrefixTypeProducer.byPlaceholder(new Placeholder("", "", "", false, null, null)));

		assertEquals(ConverterFormatSiPrefixType.NONE,
				ConverterFormatSiPrefixTypeProducer.byPlaceholder(new Placeholder("", "", "", false, null, "")));

	}

}
