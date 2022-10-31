package de.uni_due.s3.jack3.tests.core.stagetypes.fillin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.enums.EFillInEditorType;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStageField;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;

@NeedsExercise
class FillInFieldTest extends AbstractFillInStageFieldTest {

	private FillInStage stage;

	@Override
	protected FillInStage getNewStage() {
		return new FillInStage();
	}

	@Override
	protected FillInStageField addNewField() {
		FillInField field = new FillInField("name", 0);
		stage.addFillInField(field);
		return field;
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
	void testConstructor() {
		stage.addFillInField(new FillInField("name", 0));
		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();

		FillInField field = stage.getFillInFields().stream().findAny().get();

		assertEquals("name", field.getName());
		assertEquals(0, field.getOrderIndex());
		assertEquals(FillInField.DEFAULT_SIZE, field.getSize());
		assertEquals(EFillInEditorType.NONE.toString(), field.getFormularEditorType());
	}

	@Test
	void testSize() {
		stage.addFillInField(new FillInField("name", 0));
		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();

		stage.getFillInFields().stream().findAny().get().setSize(25);

		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();

		assertEquals(25, stage.getFillInFields().stream().findAny().get().getSize());
	}

	@Test
	void testFormularEditorType() {
		stage.addFillInField(new FillInField("name", 0));
		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();

		stage.getFillInFields().stream().findAny().get().setFormularEditorType(EFillInEditorType.NUMBER.toString());

		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();

		assertEquals(EFillInEditorType.NUMBER.toString(),
				stage.getFillInFields().stream().findAny().get().getFormularEditorType());
		assertEquals(EFillInEditorType.NUMBER,
				stage.getFillInFields().stream().findAny().get().getFormularEditorEnumType());
	}

	/**
	 * This test checks the deep copy of a default fill in field with following
	 * fields set:
	 * 
	 * - name
	 * - orderIndex
	 * - size (default = 10)
	 * - formularEditorType (default = NONE)
	 */
	@Test
	void deepCopyOfDefaultFillInField() {
		FillInField originFillInField;
		FillInField deepCopyOfFillInField;

		originFillInField = new FillInField("deep copy test of a " + "default fill in field", 12);

		deepCopyOfFillInField = originFillInField.deepCopy();

		assertNotEquals(originFillInField, deepCopyOfFillInField, "The fill in field is the origin itself.");
		assertEquals("deep copy test of a default fill in field", deepCopyOfFillInField.getName(),
				"The name of the fill in field are different");
		assertEquals(12, deepCopyOfFillInField.getOrderIndex(), "The order index of the fill in field are different");
		assertEquals(10, deepCopyOfFillInField.getSize(), "The size of the fill in field are different");
		assertEquals(EFillInEditorType.NONE, deepCopyOfFillInField.getFormularEditorEnumType(),
				"The formular edit type of the fill in field are different");
	}

	/**
	 * This test checks the deep copy of a complete fill in field
	 * with following fields set:
	 * 
	 * - name
	 * - orderIndex
	 * - size
	 * - formularEditorType
	 */
	@Test
	void deepCopyOfFullFillInField() {
		FillInField originFillInField;
		FillInField deepCopyOfFillInField;

		originFillInField = new FillInField("deep copy test of a " + "default fill in field", 12);
		originFillInField.setSize(23);
		originFillInField.setFormularEditorType(EFillInEditorType.NUMBER.toString());


		deepCopyOfFillInField = originFillInField.deepCopy();

		assertNotEquals(originFillInField, deepCopyOfFillInField, "The fill in field is the origin itself.");
		assertEquals("deep copy test of a default fill in field", deepCopyOfFillInField.getName(),
				"The name of the fill in field are different");
		assertEquals(12, deepCopyOfFillInField.getOrderIndex(), "The order index of the fill in field are different");
		assertEquals(23, deepCopyOfFillInField.getSize(), "The size of the fill in field are different");
		assertEquals(EFillInEditorType.NUMBER, deepCopyOfFillInField.getFormularEditorEnumType(),
				"The formular edit type of the fill in field are different");
	}

}
