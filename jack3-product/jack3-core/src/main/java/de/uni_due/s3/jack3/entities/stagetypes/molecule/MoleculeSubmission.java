package de.uni_due.s3.jack3.entities.stagetypes.molecule;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.tenant.StageSubmission;

@Audited
@Entity
public class MoleculeSubmission extends StageSubmission {

	private static final long serialVersionUID = -6874820990974288827L;

	@Column
	@Type(type = "text")
	private String inchiString;

	@Column
	@Type(type = "text")
	private String molfileString;

	@Column
	@Type(type = "text")
	private String editorContentString;

	public String getInchiString() {
		return inchiString;
	}

	public void setInchiString(String inchiString) {
		this.inchiString = inchiString;
	}

	public String getMolfileString() {
		return molfileString;
	}

	public void setMolfileString(String molfileString) {
		this.molfileString = molfileString;
	}

	public String getEditorContentString() {
		return editorContentString;
	}

	public void setEditorContentString(String editorContentString) {
		this.editorContentString = editorContentString;
	}

	@Override
	public void copyFromStageSubmission(StageSubmission stageSubmission) {
		// TODO Auto-generated method stub

	}

}
