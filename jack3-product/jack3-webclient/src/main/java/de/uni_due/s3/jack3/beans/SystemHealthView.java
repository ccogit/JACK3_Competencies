package de.uni_due.s3.jack3.beans;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import de.uni_due.s3.jack3.business.SystemHealthBusiness;
import de.uni_due.s3.jack3.business.SystemHealthBusiness.TestResult;

@RequestScoped
@Named
public class SystemHealthView extends AbstractView implements Serializable {

	private static final long serialVersionUID = 2851347096211729565L;
	
	@Inject
	private SystemHealthBusiness healthBusiness;
	
	private List<TestResult> results;
	
	private boolean allTestsPassed;
	
	@PostConstruct
	void performTests() {
		this.results = healthBusiness.getResults();
		this.allTestsPassed = true;
		for (final TestResult result : results) {
			allTestsPassed &= result.isPassed();
		}
	}

	public List<TestResult> getTestResults() {
		return healthBusiness.getResults();
	}
	
	public boolean isAllTestsPassed() {
		return allTestsPassed;
	}
}
