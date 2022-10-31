package de.uni_due.s3.jack3.services.utils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import de.uni_due.s3.jack3.multitenancy.LoggerProvider;

/**
 * Utility class providing methods to scan for classes in a given package.
 *
 * @author http://stackoverflow.com/questions/15519626/how-to-get-all-classes-
 *         names-in-a-package (for most of the code)
 * @author striewe (for restriction to specific superclass)
 *
 */
public class ClassFinder {

	private static final char PKG_SEPARATOR = '.';

	private static final char DIR_SEPARATOR = '/';

	private static final String CLASS_FILE_SUFFIX = ".class";

	private static final String BAD_PACKAGE_ERROR = "Unable to get resources from path '%s'. Are you sure the package '%s' exists?";

	@SuppressWarnings("unchecked")
	public static <T> List<Class<? extends T>> find(String scannedPackage, Class<T> superClass) {
		final List<Class<?>> candidates = find(scannedPackage);
		final List<Class<? extends T>> resultList = new ArrayList<>();
		for (final Class<?> clazz : candidates) {
			if (superClass.isAssignableFrom(clazz)) {
				resultList.add((Class<? extends T>) clazz);
			}
		}
		return resultList;
	}

	public static List<Class<?>> find(String scannedPackage) {
		final String scannedPath = scannedPackage.replace(PKG_SEPARATOR, DIR_SEPARATOR);
		final URL scannedUrl = Thread.currentThread().getContextClassLoader().getResource(scannedPath);
		if (scannedUrl == null) {
			throw new IllegalArgumentException(String.format(BAD_PACKAGE_ERROR, scannedPath, scannedPackage));
		}
		final File scannedDir = new File(scannedUrl.getFile());
		final List<Class<?>> classes = new ArrayList<>();
		if (scannedDir.listFiles() != null) {
			for (final File file : scannedDir.listFiles()) {
				classes.addAll(find(file, scannedPackage));
			}
		} else {
			LoggerProvider.get(ClassFinder.class).debug("Could not find any files within "
					+ scannedUrl.toString() + ". Check access rights for this location.");
		}
		return classes;
	}

	private static List<Class<?>> find(File file, String scannedPackage) {
		final List<Class<?>> classes = new ArrayList<>();
		final String resource = scannedPackage + PKG_SEPARATOR + file.getName();
		if (file.isDirectory() && file.listFiles() != null) {
			for (final File child : file.listFiles()) {
				classes.addAll(find(child, resource));
			}
		} else if (resource.endsWith(CLASS_FILE_SUFFIX)) {
			final int endIndex = resource.length() - CLASS_FILE_SUFFIX.length();
			final String className = resource.substring(0, endIndex);
			try {
				// TODO ms: Only add classes for which also a StageBusiness implementation is available.
				classes.add(Class.forName(className));
			} catch (final ClassNotFoundException ignore) {
			}
		}
		return classes;
	}

}