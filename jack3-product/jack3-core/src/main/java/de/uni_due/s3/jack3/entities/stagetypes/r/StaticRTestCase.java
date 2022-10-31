package de.uni_due.s3.jack3.entities.stagetypes.r;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.uni_due.s3.jack3.annotations.DeepCopyOmitField;

@Audited
@Entity
@XStreamAlias("StaticRTestCase")
public class StaticRTestCase extends AbstractTestCase {

	private static final long serialVersionUID = 8306049235022304753L;

	@Deprecated
	@Column
	@Type(type = "text")
	@DeepCopyOmitField(reason = "Deprecated")
	private String query;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private List<RGReQLQuery> queries = new LinkedList<>();

	@PostConstruct
	public void migrateSingleQueries() {
		if (queries == null || queries.isEmpty()) {
			queries = new LinkedList<RGReQLQuery>();
			queries.add(new RGReQLQuery(query));
		}
	}

	@Override
	public StaticRTestCase deepCopy() {
		StaticRTestCase copy = new StaticRTestCase();
		copy.deepCopyAbstractTestCaseVars(this);

		// Make sure copy works on migrated data
		migrateSingleQueries();

		copy.queries = new LinkedList<>();
		for (RGReQLQuery query : queries) {
			RGReQLQuery queryCopy = query.deepCopy();
			copy.queries.add(queryCopy);
		}
		copy.ruleMode = ruleMode;

		return copy;
	}

	public StaticRTestCase copyFrom(StaticRTestCase other) {
		queries = new LinkedList<>();
		queries.addAll(other.queries);
		deepCopyAbstractTestCaseVars(other);
		return this;
	}

	@Deprecated
	public String getQuery() {
		return query;
	}

	@Deprecated
	public void setQuery(String query) {
		this.query = query;
	}

	public List<String> getQueries() {
		List<String> returnList = new LinkedList<>();
		for (RGReQLQuery query : queries) {
			returnList.add(query.getQuery());
		}
		return returnList;
	}

	public List<RGReQLQuery> getEditableQueries() {
		return queries;
	}

	public void addQuery(String query) {
		queries.add(new RGReQLQuery(query));
	}

	public void removeQuery(RGReQLQuery query) {
		queries.remove(query);
	}

	@Override
	public boolean isDynamic() {
		return false;
	}

	@Override
	public boolean isStatic() {
		return true;
	}
}
