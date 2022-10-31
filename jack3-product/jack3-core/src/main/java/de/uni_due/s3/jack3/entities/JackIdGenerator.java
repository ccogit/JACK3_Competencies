package de.uni_due.s3.jack3.entities;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.inject.spi.CDI;

import de.uni_due.s3.jack3.multitenancy.LoggerProvider;
import de.uni_due.s3.jack3.services.IdentifierService;

/**
 * Generates entity IDs based on an {@link AtomicLong}.
 * 
 * @author Bj&ouml;rn Zurmaar
 */
class JackIdGenerator {

	private static final AtomicLong SEQUENCE;

	static {
		final IdentifierService identifierService = CDI.current().select(IdentifierService.class).get();

		final long start = System.nanoTime();
		final long nextId = identifierService.getMaximumEntityId() + 1;
		final long duration = Duration.ofNanos(System.nanoTime() - start).toMillis();
		LoggerProvider.get(JackIdGenerator.class).infof(
				"The ID generator will start at %d (Initialization took %d ms).",nextId,duration);

		SEQUENCE = new AtomicLong(nextId);
	}

	/**
	 * @return The next available ID.
	 */
	static final Long next() {
		return SEQUENCE.getAndIncrement();
	}
}
