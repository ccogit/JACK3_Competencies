package de.uni_due.s3.jack3.entities.stagetypes.fillin;

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

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.uni_due.s3.jack3.entities.enums.EFormularEditorPalette;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageHint;

@Audited
@Entity
@AttributeOverrides({ //
	@AttributeOverride(
			name = "id", //
			column = @Column(name = "id")) //
})
@XStreamAlias("FillInStage")
public class FillInStage extends Stage {

	private static final long serialVersionUID = 1567789626666087710L;

	@OneToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
	@JoinTable( name = "fillinstage_feedbackrule",
	joinColumns =  @JoinColumn(name="fillinstage_id"),
	inverseJoinColumns = @JoinColumn(name = "rule_id"))
	private Set<Rule> rules = new HashSet<>();

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Set<FillInField> fillInFields = new HashSet<>();

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Set<DropDownField> dropDownFields =  new HashSet<>();

	@Column
	private EFormularEditorPalette formularEditorPalette = EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_NO_PALETTE;

	@Column
	@Type(type = "text")
	private String defaultFeedback;


	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@JoinTable( name = "fillinstage_correctanswerrule",
	joinColumns =  @JoinColumn(name="fillinstage_id"),
	inverseJoinColumns = @JoinColumn(name = "rule_id"))
	private Set<Rule> correctAnswerRules = new HashSet<>();

	@Column
	@Type(type = "text")
	private String correctAnswerFeedback;

	@Column
	private int defaultResult;


	public EFormularEditorPalette getFormularEditorPaletteEnum(){
		return formularEditorPalette;
	}

	public String getFormularEditorPalette() {
		return formularEditorPalette.toString();
	}

	public void setFormularEditorPalette(String formularEditorPalette){
		this.formularEditorPalette = EFormularEditorPalette.valueOf(formularEditorPalette);
	}

	/*
	 * @return unmodifiableSet of fillInFields
	 */
	public Set<FillInField> getFillInFields(){
		return Collections.unmodifiableSet(fillInFields);
	}

	public void removeFillInField(FillInField fillInField){
		fillInFields.remove(fillInField);
	}

	public void addFillInField(FillInField fillInField){
		fillInFields.add(fillInField);
	}

	/*
	 * @return unmodifiableSet of fillInFields
	 */
	public Set<DropDownField> getDropDownFields(){
		return Collections.unmodifiableSet(dropDownFields);
	}

	public void addDropDownField(DropDownField dropDownField){
		dropDownFields.add(dropDownField);
	}

	public void removeDropDownField(DropDownField dropDownField){
		dropDownFields.remove(dropDownField);
	}

	public void addFeedbackRule(Rule feedbackRule) {
		rules.add(feedbackRule);
	}

	public void removeFeedbackRule(int ruleOrderIndex) {
		removeRule(ruleOrderIndex,rules);
	}

	private void removeRule(int ruleOrderIndex,Set<Rule> rules) {
		Rule ruleToRemove = null;
		for(Rule rule : rules) {
			if(rule.getOrderIndex()==ruleOrderIndex) {
				ruleToRemove = rule;
				break;
			}
		}
		if(ruleToRemove==null) {
			throw new IllegalStateException("Rule to remove isn't in rules list");
		}
		rules.remove(ruleToRemove);
	}

	public void addFeedbackRules(List<Rule> rules) {
		this.rules.clear();
		this.rules.addAll(rules);
	}

	public List<Rule> getFeedbackRulesAsList(){
		return getRulesAsList(rules);
	}

	private List<Rule> getRulesAsList(Set<Rule> rules) {
		final List<Rule> rulesList = new ArrayList<>(rules);

		Collections.sort(rulesList, (r1, r2) -> r1.compareTo(r2));

		return rulesList;
	}

	public void addCorrectAnswerRule(Rule feedbackRule) {
		correctAnswerRules.add(feedbackRule);
	}

	public void addCorrectAnswerRules(List<Rule> rules) {
		this.rules.clear();
		this.rules.addAll(rules);
	}

	public void removeCorrectAnswerRule(int ruleOrderIndex) {
		removeRule(ruleOrderIndex,correctAnswerRules);
	}

	public List<Rule> getCorrectAnswerRulesAsList(){
		return getRulesAsList(correctAnswerRules);
	}

	public String getCorrectAnswerFeedback() {
		return correctAnswerFeedback;
	}

	public void setCorrectAnswerFeedback(String correctAnswerFeedback) {
		this.correctAnswerFeedback = correctAnswerFeedback;
	}

	public String getDefaultFeedback() {
		return defaultFeedback;
	}

	public void setDefaultFeedback(String defaultFeedback) {
		this.defaultFeedback = defaultFeedback;
	}

	public int getDefaultResult() {
		return defaultResult;
	}

	public void setDefaultResult(int defaultResult) {
		this.defaultResult = defaultResult;
	}

	@Override
	public FillInStage deepCopy() {
		FillInStage fillInStageClone = new FillInStage();

		fillInStageClone.deepCopyStageVars(this);

		for (Rule rule : rules) {
			fillInStageClone.rules.add(rule.deepCopy());
		}

		for (Rule correctAnswerRule : correctAnswerRules) {
			fillInStageClone.correctAnswerRules.add(correctAnswerRule.deepCopy());
		}

		for (FillInField fillInField : fillInFields) {
			fillInStageClone.fillInFields.add(fillInField.deepCopy());
		}

		for (DropDownField dropDownField : dropDownFields) {
			fillInStageClone.dropDownFields.add(dropDownField.deepCopy());
		}

		fillInStageClone.formularEditorPalette = formularEditorPalette;

		// Fix stage-references in StageHint. We can do this here, since all stageHints in this stage reference
		// this stage here anyway.
		for (StageHint hint : fillInStageClone.getHints()) {
			hint.setStage(fillInStageClone);
		}

		fillInStageClone.defaultFeedback = defaultFeedback;
		fillInStageClone.defaultResult = defaultResult;
		fillInStageClone.correctAnswerFeedback = correctAnswerFeedback;

		return fillInStageClone;
	}

	@Override
	public boolean mustWaitForPendingJobs() {
		return true;
	}

}
