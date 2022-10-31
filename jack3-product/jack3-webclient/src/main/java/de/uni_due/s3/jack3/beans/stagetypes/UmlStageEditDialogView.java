package de.uni_due.s3.jack3.beans.stagetypes;

import de.uni_due.s3.jack3.entities.stagetypes.uml.UmlStage;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;

public class UmlStageEditDialogView extends AbstractStageEditDialogView {

	private static final long serialVersionUID = -3992157362947057794L;

	private UmlStage stage;

	@Override
	public void setStage(Stage stage) {
		if (!(stage instanceof UmlStage)) {
			throw new IllegalArgumentException("UmlStageEditDialogView must be used with instances of UmlStage");
		}

		this.stage = (UmlStage) stage;
	}

	@Override
	public Stage getStage() {
		return stage;
	}

	public void removeStageTransition(StageTransition transition) {
		stage.removeStageTransition(transition);
	}

	public void addNewStageTransition() {
		stage.addStageTransition(new StageTransition());
	}

}
