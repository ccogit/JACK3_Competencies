package de.uni_due.s3.jack3.entities.stagetypes.fillin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.tenant.StageSubmission;

@Audited
@Entity
public class FillInSubmission extends StageSubmission {

	private static final long serialVersionUID = 8578356467540088079L;

	@OneToMany(cascade = CascadeType.ALL,fetch = FetchType.EAGER)
	private Set<SubmissionField> submissionFields = new HashSet<>();

	public Set<SubmissionField> getSubmissionFields() {
		return Collections.unmodifiableSet(submissionFields);
	}

	public List<SubmissionField> getOrderedSubmissionFields(){
		List<SubmissionField> submissionFieldList = new ArrayList<>(submissionFields.size());
		submissionFieldList.addAll(submissionFields);
		Collections.sort(submissionFieldList);
		return submissionFieldList;
	}

	public void addSubmissionFields(List<SubmissionField> submissionFields) {
		for(SubmissionField field : submissionFields) {
			addSubmissionField(field);
		}
	}

	public void addSubmissionField(SubmissionField submissionField) {
		submissionField.setOrderIndex(submissionFields.size());
		this.submissionFields.add(submissionField);
	}

	@Override
	public void copyFromStageSubmission(StageSubmission stageSubmission) {
		if (!(stageSubmission instanceof FillInSubmission)) {
			throw new IllegalArgumentException("Method must be used with instances of FillInSubmission");
		}

		overrideInputAndItemOrderFromOldSubmission((FillInSubmission) stageSubmission, this);
	}

	/**
	 * When Stage repeat is used the input fields has the previous user input and drop Down Elements the same order
	 * (even if random Option is selected)
	 */
	private static void overrideInputAndItemOrderFromOldSubmission(FillInSubmission oldFillInSubmission,
			FillInSubmission fillInSubmission) {
		List<SubmissionField> oldSubmissionFields = oldFillInSubmission.getOrderedSubmissionFields();
		List<SubmissionField> currentSubmissionFields = fillInSubmission.getOrderedSubmissionFields();

		for (int i = 0; i < oldSubmissionFields.size(); i++) {
			SubmissionField oldSubmissionField = oldSubmissionFields.get(i);
			SubmissionField currentSubmissionField = currentSubmissionFields.get(i);
			
			currentSubmissionField.setUserInput(oldSubmissionField.getUserInput());

			if (oldSubmissionField instanceof DropDownSubmissionField) {
				DropDownSubmissionField currentDropDownSubmissionField = (DropDownSubmissionField) currentSubmissionField;
				DropDownSubmissionField oldDropDownSubmissionField = (DropDownSubmissionField) oldSubmissionField;
				
					currentDropDownSubmissionField.setItems(oldDropDownSubmissionField.getItems());
					currentDropDownSubmissionField.setItemsWithoutRandomizedOrder(
							oldDropDownSubmissionField.getItemsWithoutRandomizedOrder());				
			} 
		}
	}
}
