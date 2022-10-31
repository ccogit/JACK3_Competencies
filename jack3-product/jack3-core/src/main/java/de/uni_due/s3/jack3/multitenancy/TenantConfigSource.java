package de.uni_due.s3.jack3.multitenancy;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.spi.ConfigSource;

@ApplicationScoped
public class TenantConfigSource implements ConfigSource {

	/*
	 * We're using the initialization-on-demand holder idiom here.
	 * The use of this class is critical to thread safety.
	 */
	private static final class ValueHolder {

		private static final String GRADER_TOPIC_NAME;
		private static final String CONSOLE_TOPIC_NAME;

		static {
			try {
				final String tenantIdentifier = TenantIdentifier.get();
				final String jackCustomHostname = System.getProperty("JackCustomHostname");
				if (jackCustomHostname != null) {
					GRADER_TOPIC_NAME = tenantIdentifier + "-grader-results" + "." + jackCustomHostname;
					CONSOLE_TOPIC_NAME = tenantIdentifier + "-console-results" + "." + jackCustomHostname;
				} else {
					final String hostName = InetAddress.getLocalHost().getCanonicalHostName();
					GRADER_TOPIC_NAME = tenantIdentifier + "-grader-results" + "." + hostName;
					CONSOLE_TOPIC_NAME = tenantIdentifier + "-console-results" + "." + hostName;
				}
			} catch (UnknownHostException e) {
				throw new AssertionError("Unable to detect hostname.", e);
			}
		}
	}

	public static final String CHECKER_TOPIC_KEY = "mp.messaging.incoming.checker-results.topic";
	public static final String CONSOLE_TOPIC_KEY = "mp.messaging.incoming.console-results.topic";

	/**
	 * If a property is specified in multiple config sources, the value in the config source with the highest ordinal
	 * takes precedence. Any configuration source which is a part of an application will typically use an ordinal
	 * between 0 and 200.
	 * Configuration sources provided by the container or 'environment' typically use an ordinal higher than 200. A
	 * framework which intends have values overridden by the application will use ordinals between 0 and 100.
	 */
	private static final int ORDINAL = 200;

	@Override
	public int getOrdinal() {
		return ORDINAL;
	}

	@Override
	public Set<String> getPropertyNames() {
		return Set.of(CHECKER_TOPIC_KEY, CONSOLE_TOPIC_KEY);
	}

	@Override
	public String getValue(final String propertyName) {
		if (CHECKER_TOPIC_KEY.equals(propertyName)) {
			return ValueHolder.GRADER_TOPIC_NAME;
		}
		if (CONSOLE_TOPIC_KEY.equals(propertyName)) {
			return ValueHolder.CONSOLE_TOPIC_NAME;
		}
		return null;
	}

	@Override
	public String getName() {
		return toString();
	}

}
