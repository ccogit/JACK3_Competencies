package de.uni_due.s3.jack3.entities.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQuery;
import javax.validation.constraints.NotBlank;

import org.hibernate.annotations.Type;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;

@NamedQuery(
	name = LtiLaunch.GET_BY_LOGINNAME_AND_CONSUMER,
	query = "SELECT l FROM LtiLaunch l " //
	+ "WHERE l.userName = :userName " //
	+ "AND l.consumerInstanceGuid = :consumerInstanceGuid " //
	+ "AND l.resourceLinkId = :resourceLinkId")

@Entity
public class LtiLaunch extends AbstractEntity {

	private static final long serialVersionUID = 1920373200220734379L;

	public static final String GET_BY_LOGINNAME_AND_CONSUMER = "LtiLaunch.byLoginnameAndConsumer";

	public static final char ID_SEPARATOR = '#';

	@Column(nullable = false)
	@Type(type = "text")
	@NotBlank
	private String consumerInstanceGuid;

	@ToString
	@Column(nullable = false)
	@Type(type = "text")
	@NotBlank
	private String userName;

	@ToString
	@Column(nullable = false)
	@Type(type = "text")
	@NotBlank
	private String token;

	@Column
	@Type(type = "text")
	@NotBlank
	private String resourceLinkId;

	@Column
	@Type(type = "text")
	private String contextLabel;

	@Column
	@Type(type = "text")
	private String returnUrl;

	public LtiLaunch() {}

	public LtiLaunch(String consumerInstanceGuid, final String resourceLinkId, final String userName, final String token, String contextLabel, String returnUrl) {
		this.userName = userName;
		this.consumerInstanceGuid = consumerInstanceGuid;
		this.resourceLinkId = resourceLinkId;
		this.token = token;
		this.contextLabel = contextLabel;
		this.returnUrl = returnUrl;
	}

	public String getConsumerInstanceGuid() {
		return consumerInstanceGuid;
	}

	public String getToken() {
		return token;
	}

	public String getResourceLinkId() {
		return resourceLinkId;
	}

	public String getUserName() {
		return userName;
	}

	public String getLoginName() {
		return userName + ID_SEPARATOR + getId();
	}
}
