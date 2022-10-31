package de.uni_due.s3.jack3.tests.core.stagetypes.fillin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.enums.EFillInSubmissionFieldType;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInSubmission;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInSubmissionField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.SubmissionField;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCSubmission;
import de.uni_due.s3.jack3.tests.utils.AbstractStageSubmissionTest;

class FillInSubmissionTest extends AbstractStageSubmissionTest<FillInStage, FillInSubmission> {

	@Override
	protected FillInStage getNewStage() {
		return new FillInStage();
	}

	@Override
	protected FillInSubmission getNewSubmission() {
		return new FillInSubmission();
	}

	@Test
	void getSubmissionFieldsTest() {
		assertEquals(0, stagesubmission.getSubmissionFields().size());
		FillInField field = new FillInField("Field0", 0);
		stagesubmission.addSubmissionField(new FillInSubmissionField(field.getName(),
				EFillInSubmissionFieldType.TYPE_FILL_IN_FIELD,field.getFormularEditorEnumType(),field.getSize()));
		assertEquals(1, stagesubmission.getSubmissionFields().size());
	}

	@Test
	void getOrderedSubmissionFieldsTest() {
		FillInField fillInfield1 = new FillInField("Field1", 0);
		FillInField fillInfield2 = new FillInField("Field2", 0);
		FillInField fillInfield3 = new FillInField("Field3", 0);
		
		SubmissionField field1 = new FillInSubmissionField(fillInfield1.getName(),
				EFillInSubmissionFieldType.TYPE_FILL_IN_FIELD,fillInfield1.getFormularEditorEnumType(),fillInfield1.getSize());
		SubmissionField field2 = new FillInSubmissionField(fillInfield2.getName(),
				EFillInSubmissionFieldType.TYPE_FILL_IN_FIELD,fillInfield2.getFormularEditorEnumType(),fillInfield2.getSize());
		SubmissionField field3 = new FillInSubmissionField(fillInfield3.getName(),
				EFillInSubmissionFieldType.TYPE_FILL_IN_FIELD,fillInfield3.getFormularEditorEnumType(),fillInfield3.getSize());
		stagesubmission.addSubmissionField(field3);
		stagesubmission.addSubmissionField(field1);
		stagesubmission.addSubmissionField(field2);

		List<SubmissionField> fields = stagesubmission.getOrderedSubmissionFields();

		assertEquals(field3, fields.get(0));
		assertEquals(field1, fields.get(1));
		assertEquals(field2, fields.get(2));
	}

	@Test
	void addSubmissionFieldsTest() {
		FillInField fillInfield1 = new FillInField("Field1", 0);
		FillInField fillInfield2 = new FillInField("Field2", 0);

		FillInSubmissionField field1 = new FillInSubmissionField(fillInfield1.getName(),
				EFillInSubmissionFieldType.TYPE_FILL_IN_FIELD,fillInfield1.getFormularEditorEnumType(),fillInfield1.getSize());
		FillInSubmissionField field2 = new FillInSubmissionField(fillInfield2.getName(),
				EFillInSubmissionFieldType.TYPE_FILL_IN_FIELD,fillInfield2.getFormularEditorEnumType(),fillInfield2.getSize());

		stagesubmission.addSubmissionField(field1);
		stagesubmission.addSubmissionField(field2);

		assertEquals(2, stagesubmission.getSubmissionFields().size());
		assertTrue(stagesubmission.getSubmissionFields().contains(field1));
		assertTrue(stagesubmission.getSubmissionFields().contains(field2));
		
		FillInField fillInfield3 = new FillInField("Field3", 0);
		FillInField fillInfield4 = new FillInField("Field4", 0);
		FillInField fillInfield5 = new FillInField("Field5", 0);

		List<SubmissionField> fields = new ArrayList<>();
		FillInSubmissionField field3 = new FillInSubmissionField(fillInfield3.getName(),
				EFillInSubmissionFieldType.TYPE_FILL_IN_FIELD,fillInfield3.getFormularEditorEnumType(),fillInfield3.getSize());
		FillInSubmissionField field4 = new FillInSubmissionField(fillInfield4.getName(),
				EFillInSubmissionFieldType.TYPE_FILL_IN_FIELD,fillInfield4.getFormularEditorEnumType(),fillInfield4.getSize());
		FillInSubmissionField field5 =  new FillInSubmissionField(fillInfield5.getName(),
				EFillInSubmissionFieldType.TYPE_FILL_IN_FIELD,fillInfield5.getFormularEditorEnumType(),fillInfield5.getSize());
		fields.add(field3);
		fields.add(field4);
		fields.add(field5);

		stagesubmission.addSubmissionFields(fields);

		final Set<SubmissionField> subFields = stagesubmission.getSubmissionFields();
		assertEquals(5, subFields.size());
		assertTrue(subFields.containsAll(fields));
		assertTrue(subFields.contains(field2));
		assertTrue(subFields.contains(field1));

	}

	@Test
	void copyFromStageSubmissionTest() {
		FillInField fillInfield0 = new FillInField("Field0", 0);
		
		FillInSubmissionField subField = new FillInSubmissionField(fillInfield0.getName(),
				EFillInSubmissionFieldType.TYPE_FILL_IN_FIELD,fillInfield0.getFormularEditorEnumType(),fillInfield0.getSize());
		subField.setUserInput("input");

		FillInSubmission toCopy = new FillInSubmission();
		toCopy.addSubmissionField(subField);

		stagesubmission.addSubmissionField(new FillInSubmissionField(fillInfield0.getName(),
				EFillInSubmissionFieldType.TYPE_FILL_IN_FIELD,fillInfield0.getFormularEditorEnumType(),fillInfield0.getSize()));

		stagesubmission.copyFromStageSubmission(toCopy);
		assertEquals(1, stagesubmission.getSubmissionFields().size());
		assertEquals("input",
				((FillInSubmissionField) stagesubmission.getSubmissionFields().stream().findFirst().get())
						.getUserInput());

	}

	@Test
	void exceptionInCopyFromStageSubmissionTest() {
		final MCSubmission wrongSubmission = new MCSubmission();
		assertThrows(IllegalArgumentException.class, () -> {
			stagesubmission.copyFromStageSubmission(wrongSubmission);
		});
	}
}
