package de.uni_due.s3.jack3.filters;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.uni_due.s3.jack3.beans.ViewId;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.EnrollmentBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;
import de.uni_due.s3.jack3.multitenancy.TenantIdentifier;

/**
 * Checks if a user has an open CourseRecord for a Course that does not allow pauses. If that exists,
 * the user is redirected there. Otherwise, nothing is done by this filter.
 */
public class CoursePauseFilter implements Filter {

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private CourseBusiness courseBusiness;
	
	@Inject
	private EnrollmentBusiness enrollmentBusiness;

	@Inject
	private ViewId viewId;

	private String showCourseRecordViewId;

	private String redirectUrl;

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
			throws IOException, ServletException {

		final Optional<CourseRecord> record = getRedirectionTargetFor((HttpServletRequest) request);
		if (record.isPresent()) {
			final HttpServletResponse servletResponse = (HttpServletResponse) response;
			servletResponse.sendRedirect(redirectUrl + "&" + ViewId.toParameter(record.get()));
		} else {
			chain.doFilter(request, response);
		}
	}

	private final Optional<CourseRecord> getRedirectionTargetFor(final HttpServletRequest request) {
		// If the request does not need redirection we return an empty optional.
		if (!needsToBeRedirected(request)) {
			return Optional.empty();
		}

		// We can be sure there is an authenticated user because this filter applies to a protected folder.
		final String userName = TenantIdentifier.dequalify(request.getRemoteUser());
		final User user = userBusiness.getUserByName(userName).orElseThrow(IllegalStateException::new);

		// We iterate over the user's open course records.
		final List<CourseRecord> openRecords = enrollmentBusiness.getOpenCourseRecords(user);
		for (CourseRecord record : openRecords) {
			// If there is an open course record referencing a non-pausable and visible course offer we return it.
			// Test course records are not linked to a course offer and are pausable.
			final Optional<CourseOffer> offer = record.getCourseOffer();
			if (offer.isPresent() && !offer.get().isAllowPauses()
					&& enrollmentBusiness.isCourseOfferVisibleForStudent(user, offer.get())) {
					return Optional.of(record);
			}
		}

		// If we did not find a matching course record we return an empty optional.
		return Optional.empty();
	}

	private boolean needsToBeRedirected(final HttpServletRequest request) {
		// The requested view id is not the course record view. We need to redirect.
		if (!request.getServletPath().startsWith(showCourseRecordViewId)) {
			return true;
		}

		// The view is correct and there is no course record parameter.
		// We don't need to redirect as the previous value must have passed this filter.
		final String courseRecordId = request.getParameter("courseRecord");
		if (courseRecordId == null) {
			return false;
		}

		try {
			final long id = Long.parseLong(courseRecordId);
			CourseRecord record;
			try {
				record = courseBusiness.getCourseRecordById(id);
			} catch (NoSuchJackEntityException e) {
				// The id references a non-existing course. The view will handle this error.
				return false;
			}

			// We don't redirect to test course records (course records without a course offer)
			if (record.isTestSubmission()) {
				return false;
			}

			// The user wants to go to its current course anyways. We don't need to redirect.
			// REVIEW lg - Was war die ursprüngliche Intention hinter dieser Bedingung? Aufgrund
			//             "record = courseBusiness.getCourseRecordById(id);"
			//             müsste diese Bedingung immer true sein. Dementsprechend wäre die untere Bedingung
			//             unerreichbar und obsolet.
			if (id == record.getId())
				return false;

			// If the record's course offer forbids pauses we need to redirect.
			return !record
					.getCourseOffer()
					.orElseThrow(() -> new IllegalStateException(
							record + " is not a test record and should be linked to a valid course offer!"))
					.isAllowPauses();
		}
		catch (final NumberFormatException e) {
			// The course record id is syntactically incorrect. The view will handle this error.
			return false;
		}
	}

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
		showCourseRecordViewId = viewId.getCourseRecordView().toOutcome();
		final String contextPath = filterConfig.getServletContext().getContextPath();
		redirectUrl = contextPath + showCourseRecordViewId + ".xhtml?redirected=true";
	}

	@Override
	public void destroy() {
		// This method is intentionally empty.
	}
}
