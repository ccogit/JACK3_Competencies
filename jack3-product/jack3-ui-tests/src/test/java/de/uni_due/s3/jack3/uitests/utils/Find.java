package de.uni_due.s3.jack3.uitests.utils;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Find elements
 */
public final class Find {

	public static final By ALL = By.cssSelector("*");

	/**
	 * Search a parent web element for child elements that have a specific tag and filter by their displayed text.
	 * 
	 * @param parent
	 *            Where to search for the child elements
	 * @param tag
	 *            HTML tag to search for
	 * @param text
	 *            All alements' texts that are searched for
	 * @return An ordered list of founded elements.
	 */
	public static List<WebElement> findChildElementsByTagAndText(WebElement parent, String tag, String... text) {
		return parent.findElements(By.tagName(tag))
				.stream()
				.filter(element -> {
					for (String s : text) {
						if (element.getText().equals(s))
							return true;
					}
					return false;
				})
				.collect(Collectors.toList());
	}

	/**
	 * Search a parent web element for a child element that has a specific tag and displays a specifig text.
	 * 
	 * @param parent
	 *            Where to search for the child element
	 * @param tag
	 *            HTML tag to search for
	 * @param text
	 *            Displayed text to search for
	 * @return Any element that matches the criteria.
	 * @throws NoSuchElementException
	 *             If no element was found
	 * @see Stream#findAny()
	 */
	public static WebElement findChildElementByTagAndText(WebElement parent, String tag, String text) {
		return parent.findElements(By.tagName(tag))
				.stream()
				.filter(element -> element.getText().equals(text))
				.findAny()
				.get();
	}

	/**
	 * Search a parent web element for a child element that has a specific tag and fulfils a predicate.
	 * 
	 * @param parent
	 *            Where to search for the child element
	 * @param tag
	 *            HTML tag to search for
	 * @param predicate
	 *            Additional restriction
	 * @return Any element that matches the criteria.
	 * @throws NoSuchElementException
	 *             If no element was found
	 * @see Stream#findAny()
	 */
	public static WebElement findChildElementByTag(WebElement parent, String tag, Predicate<WebElement> predicate) {
		return parent.findElements(By.tagName(tag))
				.stream()
				.filter(predicate)
				.findAny()
				.get();
	}

	/**
	 * Search a parent web element for a button with a specific text.
	 * 
	 * @param parent
	 *            Where to search for the button
	 * @param text
	 *            Displayed text to search for
	 * @return Any button with this text
	 * @throws NoSuchElementException
	 *             If no button was found
	 * @see Stream#findAny()
	 * @see #findChildElementByTagAndText(WebElement, String, String)
	 */
	public static WebElement findButtonByText(WebElement parent, String text) {
		return findChildElementByTagAndText(parent, "button", text);
	}

	public static WebElement find(By by) {
		return Driver.get().findElement(by);
	}
	
	public static WebElement getParent(WebElement webElement) {
		return webElement.findElement(By.xpath("./.."));
	}

	public static WebElement find(String id) {
		return Driver.get().findElement(By.id(id));
	}

	public static List<WebElement> findChildren(WebElement parent) {
		return parent.findElements(ALL);
	}

	public static Set<String> collectElementIDs(WebElement parent) {
		return findChildren(parent).stream().map(element -> element.getAttribute("id")).collect(Collectors.toSet());
	}

	/**
	 * Finds a clickable radio button inside a radio button group with the given index. This is necessary because single
	 * radio buttons don't have an ID that refer to a clickable element.
	 */
	public static WebElement findClickableRadioButton(String radioButtonGroupID, int index) {
		return find(radioButtonGroupID)
				.findElements(By.className("ui-radiobutton"))
				.get(index);

	}

}
