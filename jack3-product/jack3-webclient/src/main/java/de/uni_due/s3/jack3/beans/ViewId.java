package de.uni_due.s3.jack3.beans;

import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.application.ViewHandler;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.ServletContext;

import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;

/**
 * This bean provides centralized access to all view names and provides a builder that lets you
 * easily enable JSF features and add parameters.
 *
 * This class does not have any state and is hence thread-safe.
 */
@Named
@ApplicationScoped
public class ViewId implements Serializable {

	private static final long serialVersionUID = -8409632551372174086L;

	/**
	 * This class allows to configure view ids with additional parameters. It is not thread-safe
	 * and should be used on a fire and forget basis, i.e. instances should be used to create
	 * a certain outcome or URL and then be discarded immediately.
	 */
	public static final class Builder {

		private final String viewId;

		private final Map<String,String> parameters;

		private Builder(final String viewId) {
			this.viewId = requireViewId(viewId);
			this.parameters = new LinkedHashMap<>();
		}

		private static String requireViewId(final String viewId) {
			Objects.requireNonNull(viewId,"viewId must not be null");

			if (viewId.isEmpty()) {
				throw new IllegalArgumentException("viewId must not be empty.");
			}
			if (!viewId.startsWith("/")) {
				// This is not a JSF requirement. Using relative view IDs though would make the
				// builder behave differently dependent on the current view ID. We do not want this
				// extra complexity.
				throw new IllegalArgumentException("viewIds for this builder must start with a slash.");
			}
			return viewId;
		}

		public Builder redirecting() {
			return withParam("faces-redirect","true");
		}

		public Builder includingViewParams() {
			return withParam("includeViewParams","true");
		}

		public Builder withParam(final String name,final long value) {
			return withParam(name,Long.toString(value));
		}

		public Builder withParam(final String name, final List<Long> value) {
			// convert list to string and remove [ and ]
			String param = value.toString();
			int end = param.length() - 1;
			param = param.substring(1, end);

			return withParam(name, param);
		}

		public Builder withParam(final String name,final boolean value) {
			return withParam(name,Boolean.toString(value));
		}

		public Builder withParam(final AbstractEntity entity) {
			return withParam(entity.getClass(),entity.getId());
		}

		public Builder withParam(final Class<? extends AbstractEntity> type,final long id) {
			return withParam(toParamName(type),id);
		}

		public Builder withParam(final String name, final String value) {
			parameters.put(name,value);
			return this;
		}

		/**
		 * Returns the action URL build from the view ID, i.e. the context path, the view id, the
		 * file suffix and all parameters attached.
		 * @return The view ID's complete action URL.
		 * @see ViewHandler#getActionURL(FacesContext, String)
		 */
		public String toActionUrl() {
			final FacesContext facesContext = FacesContext.getCurrentInstance();
			return facesContext
				.getApplication()
				.getViewHandler()
				.getActionURL(facesContext,viewId) + parametersToString();
		}

		/**
		 * Returns the action URL build the view ID, i.e. the context path, the view id,
		 * the file suffix and all parameters attached. This method is intended for use
		 * when there is no faces context available. If there is you should prefer calling
		 * {@link #toActionUrl()} over this method.
		 * @param context The servlet context to be used when building the url.
		 * @return The view ID's complete action URL.
		 * @see #toActionUrl()
		 */
		public String toActionUrl(final ServletContext context) {
			return context.getContextPath() + viewId + ".xhtml" + parametersToString();
		}

		/**
		 * Return's the view's id followed by all the configured view parameters.
		 * View parameters are encoded automatically and do not need any further processing.
		 * @return The view ID converted to an outcome.
		 */
		public final String toOutcome() {
			return viewId + parametersToString();
		}

		private final String parametersToString() {
			if (parameters.isEmpty())
				return "";

			return parameters.entrySet().stream()
				.map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
				.collect(Collectors.joining("&","?",""));
		}

		@Override
		public final String toString() {
			// It's important to delegate to toOutcome() here because JSF calls this method
			// from its EL parser to determine the outcome of commands and links.
			return toOutcome();
		}
	}


