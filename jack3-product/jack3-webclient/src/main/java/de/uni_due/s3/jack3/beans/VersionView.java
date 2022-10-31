package de.uni_due.s3.jack3.beans;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import de.uni_due.s3.jack3.multitenancy.LoggerProvider;

/**
 * This bean provides the application's version.
 */
@Named
@ApplicationScoped
public class VersionView implements Serializable {

	private static final long serialVersionUID = -7857840926140943251L;

	private static final String UNKNOWN_VERSION = "UNKNOWN VERSION";

	private String version;

	@PostConstruct
	private void initialize() {
		this.version = loadVersion();
	}

	private String loadVersion() {
		// This works for the packed version inside a jar.
		version = VersionView.class.getPackage().getImplementationVersion();
		if (version != null) {
			return version;
		}

		// When the war is exploded during development we have to read it from the manifest manually.
	    final ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
	    try (InputStream in = externalContext.getResourceAsStream("/META-INF/MANIFEST.MF")) {
	    	if (in != null) {
	    		Properties prop = new Properties();
	    		prop.load(in);
	    		return prop.getProperty("Implementation-Version",UNKNOWN_VERSION);
	    	}
	    }
	    catch (IOException e) {
	    	LoggerProvider.get(getClass()).error("Failed to read version info from manifest.",e);
	    }

	    // When we're here loading the version failed. We should investigate the reason why.
    	LoggerProvider.get(getClass()).error("Failed to load version info!");
    	return UNKNOWN_VERSION;
	}

	public String getVersion() {
		return version;
	}
}
