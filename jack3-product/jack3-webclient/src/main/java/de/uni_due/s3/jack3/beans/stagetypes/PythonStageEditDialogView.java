package de.uni_due.s3.jack3.beans.stagetypes;

import de.uni_due.s3.jack3.entities.stagetypes.python.AbstractPythonCheckerConfiguration;
import de.uni_due.s3.jack3.entities.stagetypes.python.GreqlPythonGradingConfig;
import de.uni_due.s3.jack3.entities.stagetypes.python.PythonStage;
import de.uni_due.s3.jack3.entities.stagetypes.python.TracingPythonGradingConfig;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;

public class PythonStageEditDialogView extends AbstractStageEditDialogView {

	private static final long serialVersionUID = -3992157362947057794L;

	private PythonStage stage;

	@Override
	public void setStage(Stage stage) {
		if (!(stage instanceof PythonStage)) {
			throw new IllegalArgumentException("PythonStageEditDialogView must be used with instances of PythonStage");
		}

		this.stage = (PythonStage) stage;
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

	public void addGreqlConfig() {
		stage.addGradingStep(new GreqlPythonGradingConfig());
	}

	public void addTracingConfig() {
		stage.addGradingStep(new TracingPythonGradingConfig());
	}

	public void deleteGraderConfig(AbstractPythonCheckerConfiguration config) {
		stage.removeGradingStep(config);
	}
}
