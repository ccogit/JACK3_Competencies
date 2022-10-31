package de.uni_due.s3.jack3.uitests.utils.pages;

import static de.uni_due.s3.jack3.uitests.utils.Click.click;
import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import de.uni_due.s3.jack3.uitests.utils.Driver;

/**
 * 
 * This page is embedded in different pages which effects the possible Id's. Because of this, UserRightsTablePage
 * doesn't have static elements.
 * 
 * @author Kilian.Kraus
 *
 */

public class UserRightsTablePage {
	private final String SAVE_ID;
	private final String RESET_ID;
	private final String USER_RIGHTS_TABLE;

	public UserRightsTablePage() {
		final String baseId = "editRightsDialogForm";
		this.SAVE_ID = baseId + ":save";
		this.RESET_ID = baseId + ":reset";
		this.USER_RIGHTS_TABLE = baseId + ":userRightsTable:";
	}

	public void saveChanges() {
		waitClickable(By.id(SAVE_ID));
		click(find(SAVE_ID));
	}

	public void resetChanges() {
		waitClickable(By.id(RESET_ID));
		click(find(RESET_ID));
	}

	public void changeRights(String lecturerName, Permission... permissions) {
		int row = getRowOfLecturer(lecturerName);
		for (Permission permission : permissions) {
			switch (permission) {
			case read:
				waitClickable(By.id(USER_RIGHTS_TABLE + row + ":notInheritedReadRights"));
				find(USER_RIGHTS_TABLE + row + ":notInheritedReadRights").click();
				break;
			case extendedRead:
				waitClickable(By.id(USER_RIGHTS_TABLE + row + ":notInheritedExtReadRights"));
				find(USER_RIGHTS_TABLE + row + ":notInheritedExtReadRights").click();
				break;
			case write:
				waitClickable(By.id(USER_RIGHTS_TABLE + row + ":notInheritedWriteRights"));
				find(USER_RIGHTS_TABLE + row + ":notInheritedWriteRights").click();
				break;
			case manage:
				waitClickable(By.id(USER_RIGHTS_TABLE + row + ":notInheritedManageRights"));
				find(USER_RIGHTS_TABLE + row + ":notInheritedManageRights").click();
				break;
			}
		}
	}

	public int getRowOfLecturer(String lecturerName) {
		// Search through xPath the WebElement which contains the folderName
		lecturerName = lecturerName.toLowerCase();
		WebElement lecturer = Driver.get().findElement(By.xpath("//*[text()[contains(., '" + lecturerName + "')]]"));
		WebElement parentElement = lecturer.findElement(By.xpath("./.."));
		return Integer.parseInt(parentElement.getAttribute("data-ri"));
	}

	@Deprecated(forRemoval = true) // TODO Can be replaced by the Access Right constants in the future
	public enum Permission {
		read, extendedRead, write, manage
	}
}
