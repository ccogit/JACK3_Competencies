package de.uni_due.s3.jack3.entities.stagetypes.r;

import static de.uni_due.s3.jack3.utils.DeepCopyHelper.deepCopyOrNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.tenant.CheckerConfiguration;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;

/**
 * This class represents a collection of Abstract(R-)TestCases that are considered a Unit. Those can be any
 * AbstractTestCase, hence this allows grouping static and dynamic Testcases together as a semantic unit.
 * 
 * @author Benjamin Otto
 *
 */
@Audited
@Entity
@XStreamAlias("TestCaseTuple")
public class TestCaseTuple extends AbstractEntity implements DeepCopyable<TestCaseTuple> {

	private static final long serialVersionUID = -6339536931637788701L;

	@ToString
	@Column
	@Type(type = "text")
	String name;

	@ToString
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private List<AbstractTestCase> testCases = new ArrayList<>();

	@ToString
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	CheckerConfiguration checkerConfiguration;

	public TestCaseTuple() {

	}

	public TestCaseTuple(AbstractTestCase abstractTestCase) {
		testCases.add(abstractTestCase);
	}

	public List<AbstractTestCase> getTestCases() {
		return testCases;
	}

	public void addTestCase(AbstractTestCase abstractTestCase) {
		testCases.add(abstractTestCase);
	}

	public boolean removeTestCase(AbstractTestCase abstractTestCase) {
		return 	testCases.remove(abstractTestCase);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public CheckerConfiguration getCheckerConfiguration() {
		return checkerConfiguration;
	}

	public void setCheckerConfiguration(CheckerConfiguration checkerConfiguration) {
		this.checkerConfiguration = checkerConfiguration;
	}

	@Override
	public TestCaseTuple deepCopy() {
		TestCaseTuple copy = new TestCaseTuple();
		copy.name = name;
		
		copy.testCases = testCases //
				.stream() //
				.map(AbstractTestCase::deepCopy) //
				.collect(Collectors.toList());
		
		copy.checkerConfiguration = deepCopyOrNull(checkerConfiguration);

		return copy;
	}
}
