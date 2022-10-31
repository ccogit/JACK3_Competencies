package de.uni_due.s3.jack3.beans;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.inject.Named;

/**
 * This bean provides the ACE URL to JSF files.
 */
@Named
@RequestScoped
public class AceView extends ConfigurableUrlView {

	private static final long serialVersionUID = -6765503912671951253L;

	public static final String ACE_URL_KEY = "AceURL";

	private boolean rendered;

	public AceView() {
		super(ACE_URL_KEY);
		rendered = false;
	}

	@Override
	public String getConfigurationHint() {
		return formatLocalizedMessage("missingConfiguration.ace.detail", new Object[] { ACE_URL_KEY });
	}

	public boolean isRendered() {
		return rendered;
	}

	public void setRendered(boolean rendered) {
		// This method is also called in the RESTORE_VIEW phase. We must only accept calls
		// in the RENDER_RESPONSE phase because in this face the script is actually rendered.
		if (FacesContext.getCurrentInstance().getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
			this.rendered = rendered;
		}
	}
}
