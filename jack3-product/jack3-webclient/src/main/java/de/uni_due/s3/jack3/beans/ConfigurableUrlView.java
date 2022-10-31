package de.uni_due.s3.jack3.beans;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import de.uni_due.s3.jack3.business.ConfigurationBusiness;

@Named
@RequestScoped
abstract class ConfigurableUrlView extends AbstractView implements Serializable {

	private static final long serialVersionUID = -858085859004392716L;

	private final String configKey;

	@Inject
	private ConfigurationBusiness configurationBusiness;

	private Optional<String> url;

	protected ConfigurableUrlView(final String configKey) {
		this.configKey = configKey;
	}

	@PostConstruct
	private void init() {
		this.url = configurationBusiness.getSingleValue(configKey);
	}

	/**
	 * Returns {@code true} in case the requested URL is configured, {@code false} otherwise.
	 * @return {@code true} in case the requested URL configured, {@code false} otherwise.
	 */
	public boolean isConfigured() {
		return url.isPresent();
	}

	/**
	 * Returns the URL this bean manages.
	 * @return the URL this bean manages.
	 * throws {@link NoSuchElementException} if the URL is not configured on this server.
	 */
	public String getUrl() {
		return url.get();
	}

	/**
	 * Returns the configuration key used for storing this bean's URL.
	 * @return The configuration key used for storing this bean's URL.
	 */
	public String getConfigurationKey() {
		return configKey;
	}

	/**
	 * Returns a localized hint on how to fix a missing configuration
	 *
	 * @return A localized hint on how to fix a missing configuration
	 */
	abstract public String getConfigurationHint();
}
