package de.uni_due.s3.jack3.beans;

import java.io.IOException;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.annotation.Nullable;
import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.application.FacesMessage.Severity;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jboss.logging.Logger;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.MenuModel;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.business.helpers.PublicUserName;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.ExerciseResource;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.FrozenCourse;
import de.uni_due.s3.jack3.entities.tenant.FrozenExercise;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.multitenancy.LoggerProvider;
import de.uni_due.s3.jack3.multitenancy.TenantIdentifier;

/**
 * This is an abstract base class for all view beans we use in this project and
 * offers some convenience methods delegating to frequently used accessors as
 * well as a default logger.
 */

public abstract class AbstractView {

	private final Logger logger;

	@Inject
	private UserSession userSession;

	@Inject
	private UserBusiness userBusiness;
	
	@Inject
	private ExerciseBusiness exerciseBusiness;
	
	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	protected ViewId viewId;

	@Inject
	private PathComponent pathComponent;

	/**
	 * Default constructor which just obtains a default logger instance for this
	 * view based on its class.
	 */
	protected AbstractView() {
		logger = LoggerProvider.get(getClass());
	}

	/**
	 * Returns the {@link Locale} set in the current faces context.
	 *
	 * @return {@link Locale}
	 * @see #getResponseLocale()
	 */
	// REVIEW lg - Wir sollten diese Methode entfernen, da zwei Methoden, die dasselbe machen, Verwirrung stiften.
	protected Locale getCurrentLocale() {
		return getResponseLocale();
	}

	/**
	 * Returns the logged in user that is associated with the current request or
	 * {@code null} if no such user exists.
	 *
	 * @return the logged in user that is associated with the current request.
	 */
	public User getCurrentUser() {
		final Principal principal = getRequest().getUserPrincipal();

		if (principal == null) {
			return null;
		}

		final String userName = TenantIdentifier.dequalify(principal.getName());
		final Optional<User> user = userBusiness.getUserByName(userName);

		if (user.isPresent()) {
			return user.get();
		}

		getLogger().warn("Principal " + userName + " could not be retreived from user service. Logging out...");
		// If we can't find our current user in Database we destroy the session and logging them out.
		// Throwing an Exception here could lead to a not usable server!
		final HttpServletRequest request = getRequest();
		try {
			request.logout();
		} catch (ServletException e) {
			getLogger().error(e);
		}

		final HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}

