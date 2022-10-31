package de.uni_due.s3.jack3.business.microservices.converterutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.evaluator_api.converter.properties.ConverterFormatDecimalsType;
import de.uni_due.s3.evaluator_api.converter.properties.ConverterFormatSiPrefixType;
import de.uni_due.s3.evaluator_api.converter.request.ConverterTask;
import de.uni_due.s3.evaluator_api.converter.request.ConverterTaskType;
import de.uni_due.s3.evaluator_api.properties.EvaluatorDomainType;
import de.uni_due.s3.evaluator_api.properties.EvaluatorVariableType;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.business.microservices.evaluatorutils.EvaluatorContextProducer;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.Placeholder;

class ConverterTaskProducerTest {

	@Test
	void createConverterTask1() throws Exception {
		EvaluatorMaps maps = new EvaluatorMaps();
		maps.addMetaVariable("meta1", 0.123);
		maps.addCheckVariable("meta1", 4.567);

		Placeholder description = new Placeholder("[check=meta1]", "check", "meta1", false, null, null);
		ConverterTask actual = ConverterTaskProducer.byPlaceholder(description,
				EvaluatorContextProducer.byEvaluatorMaps(maps));

		assertEquals("meta1", actual.properties.name);
		assertEquals(EvaluatorDomainType.MATHEMATICS, actual.properties.domainType);
		assertEquals(EvaluatorVariableType.CHECK, actual.properties.variableType);
		assertEquals(ConverterTaskType.STRING, actual.type);
		assertEquals(ConverterFormatDecimalsType.NONE, actual.format.decimals);
		assertEquals(ConverterFormatSiPrefixType.NONE, actual.format.siPrefix);
	}

	@Test
	void createConverterTask2() throws Exception {
		EvaluatorMaps maps = new EvaluatorMaps();
		maps.addMetaVariable("meta1", 0.123);
		maps.addCheckVariable("meta1", 4.567);

		Placeholder description = new Placeholder("[meta=meta1.latex,decimals=3,siprefix=symbol]", "meta", "meta1",
				true, "3", "symbol");
		ConverterTask actual = ConverterTaskProducer.byPlaceholder(description,
				EvaluatorContextProducer.byEvaluatorMaps(maps));

		assertEquals("meta1", actual.properties.name);
		assertEquals(EvaluatorDomainType.MATHEMATICS, actual.properties.domainType);
		assertEquals(EvaluatorVariableType.META, actual.properties.variableType);
		assertEquals(ConverterTaskType.LATEX, actual.type);
		assertEquals(ConverterFormatDecimalsType.PLUS_3, actual.format.decimals);
		assertEquals(ConverterFormatSiPrefixType.SYMBOL, actual.format.siPrefix);
	}

}
