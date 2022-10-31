package de.uni_due.s3.jack3.entities.stagetypes.r;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;

import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;

@Audited
@Entity
public class TestCaseTupleResult extends AbstractEntity {

	private static final long serialVersionUID = 8720597899813026261L;

	/**
	 * Unfortunatly it seems that we can't use Boolean.class as targetClass, or else the values of the map won`t get
	 * written to the database on persist. Looks like a Hibernate-bug, we use Hibernate 5.3.10.Final at the time of
	 * writing this comment.
	 */
	@ToString
	@ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
	@MapKeyColumn(name = "testcase")
	@Column(name = "result_bool_as_string")
	private Map<AbstractTestCase, String /* boolean ruleMatched */> testCaseResultsMap = new HashMap<>();

	@ManyToOne
	private TestCaseTuple testCaseTuple;

	@ElementCollection
	private List<String> feedbackList;

	public TestCaseTupleResult() {
		// only for hibernate
	}

	/**
	 * Sets the map with null-values for result, so we can differentiate between "passed", "not passed" and "not tested"
	 *
	 * @param testCaseTuple
	 *            The Testcasetuple to initialise our result map with
	 */
	public TestCaseTupleResult(TestCaseTuple testCaseTuple) {
		for (AbstractTestCase testCase : testCaseTuple.getTestCases()) {
			addTestCaseResult(testCase, null);
		}
		this.testCaseTuple = testCaseTuple;
	}

	public Map<AbstractTestCase, String> getTestCaseResultsMap() {
		return testCaseResultsMap;
	}

	public void setTestCaseResultsMap(Map<AbstractTestCase, String> testCaseResultsMap) {
		this.testCaseResultsMap = testCaseResultsMap;
	}

	public void addTestCaseResult(AbstractTestCase abstractTestCase, Boolean bool) {
		if (testCaseResultsMap == null) {
			testCaseResultsMap = new HashMap<>();
		}
		testCaseResultsMap.put(abstractTestCase, String.valueOf(bool));
	}

	/**
	 * @return false, when all testcases have results (no testcase is null)
	 *         true, when at least one is not set (null)
	 */
	public boolean hasPendingChecks() {
		for (Entry<AbstractTestCase, String> entry : testCaseResultsMap.entrySet()) {

			String boolAsString = entry.getValue();
			// this not how it should be done, but see testCaseResultsMap, for why we "proxy" booleans as strings here
			if ((boolAsString == null) || boolAsString.equals("null")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Updates result of given testcase in "testCaseResultsMap" with given "boolean"
	 *
	 * @param testCase
	 *            Testcase to update
	 * @param ruleMatched
	 *            Did this testcase pass or not?
	 * @return true, if testcase was in the map.
	 *         false, otherwise
	 */
	public boolean setRuleMatched(AbstractTestCase testCase, boolean ruleMatched) {
		for (Entry<AbstractTestCase, String> entry : testCaseResultsMap.entrySet()) {
			AbstractTestCase currentTestCase = entry.getKey();

			if (currentTestCase.equals(testCase)) {
				entry.setValue(String.valueOf(ruleMatched));
				return true;
			}
		}
		return false;
	}

	public String getFailureFeedback(AbstractTestCase testcase) {
		return testCaseResultsMap.entrySet().stream() //
				.map(Entry::getKey) //
				.filter(currentTestcase -> currentTestcase.equals(testcase))
				.map(AbstractTestCase::getFeedbackIfFailed) //
				.findFirst() //
				.orElseThrow();
	}

	public TestCaseTuple getTestCaseTuple() {
		return testCaseTuple;
	}

	public List<String> getFeedbackList() {
		return feedbackList;
	}

	public void addFeedback(String feedback) {
		feedbackList.add(feedback);
	}
}
