package de.uni_due.s3.jack3.beans.stagetypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;

import org.primefaces.PrimeFaces;
import org.primefaces.event.ReorderEvent;

import de.uni_due.s3.jack3.business.stagetypes.FillInStageBusiness;
import de.uni_due.s3.jack3.entities.enums.EFillInEditorType;
import de.uni_due.s3.jack3.entities.enums.EFormularEditorPalette;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.DropDownField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStageField;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.Rule;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;

public class FillInStageEditDialogView extends AbstractStageEditDialogView implements Serializable {

	private static final long serialVersionUID = 814899590862133609L;

	@Inject
	private FillInStageBusiness fillInStageBusiness;

	private FillInStage stage;

	private List<SelectItem> fillInFieldEditorType;

	private List<SelectItem> mathdoxFormularEditorPalettes;

	private transient DataModel<FillInField> fillInModel;
	private transient DataModel<DropDownField> dropDownModel;

	public void removeFillInField() {
		FillInField fieldToDelete = fillInModel.getRowData();

		String regex = "<input name=\"" + fieldToDelete.getName()
				+ "\" size=[\"]([^\"]*)[\"] type=\"text\" value=[\"]([^\"]*)[\"] />";
		String changedTaskDescription = stage.getTaskDescription().replace(regex, "");
		stage.setTaskDescription(changedTaskDescription);

		stage.removeFillInField(fieldToDelete);
		reintializeFillInDataModel();
	}

	public void addFeedbackRule(String name) {
		int countFeedbackRules = stage.getFeedbackRulesAsList().size();
		name += " ";
		Rule feedbackRule = new Rule(name, countFeedbackRules);
		stage.addFeedbackRule(feedbackRule);
	}
	
	public void addCorrectAnswerRule(String name) {
		int highesRuleNumber =0;
		for(Rule rule :stage.getCorrectAnswerRulesAsList()) {
			if(highesRuleNumber<rule.getOrderIndex()) {
				highesRuleNumber = rule.getOrderIndex();
			}
		}
		
		name = name+ " "+ (++highesRuleNumber);
		Rule correctAnswerRule = new Rule(name, highesRuleNumber);
		stage.addCorrectAnswerRule(correctAnswerRule);
	}

	public void removeFeedbackRule(int feedbackRuleOrderIndex) {
		fillInStageBusiness.removeFeedbackFromStage(feedbackRuleOrderIndex, stage);
	}
	
	public void removeCorrectAnswerRule(int feedbackRuleOrderIndex) {
		stage.removeCorrectAnswerRule(feedbackRuleOrderIndex);
	}

	public void addFillInField(String name) {
		int countFillInFields = stage.getFillInFields().size();
		name += getNextFreeFieldNumber(name, countFillInFields);
		FillInField fillInField = new FillInField(name, countFillInFields);
		stage.addFillInField(fillInField);
		reintializeFillInDataModel();

		String fillInFieldHtmlCode = fillInStageBusiness.getFillInFieldHtmlCode(name, FillInField.DEFAULT_SIZE);
		insertHtmlElementInEditor(fillInFieldHtmlCode);
	}

	private void insertHtmlElementInEditor(String htmlElement) {
		PrimeFaces primeFacesCurrent = PrimeFaces.current();
		String editorInstance = "PF('taskDescriptionCkEditor_"+stage.getOrderIndex()+"').instance";
		primeFacesCurrent.executeScript(editorInstance + ".fire('saveSnapshot')");
		primeFacesCurrent.executeScript(editorInstance + ".insertHtml('" + htmlElement + "')");
		primeFacesCurrent.executeScript(editorInstance + ".fire('saveSnapshot')");
	}

