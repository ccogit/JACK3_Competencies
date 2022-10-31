package de.uni_due.s3.jack3.tests.core.stagetypes.fillin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.enums.EFillInSubmissionFieldType;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInSubmissionField;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

@NeedsExercise
class FillInSubmissionFieldTest extends AbstractContentTest {

	private FillInStage stage;

	private FillInField field;

	private FillInStage getNewStage() {
		return new FillInStage();
	}

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();

		field = new FillInField("name", 0);

		stage = getNewStage();
		exercise.addStage(stage);
		stage.addFillInField(field);
		exercise = baseService.merge(exercise);
		stage = (FillInStage) exercise.getStages().iterator().next();

	}

	@Test
	void testConstructor() {
		FillInSubmissionField stagesubmissionField = new FillInSubmissionField(field.getName(),
				EFillInSubmissionFieldType.TYPE_FILL_IN_FIELD,field.getFormularEditorEnumType(),field.getSize());
		assertEquals(field.getName(), stagesubmissionField.getFieldName());
		assertEquals(EFillInSubmissionFieldType.TYPE_FILL_IN_FIELD, stagesubmissionField.getFieldType());

	}

	@Test
	void testGetSize() {
		field.setSize(50);
		FillInSubmissionField stagesubmissionField = new FillInSubmissionField(field.getName(),
				EFillInSubmissionFieldType.TYPE_FILL_IN_FIELD,field.getFormularEditorEnumType(),field.getSize());
		assertEquals(50, stagesubmissionField.getSize());

	}

	@Test
	void testGetFieldName() {
		FillInSubmissionField stagesubmissionField = new FillInSubmissionField(field.getName(),
				EFillInSubmissionFieldType.TYPE_FILL_IN_FIELD,field.getFormularEditorEnumType(),field.getSize());
		assertEquals(field.getName(), stagesubmissionField.getFieldName());
	}

	@Test
	void testUserInput() {
		FillInSubmissionField stagesubmissionField = new FillInSubmissionField();

		assertNull(stagesubmissionField.getUserInput());

		stagesubmissionField.setUserInput("Apple");

		assertEquals("Apple", stagesubmissionField.getUserInput());

	}

}
