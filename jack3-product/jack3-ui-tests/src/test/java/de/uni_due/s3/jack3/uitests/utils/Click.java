package de.uni_due.s3.jack3.uitests.utils;

import org.jboss.arquillian.graphene.Graphene;
import org.jboss.arquillian.graphene.request.RequestGuardException;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 * Selenium click interactions
 */
public final class Click {

	public static void rightClick(By by) {
		rightClick(Driver.get().findElement(by));
	}

	public static void rightClick(WebElement webElement) {
		Actions actionBuilder = new Actions(Driver.get());
		actionBuilder.moveToElement(webElement);
		actionBuilder.contextClick();
		actionBuilder.build().perform();
	}
	
	public static void doubleClick(WebElement webElement) {
		Actions actions = new Actions(Driver.get());
		actions.doubleClick(webElement).perform();
	}

	public static void click(WebElement element) {
		element.click();
	}

	public static void click(By by) {
		Driver.get().findElement(by).click();
	}

	/**
	 * Click a web element and expect no resulting request, neither HTTP nor AJAX.
	 * 
	 * @param element
	 *            Web element
	 * @throws RequestGuardException
	 *             when a HTTP or AJAX request is observed
	 * @see Graphene#guardNoRequest(Object)
	 **/
	public static void clickWithNoRequest(WebElement element) {
		Graphene.guardNoRequest(element).click();
	}

	/**
	 * Click a web element and expect no resulting request, neither HTTP nor AJAX.
	 * 
	 * @param by
	 *            Element locator
	 * @throws RequestGuardException
	 *             when a HTTP or AJAX request is observed
	 * @see Graphene#guardNoRequest(Object)
	 */
	public static void clickWithNoRequest(By by) {
		Graphene.guardNoRequest(Driver.get().findElement(by)).click();
	}

	/**
	 * Click a web element that performs a HTTP redirect.
	 * 
	 * @param element
	 *            Web element
	 * @throws RequestGuardException
	 *             when no HTTP request is observed
	 * @see Graphene#waitForHttp(Object)
	 **/
	public static void clickWithRedirect(WebElement element) {
		Graphene.waitForHttp(element).click();
	}

	/**
	 * Click a web element that performs a HTTP redirect.
	 * 
	 * @param by
	 *            Element locator
	 * @throws RequestGuardException
	 *             when no HTTP request is observed
	 * @see Graphene#waitForHttp(Object)
	 */
	public static void clickWithRedirect(By by) {
		Graphene.waitForHttp(Driver.get().findElement(by)).click();
	}

	/**
	 * Click a web element and expect an AJAX request.
	 * 
	 * @param element
	 *            Web element
	 * @throws RequestGuardException
	 *             when no AJAX request is observed
	 * @see Graphene#guardAjax(Object)
	 */
	public static void clickWithAjax(WebElement element) {
		Graphene.guardAjax(element).click();
	}

	/**
	 * Click a web element and expect an AJAX request.
	 * 
	 * @param by
	 *            Element locator
	 * @throws RequestGuardException
	 *             when no AJAX request is observed
	 * @see Graphene#guardAjax(Object)
	 */
	public static void clickWithAjax(By by) {
		Graphene.guardAjax(Driver.get().findElement(by)).click();
	}

	/**
	 * Click a web element using JavaScript.
	 * 
	 * @param element
	 *            Web element
	 */
	public static void clickWithJs(WebElement element) {
		JavascriptExecutor executor = (JavascriptExecutor) Driver.get();
		executor.executeScript("arguments[0].click();", element);
	}

}
