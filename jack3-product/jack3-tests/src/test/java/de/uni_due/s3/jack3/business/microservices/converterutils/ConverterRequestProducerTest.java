package de.uni_due.s3.jack3.business.microservices.converterutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.evaluator_api.converter.properties.ConverterFormatDecimalsType;
import de.uni_due.s3.evaluator_api.converter.properties.ConverterFormatProperties;
import de.uni_due.s3.evaluator_api.converter.properties.ConverterFormatSiPrefixType;
import de.uni_due.s3.evaluator_api.converter.request.ConverterRequest;
import de.uni_due.s3.evaluator_api.converter.request.ConverterTask;
import de.uni_due.s3.evaluator_api.converter.request.ConverterTaskType;
import de.uni_due.s3.evaluator_api.properties.EvaluatorDomainType;
import de.uni_due.s3.evaluator_api.properties.EvaluatorProperties;
import de.uni_due.s3.evaluator_api.properties.EvaluatorVariableType;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.Placeholder;

public class ConverterRequestProducerTest {

	@Test
	public void testEmpty() throws Exception {
		ConverterRequest request = ConverterRequestProducer.ofPlaceholdersAndMaps(List.of(), new EvaluatorMaps());
		assertEquals(0, request.getTasks().size());
	}

	@Test
	public void test2Placeholders() throws Exception {
		EvaluatorMaps maps = new EvaluatorMaps();
		maps.addMetaVariable("meta1", 0.123);
		maps.addCheckVariable("meta1", 4.567);
		List<Placeholder> placeholders = List.of(
				new Placeholder("[meta=meta1,latex]", "meta", "meta1", true, null, null),
				new Placeholder("[check=meta1]", "check", "meta1", false, null, null));

		ConverterRequest request = ConverterRequestProducer.ofPlaceholdersAndMaps(placeholders, maps);
		assertEquals(2, request.getTasks().size());
		assertEquals(new ConverterTask(
				new EvaluatorProperties("meta1", EvaluatorVariableType.META, EvaluatorDomainType.MATHEMATICS),
				ConverterTaskType.LATEX,
				new ConverterFormatProperties(ConverterFormatSiPrefixType.NONE, ConverterFormatDecimalsType.NONE)),
				request.getTasks().get(0));

		assertEquals(new ConverterTask(
				new EvaluatorProperties("meta1", EvaluatorVariableType.CHECK, EvaluatorDomainType.MATHEMATICS),
				ConverterTaskType.STRING,
				new ConverterFormatProperties(ConverterFormatSiPrefixType.NONE, ConverterFormatDecimalsType.NONE)),
				request.getTasks().get(1));
	}

}
