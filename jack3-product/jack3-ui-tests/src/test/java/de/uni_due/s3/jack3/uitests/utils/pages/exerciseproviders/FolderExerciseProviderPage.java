package de.uni_due.s3.jack3.uitests.utils.pages.exerciseproviders;

import static de.uni_due.s3.jack3.uitests.utils.Find.find;
import static de.uni_due.s3.jack3.uitests.utils.Time.waitClickable;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.opentest4j.AssertionFailedError;

import de.uni_due.s3.jack3.uitests.utils.fragments.CourseEditExerciseTree;

public class FolderExerciseProviderPage {

	private static final String CONTENT_TREE = "courseEditMainForm:chooseFolder_tree";

	public static void addFolderWithName(String... breadcrumb) {
		waitClickable(By.id(CONTENT_TREE));
		WebElement foundFolder = CourseEditExerciseTree.expandUpToElement(find(CONTENT_TREE), breadcrumb);
		if (foundFolder == null) {
			throw new AssertionFailedError("Exercise '" + breadcrumb[breadcrumb.length - 1] + "' not found");
		}
		CourseEditExerciseTree.selectNodeViaCheckbox(foundFolder, true);
		// Otherwise checkbox is already checked.
	}
}
