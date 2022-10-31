package de.uni_due.s3.jack3.beans.stagetypes;

import java.util.Map;

import javax.faces.context.FacesContext;
import javax.inject.Inject;

import org.primefaces.event.ReorderEvent;

import de.uni_due.s3.jack3.business.stagetypes.MoleculeStageBusiness;
import de.uni_due.s3.jack3.entities.stagetypes.molecule.MoleculeRule;
import de.uni_due.s3.jack3.entities.stagetypes.molecule.MoleculeStage;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;

public class MoleculeStageEditDialogView extends AbstractStageEditDialogView {

	private static final long serialVersionUID = -1162779214058731784L;

	private MoleculeStage moleculeStage;

	@Inject
	private MoleculeStageBusiness moleculeStageBusiness;

	@Override
	public void setStage(Stage stage) {
		if (!(stage instanceof MoleculeStage)) {
			throw new IllegalArgumentException(
					"MoleculeStageEditDialogView must be used with instances of MoleculeStage");
		}

		this.moleculeStage = (MoleculeStage) stage;
	}

	public String getStageMolfile() {
		return moleculeStage.getExpectedMolString();
	}

	public String getStageEditorContent() {
		return moleculeStage.getExpectedEditorContentString() != null ? moleculeStage.getExpectedEditorContentString()
				: "''";
	}

	public String getStageEditorContentFormat() {
		if (moleculeStage.getExpectedEditorContentString() != null) {
			return "Kekule.IO.DataFormat.KEKULE_JSON";
		} else {
			return "Kekule.IO.DataFormat.MOL";
		}
	}

	/**
	 * This gets called through an onclick javascript event "updateMoleculeInStage()" on the submit-button in the
	 * moleculeEditorOverlay.xhtml!
	 */
	public void updateMolecule() {
		Map<String, String[]> requestParameterValuesMap = FacesContext.getCurrentInstance().getExternalContext()
				.getRequestParameterValuesMap();

		String moleculeInchi = requestParameterValuesMap.get("moleculeInChIs")[0];
		moleculeStage.setExpectedInchiString(moleculeInchi);
		String moleculeMol = requestParameterValuesMap.get("moleculeMols")[0];
		moleculeStage.setExpectedMolString(moleculeMol);
		String moleculeEditorContent = requestParameterValuesMap.get("moleculeEditorContent")[0];
		moleculeStage.setExpectedEditorContentString(moleculeEditorContent);
	}

	public void addFeedbackRule(String name) {
		int countFeedbackRules = moleculeStage.getFeedbackRulesAsList().size();
		name += " ";
		MoleculeRule feedbackRule = new MoleculeRule(name, countFeedbackRules);
		moleculeStage.addFeedbackRule(feedbackRule);
	}

	public void removeFeedbackRule(int feedbackRuleOrderIndex) {
		moleculeStageBusiness.removeFeedbackFromStage(feedbackRuleOrderIndex, moleculeStage);
	}

	public void feedbackRuleReorder(ReorderEvent event) {
		moleculeStageBusiness.reorderFeedbackRules(moleculeStage, event.getFromIndex(), event.getToIndex());
	}

	public void addNewStageTransition() {
		moleculeStage.addStageTransition(new StageTransition());
	}

	@Override
	public Stage getStage() {
		return moleculeStage;
	}

}
