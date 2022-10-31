package de.uni_due.s3.jack3.entities.stagetypes.fillin;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.uni_due.s3.jack3.entities.enums.EFillInEditorType;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;

@Audited
@Entity
@XStreamAlias("FillInField")
public class FillInField extends FillInStageField implements DeepCopyable<FillInField> {

	private static final long serialVersionUID = -6247701303836976146L;

	public static final int DEFAULT_SIZE = 10;

	public FillInField() {
	}

	public FillInField(String name, int orderIndex) {
		super(name, orderIndex);
		size = DEFAULT_SIZE;
		setFormularEditorType(EFillInEditorType.NONE.toString());
	}

	@Column
	private int size;

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	@Column
	private EFillInEditorType formularEditorType;

	public EFillInEditorType getFormularEditorEnumType() {
		return formularEditorType;
	}

	public String getFormularEditorType() {
		return formularEditorType.toString();
	}

	public void setFormularEditorType(String formularEdtitorType) {
		formularEditorType = EFillInEditorType.valueOf(formularEdtitorType);
	}

	@Override
	public FillInField deepCopy() {
		FillInField deepCopy = new FillInField();
		deepCopy.deepCopyAbstractVars(this);

		deepCopy.size = size;
		deepCopy.formularEditorType = formularEditorType;

		return deepCopy;
	}
}
