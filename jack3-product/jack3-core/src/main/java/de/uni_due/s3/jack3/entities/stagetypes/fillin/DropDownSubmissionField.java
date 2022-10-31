package de.uni_due.s3.jack3.entities.stagetypes.fillin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OrderColumn;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.enums.EFillInSubmissionFieldType;


@Audited
@Entity
public class DropDownSubmissionField extends SubmissionField{

	private static final long serialVersionUID = 2610473766731671615L;

	public static final int NO_ITEM_SELECTED = -1;

	@Column
	@Type(type = "text")
	private String userInput;
	
	/**
	 * The items in the order in which they were displayed
	 */
	@ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
	@Type(type = "text")
	@OrderColumn
	private List<String> items = new ArrayList<>();
	
	@Column
	private boolean randomized;

	/**
	 * The items without randomized order
	 */
	@ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
	@Type(type = "text")
	@OrderColumn
	private List<String> itemsWithoutRandomizedOrder = new ArrayList<>();

	public DropDownSubmissionField() {
	}

	public DropDownSubmissionField(String fieldName,EFillInSubmissionFieldType fieldType, List<String> items,boolean randomized,
			List<String> itemsWithoutRandomizedOrder) {
		super(fieldName,fieldType);
		this.items.addAll(items);
		this.randomized = randomized;
		this.itemsWithoutRandomizedOrder.addAll(itemsWithoutRandomizedOrder);
	}

	/**
	 * Get items with eventual randomized order.
	 */
	public List<String> getItems() {
		return Collections.unmodifiableList(items);
	}

	public void setItems(List<String> items) {
		this.items.clear();
		this.items.addAll(items);
	}

	/**
	 * Get items without randomized order.
	 */
	public List<String> getItemsWithoutRandomizedOrder(){
		return Collections.unmodifiableList(itemsWithoutRandomizedOrder);
	}

	public void setItemsWithoutRandomizedOrder(List<String> itemsWithoutRandomizedOrder) {
		this.itemsWithoutRandomizedOrder.clear();
		this.itemsWithoutRandomizedOrder.addAll(itemsWithoutRandomizedOrder);
	}

	public String getUserInput() {
		return userInput;
	}

	public void setUserInput(String userInput) {
		this.userInput = userInput;
	}
	
	public boolean isDropDownRandomized() {
		return randomized;
	}
}