	public void insertFillInFieldInEdidor() {
		FillInField fieldToInsert = fillInModel.getRowData();
		String fillInFieldHtmlCode = fillInStageBusiness.getFillInFieldHtmlCode(fieldToInsert.getName(),
				fieldToInsert.getSize());
		// only insert the FillinField if it's not already in the editor
		if (!isFillInFieldInEditor(fieldToInsert)) {
			insertHtmlElementInEditor(fillInFieldHtmlCode);
		} else {
			parentView.addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR,
					"exerciseEdit.fillIn.fieldAlreadyInEditor.summary",
					"exerciseEdit.fillIn.fieldAlreadyInEditor.details");
		}
	}

	private boolean isFillInFieldInEditor(FillInField toCheck) {
		Pattern pattern = Pattern.compile("input name=\"" + toCheck.getName()
				+ "\" size=[\"]([^\"]*)[\"] type=\"text\" value=[\"]([^\"]*)[\"]");
		return pattern.matcher(stage.getTaskDescription()).find();
	}

	public void addDropDownField(String name) {
		int countDropDownFields = stage.getDropDownFields().size();
		name += getNextFreeFieldNumber(name, countDropDownFields);
		DropDownField dropDownField = new DropDownField(name, countDropDownFields);
		stage.addDropDownField(dropDownField);
		reintializeDropDownDataModel();
		String dropDownFieldHtmlCode = fillInStageBusiness.getDropDownFieldHtmlCode(name);
		insertHtmlElementInEditor(dropDownFieldHtmlCode);
	}

	public void insertDropDownFieldInEditor() {
		DropDownField fieldToInsert = dropDownModel.getRowData();
		String dropDownFieldHtmlCode = fillInStageBusiness.getDropDownFieldHtmlCode(fieldToInsert.getName());
		// only insert the DropDownField if it's not already in the editor
		if (!isDropDownFieldInEditor(fieldToInsert)) {
			insertHtmlElementInEditor(dropDownFieldHtmlCode);
		} else {
			parentView.addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR,
					"exerciseEdit.fillIn.fieldAlreadyInEditor.summary",
					"exerciseEdit.fillIn.fieldAlreadyInEditor.details");
		}

	}

	private boolean isDropDownFieldInEditor(DropDownField toCkeck) {
		Pattern pattern = Pattern.compile("select name=\"" + toCkeck.getName()+"\"><option value=\"0\">" + toCkeck.getName());
		return pattern.matcher(stage.getTaskDescription()).find();
	}

	public DataModel<FillInField> getFillInFieldsDataModel() {
		if (fillInModel == null) {
			reintializeFillInDataModel();
		}
		return fillInModel;
	}

	private void reintializeFillInDataModel() {
		List<FillInField> listFillInFields = new ArrayList<>(stage.getFillInFields());
		Collections.sort(listFillInFields, getAbstractFillInFieldComparator());
		fillInModel = new ListDataModel<>(listFillInFields);
	}

	public DataModel<DropDownField> getDropDownFieldsDataModel() {
		if (dropDownModel == null) {
			reintializeDropDownDataModel();
		}
		return dropDownModel;
	}

	private void reintializeDropDownDataModel() {
		List<DropDownField> listDropDownFields = new ArrayList<>(stage.getDropDownFields());
		Collections.sort(listDropDownFields, getAbstractFillInFieldComparator());
		dropDownModel = new ListDataModel<>(listDropDownFields);
	}

	private static Comparator<FillInStageField> getAbstractFillInFieldComparator() {
		return (FillInStageField f1, FillInStageField f2) -> Integer.compare(f1.getOrderIndex(), f2.getOrderIndex());
	}

	public Set<FillInField> getSortedFillFields() {
		return stage.getFillInFields();
	}

	/**
	 * Checks if the Field name is already used in a fillInField or dropDownField. Used by JSF Validator throws
	 * ValidatorException if validation fails. Parameters are delivered by JSF Validator.
	 */
	public void checkIsStageFieldNameUnique(FacesContext fContext, UIComponent component, Object value) {
		String oldName = ((UIInput) component).getValue().toString();
		String newName = value.toString();

		if ((value == null) || oldName.equals(newName)) {
			return;
		}
		
		FillInStageField fieldToChange = null;
		ArrayList<FillInStageField> allStageFields = new ArrayList<>();
		allStageFields.addAll(stage.getFillInFields());
		allStageFields.addAll(stage.getDropDownFields());
		for (FillInStageField field : allStageFields) {
			if (field.getName().equals(newName)) {
				String nameNotUniqueString = getLocalizedMessage("exerciseEdit.fillIn.nameNotUnique");
				FacesMessage msgNameNotUnique = new FacesMessage(nameNotUniqueString);
				msgNameNotUnique.setSeverity(FacesMessage.SEVERITY_ERROR);
				throw new ValidatorException(msgNameNotUnique);
			}
			if (field.getName().equals(oldName)) {
				fieldToChange = field;
			}
		}
		if (fieldToChange == null) {
			throw new ValidatorException(new FacesMessage("Field not found"));
		}

	}

	private String getNextFreeFieldNumber(String fieldName, int startNumber) {
		ArrayList<FillInStageField> allStageFields = new ArrayList<>();
		allStageFields.addAll(stage.getFillInFields());
		allStageFields.addAll(stage.getDropDownFields());
		boolean fieldNameUnique = false;
		while (!fieldNameUnique) {
			startNumber += 1;
			fieldNameUnique = true;
			for (FillInStageField field : allStageFields) {
				if (field.getName().equals(fieldName + Integer.toString(startNumber))) {
					fieldNameUnique = false;
				}
			}
		}
		return Integer.toString(startNumber);
	}

	public void updateDropDownFieldNameOnTaskDescription(ValueChangeEvent event) {
		String newName = event.getNewValue().toString();
		String oldName = event.getOldValue().toString();

		String regex = "select name=\"" +oldName+ "\"><option value=\"0\">"
				+ oldName;
		String replaceWith = "select name=\"" + newName+ "\"><option value=\"0\">"
				+ newName;
		String changedTaskDescription = stage.getTaskDescription().replace(regex, replaceWith);
		stage.setTaskDescription(changedTaskDescription);
	}

	public void removeDropDownField() {
		DropDownField fieldToDelete = dropDownModel.getRowData();

		String regex = "<select name=\"" +  fieldToDelete.getName()+"\"><option value=\"0\">"
				+ fieldToDelete.getName()+"</option></select>";
		String changedTaskDescription = stage.getTaskDescription().replace(regex, "");
		stage.setTaskDescription(changedTaskDescription);

		stage.removeDropDownField(fieldToDelete);
		reintializeDropDownDataModel();
	}

	/**
	 * Updates the name of a FillInField in the Editor.
	 *
	 * @param event
	 *            is delivered by valueChangeListener jsf Element
	 */
	public void updateFillInFieldNameOnTaskDescription(ValueChangeEvent event) {
		String newName = event.getNewValue().toString();
		String oldName = event.getOldValue().toString();

		FillInField fieldToChange = fillInModel.getRowData();
		String regex = "input name=\"" 	+ oldName+ "\" size=[\"]([^\"]*)[\"] type=\"text\" value=[\"]([^\"]*)[\"]";
		String replaceWith = "input name=\"" + newName+ "\" size=\"" + fieldToChange.getSize()
		+ "\" type=\"text\" value=\"" + newName + "\"";
		String changedTaskDescription = stage.getTaskDescription().replace(regex, replaceWith);
		stage.setTaskDescription(changedTaskDescription);
	}

	/**
	 * Updates the Size of a FillInField in the Editor.
	 *
	 * @param event
	 *            is delivered by valueChangeListener jsf Element
	 */
	public void updateSizeOnTaskDescription(ValueChangeEvent event) {
		String newSize = event.getNewValue().toString();
		FillInField fieldToChange = fillInModel.getRowData();
		String regex = "input name=\"" + fieldToChange.getName()+"\" size=[\"]([^\"]*)[\"]";
		String replaceWith = "input name=\"" + fieldToChange.getName()+ "\" size=\"" + newSize
				+ "\"";
		String changedTaskDescription = stage.getTaskDescription().replace(regex, replaceWith);
		stage.setTaskDescription(changedTaskDescription);
	}

	@Override
	public void setStage(Stage stage) {
		if (!(stage instanceof FillInStage)) {
			throw new IllegalArgumentException("FillInStageEditDialogView must be used with instances of FillInStage");
		}
		this.stage = (FillInStage) stage;
	}

	@Override
	public Stage getStage() {
		return stage;
	}

	public List<SelectItem> getFormularEditorPalettes() {
		if (mathdoxFormularEditorPalettes != null) {
			return mathdoxFormularEditorPalettes;
		}

		mathdoxFormularEditorPalettes = new ArrayList<>();
		mathdoxFormularEditorPalettes
		.add(new SelectItem(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE_NOT_SELECTED,
				EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE_NOT_SELECTED.getTypeLabel()));
		mathdoxFormularEditorPalettes.add(new SelectItem(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_NO_PALETTE,
				EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_NO_PALETTE.getTypeLabel()));
		mathdoxFormularEditorPalettes.add(new SelectItem(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_BASIC,
				EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_BASIC.getTypeLabel()));
		mathdoxFormularEditorPalettes.add(new SelectItem(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_TRIGONOMETRY,
				EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_TRIGONOMETRY.getTypeLabel()));
		mathdoxFormularEditorPalettes.add(new SelectItem(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE,
				EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE.getTypeLabel()));
		mathdoxFormularEditorPalettes.add(new SelectItem(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE_FULL,
				EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE_FULL.getTypeLabel()));
		mathdoxFormularEditorPalettes.add(new SelectItem(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE_SPLIT,
				EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE_SPLIT.getTypeLabel()));
		mathdoxFormularEditorPalettes.add(new SelectItem(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE_2,
				EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE_2.getTypeLabel()));
		mathdoxFormularEditorPalettes.add(new SelectItem(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE_3,
				EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE_3.getTypeLabel()));
		mathdoxFormularEditorPalettes.add(new SelectItem(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE_4,
				EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE_4.getTypeLabel()));
		mathdoxFormularEditorPalettes.add(new SelectItem(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE_5,
				EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE_5.getTypeLabel()));
		mathdoxFormularEditorPalettes.add(new SelectItem(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE_6,
				EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_PALETTE_6.getTypeLabel()));
		mathdoxFormularEditorPalettes.add(new SelectItem(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_UDE_MATH_DIDACTICS,
				EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_UDE_MATH_DIDACTICS.getTypeLabel()));

		return mathdoxFormularEditorPalettes;
	}

	public List<SelectItem> getFillInFieldEditorTypes() {
		if (fillInFieldEditorType != null) {
			return fillInFieldEditorType;
		}

		fillInFieldEditorType = new ArrayList<>();
		SelectItem none = new SelectItem(EFillInEditorType.NONE, EFillInEditorType.NONE.getTypeLabel());
		fillInFieldEditorType.add(none);

		SelectItem text = new SelectItem(EFillInEditorType.TEXT, EFillInEditorType.TEXT.getTypeLabel());
		fillInFieldEditorType.add(text);

		SelectItem number = new SelectItem(EFillInEditorType.NUMBER, EFillInEditorType.NUMBER.getTypeLabel());
		fillInFieldEditorType.add(number);

		String nameFormularEditor = FacesContext.getCurrentInstance().getApplication()
				.getResourceBundle(FacesContext.getCurrentInstance(), "msg")
				.getString("exerciseEdit.fillIn.formularEditorType.formularEditor");

		SelectItem formularEditor = new SelectItem(EFillInEditorType.MATHDOX_FORMULAR_EDITOR, nameFormularEditor);
		fillInFieldEditorType.add(formularEditor);

		return fillInFieldEditorType;
	}

	public void dropDownAnswerReorder(ReorderEvent event) {
		DropDownField currentField = dropDownModel.getRowData();
		currentField.reorderAnswerOptions(event.getFromIndex(), event.getToIndex());
	}

	public void feedbackRuleReorder(ReorderEvent event) {
		fillInStageBusiness.reorderFeedbackRules(stage, event.getFromIndex(), event.getToIndex());
	}

	public void removeStageTransition(StageTransition transition) {
		stage.removeStageTransition(transition);
	}

	public void addNewStageTransition() {
		stage.addStageTransition(new StageTransition());
	}

}
