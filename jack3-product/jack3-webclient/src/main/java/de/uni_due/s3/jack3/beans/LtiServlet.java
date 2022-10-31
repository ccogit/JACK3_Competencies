package de.uni_due.s3.jack3.beans;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;

import de.uni_due.s3.jack3.business.ConfigurationBusiness;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.LtiBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.LtiLaunch;
import de.uni_due.s3.jack3.multitenancy.LoggerProvider;
import de.uni_due.s3.jack3.multitenancy.TenantIdentifier;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.SimpleOAuthValidator;
import net.oauth.server.OAuthServlet;

@WebServlet(LtiServlet.SERVLET_URL)
public class LtiServlet extends HttpServlet {

	private static final class LtiException extends ServletException {

		private static final long serialVersionUID = -6810741940156333763L;

		private int status;

		private String message;

		private LtiException(final int status,final String message) {
			this.status = status;
			this.message = message;
		}

		private LtiException(final int status) {
			this(status,null);
		}

		private final void sendError(final HttpServletResponse response) throws IOException {
			if (message != null) {
				response.sendError(status,message);
			} else {
				response.sendError(status);
			}
		}
	}

	/** Generated serial version UID. */
	private static final long serialVersionUID = -7093883260781457404L;

	/** The URL under which this servlet should be made available by its container. */
	static final String SERVLET_URL = "/public/lti";

	static final String LTI_CONFIGURATION_KEY = "LtiEnabled";

	private static final String LTI_RESOURCE_LINK_ID = "resource_link_id";

	private static final String LTI_CONSUMER_KEY = "oauth_consumer_key";

	private static final String LTI_CONSUMER_INSTANCE_GUID = "tool_consumer_instance_guid";

	private static final String LTI_LAUNCH_PRESENTATION_RETURN_URL = "launch_presentation_return_url";

	private static final String LTI_CONTEXT_LABEL = "context_label";

	private static final String UTF_8 = StandardCharsets.UTF_8.name();

	private static final List<String> DEFAULT_USERNAME_PARAMETERS = List.of("user_name","ext_user_username");

	@Inject
	private ViewId viewId;

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private LtiBusiness ltiBusiness;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private ConfigurationBusiness configurationBusiness;

	private final Logger logger;

	public LtiServlet() {
		this.logger = LoggerProvider.get(getClass());
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// We have to explicitly set an encoding if none has been set. Otherwise validation will fail.
		if (!UTF_8.equals(request.getCharacterEncoding())) {
			request.setCharacterEncoding(UTF_8);
		}

		try {
			//Step 1: We check if LTI is globally enabled.
			if (!configurationBusiness.booleanOf(LTI_CONFIGURATION_KEY)) {
				throw new LtiException(HttpServletResponse.SC_FORBIDDEN,"LTI disabled.");
			}

			// Step 2: We attempt to extract the requested course offer.
			final CourseOffer courseOffer = loadCourseOffer(request,response);

			// Step 3: We check the request for cryptographic validity.
			validateOAuthMessage(request,courseOffer.getLtiConsumerSecret(),response);

			// Step 4: The request is validated. We prepare a LTI launch.
			final String userName = getParameter(request,getUsernameParameterNames());
			final String consumerInstanceGuid = getParameter(request,LTI_CONSUMER_INSTANCE_GUID);
			final String resourceLinkId = getParameter(request,LTI_RESOURCE_LINK_ID);
			final String contextLabel = getParameter(request,LTI_CONTEXT_LABEL);
			final String returnUrl = getParameter(request,LTI_LAUNCH_PRESENTATION_RETURN_URL);
			final LtiLaunch launch = ltiBusiness.prepareLaunch(userName,consumerInstanceGuid,resourceLinkId,contextLabel,returnUrl);

			// Step 5: We login the request's user.
			performLogin(request, response, launch);
			if (response.isCommitted()) {
				return;
			}

			// Step 6: We redirect to the requested course offer.
			final String url = viewId.getCourseMainMenu()
				.withParam(courseOffer)
				.toActionUrl(request.getServletContext());
			response.sendRedirect(url);
		}
		catch (final LtiException e) {
			e.sendError(response);
			logFailedAttempt(request, response);
		}
	}

	private List<String> getUsernameParameterNames() {
		final List<String> parameterNames = configurationBusiness.getValueList("LtiUsernameParameterNames");
		if (!parameterNames.isEmpty()) {
			return parameterNames;
		}

		return DEFAULT_USERNAME_PARAMETERS;
	}

	private String getParameter(final HttpServletRequest request,final String ... parameterNames) throws LtiException {
		return getParameter(request, List.of(parameterNames));
	}

