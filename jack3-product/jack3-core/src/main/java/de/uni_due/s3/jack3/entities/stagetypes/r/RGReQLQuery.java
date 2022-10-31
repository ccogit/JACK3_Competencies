package de.uni_due.s3.jack3.entities.stagetypes.r;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;

@Audited
@Entity
@AttributeOverride(name = "id", column = @Column(name = "id"))
@XStreamAlias("RGReQLQuery")
public class RGReQLQuery extends AbstractEntity implements DeepCopyable<RGReQLQuery> {

	private static final long serialVersionUID = 4607072696995895675L;

	@ToString
	@Column
	@Type(type = "text")
	private String query;

	public RGReQLQuery() {
	}

	public RGReQLQuery(String query) {
		this.query = query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getQuery() {
		return query;
	}

	@Override
	public RGReQLQuery deepCopy() {
		RGReQLQuery copy = new RGReQLQuery(query);
		return copy;
	}

}
