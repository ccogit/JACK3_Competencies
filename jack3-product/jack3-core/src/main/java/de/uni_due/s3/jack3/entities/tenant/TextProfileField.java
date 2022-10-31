package de.uni_due.s3.jack3.entities.tenant;

import java.util.Map;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.MapKeyColumn;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

/**
 * This class implements a user profile field that can be freely edited by the user.
 */
@Audited
@Entity
public class TextProfileField extends ProfileField {

	private static final long serialVersionUID = -4328354587753928441L;

	@Column
	@Type(type = "text")
	String defaultContent;

	@Column
	@Type(type = "text")
	String validationRegex;

	/**
	 * This map contains locale specific translations of the error message to be shown when
	 * validation against {@link #validationRegex} fails.
	 */
	@ElementCollection
	@Type(type = "text")
	@MapKeyColumn(length = 10485760)
	Map<String, String> localValidationMessage;

	public TextProfileField() {
	}

	public String getDefaultContent() {
		return defaultContent;
	}

	public void setDefaultContent(String defaultContent) {
		this.defaultContent = defaultContent;
	}

	public String getValidationRegex() {
		return validationRegex;
	}

	public void setValidationRegex(String validationRegex) {
		this.validationRegex = validationRegex;
	}
}
