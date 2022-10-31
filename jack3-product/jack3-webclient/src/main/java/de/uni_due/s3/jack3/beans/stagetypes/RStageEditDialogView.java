package de.uni_due.s3.jack3.beans.stagetypes;

import java.util.StringJoiner;

import javax.inject.Inject;

import org.primefaces.PrimeFaces;

import de.uni_due.s3.jack3.business.stagetypes.RStageBusiness;
import de.uni_due.s3.jack3.entities.stagetypes.r.AbstractTestCase;
import de.uni_due.s3.jack3.entities.stagetypes.r.DynamicRTestCase;
import de.uni_due.s3.jack3.entities.stagetypes.r.ETestCasePointsMode;
import de.uni_due.s3.jack3.entities.stagetypes.r.ETestcaseRuleMode;
import de.uni_due.s3.jack3.entities.stagetypes.r.RStage;
import de.uni_due.s3.jack3.entities.stagetypes.r.StaticRTestCase;
import de.uni_due.s3.jack3.entities.stagetypes.r.TestCaseTuple;
import de.uni_due.s3.jack3.entities.tenant.CheckerConfiguration;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;

/**
 * This class is the one referenced as "stageBean" in the R-Stage (included for the R-Stage in ExercisePlayer.xhtml)
 * 
 * @author Benjamin Otto
 *
 */
public class RStageEditDialogView extends AbstractStageEditDialogView {

	private static final long serialVersionUID = -8840393328143357596L;

	private RStage stage;

	private AbstractTestCase currentDynamicRTestcase = new DynamicRTestCase();
	private AbstractTestCase currentDynamicRTestcaseToUpdate = new DynamicRTestCase();

	private StaticRTestCase currentStaticRTestcase = new StaticRTestCase();
	private StaticRTestCase currentStaticRTestcaseToUpdate = new StaticRTestCase();

	private TestCaseTuple currentTestCaseTuple;

	private CheckerConfiguration currentCheckerConfiguration;

	@Inject
	private RStageBusiness rStageBusiness;

	private long rTestCaseToUpdateOriginalId;

	public void addDynamicTestcase() {
		currentTestCaseTuple.addTestCase(currentDynamicRTestcase);
		currentDynamicRTestcase = new DynamicRTestCase();
	}

	public void addStaticTestcase() {
		currentTestCaseTuple.addTestCase(currentStaticRTestcase);
		currentStaticRTestcase = new StaticRTestCase();
	}

	public void addNewTestCasetuple() {
		rStageBusiness.addNewTestCasetuple(stage);
	}

	public AbstractTestCase getCurrentDynamicTestcase() {
		return currentDynamicRTestcase;
	}

	public AbstractTestCase getCurrentDynamicTestcaseToUpdate() {
		return currentDynamicRTestcaseToUpdate;
	}

	public TestCaseTuple getCurrentTestCasetuple() {
		return currentTestCaseTuple;
	}

	public ETestcaseRuleMode[] getRuleModes() {
		return ETestcaseRuleMode.values();
	}

	public ETestCasePointsMode[] getPointModes() {
		return ETestCasePointsMode.values();
	}

	@Override
	public Stage getStage() {
		return stage;
	}

	public void removeTestCase(TestCaseTuple testCasetuple, AbstractTestCase testCase) {
		rStageBusiness.removeTestCase(testCasetuple, testCase);
	}

	public void removeTestCasetuple(TestCaseTuple testCasetuple) {
		rStageBusiness.removeTestCasetuple(stage, testCasetuple);
	}

	public void setCurrentDynamicTestcase(AbstractTestCase currentTestcase) {
		currentDynamicRTestcase = currentTestcase;
	}

	public void setCurrentDynamicTestcaseToUpdate(AbstractTestCase currentDynamicTestcaseToUpdate) {
		currentDynamicRTestcaseToUpdate = currentDynamicTestcaseToUpdate;
	}

	public void addDynamicTestCase(TestCaseTuple currentTestCasetuple) {
		// We just need to set the currentTestCasetuple
		currentTestCaseTuple = currentTestCasetuple;
		// ...and rerender the addDynamicTestCaseOverlay, since the view gets renderd on opening the site. Fortunatly
		// stage.getOrderIndex() contains the current tab id.
		PrimeFaces.current().executeScript("PF('addDynamicTestCaseOverlay" + stage.getOrderIndex() + "').show();");
	}

	public void addStaticTestCase(TestCaseTuple currentTestCasetuple) {
		// We just need to set the currentTestCasetuple
		currentTestCaseTuple = currentTestCasetuple;

		// ...and rerender the addStaticTestCaseOverlay, since the view gets renderd on opening the site. Fortunatly
		// stage.getOrderIndex() contains the current tab id.
		PrimeFaces.current().executeScript("PF('addStaticTestCaseOverlay" + stage.getOrderIndex() + "').show();");
	}

	public void cancelEdit(AbstractTestCase tc) {
		tc = null;
	}

