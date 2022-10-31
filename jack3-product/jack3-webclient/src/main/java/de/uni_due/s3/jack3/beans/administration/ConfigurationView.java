package de.uni_due.s3.jack3.beans.administration;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import de.uni_due.s3.jack3.beans.AbstractView;
import de.uni_due.s3.jack3.business.ConfigurationBusiness;
import de.uni_due.s3.jack3.entities.tenant.Config;

@ViewScoped
@Named
public class ConfigurationView extends AbstractView implements Serializable {

	private static final long serialVersionUID = 1251408690245492099L;

	private String newKey;
	private String newValue;

	private List<Config> configList;

	@Inject
	private ConfigurationBusiness configurationBusiness;

	@PostConstruct
	private void init() {
		configList = configurationBusiness.getAllConfigs();
	}

	public List<Config> getConfigs() {
		return configList;
	}

	public void updateConfig(Config config) {
		if (configList.contains(config)) {
			configurationBusiness.merge(config);
			addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.save", "global.success");
		} else {
			getLogger().warn("Could not update Config: '" + config + "' since it was not stored in the view!");
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.save", "global.error");
		}
	}

	public void deleteConfig(Config config) {
		if (configList.contains(config)) {
			configurationBusiness.deleteConfigKeyValuePair(config);
			configList.remove(config);
			addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.delete", "global.success");
		} else {
			getLogger().warn("Could not delete Config: '" + config + "' since it was not stored in the view!");
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.delete", "global.error");
		}
	}

	public void addNewConfig() {
		if (configurationBusiness.addNewConfig(newKey, newValue)) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.save", "global.success");
			configList = configurationBusiness.getAllConfigs();
			newKey = null;
			newValue = null;
		} else {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.save", "global.error");
		}
	}

	public String getNewKey() {
		return newKey;
	}

	public void setNewKey(String newKey) {
		this.newKey = newKey;
	}

	public String getNewValue() {
		return newValue;
	}

	public void setNewValue(String newValue) {
		this.newValue = newValue;
	}

	public boolean isCacheOutdated() {
		return configurationBusiness.isCacheOutdated();
	}

	public void resetCache() {
		configurationBusiness.clearCache();
		addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "tenantadmin.resetCacheMessage", "global.success");
	}
}
