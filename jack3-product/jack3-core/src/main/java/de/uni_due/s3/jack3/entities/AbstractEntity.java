package de.uni_due.s3.jack3.entities;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;

import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.GeneratorType;
import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.services.utils.LoggedInUserNameGenerator;
import de.uni_due.s3.jack3.utils.EntityReflectionHelper;

/**
 * Abstract superclass for all other entities. Provides id and universal methods.
 */
@MappedSuperclass
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class AbstractEntity implements Serializable {

	protected static final long serialVersionUID = -5728859225807258367L;

	/*
	 * Hibernate uses this field as a primary key. All IDs are unique across the entire database. If the entity is new
	 * (not already stored in the database), this field may be null.
	 */
	@XStreamOmitField
	@Id
	@Column(name = "id")
	private Long hibernateId;

	/*
	 * This field is the internal id of an object and generated the first time it is requested.
	 * It intentionally has the same column name as hibernateId to get it loaded from the same column.
	 * It is neither insertable nor updatable because writing this column happens via hibernateId.
	 * This field must not be transient because this prevents hibernate from copying it when merging.
	 */
	@XStreamOmitField
	@Column(name = "id", insertable = false, updatable = false)
	private long jackId;

	/**
	 * When the entity was last updated. This is either the time when the entity was persisted or the time of the last
	 * merge.
	 */
	// NOTE: This field is NOT updated automatically, see JACK/jack3-core#984 for discussion
	@XStreamOmitField
	@Audited
	@Column
	private LocalDateTime updateTimeStamp;

	/**
	 * On INSERT or UPDATE hibernate writes the username of the current user here.
	 * 
	 * @see de.uni_due.s3.jack3.services.utils.LoggedInUserNameGenerator
	 */
	@XStreamOmitField
	@Audited
	@Column(name = "updated_by")
	@GeneratorType(type = LoggedInUserNameGenerator.class, when = GenerationTime.ALWAYS)
	private String updatedBy;

	protected AbstractEntity() {
		hibernateId = null;
		jackId = 0;
	}

	/**
	 * @return ID of this entity. The ID is always present, unique across the entire database and may not be 0. Once
	 *         created the ID does not change.
	 */
	public long getId() {
		if (jackId == 0) {
			if (hibernateId != null) {
				// The entity was loaded from the database and its ID has not been transferred yet.
				// Although it gets loaded automatically this state can still be reached when
				// calculating the entity's hash for hibernate's first level cache.
				jackId = hibernateId;
			} else {
				// The entity is transient and has not been assigned an ID yet.
				jackId = JackIdGenerator.next();
			}
		}
		return jackId;
	}

	@PrePersist
	private final void beforePersisting() {
		hibernateId = getId();
	}

	@Override
	public final int hashCode() {
		return Long.hashCode(getId());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @return {@code TRUE} if both entities have the same ID. "Deep" equality is not checked.
	 * @see #getId()
	 */
	@Override
	public final boolean equals(final Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AbstractEntity)) {
			return false;
		}
		return (getId() == ((AbstractEntity) other).getId());
	}

	public void markTransient() {
		jackId = 0;
		hibernateId = null;
	}

	/**
	 * @return {@code true} if the entity is transient. This is the case when it has neither been saved to nor read from
	 *         the database.
	 */
	public boolean isTransient() {
		return hibernateId == null;
	}

	/**
	 * <strong>DO NOT USE (unless you know what you're doing)</strong>. This is here to allow a fresh entity to trick
	 * hibernate to take it as a changed version of an old entity as its the case when using a old revison of a course
	 * "as new".
	 * 
	 * @param other
	 *            the entity the ids are copied from
	 */
	public void copyHibernateAndJackIdsOf(AbstractEntity other) {
		jackId = other.jackId;
		hibernateId = other.hibernateId;
	}

	/**
	 * Returns a string representation of the object with all attributes marked with the {@link ToString} annotation.
	 * 
	 * @see ToString
	 */
	@Override
	public String toString() {
		return EntityReflectionHelper.generateToString(this);
	}

	public LocalDateTime getUpdateTimeStamp() {
		return updateTimeStamp;
	}

	/**
	 * Sets {@link #updateTimeStamp} to the current date-time.
	 */
	public void setUpdateTimeStampToNow() {
		updateTimeStamp = LocalDateTime.now();
	}

	/**
	 * Throws a {@link NullPointerException} if the String {@code s} is {@code null}
	 * or an {@link IllegalArgumentException} if {@code s} is empty. This method is
	 * intended for use in parameter validation.
	 *
	 * @param string
	 *            The string to be validated.
	 * @param message
	 *            The message that is to be used as the exception's message in case
	 *            {@code s} is {@code null} or empty.
	 * @return The string {@code s}.
	 * @throws NullPointerException
	 *             if {@code s} is {@code null}.
	 * @throws IllegalArgumentException
	 *             if {@code s} is empty.
	 */
	protected final String requireNonEmptyString(final String string, final String message) {
		Objects.requireNonNull(string, message);

		if (string.isEmpty()) {
			throw new IllegalArgumentException(message);
		}

		return string;
	}

	/**
	 * Throws a {@link NullPointerException} if the String {@code s} is {@code null}
	 * or an {@link IllegalArgumentException} if {@code s} is empty. This method is
	 * intended for use in parameter validation.
	 *
	 * @param string
	 *            The string to be validated.
	 * @param message
	 *            The message that is to be used as the exception's message in case
	 *            {@code s} is {@code null} or empty.
	 * @return The string {@code s}.
	 * @throws NullPointerException
	 *             if {@code s} is {@code null}.
	 * @throws IllegalArgumentException
	 *             if {@code s} is empty or consists solely of whitespaces.
	 */
	protected final String requireIdentifier(final String string, final String message) {
		Objects.requireNonNull(string, message);

		if (string.isBlank()) {
			throw new IllegalArgumentException(message);
		}

		return string;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}
}