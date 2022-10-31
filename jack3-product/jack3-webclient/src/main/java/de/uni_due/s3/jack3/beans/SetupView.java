package de.uni_due.s3.jack3.beans;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;

import de.uni_due.s3.jack3.business.ConfigurationBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.business.microservices.EurekaBusiness;
import de.uni_due.s3.jack3.utils.JackStringUtils;

// Kafka is configured in /jack3-core/src/main/resources/META-INF/microprofile-config.properties and command-line params
@ViewScoped
@Named
public class SetupView extends AbstractView implements Serializable {

	private static final String DEFAULT_MATHJAX_URL = "https://cdn.jsdelivr.net/npm/mathjax@3.1.0/es5/tex-chtml-full.js";

	private static final long serialVersionUID = -2600863607663345342L;

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private ConfigurationBusiness configurationBusiness;

	private String username;
	private String password;
	private String email;

	private boolean useEureka = false;
	private String eurekaServer = "http://10.168.68.74:8761/eureka";

	private String mathJaxURL = DEFAULT_MATHJAX_URL;

	private List<String> mathJaxURLs = new ArrayList<>(
			Arrays.asList(DEFAULT_MATHJAX_URL, "/resources/mathjax/tex-chtml-full.js"));

	public void doFirstTimeSetup() throws IOException {
		// Check if any user exists: If not redirect to setup page.
		if (userBusiness.hasNoUser()) {
			redirect(viewId.getSetup());
		}
	}

	public void accessAllowed() throws IOException {
		if (!userBusiness.hasNoUser()) {
			sendErrorResponse(HttpServletResponse.SC_FORBIDDEN,"Nope, Chuck Testa!");
		}
	}

	public void performSetup() throws IOException, IllegalStateException {
		writeConfig();
		createUser();
	}

	private void createUser() throws IOException, IllegalStateException {
		if (!userBusiness.hasNoUser()) {
			throw new IllegalStateException("Function called in an initalized setup of this instance!");
		}
		String usermail = JackStringUtils.stripOrNull(email);
		userBusiness.createUser(username, password, usermail, true, true);
		redirect(viewId.getLogin());
	}

	private void writeConfig() {
		if (useEureka) {
			List<String> eurekaServers = new LinkedList<>();
			eurekaServers.add(eurekaServer);
			String eurekaConfigValue = ConfigurationBusiness.serializeStringListToJson(eurekaServers);
			configurationBusiness.addNewConfig(EurekaBusiness.EUREKA_SERVER_URLS_KEY, eurekaConfigValue);
		}

		configurationBusiness.addNewConfigSingleValue(MathJaxView.MATH_JAX_URL_KEY, getMathJaxURL());

		configurationBusiness.clearCache();
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(final String email) {
		this.email = email;
	}

	public String getEurekaServer() {
		return eurekaServer;
	}

	public void setEurekaServer(String eurekaServer) {
		this.eurekaServer = eurekaServer;
	}

	public boolean isUseEureka() {
		return useEureka;
	}

	public void setUseEureka(boolean useEureka) {
		this.useEureka = useEureka;
	}

	public String getMathJaxURL() {
		return mathJaxURL;
	}

	public void setMathJaxURL(String mathJaxURL) {
		this.mathJaxURL = mathJaxURL;
	}

	public List<String> getMathJaxURLs() {
		return mathJaxURLs;
	}

	public void setMathJaxURLs(List<String> mathJaxURLs) {
		this.mathJaxURLs = mathJaxURLs;
	}
}
