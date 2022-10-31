package de.uni_due.s3.jack3.beans.dialogs;

import java.io.Serializable;

import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import de.uni_due.s3.jack3.beans.AbstractView;

/**
 * Important for usage: After a deletion-attempt, closeDeletionDialog() is ment to be executed.
 * 
 */
@ViewScoped
@Named
public class DeletionDialogView extends AbstractView implements Serializable {

	private static final long serialVersionUID = 1L;

	private String nameToCheckForDeletion;

	private String inputTextForDeletion;

	public DeletionDialogView() {
		// not needed
	}

	public void prepareDeletionDialog() {
		prepareDeletionDialog("");
	}

	/**
	 * 
	 * @param nameToCheckForDeletion
	 */
	public void prepareDeletionDialog(String nameToCheckForDeletion) {
		setInputTextForDeletion("");
		setNameToCheckForDeletion(nameToCheckForDeletion);
		PrimeFaces.current().executeScript("PF('deleteButton').disable()");
	}

	/**
	 * @return the nameCourseOfferForDeletion
	 */
	public String getNameToCheckForDeletion() {
		return nameToCheckForDeletion;
	}

	/**
	 * @param nameCourseOfferForDeletion
	 *            the nameCourseOfferForDeletion to set
	 */
	public void setNameToCheckForDeletion(String nameCourseOfferForDeletion) {
		this.nameToCheckForDeletion = nameCourseOfferForDeletion;
	}

	/**
	 * @return the inputTextForDeletion
	 */
	public String getInputTextForDeletion() {
		return inputTextForDeletion;
	}

	/**
	 * @param inputTextForDeletion
	 *            the inputTextForDeletion to set
	 */
	public void setInputTextForDeletion(String inputTextForDeletion) {
		this.inputTextForDeletion = inputTextForDeletion;
	}

	/**
	 * Should be executed after the deletion(successful or not)
	 */
	public void closeDeletionDialog() {
		followupDeletionDialog(null);
		PrimeFaces.current().executeScript("PF('deleteDialog').hide(),PF('deleteButton').disable()");
	}

	/**
	 * Should be executed when the deletion Dialog is closed
	 * We need to search this id, because this dialog can be included in multiple paths and hence doesn't always have
	 * the same id
	 * for References: document.getElementById('treeDeletionForm:deletionInput').setAttribute('value','')
	 * 
	 * @param ignored
	 */
	public void followupDeletionDialog(ActionEvent ignored) {
		setInputTextForDeletion("");//associated to Script-execution below
		PrimeFaces.current().executeScript(
				"var tmp=document.getElementsByClassName('ui-inputtext'); for(var tmpNbr=0,length=tmp.length;tmpNbr<length;tmpNbr++){if(tmp[tmpNbr].id.includes('deletionInput')){tmp[tmpNbr].setAttribute('value','')}}");//associated to setInputTextForDeletion("");
	}

	public boolean isInputtextEqualsNameToCheck() {
		return nameToCheckForDeletion.equals(inputTextForDeletion);
	}

}
