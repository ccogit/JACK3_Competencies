package de.uni_due.s3.jack3.uitests.utils;

import java.net.URL;

import org.openqa.selenium.WebDriver;

import de.uni_due.s3.jack3.exceptions.JackRuntimeException;

/**
 * Caches web driver object
 */
public class Driver {

	private static WebDriver cachedDriver;
	private static URL cachedURL;

	public static WebDriver get() {
		if (cachedDriver == null) {
			throw new JackRuntimeException("WebDriver not initialized!");
		}
		return cachedDriver;
	}

	public static void set(WebDriver driver) {
		cachedDriver = driver;
	}

	public static URL getUrl() {
		return cachedURL;
	}

	public static void setUrl(URL url) {
		cachedURL = url;
	}

}
