package de.uni_due.s3.jack3.business.microservices.converterutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.evaluator_api.converter.properties.ConverterFormatDecimalsType;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.Placeholder;

public class ConverterFormatDecimalsTypeProducerTest {

	@Test
	public void test() {
		assertEquals(ConverterFormatDecimalsType.PLUS_0,
				ConverterFormatDecimalsTypeProducer.byPlaceholder(new Placeholder("", "", "", false, "0", null)));

		assertEquals(ConverterFormatDecimalsType.MINUS_1,
				ConverterFormatDecimalsTypeProducer.byPlaceholder(new Placeholder("", "", "", false, "-1", null)));

		assertEquals(ConverterFormatDecimalsType.PLUS_1,
				ConverterFormatDecimalsTypeProducer.byPlaceholder(new Placeholder("", "", "", false, "1", null)));

		assertEquals(ConverterFormatDecimalsType.NONE,
				ConverterFormatDecimalsTypeProducer.byPlaceholder(new Placeholder("", "", "", false, "", null)));

		assertEquals(ConverterFormatDecimalsType.NONE,
				ConverterFormatDecimalsTypeProducer.byPlaceholder(new Placeholder("", "", "", false, null, null)));

		assertEquals(ConverterFormatDecimalsType.NONE,
				ConverterFormatDecimalsTypeProducer.byPlaceholder(new Placeholder("", "", "", false, "13", null)));

		assertEquals(ConverterFormatDecimalsType.NONE,
				ConverterFormatDecimalsTypeProducer.byPlaceholder(new Placeholder("", "", "", false, "-13", null)));
	}

}
