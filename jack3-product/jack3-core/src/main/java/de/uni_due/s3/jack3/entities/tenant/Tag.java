package de.uni_due.s3.jack3.entities.tenant;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.interfaces.Namable;

@Audited
@NamedQuery(
		name = Tag.ALL_TAGS_AS_STRINGS, //
		query = "SELECT t.name FROM Tag t " //
		+ "ORDER BY t.name ASC")
@NamedQuery(
		name = Tag.TAG_BY_NAME, //
		query = "SELECT t FROM Tag t " //
		+ "WHERE t.name = :name")
@Entity
public class Tag extends AbstractEntity implements Namable {

	private static final long serialVersionUID = 6522864489891251481L;

	public static final String ALL_TAGS_AS_STRINGS = "Tag.allTagsAsStrings";

	public static final String TAG_BY_NAME = "Tag.tagByName";

	@Column(unique = true, nullable = false)
	@Type(type = "text")
	@ToString
	private String name;

	public Tag() {
		super();
	}

	public Tag(String name) {
		setName(name);
	}

	@Override
	@Nonnull
	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = requireIdentifier(name, "A tag's name must be a non-empty string.").strip();
	}
}
