package de.uni_due.s3.jack3.beans.stagetypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import de.uni_due.s3.jack3.beans.stagetypes.helpers.FillInSubmissionViewField;
import de.uni_due.s3.jack3.business.stagetypes.FillInStageBusiness;
import de.uni_due.s3.jack3.entities.enums.EFormularEditorPalette;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInSubmission;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.SubmissionField;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.Submission;

public class FillInSubmissionView extends AbstractSubmissionView implements Serializable {

	private static final long serialVersionUID = 2884667639614355282L;

	@Inject
	private FillInStageBusiness fillInStageBusiness;

	private FillInStage stage;
	private FillInSubmission stageSubmission;

	@Override
	public Stage getStage() {
		return stage;
	}

	private String formularEditorPalette  = EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE_NOT_SELECTED.getTypeLabel()+ ".xml";

	public String getFormularEditorPalette() {
		return formularEditorPalette;
	}

	public boolean isFormularEditorPaletteInUse() {
		String noPalleteSelected = EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE_NOT_SELECTED.getTypeLabel()+ ".xml";
		String paletteInvisible =  EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_NO_PALETTE.getTypeLabel()+ ".xml";
		return (!formularEditorPalette.equals(noPalleteSelected) && !formularEditorPalette.equals(paletteInvisible));
	}

	private List<FillInSubmissionViewField> fillInSubmissionViewFields;

	public List<FillInSubmissionViewField> getFillInSubmissionViewFields() {
		return fillInSubmissionViewFields;
	}

	private void createFillInSubmissionViewFields(Submission submission) {
		List<SubmissionField> stagesubmissionFields = stageSubmission.getOrderedSubmissionFields();

		String text = exercisePlayerBusiness.resolvePlaceholders(stage.getTaskDescription(), submission,
				stageSubmission,
				stage,
				true);
		fillInSubmissionViewFields = new ArrayList<>(stagesubmissionFields.size());

		Pattern patternInputOrSelect = Pattern.compile(FillInStageBusiness.TASK_DESCRIPTION_DIVIDER_PATTERN);
		Matcher matcherInputOrSelect = patternInputOrSelect.matcher(text);

		int beginIndex = 0;
		while (matcherInputOrSelect.find()) {

			String htmlCodeBeforeNotTextField = text.substring(beginIndex, matcherInputOrSelect.start());
			if (htmlCodeBeforeNotTextField.length() > 0) {
				fillInSubmissionViewFields.add(new FillInSubmissionViewField(htmlCodeBeforeNotTextField));
			}

			String notTextField = matcherInputOrSelect.group();
			createInputField(notTextField, stagesubmissionFields);

			beginIndex = matcherInputOrSelect.end();
		}
		String htmlCodeAfterLastNotTextField = text.substring(beginIndex);
		if (htmlCodeAfterLastNotTextField.length() > 0) {
			fillInSubmissionViewFields.add(new FillInSubmissionViewField(htmlCodeAfterLastNotTextField));
		}
	}

	private void createInputField(String notTextField, List<SubmissionField> stagesubmissionFields) {
		if (   (FillInStageBusiness.HTML_INPUT.equals(notTextField.substring(0, 6)))
				|| (FillInStageBusiness.HTML_SELECT.equals(notTextField.substring(0, 7)))) {

			String nameValue = fillInStageBusiness.getAttributeValue("name", notTextField);
			SubmissionField stagesubmissionFieldForName = null;

			// REVIEW ms: Is this loop the most efficient way to find the field?
			for (SubmissionField field : stagesubmissionFields) {
				if (nameValue.equals(field.getFieldName())) {
					stagesubmissionFieldForName = field;
					break;
				}
			}

			if (stagesubmissionFieldForName != null) {
				FillInSubmissionViewField stagesubmissionField = new FillInSubmissionViewField(stagesubmissionFieldForName);
				fillInSubmissionViewFields.add(stagesubmissionField);
			}
		}
	}

	public void setFormularEditorPalette() {
		formularEditorPalette = stage.getFormularEditorPaletteEnum().getTypeLabel() + ".xml";
	}

	@Override
	public void setStageSubmission(Submission submission, StageSubmission stageSubmission, Stage stage) {
		if (!(stageSubmission instanceof FillInSubmission)) {
			throw new IllegalArgumentException("FillInSubmissionView must be used with instances of FillInSubmission");
		}
		if (!(stage instanceof FillInStage)) {
			throw new IllegalArgumentException("FillInSubmissionView must be used with instances of FillInStage");
		}

		this.stageSubmission = (FillInSubmission) stageSubmission;
		this.stage = (FillInStage) stage;
		createFillInSubmissionViewFields(submission);
		setFormularEditorPalette();
	}
}
