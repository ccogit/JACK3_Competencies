package de.uni_due.s3.jack3.entities.tenant;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;

@Audited
@Entity
public class ConsoleResult extends AbstractEntity {

	private static final long serialVersionUID = 1L;

	@ToString
	@Column
	@Type(type = "text")
	private String response;

	@ToString
	@Column
	@Type(type = "text")
	private String input;

	@ToString
	@Column
	private String handlerStageType;

	// The backend that has processed the job. This can of course only be set after the Job has finished.
	@ToString
	@Column
	private String graderId;

	@ToString
	@Column
	private boolean responseReceived = false;

	@ToString
	@Column
	private String initiatingUser;

	@ToString
	@Column
	private LocalDateTime startedAt;

	@ToString
	@Column
	private LocalDateTime finishedAt;

	public LocalDateTime getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(LocalDateTime startedAt) {
		this.startedAt = startedAt;
	}

	public LocalDateTime getFinishedAt() {
		return finishedAt;
	}

	public void setFinishedAt(LocalDateTime finishedAt) {
		this.finishedAt = finishedAt;
	}

	public boolean isResponseReceived() {
		return responseReceived;
	}

	public void setResponseReceived(boolean responseReceived) {
		this.responseReceived = responseReceived;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public String getGraderId() {
		return graderId;
	}

	public void setGraderId(String graderId) {
		this.graderId = graderId;
	}

	public String getInitiatingUser() {
		return initiatingUser;
	}

	public void setInitiatingUser(String initiatingUser) {
		this.initiatingUser = initiatingUser;
	}

	public String getHandlerStageType() {
		return handlerStageType;
	}

	public void setHandlerStageType(String handlerStageType) {
		this.handlerStageType = handlerStageType;
	}

}