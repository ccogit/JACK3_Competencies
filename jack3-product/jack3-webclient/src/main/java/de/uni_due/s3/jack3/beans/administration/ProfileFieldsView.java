package de.uni_due.s3.jack3.beans.administration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import de.uni_due.s3.jack3.beans.AbstractView;
import de.uni_due.s3.jack3.business.ConfigurationBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.tenant.Config;
import de.uni_due.s3.jack3.entities.tenant.ProfileField;
import de.uni_due.s3.jack3.utils.JackStringUtils;

@ViewScoped
@Named
public class ProfileFieldsView extends AbstractView implements Serializable {

	private static final long serialVersionUID = 1251408690245492099L;

	private List<ProfileField> profileFields;
	private List<ProfileField> fixedProfileFields;
	private String primaryDisplayName;
	private String secondaryDisplayName;

	@Inject
	private ConfigurationBusiness configurationBusiness;

	@Inject
	private UserBusiness userBusiness;

	@PostConstruct
	private void init() {
		profileFields = userBusiness.getAllProfileFields();
		Optional<Config> displayNamesConfig = configurationBusiness.getSingleConfig("publicUserName");
		if (displayNamesConfig.isPresent()) {
			List<String> displayNames = ConfigurationBusiness
					.deSerializeToStringList(displayNamesConfig.get().getValue());
			if (displayNames.size() > 0) {
				primaryDisplayName = displayNames.get(0);
			}
			if (displayNames.size() > 1) {
				secondaryDisplayName = displayNames.get(1);
			}
		}
		fixedProfileFields = new ArrayList<>();
		fixedProfileFields.add(new MixedProfileField("loginname", false));
		fixedProfileFields.add(new MixedProfileField("email", true));
	}

	public String getPrimaryDisplayName() {
		return primaryDisplayName;
	}

	public void setPrimaryDisplayName(String displayName) {
		this.primaryDisplayName = displayName;
	}

	public String getSecondaryDisplayName() {
		return secondaryDisplayName;
	}

	public void setSecondaryDisplayName(String displayName) {
		this.secondaryDisplayName = displayName;
	}
	
	public List<ProfileField> getFixedProfileFields() {
		return fixedProfileFields;
	}

	public List<ProfileField> getProfileFields() {
		return profileFields;
	}

	public void updateField(ProfileField profileField) {
		userBusiness.updateProfileField(profileField);
		addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.save", "global.success");
	}

	public void saveDisplayName() {
		Optional<Config> config = configurationBusiness.getSingleConfig("publicUserName");

		List<String> values = new LinkedList<>();
		if (JackStringUtils.isNotBlank(primaryDisplayName)) {
			values.add(primaryDisplayName);
		}
		if (JackStringUtils.isNotBlank(secondaryDisplayName)) {
			values.add(secondaryDisplayName);
		}

		String jsonDisplayName = ConfigurationBusiness.serializeStringListToJson(values);

		if (config.isPresent()) {
			Config theConfig = config.get();
			theConfig.setValue(jsonDisplayName);
			configurationBusiness.merge(theConfig);
			addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.save", "global.success");
		} else {
			Config newConfig = new Config("publicUserName", jsonDisplayName);
			configurationBusiness.saveConfig(newConfig);
			addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.save", "global.success");
		}
	}
	
	public static class MixedProfileField extends ProfileField {

		private static final long serialVersionUID = -8639628324765498991L;
		
		public MixedProfileField(final String name, final boolean isPublic) {
			setName(name);
			setPublic(isPublic);
		}

	}
}
