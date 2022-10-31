package de.uni_due.s3.jack3.beans;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

/**
 * This bean's purpose is to keep the session of logged in users alive.
 */
@Named
@RequestScoped
public class SessionKeeperView {

	private static final int ONE_MINUTE = 60;

	private static final int MIN_INTERVAL = ONE_MINUTE;

	@Inject
	private HttpServletRequest request;

	public int getIntervalInSeconds() {
		final int maxInactiveInterval = request.getSession().getMaxInactiveInterval();
		return Math.max(MIN_INTERVAL,maxInactiveInterval - ONE_MINUTE);
	}

	public boolean isEnabled() {
		return request.getUserPrincipal() != null;
	}

	public void keepAlive() {
		// This method is intentionally empty.
		// It's used as a poll listener to keep the session alive.
	}
}
