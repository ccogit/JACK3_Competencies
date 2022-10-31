package de.uni_due.s3.jack3.filters;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.multitenancy.TenantIdentifier;

public class UserRightsFilter implements Filter {

	@Inject
	UserBusiness userBusiness;

	@Override
	public void init(final FilterConfig config) throws ServletException {}

	@Override
	public void doFilter(final ServletRequest req,final ServletResponse resp, final FilterChain chain)
		throws IOException, ServletException {

		final HttpServletRequest request = (HttpServletRequest) req;
		final HttpServletResponse response = (HttpServletResponse) resp;

		final String userName = request.getRemoteUser();
		if (userName == null) {
			// If this happened we failed big time because the web.xml should guarantee the
			// user, editor and administrator folders can only be accessed by authenticated users.
			throw new AssertionError("Unauthenticated access to a protected folder.");
		}

		final String rawUserName = TenantIdentifier.dequalify(userName);
		// If the user is authenticated and not in our database we failed big time in our code.
		final User user = userBusiness.getUserByName(rawUserName)
			.orElseThrow(() -> new AssertionError("Authenticated user not in database."));

		final String pathInfo = request.getRequestURI();
		final String contextPath = request.getContextPath();
		final String relativePath = pathInfo.substring(contextPath.length());

		// Only users with edit rights may see the /editor subfolder.
		if (relativePath.startsWith("/editor") && !user.isHasEditRights()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		// Only users with admin rights may see the /adminstrator subfolder.
		if (relativePath.startsWith("/administrator") && !user.isHasAdminRights()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		chain.doFilter(request,response);
	}

	@Override
	public void destroy() {}
}
