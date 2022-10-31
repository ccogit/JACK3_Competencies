package de.uni_due.s3.jack3.beans;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.uni_due.s3.jack3.business.PerformanceBusiness;

public class PerformanceDataCollector implements PhaseListener {

	private static final long serialVersionUID = 1529126364573605064L;

	private final Map<HttpServletRequest,Long> startTime;

	@Inject
	PerformanceBusiness performanceBusiness;

	public PerformanceDataCollector() {
		this.startTime = new ConcurrentHashMap<HttpServletRequest, Long>();
	}

	@Override
	public PhaseId getPhaseId() {
		return PhaseId.ANY_PHASE;
	}

	@Override
	public void beforePhase(final PhaseEvent event) {
		if (event.getPhaseId() != PhaseId.RESTORE_VIEW) {
			return;
		}
		startTime.put(requestOf(event),System.nanoTime());
	}

	@Override
	public void afterPhase(final PhaseEvent event) {
		if (event.getPhaseId() != PhaseId.RENDER_RESPONSE) {
			return;
		}

		final HttpServletRequest request = requestOf(event);
		final HttpServletResponse response = responseOf(event);
		final long ms = durationOf(request);
		performanceBusiness.addEntry(request,response,ms);
	}

	private HttpServletRequest requestOf(final PhaseEvent event) {
		return (HttpServletRequest) event.getFacesContext().getExternalContext().getRequest();
	}

	private HttpServletResponse responseOf(final PhaseEvent event) {
		return (HttpServletResponse) event.getFacesContext().getExternalContext().getResponse();
	}

	private long durationOf(HttpServletRequest request) {
		final long nanos = System.nanoTime() - startTime.remove(request);
		return TimeUnit.NANOSECONDS.toMillis(nanos);
	}
}
