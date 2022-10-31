package de.uni_due.s3.jack3.entities.stagetypes.fillin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.event.ValueChangeEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OrderColumn;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.interfaces.DeepCopyable;

@Audited
@Entity
public class DropDownField extends FillInStageField implements DeepCopyable<DropDownField> {

	private static final long serialVersionUID = 2661539301975712863L;

	private transient DataModel<String> answerOptionsModell;

	public DropDownField() {
	}

	public DropDownField(String name, int orderIndex) {
		super(name, orderIndex);
		randomize = false;
	}

	@Column
	private boolean randomize;

	public boolean getRandomize() {
		return randomize;
	}

	public void setRandomize(boolean randomize) {
		this.randomize = randomize;
	}

	@ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
	@Type(type = "text")
	@OrderColumn
	private List<String> items = new ArrayList<>();

	/*
	 * @return unmodifiableList of items
	 */
	public List<String> getItems() {
		return Collections.unmodifiableList(items);
	}

	public void addItem(String name) {
		name = name + " " + (items.size() + 1);
		items.add(name);
		reintializeAnswerOptionsModell();
	}
	
	public void addItemPerImport(String item) {
		items.add(item);
	}

	public void removeItem() {
		String answerOption = answerOptionsModell.getRowData();
		items.remove(answerOption);
		reintializeAnswerOptionsModell();
	}

	public void updateAnswerOption(ValueChangeEvent event) {
		String newName = event.getNewValue().toString();
		int index = answerOptionsModell.getRowIndex();
		items.set(index, newName);
		reintializeAnswerOptionsModell();
	}

	public DataModel<String> getAnswerOptionsModell() {
		if (answerOptionsModell == null) {
			reintializeAnswerOptionsModell();
		}
		return answerOptionsModell;
	}

	private void reintializeAnswerOptionsModell() {
		List<String> listAnswerOptions = new ArrayList<>(items);
		answerOptionsModell = new ListDataModel<>(listAnswerOptions);
	}

	public void reorderAnswerOptions(int from, int to) {
		String answerToReorder = items.get(from);
		items.remove(answerToReorder);
		items.add(to, answerToReorder);
		reintializeAnswerOptionsModell();
	}

	@Override
	public DropDownField deepCopy() {
		DropDownField deepCopy = new DropDownField();

		deepCopy.deepCopyAbstractVars(this);
		deepCopy.items.addAll(items);
		deepCopy.randomize = randomize;

		return deepCopy;
	}
}
