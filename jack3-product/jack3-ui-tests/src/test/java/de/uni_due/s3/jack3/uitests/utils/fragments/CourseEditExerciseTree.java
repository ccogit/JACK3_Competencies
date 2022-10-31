package de.uni_due.s3.jack3.uitests.utils.fragments;

import static de.uni_due.s3.jack3.uitests.utils.Click.clickWithAjax;

import java.util.Arrays;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;

/**
 * Contains utility methods for the Tree element where the available Exercises are.
 */
public class CourseEditExerciseTree {

	private static WebElement getExactElement(WebElement root, String name) {
		// Search through xPath the WebElement which contains the folderName
		try {
			return root.findElement(getExactByForElement(name));
		} catch (TimeoutException e) {
			throw new TimeoutException("Element could not be found! Maybe the parent Folder was not expanded", e);
		}
	}

	public static By getExactByForElement(String name) {
		return By.xpath(".//*[text() = '" + name + "']");
	}

	/**
	 * Expands the tree step by step to the last element of the breadcrumb. In contrast to {@link #expandFolder(String)}
	 * and similar methods, this method recognises wether the element is already expanded.
	 * 
	 * @return The element to which the view was expanded. It may NOT be expanded itself.
	 */
	public static WebElement expandUpToElement(WebElement root, String... breadcrumb) {
		// TODO lg - This method is really convenient and should be used in other tree tests.
		if (breadcrumb.length < 2)
			throw new IllegalArgumentException("The array must contain at least one element and one breadcrumb!");

		// The last element is NOT expanded!
		String[] breadcrumbToExpand = Arrays.copyOfRange(breadcrumb, 0, breadcrumb.length - 1);

		WebElement fragment = null;
		for (String pathFragment : breadcrumbToExpand) {
			// Search for the text.
			fragment = getExactElement(root, pathFragment);
			// "fragment" is the text element next to the toggler, but we need the higher parent
			fragment = fragment.findElement(By.xpath("./../.."));
			if (!Boolean.parseBoolean(fragment.getAttribute("aria-expanded"))) {
				// Now click on the toggler if not already expanded
				clickWithAjax(fragment.findElement(By.className("ui-tree-toggler")));
			}
		}
		return getExactElement(root, breadcrumb[breadcrumb.length - 1]).findElement(By.xpath("./../.."));
	}

	public static void selectNodeViaCheckbox(WebElement parent, boolean selection) {
		WebElement foundExerciseCheckbox = parent.findElement(By.className("ui-chkbox-icon"));
		if (foundExerciseCheckbox.getAttribute("class").contains("ui-icon-check") != selection) {
			clickWithAjax(foundExerciseCheckbox);
		}
	}

}
