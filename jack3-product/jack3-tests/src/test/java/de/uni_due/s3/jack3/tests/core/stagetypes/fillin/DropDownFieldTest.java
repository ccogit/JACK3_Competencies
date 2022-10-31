package de.uni_due.s3.jack3.tests.core.stagetypes.fillin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.stagetypes.fillin.DropDownField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStageField;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;

@NeedsExercise
class DropDownFieldTest extends AbstractFillInStageFieldTest {

	private FillInStage stage;

	@Override
	protected FillInStage getNewStage() {
		return new FillInStage();
	}

	@Override
	protected FillInStageField addNewField() {
		DropDownField field = new DropDownField("First Question", 0);
		stage.addDropDownField(field);
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

		stage.addDropDownField(new DropDownField("First Question", 0));
		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();

		assertEquals(1, stage.getDropDownFields().size());
		assertEquals("First Question", stage.getDropDownFields().stream().findAny().get().getName());
		assertEquals(0, stage.getDropDownFields().stream().findAny().get().getOrderIndex());
	}

	@Test
	void testRandomize() {
		DropDownField field = new DropDownField();
		field.setName("");
		field.setRandomize(true);
		stage.addDropDownField(field);
		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();

		assertTrue(stage.getDropDownFields().stream().findAny().get().getRandomize());

		stage.getDropDownFields().stream().findAny().get().setRandomize(false);
		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();

		assertFalse(stage.getDropDownFields().stream().findAny().get().getRandomize());
	}

	@Test
	void addGetAndRemoveItemsTest() {
		stage.addDropDownField(new DropDownField("What is The Answer?", 0));
		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();

		// Testing without any Items
		DropDownField field = stage.getDropDownFields().stream().findAny().get();
		assertTrue(field.getItems().isEmpty());

		// Testing addItem
		field.addItem("good looking answer");
		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();

		field = stage.getDropDownFields().stream().findAny().get();
		assertEquals(1, field.getItems().size());

		// Testing removeItem
		field.addItem("Also good looking answer");
		field.removeItem();
		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();

		field = stage.getDropDownFields().stream().findAny().get();
		assertEquals(1, field.getItems().size());

	}

	@Test
	void getAnswerOptionsModellTest() {

		stage.addDropDownField(new DropDownField("What is The Answer?", 0));
		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();

		assertEquals(new ArrayList<>(),
				stage.getDropDownFields().stream().findAny().get().getAnswerOptionsModell().getWrappedData());

		DropDownField field = stage.getDropDownFields().stream().findAny().get();

		field.addItem("Item");
		field.addItem("Another Item");
		ArrayList<String> list = new ArrayList<>();
		list.add(field.getItems().get(0));
		list.add(field.getItems().get(1));

		assertEquals(list,
				stage.getDropDownFields().stream().findAny().get().getAnswerOptionsModell().getWrappedData());

		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();

		assertEquals(list,
				stage.getDropDownFields().stream().findAny().get().getAnswerOptionsModell().getWrappedData());

	}

	@Test
	void reorderAnswerOptionsTest() {
		stage.addDropDownField(new DropDownField("What is The Answer?", 0));
		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();
		DropDownField dropDownField = stage.getDropDownFields().stream().findAny().get();
		dropDownField.addItem("A)");
		dropDownField.addItem("B)");
		dropDownField.addItem("C)");
		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();

		dropDownField = stage.getDropDownFields().stream().findAny().get();

		assertEquals(3, dropDownField.getItems().size());
		assertEquals("B) 2", dropDownField.getItems().get(1));

		dropDownField.reorderAnswerOptions(1, 2);
		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();
		dropDownField = stage.getDropDownFields().stream().findAny().get();

		assertEquals(3, dropDownField.getItems().size());
		assertEquals("B) 2", dropDownField.getItems().get(2));
	}

	/**
	 * This test checks the deep copy of a complete drop down field
	 * with following fields set:
	 *
	 * - name
	 * - orderIndex
	 * - rules
	 * - items
	 * - randomize
	 */
	@Test
	void deepCopyOfFullDropDownField() {
		DropDownField originDropDownField;
		DropDownField deepCopyOfDropDownField;

		originDropDownField = new DropDownField("deep copy test of a " + "drop down field", 12);
		originDropDownField.setRandomize(true);
		originDropDownField.addItem("item one");
		originDropDownField.addItem("item two");

		deepCopyOfDropDownField = originDropDownField.deepCopy();

		assertNotEquals(originDropDownField, deepCopyOfDropDownField, "The drop down field is the origin itself.");
		assertEquals("deep copy test of a drop down field", deepCopyOfDropDownField.getName(),
				"The names of the drop down field are different");
		assertEquals(12, deepCopyOfDropDownField.getOrderIndex(),
				"The order indexes of the drop down field are different");

		assertTrue(deepCopyOfDropDownField.getRandomize(), "The drop down field is not randomize.");
		assertFalse(deepCopyOfDropDownField.getItems().isEmpty(), "The drop down field has no items.");
		assertEquals(2, deepCopyOfDropDownField.getItems().size(),
				"The item size of drop down field is not equalt to 2");
		assertEquals("item one 1", deepCopyOfDropDownField.getItems().get(0), "The name of first item is different.");
		assertEquals("item two 2", deepCopyOfDropDownField.getItems().get(1), "The name of second item is different.");
	}

}
