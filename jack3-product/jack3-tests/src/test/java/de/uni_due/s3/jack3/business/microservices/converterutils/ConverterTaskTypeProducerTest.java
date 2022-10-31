package de.uni_due.s3.jack3.business.microservices.converterutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.evaluator_api.converter.request.ConverterTaskType;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.Placeholder;

public class ConverterTaskTypeProducerTest {

	@Test
	public void test() {
		assertEquals(ConverterTaskType.LATEX,
				ConverterTaskTypeProducer.byPlaceholder(new Placeholder("", "", "", true, null, null)));

		assertEquals(ConverterTaskType.STRING,
				ConverterTaskTypeProducer.byPlaceholder(new Placeholder("", "", "", false, null, null)));
	}

}
