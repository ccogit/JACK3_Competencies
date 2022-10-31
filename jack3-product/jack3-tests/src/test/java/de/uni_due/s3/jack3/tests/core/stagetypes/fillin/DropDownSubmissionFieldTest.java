package de.uni_due.s3.jack3.tests.core.stagetypes.fillin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.enums.EFillInSubmissionFieldType;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.DropDownField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.DropDownSubmissionField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

@NeedsExercise
class DropDownSubmissionFieldTest extends AbstractContentTest {

	private FillInStage stage;

	private DropDownField field;

	private FillInStage getNewStage() {
		return new FillInStage();
	}

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();

		field = new DropDownField("name", 0);

		stage = getNewStage();
		exercise.addStage(stage);
		stage.addDropDownField(field);
		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();

	}

	@Test
	void testConstructor() {
		List<String> items = new java.util.ArrayList<>(3);
		items.add("A");
		items.add("B");
		items.add("C");
		DropDownSubmissionField stagesubmissionField = new DropDownSubmissionField(field.getName(),
				EFillInSubmissionFieldType.TYPE_DROP_DOWN_FIELD,
				items,field.getRandomize(), new ArrayList<String>());


		assertEquals(field.getName(), stagesubmissionField.getFieldName());
		assertEquals(3, stagesubmissionField.getItems().size());
		assertEquals(EFillInSubmissionFieldType.TYPE_DROP_DOWN_FIELD, stagesubmissionField.getFieldType());
	}

	@Test
	void testItems() {
		DropDownSubmissionField stagesubmissionField = new DropDownSubmissionField(field.getName(),
				EFillInSubmissionFieldType.TYPE_DROP_DOWN_FIELD,
				new ArrayList<String>(),field.getRandomize(), new ArrayList<String>());

		assertTrue(stagesubmissionField.getItems().isEmpty());

		List<String> items = new java.util.ArrayList<>(3);
		items.add("A");
		items.add("B");
		items.add("C");

		stagesubmissionField.setItems(items);

		assertEquals("A", stagesubmissionField.getItems().get(0));
		assertEquals("B", stagesubmissionField.getItems().get(1));
		assertEquals("C", stagesubmissionField.getItems().get(2));
	}

	@Test
	void testIsDropDownRandomized() {
		field.setRandomize(true);
		DropDownSubmissionField stagesubmissionField = new DropDownSubmissionField(field.getName(),
				EFillInSubmissionFieldType.TYPE_DROP_DOWN_FIELD,
				new ArrayList<String>(),field.getRandomize(), new ArrayList<String>());

		assertTrue(stagesubmissionField.isDropDownRandomized());

		field.setRandomize(false);
		stagesubmissionField = new DropDownSubmissionField(field.getName(),
				EFillInSubmissionFieldType.TYPE_DROP_DOWN_FIELD,
				new ArrayList<String>(),field.getRandomize(), new ArrayList<String>());

		assertFalse(stagesubmissionField.isDropDownRandomized());

	}

	@Test
	void testUserInput() {
		DropDownSubmissionField stagesubmissionField = new DropDownSubmissionField();

		assertNull(stagesubmissionField.getUserInput());

		stagesubmissionField.setUserInput("Apple");

		assertEquals("Apple", stagesubmissionField.getUserInput());
	}

}
