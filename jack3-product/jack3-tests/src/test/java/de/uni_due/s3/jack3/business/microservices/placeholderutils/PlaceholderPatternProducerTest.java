package de.uni_due.s3.jack3.business.microservices.placeholderutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class PlaceholderPatternProducerTest {

	@Test
	public void test() {
		assertEquals("[input=mcindex_3]", PlaceholderPatternProducer.forMcInputVariable(3));
		assertEquals("[input=name1]", PlaceholderPatternProducer.forInputVariable("name1"));
		assertEquals("[var=name3]", PlaceholderPatternProducer.forExerciseVariable("name3"));
	}
}