		return null;
	}

	/**
	 * Returns the FacesContext object for the request being currently
	 * processed. This is a convenience method which just delegates to
	 * {@link FacesContext#getCurrentInstance()}.
	 */
	private FacesContext getFacesContext() {
		return FacesContext.getCurrentInstance();
	}

	/**
	 * Returns the localized message from the message properties file for the
	 * given key. The message is localized using the locale of the current faces
	 * context.
	 *
	 * @param key
	 *            The key of the message to be returned
	 * @return The localized message or null if there is no message property
	 *         with this key.
	 */
	public String getLocalizedMessage(final String key) {
		try {
			return getResourceBundle().getString(key);
		} catch (final MissingResourceException | ClassCastException e) {
			getLogger().error("Key " + key + " could not be found", e);
			return "???" + key + "???";
		}
	}

	protected String formatLocalizedMessage(final String key, final Object[] arguments) {
		try {
			final String pattern = getResourceBundle().getString(key);
			return MessageFormat.format(pattern, arguments);
		} catch (final MissingResourceException | ClassCastException e) {
			getLogger().error("Key " + key + " could not be found,", e);
			return "???" + key + "???";
		}
	}

	protected ResourceBundle getResourceBundle() {
		final FacesContext context = getFacesContext();
		final Application application = context.getApplication();
		final String bundleName = application.getResourceBundle(context, "msg").getBaseBundleName();
		return ResourceBundle.getBundle(bundleName, getResponseLocale());
	}

	/**
	 * Returns the {@link Locale} set in the current faces context.
	 *
	 * @return {@link Locale}
	 * @see FacesContext#getViewRoot()
	 * @see UIViewRoot#getLocale()
	 */
	protected Locale getResponseLocale() {
		return FacesContext.getCurrentInstance().getViewRoot().getLocale();
	}

	/**
	 * This method returns the language set in user preferences. If the user
	 * didn't select a preferred language the tenants default language is
	 * returned.
	 *
	 * @return the preferred locale if set, else default tenant locale.
	 * @see Locale
	 */
	protected Locale getUserLanguage() {
		final Locale userLocale = getCurrentUser().getLanguage();
		// TODO: change to tenant default language instead of response locale
		return userLocale != null ? userLocale : getResponseLocale();
	}

	/**
	 * Adds a new {@link FacesMessage} to the set of messages associated with
	 * the specified client identifier.
	 *
	 * @param clientId
	 *            The id of the client to add the message to.
	 * @param severity
	 *            The message's severity level.
	 * @param summaryKey
	 *            The key used for obtaining the localized summary message.
	 * @param detailKey
	 *            The key used to obtain the localized detailed message.
	 */
	protected void addFacesMessage(final String clientId, final Severity severity, final String summaryKey,
			final String detailKey) {

		final String summary = summaryKey != null ? getLocalizedMessage(summaryKey) : "";
		String detail = detailKey != null ? getLocalizedMessage(detailKey) : "";

		// When adding a global message we set detail to summary so that the detail is ignored by the growl component.
		if ((clientId == null) && detail.isEmpty()) {
			detail = summary;
		}

		getFacesContext().addMessage(clientId, new FacesMessage(severity, summary, detail));
	}

	/**
	 * Adds a new {@link FacesMessage} to the global set of messages.
	 *
	 * @param severity
	 *            The message's severity level.
	 * @param summaryKey
	 *            The key used for obtaining the localized summary message.
	 * @param detailKey
	 *            The key used to obtain the localized detailed message.
	 */
	public void addGlobalFacesMessage(final Severity severity, final String summaryKey, final String detailKey) {
		addFacesMessage(null, severity, summaryKey, detailKey);
	}

	/**
	 * Adds a new {@link FacesMessage} to the set of message associated with the
	 * specified client identifier. The texts of the message is created by
	 * obtaining the message format patters from the current resource bundle and
	 * formatting them with the given arguments.
	 *
	 * @param clientId
	 *            The id of the client to add the message to.
	 * @param serverity
	 *            The message's severity level.
	 * @param summaryFormatKey
	 *            The key used to obtain the localized message format for the
	 *            summary.
	 * @param detailFormatKey
	 *            The key used to obtain the localized message format for the
	 *            detail.
	 * @param arguments
	 *            The arguments to format both the summary and the details with.
	 */
	protected void addFacesMessage(final String clientId, final Severity serverity, final String summaryFormatKey,
			final String detailFormatKey, final Object... arguments) {

		final String summary = summaryFormatKey != null ? formatLocalizedMessage(summaryFormatKey, arguments) : null;
		final String detail = detailFormatKey != null ? formatLocalizedMessage(detailFormatKey, arguments) : null;

		getFacesContext().addMessage(clientId, new FacesMessage(serverity, summary, detail));
	}

	/**
	 * Adds a new global {@link FacesMessage} to the set of message. The texts
	 * of the message is created by obtaining the message format patters from
	 * the current resource bundle and formatting them with the given arguments.
	 *
	 * @param serverity
	 *            The message's severity level.
	 * @param summaryFormatKey
	 *            The key used to obtain the localized message format for the
	 *            summary.
	 * @param detailFormatKey
	 *            The key used to obtain the localized message format for the
	 *            detail.
	 * @param arguments
	 *            The arguments to format both the summary and the details with.
	 */
	protected void addGlobalFacesMessage(final Severity serverity, final String summaryFormatKey,
			final String detailFormatKey, final Object... arguments) {
		addFacesMessage(null, serverity, summaryFormatKey, detailFormatKey, arguments);
	}

	/**
	 * Returns the {@link Logger} instance associated with this bean.
	 *
	 * @return The {@link Logger} instance associated with this bean.
	 */
	protected Logger getLogger() {
		return logger;
	}

	/**
	 * Returns the {@link HttpServletRequest} instance whose response is being
	 * rendered.
	 *
	 * @see ExternalContext#getRequest()
	 * @return The {@link HttpServletRequest} instance whose response is being
	 *         rendered.
	 */
	protected HttpServletRequest getRequest() {
		return (HttpServletRequest) getFacesContext().getExternalContext().getRequest();
	}

	protected MenuModel getYouAreHereModel() {
		return userSession.getModel();
	}

	protected void createYouAreHereModelForCourse(AbstractCourse course, boolean testModus) {
		userSession.createYouAreHereMenuForCourse(course, testModus);
	}
	
	protected void createUserSpecificYouAreHereModelForCourse(AbstractCourse course, boolean testModus) {
		userSession.createUserSpecificYouAreHereMenuForCourse(course, testModus);
	}

	protected void createYouAreHereModelForCourseOffer(CourseOffer courseOffer) {
		userSession.createYouAreHereMenuForCourseOffer(courseOffer);
	}

	protected void createYouAreHereModelForCourseOfferEdit(CourseOffer courseOffer) {
		userSession.createYouAreHereMenuForCourseOfferEdit(courseOffer);
	}

	protected void createYouAreHereModelForExercise(AbstractExercise exercise) {
		userSession.createYouAreHereMenuForExercise(exercise);
	}
	
	protected void createUserSpecificYouAreHereModelForExercise(AbstractExercise exercise) {
		userSession.createUserSpecificYouAreHereMenuForExercise(exercise);
	}

	protected void createYouAreHereModelForPresentationFolder(PresentationFolder folder) {
		userSession.createYouAreHereMenuForPresentationFolder(folder);
	}

	protected void addYouAreHereModelMenuEntry(DefaultMenuItem item) {
		userSession.addYouAreHereModelMenuEntry(item);
	}

	protected void sendErrorResponse(int status, String message) throws IOException {
		FacesContext fc = getFacesContext();
		fc.getExternalContext().responseSendError(status, message);
		fc.responseComplete();
	}

	protected void redirect(final ViewId.Builder target) throws IOException {
		getFacesContext().getExternalContext().redirect(target.toActionUrl());
	}

	protected String getServerUrl() {
		final HttpServletRequest request = getRequest();
		final StringBuilder serverUrl = new StringBuilder()
			.append(request.getScheme())
			.append("://")
			.append(request.getServerName());

		if (!usesStandardPort(request)) {
			serverUrl.append(':').append(request.getServerPort());
		}

		return serverUrl.toString();
	}

	private final boolean usesStandardPort(final HttpServletRequest request) {
		switch (request.getScheme()) {
			case "http":  return request.getServerPort() ==  80;
			case "https": return request.getServerPort() == 443;
			default:      return false;
		}
	}

	protected String getTenantUrl() {
		return getServerUrl().concat(getRequest().getContextPath());
	}

	public String getExerciseResourceURL(final ExerciseResource exerciseResource) {
		return getRequest().getContextPath() + ResourceServlet.getUrlFor(exerciseResource);
	}

	@Nullable
	public PublicUserName getPublicUserName(User forUser) {
		return userBusiness.getPublicUserName(forUser, getCurrentUser(), userSession.getCurrentFolder());
	}
	
	protected MenuModel getPathAsModel(Exercise exercise, boolean isPartOfTheYouAreHereModel, boolean suppressRoot) {
		return pathComponent.getPathAsModel(exercise, isPartOfTheYouAreHereModel, suppressRoot);
	}
	
	protected MenuModel getPathAsModel(Course course, boolean isPartOfTheYouAreHereModel, boolean suppressRoot) {
		return pathComponent.getPathAsModel(course, isPartOfTheYouAreHereModel, suppressRoot);
	}
	
	protected MenuModel getPathAsModel(CourseOffer courseOffer, boolean isPartOfTheYouAreHereModel, boolean suppressRoot, boolean isToCourseMainMenu) {
		return pathComponent.getPathAsModel(courseOffer, isPartOfTheYouAreHereModel, suppressRoot, isToCourseMainMenu);
	}
	
	protected MenuModel getPathAsModel(Folder folder, boolean isPartOfTheYouAreHereModel, boolean suppressRoot, boolean isInContentTab) {
		return pathComponent.getPathAsModel(folder, isPartOfTheYouAreHereModel, suppressRoot, isInContentTab);
	}	

	protected MenuModel getUserSpecificPathAsModel(Exercise exercise, boolean isPartOfTheYouAreHereModel, boolean suppressRoot) {
		return pathComponent.getUserSpecificPathAsModel(exercise, isPartOfTheYouAreHereModel, suppressRoot, getCurrentUser());
	}
	
	protected MenuModel getUserSpecificPathAsModel(Course course, boolean isPartOfTheYouAreHereModel, boolean suppressRoot) {
		return pathComponent.getUserSpecificPathAsModel(course, isPartOfTheYouAreHereModel, suppressRoot, getCurrentUser());
	}
	
	protected MenuModel getUserSpecificPathAsModel(CourseOffer courseOffer, boolean isPartOfTheYouAreHereModel, boolean suppressRoot, boolean isToCourseMainMenu) {
		return pathComponent.getUserSpecificPathAsModel(courseOffer, isPartOfTheYouAreHereModel, suppressRoot, getCurrentUser(), isToCourseMainMenu);
	}
	
	protected MenuModel getUserSpecificPathAsModel(Folder folder, boolean isPartOfTheYouAreHereModel, boolean suppressRoot, boolean isInContentTab) {
		return pathComponent.getUserSpecificPathAsModel(folder, isPartOfTheYouAreHereModel, suppressRoot, getCurrentUser(), isInContentTab);
	}
		
	protected PathComponent getPathComponent() {
		return pathComponent;
	}

	/**
	 * Adds a DefaultMenuModel to a <b>new</b> MenuModel with similar entries like the given MenuModel and returns it.
	 *
	 * @param oldModel
	 * @param itemToAdd
	 * @return
	 */
	protected MenuModel addDefaultMenuModelToMenuModel(MenuModel oldModel, DefaultMenuItem itemToAdd) {
		return pathComponent.addDefaultMenuModelToMenuModel(oldModel, itemToAdd);
	}
	
	public String getPathAsString(Exercise entity) {
		return pathComponent.getPathOfExerciseAsString(entity);
	}
	
	public String getPathAsString(FrozenExercise entity) {
		int proxiedRevisionId = entity.getProxiedExerciseRevisionId();
		return pathComponent.getPathOfExerciseAsString(((Exercise) exerciseBusiness.getRevisionOfExerciseWithLazyData(exerciseBusiness.getNonFrozenExercise(entity), proxiedRevisionId)));
	}
	
	public String getPathAsString(Course entity) {
		return pathComponent.getPathOfCourseAsString(entity);
	}
	
	public String getPathAsString(FrozenCourse entity) {
		int proxiedRevisionId = entity.getProxiedCourseRevisionId();
		return pathComponent.getPathOfCourseAsString(((Course) courseBusiness.getRevisionOfCourseWithLazyData(courseBusiness.getNonFrozenCourse(entity), proxiedRevisionId).orElseThrow()));
	}
	
	public String getPathAsString(CourseOffer entity) {
		return pathComponent.getPathOfCourseOfferAsString(entity);
	}
	
	public String getPathAsString(Folder entity) {
		return pathComponent.getPathOfFolderAsString(entity,false);
	}

}