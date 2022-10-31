package de.uni_due.s3.jack3.tests.core.stagetypes.fillin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStageField;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

@NeedsExercise
public abstract class AbstractFillInStageFieldTest extends AbstractContentTest {

	private FillInStage stage;

	protected FillInStage getNewStage() {
		return new FillInStage();
	}

	/**
	 * add exactly one FillInField or one DropDownField to the Stage and returns the field.
	 */
	protected abstract FillInStageField addNewField();

	private final List<FillInStageField> getFillInStageFields() {
		List<FillInStageField> result = new ArrayList<>();
		result.addAll(stage.getFillInFields());
		result.addAll(stage.getDropDownFields());
		return result;
	}

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();

		stage = getNewStage();
		exercise.addStage(stage);
		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();
	}

	@Test
	final void nameTest() {
		FillInStageField field = addNewField();
		field.setName("And what is with this Question?");
		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();
		field = getFillInStageFields().get(0);

		assertEquals("And what is with this Question?", field.getName());
	}

	@Test
	final void orderIndexTest() {
		FillInStageField field = addNewField();
		field.setOrderIndex(3);
		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();
		field = getFillInStageFields().get(0);

		assertEquals(3, field.getOrderIndex());
	}
}