	private String getParameter(final HttpServletRequest request,final List<String> parameterNames)  throws LtiException {
		if (parameterNames.isEmpty()) {
			throw new IllegalArgumentException("paramater names must not be empty.");
		}

		for (final String name : parameterNames) {
			final String value = request.getParameter(name);
			if (value != null) {
				return value;
			}
		}

		throw new LtiException(HttpServletResponse.SC_BAD_REQUEST,"Missing parameter: " + parameterNames);
	}

	private CourseOffer loadCourseOffer(HttpServletRequest request,HttpServletResponse response)
			throws IOException, LtiException {
		final String consumerKey = getParameter(request,LTI_CONSUMER_KEY);
		if (!consumerKey.startsWith(CourseOffer.CONSUMER_KEY_PREFIX)) {
			throw new LtiException(HttpServletResponse.SC_UNAUTHORIZED,"Bad consumer key.");
		}
		final long courseOfferId = parseCourseOfferId(consumerKey);
		Optional<CourseOffer> offer = courseBusiness.getCourseOfferById(courseOfferId);
		if (offer.isEmpty()) {
			throw new LtiException(HttpServletResponse.SC_FORBIDDEN,"Unknown consumer key.");
		}
		if (!offer.get().isLtiEnabled()) {
			throw new LtiException(HttpServletResponse.SC_FORBIDDEN,"Disabled consumer key.");
		}
		return offer.get();
	}

	private final long parseCourseOfferId(final String consumerKey) throws LtiException {
		try {
			return Long.parseLong(consumerKey,CourseOffer.CONSUMER_KEY_PREFIX.length(),consumerKey.length(),10);
		}
		catch (final NumberFormatException e) {
			throw new LtiException(HttpServletResponse.SC_FORBIDDEN,"Bad consumer key.");
		}
	}

	private void validateOAuthMessage(HttpServletRequest request,String consumerSecret, HttpServletResponse response)
			throws IOException, LtiException {

		final OAuthMessage requestMessage = OAuthServlet.getMessage(request, null);
		final String consumerKey = requestMessage.getConsumerKey();
		final OAuthConsumer consumer = new OAuthConsumer(null, consumerKey, consumerSecret, null);
		final OAuthAccessor accessor = new OAuthAccessor(consumer);

		try {
			new SimpleOAuthValidator().validateMessage(requestMessage, accessor);
		} catch (OAuthException | URISyntaxException e) {
			logger.warn("OAuth validation failed.",e);
			throw new LtiException(HttpServletResponse.SC_BAD_REQUEST,"OAuth validation failed.");
		}
	}

	private void performLogin(HttpServletRequest request, HttpServletResponse response, LtiLaunch launch)
			throws IOException, LtiException {
		final String remoteUser = request.getRemoteUser();
		final String ltiUserName = launch.getUserName();

		// If there already is a remote user we have to check if it is the correct one.
		if (remoteUser != null) {
			final String remoteUserName = TenantIdentifier.dequalify(remoteUser);
			if (!remoteUserName.equals(ltiUserName)) {
				// If a different user is logged in we tell him about this.
				final String url = viewId.getLogin().toActionUrl(request.getServletContext());
				response.sendRedirect(url);
			}
			// If the correct user is already logged in we don't need to do anything.
			return;
		}

		try {
			request.login(TenantIdentifier.qualify(launch.getLoginName()),launch.getToken());
			userBusiness.performPostLoginOperations();
			logger.info(launch.getLoginName() + " successfully logged in via LTI.");
		}
		catch (final ServletException e) {
			logger.error("Unexpected problem during LTI login attempt.",e);
			throw new LtiException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private void logFailedAttempt(final HttpServletRequest request,final HttpServletResponse response) {
		final String parameters = parametersToString(request);
		logger.warn("LTI request failed with status code " + response.getStatus() + ".\n" + parameters);
	}

	private String parametersToString(HttpServletRequest request) {
		final int maxKeyLength = request.getParameterMap().keySet().stream()
			.mapToInt(String::length)
			.max()
			.orElse(0);

		return request.getParameterMap().entrySet().stream()
			.sorted(Comparator.comparing(Map.Entry::getKey))
			.map(entry -> toString(entry,maxKeyLength))
			.collect(Collectors.joining("\n"));
	}

	private final String toString(Map.Entry<String,? extends Object[]> entry,final int keyLength) {
		return " ".repeat(keyLength - entry.getKey().length())
			+ entry.getKey()
			+ " = "
			+ Arrays.toString(entry.getValue());
	}
}
