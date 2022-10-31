package de.uni_due.s3.jack3.beans.administration;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.uni_due.s3.jack3.beans.AbstractView;
import de.uni_due.s3.jack3.beans.lazymodels.LazyJobDataModel;

@ViewScoped
@Named
public class JobsView extends AbstractView implements Serializable {

	private static final long serialVersionUID = -1683793720590415709L;

	private LazyJobDataModel jobs;

	@PostConstruct
	private void init() {
		jobs = new LazyJobDataModel();
	}

	public LazyJobDataModel getJobs() {
		return jobs;
	}

}