	@Override
	public void setStage(Stage stage) {
		if (!(stage instanceof RStage)) {
			throw new IllegalArgumentException("RStageEditDialogView must be used with instances of RStage!");
		}

		this.stage = (RStage) stage;
	}

	public void editDynamicTestcaseCopy(AbstractTestCase testCase, TestCaseTuple testCasetuple) {
		currentTestCaseTuple = testCasetuple;
		rTestCaseToUpdateOriginalId = testCase.getId();
		currentDynamicRTestcaseToUpdate = testCase.deepCopy();

		// We need to rerender the editDynamicTestCaseOverlay, since the view gets renderd on opening the site and we 
		// just have set the currentTestcaseToUpdate to a new value. Fortunatly stage.getOrderIndex() contains the 
		// current tab id.
		PrimeFaces.current().ajax().update("exerciseEdit:tabs" + stage.getOrderIndex() + ":editDynamicTestCasePanel");
		PrimeFaces.current().executeScript("PF('editDynamicTestCaseOverlay" + stage.getOrderIndex() + "').show();");
	}

	public void editStaticTestCaseCopy(StaticRTestCase testCase, TestCaseTuple testCasetuple) {
		currentTestCaseTuple = testCasetuple;
		rTestCaseToUpdateOriginalId = testCase.getId();
		currentStaticRTestcaseToUpdate = testCase.deepCopy();
		// We need to rerender the editDynamicTestCaseOverlay, since the view gets renderd on opening the site and we 
		// just have set the currentTestcaseToUpdate to a new value. Fortunatly stage.getOrderIndex() contains the 
		// current tab id.
		PrimeFaces.current().ajax().update("exerciseEdit:tabs" + stage.getOrderIndex() + ":editStaticTestCasePanel");
		PrimeFaces.current().executeScript("PF('editStaticTestCaseOverlay" + stage.getOrderIndex() + "').show();");
	}

	public void updateStaticTestcase(StaticRTestCase copyOfTestCaseToUpdate) {
		AbstractTestCase testCaseToUpdate = getTestCaseToBeUpdated();
		StaticRTestCase staticRTestCaseToUpdate = (StaticRTestCase) testCaseToUpdate;
		staticRTestCaseToUpdate.copyFrom(copyOfTestCaseToUpdate);
		copyOfTestCaseToUpdate = null; // Prevent memory leak?
	}

	public void updateDynamicTestcase(DynamicRTestCase copyOfTestCaseToUpdate) {
		AbstractTestCase testCaseToUpdate = getTestCaseToBeUpdated();
		DynamicRTestCase dynamicRTestCaseToUpdate = (DynamicRTestCase) testCaseToUpdate;
		dynamicRTestCaseToUpdate.copyFrom(copyOfTestCaseToUpdate);
		copyOfTestCaseToUpdate = null; // Prevent memory leak?
	}

	private AbstractTestCase getTestCaseToBeUpdated() {
		return currentTestCaseTuple.getTestCases().stream() //
				.filter(testcase -> testcase.getId() == rTestCaseToUpdateOriginalId) //
				.findFirst() //
				.orElseThrow(NoSuchJackEntityException::new);
	}

	public void editCheckerConfig(TestCaseTuple testCasetuple) {

		currentCheckerConfiguration = testCasetuple.getCheckerConfiguration();
		// We need to rerender the editCheckerConfigOverlay, since the view gets renderd on opening the site and we
		// just have set the currentCheckerConfiguration to a new value. Fortunatly stage.getOrderIndex() contains the
		// current tab id.
		PrimeFaces.current().ajax().update("exerciseEdit:tabs" + stage.getOrderIndex() + ":editCheckerConfigPanel");
		PrimeFaces.current().executeScript("PF('editCheckerConfigOverlay" + stage.getOrderIndex() + "').show();");
	}

	public CheckerConfiguration getCurrentCheckerConfiguration() {
		return currentCheckerConfiguration;
	}

	public void setCurrentCheckerConfiguration(CheckerConfiguration currentCheckerConfiguration) {
		this.currentCheckerConfiguration = currentCheckerConfiguration;
	}

	public String getTestcaseTupelNamesAsUiString() {
		StringJoiner resultJoiner = new StringJoiner(", ");
		for (TestCaseTuple testCaseTuple : stage.getTestCasetuples()) {
			String checkerId = "<b>#{c" + testCaseTuple.getCheckerConfiguration().getId() + "}</b>";
			String name = testCaseTuple.getCheckerConfiguration().getName();
			if (name == null) {
				name = "Noname";
			}
			resultJoiner.add("\"" + name + "\" " + checkerId);
		}
		return resultJoiner.toString();
	}

	public StaticRTestCase getCurrentStaticRTestcase() {
		return currentStaticRTestcase;
	}

	public StaticRTestCase getCurrentStaticRTestcaseToUpdate() {
		return currentStaticRTestcaseToUpdate;
	}
}
