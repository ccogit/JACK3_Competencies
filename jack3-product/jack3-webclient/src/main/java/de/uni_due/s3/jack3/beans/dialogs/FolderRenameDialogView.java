package de.uni_due.s3.jack3.beans.dialogs;

import java.io.Serializable;

import javax.faces.application.FacesMessage;
import javax.faces.validator.ValidatorException;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;

import de.uni_due.s3.jack3.beans.AbstractView;
import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.utils.JackStringUtils;

@ViewScoped
@Named
public class FolderRenameDialogView extends AbstractView implements Serializable {

	private static final long serialVersionUID = 1L;
	private String newFolderName;
	private String oldFolderName;
	private Folder folder;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private AuthorizationBusiness authorizationBusiness;

	/**
	 * Open the rename-Dialog for the given folder.
	 * Folders can only be renamed, if the user has write-rights on the folder (see Jack3 Wiki/Rechtekonzept).
	 * 
	 */
	public void openRenameFolderDialog(Folder folder) {
		if (!authorizationBusiness.isAllowedToEditFolder(getCurrentUser(), folder)) {
			throw new ValidatorException(
					new FacesMessage(FacesMessage.SEVERITY_ERROR, getLocalizedMessage("start.missingEditRights"),
							getLocalizedMessage("start.missingEditRightsDetails")));
		} else {
			this.newFolderName = "";
			this.setOldFolderName(folder.getName());
			this.folder = folder;
			PrimeFaces.current().executeScript("PF('renameFolderDialog').show()");
		}

	}

	/**
	 * Validates the new name, updates the folder and closes the dialog
	 */
	public void renameFolder() {

		this.validateFolderRename();
		folder.setName(newFolderName);
		folderBusiness.updateFolder(folder);

	}

	public void validateFolderRename() throws ValidatorException {

		if (folder.getName().equals(this.newFolderName)) {
			return;
		}

		if (JackStringUtils.isBlank(newFolderName)) {
			throw new ValidatorException(new FacesMessage(FacesMessage.SEVERITY_ERROR,
					getLocalizedMessage("global.invalidName"), getLocalizedMessage("global.invalidName.empty")));
		}
	}

	public String getNewFolderName() {
		return newFolderName;
	}

	public void setNewFolderName(String newFolderName) {
		this.newFolderName = newFolderName;
	}

	public Folder getFolder() {
		return folder;
	}

	public void setFolder(Folder folder) {
		this.folder = folder;
	}

	public String getOldFolderName() {
		return oldFolderName;
	}

	public void setOldFolderName(String oldFolderName) {
		this.oldFolderName = oldFolderName;
	}

}
