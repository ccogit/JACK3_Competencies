package de.uni_due.s3.jack3.beans.stagetypes;

import de.uni_due.s3.jack3.entities.stagetypes.java.AbstractJavaCheckerConfiguration;
import de.uni_due.s3.jack3.entities.stagetypes.java.GreqlGradingConfig;
import de.uni_due.s3.jack3.entities.stagetypes.java.JavaStage;
import de.uni_due.s3.jack3.entities.stagetypes.java.MetricsGradingConfig;
import de.uni_due.s3.jack3.entities.stagetypes.java.TracingGradingConfig;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;

public class JavaStageEditDialogView extends AbstractStageEditDialogView {

	private static final long serialVersionUID = -3992157362947057794L;

	private JavaStage stage;

	@Override
	public void setStage(Stage stage) {
		if (!(stage instanceof JavaStage)) {
			throw new IllegalArgumentException("JavaStageEditDialogView must be used with instances of UmlStage");
		}

		this.stage = (JavaStage) stage;
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
		stage.addGradingStep(new GreqlGradingConfig());
	}

	public void addTracingConfig() {
		stage.addGradingStep(new TracingGradingConfig());
	}

	public void addMetricsConfig() {
		stage.addGradingStep(new MetricsGradingConfig());
	}

	public void deleteGraderConfig(AbstractJavaCheckerConfiguration config) {
		stage.removeGradingStep(config);
	}
}
