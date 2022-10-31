package de.uni_due.s3.jack3.entities.stagetypes.r;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.uni_due.s3.jack3.entities.tenant.Stage;

@Audited
@Entity
@AttributeOverride(name = "id", column = @Column(name = "id"))
@XStreamAlias("RStage")
public class RStage extends Stage {
	private static final long serialVersionUID = 6309830000021958929L;

	@Column
	private String initialCode;

	@Column
	private String finalResultComputationString;

	// REVIEW ms: Fehlt hier ein @OrderColumn? Wenn in irgendeiner weiteren Stage jemand eine weitere List ohne
	// OrderColumn anlegt, gibt es Fehlermeldungen bei Starten des Deployments.
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private List<TestCaseTuple> testCasetuples = new ArrayList<>();

	@Override
	public RStage deepCopy() {
		RStage copy = new RStage();

		copy.deepCopyStageVars(this);

		copy.initialCode = initialCode;
		copy.finalResultComputationString = finalResultComputationString;

		copy.testCasetuples = testCasetuples //
				.stream() //
				.map(TestCaseTuple::deepCopy) //
				.collect(Collectors.toList());

		return copy;
	}

	public List<TestCaseTuple> getTestCasetuples() {
		return testCasetuples;
	}

	public void setTestCasetuples(List<TestCaseTuple> testUnits) {
		testCasetuples = testUnits;
	}

	/**
	 * Returns false, if all testCasetupled have a CheckerConfiguration that is set to be asynchronous.
	 */
	@Override
	public boolean mustWaitForPendingJobs() {
		// TODO bo: Make configurable in UI
		return false;
	}

	public String getFinalResultComputationString() {
		return finalResultComputationString;
	}

	public void setFinalResultComputationString(String finalResultComputationString) {
		this.finalResultComputationString = finalResultComputationString;
	}

	public String getInitialCode() {
		return initialCode;
	}

	public void setInitialCode(String initialCode) {
		this.initialCode = initialCode;
	}

	@Override
	public boolean isHasTestcaseTuples() {
		return true;
	}
}
