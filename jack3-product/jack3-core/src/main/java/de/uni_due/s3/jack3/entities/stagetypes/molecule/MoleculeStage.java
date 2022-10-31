package de.uni_due.s3.jack3.entities.stagetypes.molecule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.tenant.Stage;

@Audited
@Entity
@AttributeOverrides({ @AttributeOverride(name = "id", column = @Column(name = "id")) })
public class MoleculeStage extends Stage {

	private static final long serialVersionUID = 8303456708051145943L;

	@Column
	@Type(type = "text")
	private String expectedInchiString;

	@Column
	@Type(type = "text")
	private String expectedMolString;

	@Column
	@Type(type = "text")
	private String expectedEditorContentString;

	@Column
	@Type(type = "text")
	private String correctFeedback;

	@Column
	@Type(type = "text")
	private String errorFeedback;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinTable(
			name = "moleculestage_feedbackrule",
			joinColumns = @JoinColumn(name = "moleculestage_id"),
			inverseJoinColumns = @JoinColumn(name = "moleculerule_id"))
	private Set<MoleculeRule> rules = new HashSet<>();

	public String getExpectedInchiString() {
		return expectedInchiString;
	}

	public void setExpectedInchiString(String expectedInchiString) {
		this.expectedInchiString = expectedInchiString;
	}

	public String getExpectedMolString() {
		return expectedMolString;
	}

	public void setExpectedMolString(String expectedMolString) {
		this.expectedMolString = expectedMolString;
	}

	public String getExpectedEditorContentString() {
		return expectedEditorContentString;
	}

	public void setExpectedEditorContentString(String expectedEditorContentString) {
		this.expectedEditorContentString = expectedEditorContentString;
	}

	public String getCorrectFeedback() {
		return correctFeedback;
	}

	public void setCorrectFeedback(String correctFeedback) {
		this.correctFeedback = correctFeedback;
	}

	public String getErrorFeedback() {
		return errorFeedback;
	}

	public void setErrorFeedback(String errorFeedback) {
		this.errorFeedback = errorFeedback;
	}

	public void addFeedbackRule(MoleculeRule feedbackRule) {
		rules.add(feedbackRule);
	}

	public void removeFeedbackRule(int ruleOrderIndex) {
		removeRule(ruleOrderIndex, rules);
	}

	private void removeRule(int ruleOrderIndex, Set<MoleculeRule> rules) {
		MoleculeRule ruleToRemove = null;

		for (MoleculeRule rule : rules) {
			if (rule.getOrderIndex() == ruleOrderIndex) {
				ruleToRemove = rule;
				break;
			}
		}

		if (ruleToRemove == null) {
			throw new IllegalStateException("Rule to remove isn't in rules list");
		}

		rules.remove(ruleToRemove);
	}

	public void addFeedbackRules(List<MoleculeRule> rules) {
		this.rules.clear();
		this.rules.addAll(rules);
	}

	public List<MoleculeRule> getFeedbackRulesAsList() {
		return getRulesAsList(rules);
	}

	private List<MoleculeRule> getRulesAsList(Set<MoleculeRule> rules) {
		final List<MoleculeRule> rulesList = new ArrayList<>(rules);

		Collections.sort(rulesList, (r1, r2) -> r1.compareTo(r2));

		return rulesList;
	}

	@Override
	public Stage deepCopy() {
		MoleculeStage copy = new MoleculeStage();

		copy.deepCopyStageVars(this);

		copy.expectedInchiString = expectedInchiString;
		copy.expectedMolString = expectedMolString;
		copy.expectedEditorContentString = expectedEditorContentString;
		copy.correctFeedback = correctFeedback;
		copy.errorFeedback = errorFeedback;

		for (MoleculeRule rule : rules) {
			copy.rules.add(rule.deepCopy());
		}

		return copy;
	}

	@Override
	public boolean mustWaitForPendingJobs() {
		return false;
	}

}
