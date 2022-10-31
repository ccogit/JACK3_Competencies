package de.uni_due.s3.jack3.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import de.uni_due.s3.jack3.multitenancy.TenantIdentifier;
import de.uni_due.s3.jack3.services.utils.LoggedInUserName;


/**
 * Cache username before the inner FilterChain.doFilter invocation and remove it afterwards, so hibernate can use it to
 * automatically update the updatedBy attribute of {@link de.uni_due.s3.jack3.entities.AbstractEntity}
 * 
 * @author Benjamin Otto
 * @see <a
 *      href="https://vladmihalcea.com/how-to-emulate-createdby-and-lastmodifiedby-from-spring-data-using-the-generatortype-hibernate-annotation/">How
 *      to emulate @CreatedBy and @LastModifiedBy from Spring Data using the @GeneratorType Hibernate annotation</a>
 */
public class LoggedInUserNameFilter implements Filter {

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// Nothing to do here
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {

		try {
			HttpServletRequest httpServletRequest = (HttpServletRequest) request;
			String userName = TenantIdentifier.dequalify(httpServletRequest.getUserPrincipal().getName());
			LoggedInUserName.set(userName);
			filterChain.doFilter(request, response);
		} finally {
			LoggedInUserName.remove();
		}
	}

	@Override
	public void destroy() {
		// Nothing to do here
	}
}
