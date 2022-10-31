package de.uni_due.s3.jack3.beans;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.security.PermitAll;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.multitenancy.TenantIdentifier;

@ViewScoped
@Named
public class LoginView extends AuthenticationView implements Serializable {

	/** Generated serial version UID. */
	private static final long serialVersionUID = -733972297217065347L;

	@Inject
	UserBusiness userBusiness;

	/** The login name entered by the user. */
	private String loginName;

	/** The password entered by the user. */
	private String password;

	/** The URL the user originally wanted to see. */
	private String targetUrl;

	/**
	 * Attempts to extract the URL the user originally wanted to visit out of
	 * the request in order to forward him/her to this URL after a successful
	 * login attempt.
	 */
	@PostConstruct
	public void init() {
		final ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
		final Map<String, Object> requestMap = context.getRequestMap();
		targetUrl = (String) requestMap.get(RequestDispatcher.FORWARD_REQUEST_URI);

		if (targetUrl != null) {
			final String originalQuery = (String) requestMap.get(RequestDispatcher.FORWARD_QUERY_STRING);

			if (originalQuery != null) {
				targetUrl += "?" + originalQuery;
			}
		}
	}

	/**
	 * Attempts to login the user with the credentials provided in {@link #setLoginName(String)} and
	 * {@link #setPassword(String)}. If the attempt is successful the user gets redirected to the URL
	 * request's forward URL.
	 *
	 * @throws IOException
	 *             If there is an {@link IOException} while redirecting the user.
	 */
	public String login() throws IOException {
		try {
			// We log the user's attempt to login.
			getLogger().infof("%s attempts to login from %s.", loginName, getRequest().getRemoteAddr());

			// We programmatically log the user in.
			getRequest().login(TenantIdentifier.qualify(loginName), password);

			// We log the successful attempt.
			final Principal principal = getRequest().getUserPrincipal();
			userBusiness.performPostLoginOperations();
			getLogger().infof("%s successfully logged in.", principal);

			// If the user wanted to go to a certain page we redirect him there.
			if (targetUrl != null) {
				FacesContext.getCurrentInstance().getExternalContext().redirect(targetUrl);
				return "";
			}

			return viewId.getAvailableCourses().toOutcome();
		} catch (final ServletException e) {
			// Login failed. We log that and add a faces message.
			getLogger().infof("%s failed to login: %s", loginName, e.getMessage());

			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "loginView.loginFailedSummary",
					"loginView.loginFailedDetail");

			return "";
		} finally {
			// We should get the password gc'ed as soon as possible.
			password = null;
		}
	}

	/**
	 * Logs the user out and destroys the current session. The user gets redirected to the main page of the application.
	 */
	@PermitAll
	public String logout() {
		// We need the principal for logging later on.
		final String userName = getCurrentUser().getLoginName();

		try {
			// We programmatically log out the user ...
			final HttpServletRequest request = getRequest();
			request.logout();

			// ... and invalidate the HTTP session as well.
			final HttpSession session = request.getSession(false);
			if (session != null) {
				session.invalidate();
			}

			// Finally we log the successful logout.
			getLogger().infof("%s successfully logged out.", userName);

			return viewId.getLogin().redirecting().toOutcome();
		} catch (final ServletException e) {
			getLogger().errorf(e, "Logout of %s failed.", userName);
			return "";
		}
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(final String loginName) {
		this.loginName = loginName.strip().toLowerCase();
	}

	public String getPassword() {
		return "";
	}

	public void setPassword(final String password) {
		this.password = password;
	}
}
