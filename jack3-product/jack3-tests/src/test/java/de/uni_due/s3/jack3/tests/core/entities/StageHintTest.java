package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.tenant.StageHint;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

/**
 * Tests for stage hints, using a MultipleChoice stage
 */
@NeedsExercise
class StageHintTest extends AbstractContentTest {

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();

		exercise.addStage(new MCStage());
		exercise.getStagesAsList().get(0).addHint(new StageHint());
		exercise = baseService.merge(exercise);
	}

	/**
	 * Get the hint of the exercise stage
	 */
	private StageHint getStageHint() {
		return exercise.getStagesAsList().get(0).getHints().get(0);
	}

	@Test
	void changeText() {
		getStageHint().setText("Stage hint text");
		exercise = baseService.merge(exercise);

		assertEquals("Stage hint text", getStageHint().getText());
	}

	@Test
	void changeMalus() {
		getStageHint().setMalus(10);

		exercise = baseService.merge(exercise);
		assertEquals(10, getStageHint().getMalus());
	}

	/**
	 * This test checks that negative values not allowed for malus points.
	 */
	@Test
	void changeMalusToNegative() {
		final StageHint hint = getStageHint();
		assertThrows(IllegalArgumentException.class, () -> {
			hint.setMalus(-10);
		});
	}

	/**
	 * This test checks that values grater than 100 not allowed for malus points.
	 */
	@Test
	void changeMalusToOverHundret() {
		final StageHint hint = getStageHint();
		assertThrows(IllegalArgumentException.class, () -> {
			hint.setMalus(101);
		});
	}

	/**
	 * This test checks the deepCopy of an empty stage hint.
	 */
	@Test
	void deepCopyEmptyStageHint() {
		StageHint deepCopy;
		StageHint originStageHint = new StageHint();

		deepCopy = originStageHint.deepCopy();

		assertNotEquals(originStageHint, deepCopy, "The deepcopy is the origin stage hint itself.");
		assertEquals(originStageHint.getStagehint_order(), deepCopy.getStagehint_order(),
				"The order of stage hint is different.");
		assertEquals(originStageHint.getText(), deepCopy.getText(), "The text of stage hint is different.");
		assertEquals(originStageHint.getMalus(), deepCopy.getMalus(), "The malus of stage hint is different.");
	}

	/**
	 * This test checks the deepCopy of a stage hint, witch contains only a hint order.
	 */
	@Test
	void deepCopyWithoutTextAndMalus() {
		StageHint deepCopy;
		StageHint originStageHint = new StageHint();
		originStageHint.setStagehint_order(23);

		deepCopy = originStageHint.deepCopy();

		assertNotEquals(originStageHint, deepCopy, "The deepcopy is the origin stage hint itself.");
		assertEquals(originStageHint.getStagehint_order(), deepCopy.getStagehint_order(),
				"The order of stage hint is different.");
		assertEquals(originStageHint.getText(), deepCopy.getText(), "The text of stage hint is different.");
		assertEquals(originStageHint.getMalus(), deepCopy.getMalus(), "The malus of stage hint is different.");
	}

	/**
	 * This test checks the deepCopy of a complete stage hint.
	 */
	@Test
	void deepCopyFullStageHint() {

		StageHint deepCopy;
		StageHint originStageHint = new StageHint();
		originStageHint.setStagehint_order(23);
		originStageHint.setText("originStageHint");
		originStageHint.setMalus(42);

		deepCopy = originStageHint.deepCopy();

		assertNotEquals(originStageHint, deepCopy, "The deepcopy is the origin stage hint itself.");
		assertEquals(originStageHint.getStagehint_order(), deepCopy.getStagehint_order(),
				"The order of stage hint is different.");
		assertEquals(originStageHint.getText(), deepCopy.getText(), "The text of stage hint is different.");
		assertEquals(originStageHint.getMalus(), deepCopy.getMalus(), "The malus of stage hint is different.");
	}

}