	private static final String encode(final String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

	private static final String toParamName(final Class<? extends AbstractEntity> type) {
		final String className = type.getSimpleName();
		return Character.toLowerCase(className.charAt(0)) + className.substring(1);
	}

	/**
	 * Returns the given entity as a parameter usable in HTTP requests. The parameter's name
	 * and value are generated and the result is encoded.
	 *
	 * @param entity
	 *            The entity to be represented as a HTTP parameter.
	 * @return The entity converted to a HTTP parameter.
	 */
	public static final String toParameter(final AbstractEntity entity) {
		return encode(toParamName(entity.getClass())) + "=" + entity.getId();
	}

	public Builder getAvailableCourses() {
		return new Builder("/user/availableCourses");
	}

	public Builder getCourseEditor() {
		return new Builder("/editor/courseEdit");
	}

	public Builder getCourseMainMenu() {
		return new Builder("/user/courseMainMenu");
	}

	public Builder getCourseOfferEditor() {
		return new Builder("/editor/courseOfferEdit");
	}

	public Builder getCourseOfferParticipants() {
		return new Builder("/editor/courseOfferParticipants");
	}

	public Builder getCourseRecordSubmissions() {
		return new Builder("/user/courseRecordSubmissions");
	}

	public Builder getCourseRecordView() {
		return new Builder("/user/showCourseRecord");
	}

	public Builder getCourseStatistics() {
		return new Builder("/editor/courseStatistics");
	}

	public Builder getCourseTest() {
		return new Builder("/editor/courseTest");
	}

	public Builder getCurrent() {
		return new Builder(FacesContext.getCurrentInstance().getViewRoot().getViewId());
	}

	public Builder getDeletionSuccess() {
		return new Builder("/user/deletionSuccess");
	}

	public Builder getEditorFor(final AbstractEntity entity) {
		if (entity instanceof AbstractExercise) {
			return getExerciseEditor().withParam(entity);
		}
		if (entity instanceof AbstractCourse) {
			return getCourseEditor().withParam(entity);
		}
		if (entity instanceof CourseOffer) {
			return getCourseOfferEditor().withParam(entity);
		}
		throw new IllegalArgumentException("No editor available for entity: " + entity);
	}

	public Builder getEvaluatorStatus() {
		return new Builder("/public/health");
	}

	public Builder getExerciseEditor() {
		return new Builder("/editor/exerciseEdit");
	}

	public Builder getExerciseSubmissions() {
		return new Builder("/editor/exerciseSubmissions");
	}

	public Builder getExerciseTest() {
		return new Builder("/editor/exerciseTest");
	}

	public Builder getHome() {
		if (FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal() != null) {
			return getAvailableCourses();
		}
		return getLogin();
	}

	public Builder getLicenseInformation() {
		return new Builder("/public/licenseInformation");
	}

	public Builder getLogin() {
		return new Builder("/public/login");
	}

	public Builder getMyAccount() {
		return new Builder("/user/myAccount");
	}

	public Builder getMyParticipations() {
		return new Builder("/user/myParticipations");
	}

	public Builder getMyWorkspace() {
		return new Builder("/editor/myWorkspace");
	}

	public Builder getPasswordRetrieval() {
		return new Builder("/public/passwordRetrieval");
	}

	public Builder getPerformance() {
		return new Builder("/administrator/performance");
	}

	public Builder getSubjects() {
		return new Builder("/administrator/subjects");
	}

	public Builder getPrivacyPolicy() {
		return new Builder("/public/privacy");
	}

	public Builder getQuickId() {
		return new Builder("/user/quickId");
	}

	public Builder getRegistration() {
		return new Builder("/public/register");
	}

	public Builder getSetup() {
		return new Builder("/public/setup");
	}

	public Builder getSubmissionDetails() {
		return new Builder("/user/submissionDetails");
	}

	public Builder getTenantConfiguration() {
		return new Builder("/administrator/configuration");
	}

	public Builder getTenantJobs() {
		return new Builder("/administrator/jobs");
	}

	public Builder getTenantProfileFields() {
		return new Builder("/administrator/profileFields");
	}

	public Builder getTenantUserManagement() {
		return new Builder("/administrator/userManagement");
	}

	public Builder getUserDetails() {
		return new Builder("/administrator/userDetails");
	}

	public Builder getUserGroupDetails() {
		return new Builder("/administrator/userGroupDetails");
	}
}
