package de.uni_due.s3.jack3.beans.data;

import java.io.Serializable;
import java.time.LocalDateTime;

import de.uni_due.s3.jack3.entities.tenant.CourseOffer;

/**
 * Stores the visibility status for course offers (visible, not visible, when the course offer will be visible, etc.).
 * All properties are nullable.
 */
public class CourseOfferVisibilityStatus implements Serializable {

	private static final long serialVersionUID = -3725429059287088057L;

	/** If the course offer is currently visible */
	private boolean visible;

	/** If the course offer is currently NOT visible, when the course offer will be visible. */
	private LocalDateTime willBeVisible;
	
	public void setFromCourseOffer(final CourseOffer offer) {
		final LocalDateTime start = offer.getVisibilityStartTime();
		final LocalDateTime end = offer.getVisibilityEndTime();
		final LocalDateTime now = LocalDateTime.now();

		if (!offer.isCanBeVisible()) {
			// course offer is not visible at all
			visible = false;
			willBeVisible = null;
		} else if (start == null && end == null) {
			// no restrictions
			visible = true;
			willBeVisible = null;
		} else {
			// Look for the visibility restriction
			visible = (start == null || start.isBefore(now)) && (end == null || end.isAfter(now));
			willBeVisible = (!visible && start != null && start.isAfter(now)) ? start : null;
		}
	}

	public boolean isVisible() {
		return visible;
	}
	
	public LocalDateTime getWillBeVisible() {
		return willBeVisible;
	}

}
