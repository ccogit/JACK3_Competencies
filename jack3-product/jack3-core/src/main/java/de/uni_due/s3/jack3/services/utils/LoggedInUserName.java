package de.uni_due.s3.jack3.services.utils;

/**
 * Static utility class to threadsafely cache the name of the currently logged in user with edit rights (see web.xml
 * filter for /editor/*).
 * 
 * @author Benjamin Otto
 * @see <a
 *      href="https://vladmihalcea.com/how-to-emulate-createdby-and-lastmodifiedby-from-spring-data-using-the-generatortype-hibernate-annotation/">How
 *      to emulate @CreatedBy and @LastModifiedBy from Spring Data using the @GeneratorType Hibernate annotation</a>
 */
public class LoggedInUserName {

	private static final ThreadLocal<String> userHolder = new ThreadLocal<>();

	private LoggedInUserName() {
		throw new IllegalStateException("Class must only be used statically!");
	}

	public static void set(String user) {
		userHolder.set(user);
	}

	public static void remove() {
		userHolder.remove();
	}

	public static String get() {
		return userHolder.get();
	}
}