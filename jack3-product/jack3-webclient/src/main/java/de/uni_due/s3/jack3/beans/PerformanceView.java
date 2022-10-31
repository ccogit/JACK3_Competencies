package de.uni_due.s3.jack3.beans;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.Duration;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import de.uni_due.s3.jack3.business.PerformanceBusiness;
import de.uni_due.s3.jack3.business.PerformanceBusiness.Entry;

@Named
@RequestScoped
public class PerformanceView {

	@Inject
	private PerformanceBusiness performanceBusiness;

	private List<Entry> entries;

	private OperatingSystemMXBean operatingSystemMXBean;

	@PostConstruct
	void init() {
		this.entries = performanceBusiness.getEntries();
		this.operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
	}

	public Duration getMaximumAge() {
		return PerformanceBusiness.MAX_AGE;
	}

	public List<Entry> getEntries() {
		return entries;
	}

	public double getRequestsPerMinute() {
		return entries.size() * 60.0 / PerformanceBusiness.MAX_AGE.toSeconds();
	}

	public double getAverageLatency() {
		return entries.stream().mapToLong(Entry::getMillis).average().orElse(Double.NaN);
	}

	public int getSlowResponseShare() {
		final double slowResponses = entries.stream().filter(Entry::isSlow).count();
		return Math.toIntExact(Math.round(slowResponses * 100 / entries.size()));
	}

	public double getSystemLoad() {
		return operatingSystemMXBean.getSystemLoadAverage();
	}

	public int getProcessors() {
		return operatingSystemMXBean.getAvailableProcessors();
	}
}
