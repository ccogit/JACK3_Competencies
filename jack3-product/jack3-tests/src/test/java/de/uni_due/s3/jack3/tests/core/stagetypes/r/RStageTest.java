package de.uni_due.s3.jack3.tests.core.stagetypes.r;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.stagetypes.r.RStage;
import de.uni_due.s3.jack3.entities.stagetypes.r.TestCaseTuple;
import de.uni_due.s3.jack3.tests.utils.AbstractStageTest;

class RStageTest extends AbstractStageTest<RStage> {

	@Override
	protected RStage getNewStage() {
		return new RStage();
	}

	@Override
	protected String getExpectedType() {
		return "r";
	}

	@Test
	void testInitialCode() {
		assertNull(stage.getInitialCode());

		// Change the initial code
		final String initialCode = "daten <- c([var=x], [var=y], [var=z1], [var=z2], [var=z3])";
		stage.setInitialCode(initialCode);
		saveExercise();

		assertEquals(initialCode, stage.getInitialCode());
	}

	@Test
	void testTestcaseTuples() {
		assertTrue(stage.getTestCasetuples().isEmpty());

		// add test case tuple
		final TestCaseTuple tuple = new TestCaseTuple();
		tuple.setName("Test case tuple");
		stage.setTestCasetuples(Arrays.asList(tuple));
		saveExercise();

		final Collection<TestCaseTuple> testcaseTuples = stage.getTestCasetuples();
		assertEquals(1, testcaseTuples.size());
		assertTrue(testcaseTuples.stream().anyMatch(testcases -> testcases.getName().equals("Test case tuple")));
	}

}
