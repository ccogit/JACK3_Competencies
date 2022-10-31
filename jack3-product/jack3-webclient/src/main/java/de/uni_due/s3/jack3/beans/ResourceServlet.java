package de.uni_due.s3.jack3.beans;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.uni_due.s3.jack3.business.ResourceBusiness;
import de.uni_due.s3.jack3.entities.tenant.ExerciseResource;
import de.uni_due.s3.jack3.multitenancy.LoggerProvider;

@WebServlet(ResourceServlet.SERVLET_URL)
public class ResourceServlet extends HttpServlet {

	/** The URL under which this servlet should be made available by its container. */
	static final String SERVLET_URL = "/resource";

	public static final String RESOURCE_ID = "resource";

	static final String URL_PATTERN = SERVLET_URL + "?" + RESOURCE_ID+"=";

	/** Generated serial version UID. */
	private static final long serialVersionUID = 7121171360433197638L;

	/**
	 * Returns the download URL for the given resource relative to the context path.
	 * @param resource The resource the URL should be generated for.
	 * @return The download URL for the given resource relative to the context path.
	 */
	public static final String getUrlFor(final ExerciseResource resource)	{
		return URL_PATTERN.concat(Long.toString(resource.getId()));
	}

	/** The resource business providing the resources for our responses. */
	@Inject
	private ResourceBusiness resourceBusiness;

	/**
	 * Attempts to parse the resource's id out of the request.
	 * @param request The request to be parsed.
	 * @return The id extracted from the request object.
	 * @throws NullPointerException If the request parameter "id" is missing.
	 * @throws NumberFormatException If the request parameter "id" does not contain a long identifier.
	 */
	private final long extractId(final HttpServletRequest request) {
		final String idAsString = request.getParameter(RESOURCE_ID);
		return Long.parseLong(idAsString);
	}

	/**
	 * Attempts to parse the the desired disposition type out of the request. If the request
	 * contains a parameter named "disposition-type" and the value of this parameter is (ignoring
	 * case) "attachment" then this method returns the string "attachment". Otherwise it returns
	 * "inline".
	 * @param request The request containing the parameter.
	 * @return The desired disposition type set in the request.
	 */
	private String extractDispositionType(final HttpServletRequest request)	{
		final String type = request.getParameter("disposition-type");
		return "attachment".equalsIgnoreCase(type) ? "attachment" : "inline";
	}

	/**
	 * This method attempts to return an resource for the request received. If the resource is
	 * present in the system the resource's content is written to the response's output stream.
	 * If the request was syntactically correct but no resource with the given id could be found
	 * the error is indicated by a HTTP 404 status code. Syntactically wrong requests result in
	 * a HTTP 400 - Bad Request.
	 */
	@Override
	protected void doGet(final HttpServletRequest request,
			final HttpServletResponse response)	throws ServletException, IOException {
		try {
			// We require the user to be authenticated to download resources.
			// If he isn't we send a HTTP status 403.
			if (request.getRemoteUser() == null) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

			// We try to get the resource with the requested id from the corresponding service.
			final long id = extractId(request);
			final Optional<ExerciseResource> resource = resourceBusiness.getExerciseResourceById(id);

			// If the resource was not found we send HTTP 404.
			if (!resource.isPresent()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			// We check if a specific disposition-type was requested.
			final String dispositionType = extractDispositionType(request);

			// We can now be sure all our parameters are valid and start sending the content.
			sendResource(response, resource.get(), dispositionType);
		}
		catch (final NumberFormatException e) {
			// From Sonarqube:
			// Even though the signatures for methods in a servlet include throws IOException, ServletException,
			// it's a bad idea to let such exceptions be thrown. Failure to catch exceptions in a servlet could
			// leave a system in a vulnerable state, possibly resulting in denial-of-service attacks, or the exposure
			// of sensitive information because when a servlet throws an exception, the servlet container typically
			// sends debugging information back to the user. And that information could be very valuable to an attacker.
			try {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			} catch (IOException ioe) {
				LoggerProvider.get(getClass()).error("IO Error while sending Error-Response: ", ioe);
			}
		} catch (IOException e) {
			LoggerProvider.get(getClass()).error("IO Error while sending Error-Response: ", e);
		}
	}

	/**
	 * Writes the given resource to the response object using the given disposition type.
	 * @param response The response to write the resource to.
	 * @param resource The resource to send.
	 * @param dispositionType The requested disposition type.
	 * @throws IOException If sending the content fails due to an IOException.
	 */
	private final void sendResource(final HttpServletResponse response,
			final ExerciseResource resource, final String dispositionType) throws IOException {
		// We need the filename in its URL encoded form. Unfortunately some browsers do
		// not interpret the plus sign as a space. So we have to take care of that manually.
		String fileName = encode(resource.getFilename());

		// We first set the response's headers.
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentLength(resource.getSize());
		response.setContentType(resource.getMimeType());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setHeader("Content-Disposition",
				String.format("%s; filename*=UTF-8''%s",dispositionType,fileName));

		// And then the resource's content.
		final ServletOutputStream out = response.getOutputStream();
		resource.writeToStream(out);
		out.flush();
	}

	/**
	 * Encodes the given file name so that it can be inserted into the "Content-Disposition"
	 * (or any other) header of a HTTP response or more generally speaking in any place that
	 * requires URL encoding.
	 * @param rawFileName The filename to be encode.
	 * @return The string rawFilename encoded for use in HTTP headers.
	 */
	public static final String encode(String rawFileName) {
		// We have to replace certain characters in the filname because they have special meanings:
		final String preparedFileName = rawFileName
				.replace(':','-')  // The colon separates drives from folders on windows
				.replace('\\','-') // backslashes are used as file separators on windows.
				.replace('/','-')  // Slashes are *nix file separators.
				.replace('?','-')  // Question marks separate a path from parameters.
				.replace('&','-'); // The ampersand separates parameters.

		// Now that we have a sane string we can encode it.
		final String encodedFileName = urlEncodeWithUTF8(preparedFileName);

		// Most browser misinterpret the plus sign used by URLEncode for spaces, so we have to
		// replace them with the corresponding percentage encoding instead before returning.
		return encodedFileName.replace("+","%20");
	}

	private static final String urlEncodeWithUTF8(final String s) {
		try {
			return URLEncoder.encode(s,StandardCharsets.UTF_8.name());
		}
		catch (final UnsupportedEncodingException e) {
			throw new AssertionError("UTF-8 should be supported on all platforms.",e);
		}
	}
}
