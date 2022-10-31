package de.uni_due.s3.jack3.business;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger.Level;

@ApplicationScoped
public class PerformanceBusiness extends AbstractBusiness {

	public static class Entry {

		private static final AtomicLong SEQUENCE = new AtomicLong();

		private static final long SLOW = Duration.ofSeconds(3).toMillis();

		private final long sequence;

		private final String protocol;

		private final String type;

		private final String method;

		private final String path;

		private final long millis;

		private final int status;

		private final long expiry;

		private Entry(final HttpServletRequest request,final HttpServletResponse response,final long duration) {
			this.protocol = request.getProtocol();
			this.method = request.getMethod();
			this.path = request.getRequestURI().substring(request.getContextPath().length());
			this.status = response.getStatus();
			this.millis = duration;
			this.expiry = System.currentTimeMillis() + MAX_AGE.toMillis();
			this.type = "XMLHttpRequest".equals(request.getHeader("X-Requested-With")) ? "XHR" : "HTTP";
			this.sequence = SEQUENCE.getAndIncrement();
		}

		public long getSequence() {
			return sequence;
		}

		public String getProtocol() {
			return protocol;
		}

		public String getType() {
			return type;
		}

		public String getMethod() {
			return method;
		}

		public String getPath() {
			return path;
		}

		public long getMillis() {
			return millis;
		}

		public int getStatus() {
			return status;
		}

		public boolean isSlow() {
			return millis >= SLOW;
		}

		boolean isExpired() {
			return expiry < System.currentTimeMillis();
		}

		@Override
		public String toString() {
			return String.format("%s %s HTTP %d %d ms.",method,path,status,millis);
		}
	}

	public static final Duration MAX_AGE = Duration.ofMinutes(5);

	private final Object lock;

	private final LinkedList<Entry> entries;

	private long nextLogging;

	PerformanceBusiness() {
		this.entries = new LinkedList<>();
		this.lock = new Object();
		this.nextLogging = nextLoggingTime();
	}

	public void addEntry(final HttpServletRequest request, final HttpServletResponse response, final long ms) {
		final Entry entry = new Entry(request,response,ms);
		List<Entry> entriesToLog;

		synchronized (lock) {
			removeOutdateEntries();
			entries.add(entry);
			entriesToLog = getEntriesToLog();
		}

		if (entry.isSlow()) {
			logSlowRequest(request,entry);
		}

		if (!entriesToLog.isEmpty()) {
			logEntrySummary(entriesToLog);
		}
	}

	public List<Entry> getEntries() {
		synchronized (lock) {
			removeOutdateEntries();
			return new ArrayList<>(entries);
		}
	}

	/* Must be called with lock acquired! */
	private void removeOutdateEntries() {
		final Iterator<Entry> it = entries.iterator();
		while (it.hasNext() && it.next().isExpired()) {
			it.remove();
		}
	}

	/* Must be called with lock acquired! */
	private List<Entry> getEntriesToLog() {
		if (System.currentTimeMillis() < nextLogging) {
			return Collections.emptyList();
		}

		nextLogging = nextLoggingTime();
		return new ArrayList<>(entries);
	}

	private long nextLoggingTime() {
		return System.currentTimeMillis() + MAX_AGE.toMillis();
	}

	private void logSlowRequest(final HttpServletRequest request,final Entry entry) {
		final String user = request.getRemoteUser();
		final String msg = "Slow response" + (user != null ? " to " + user : "") + ": ";
		getLogger().warn(msg + entry);
	}

	private void logEntrySummary(final List<Entry> entriesToLog) {
		final LongSummaryStatistics statistics = entriesToLog.stream()
			.mapToLong(Entry::getMillis)
			.summaryStatistics();
		final long slow = entriesToLog.stream().filter(Entry::isSlow).count();

		getLogger().logf(
			slow > 0 ? Level.WARN : Level.INFO,
			"%d responses processed, %d slow (%d ms - %d ms, avg. %.0f ms)",
			statistics.getCount(),slow,statistics.getMin(),statistics.getMax(),statistics.getAverage());
	}
}
