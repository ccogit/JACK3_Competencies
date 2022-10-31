package de.uni_due.s3.jack3.beans.administration;

import de.uni_due.s3.jack3.beans.AbstractView;
import de.uni_due.s3.jack3.beans.lazymodels.LazySubjectDataModel;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import java.io.Serializable;

@ViewScoped
@Named
public class SubjectsView extends AbstractView implements Serializable {

	private static final long serialVersionUID = 8624156385538278239L;

	private LazySubjectDataModel subjects;

	@PostConstruct
	private void init() {
		subjects = new LazySubjectDataModel();
	}

	public LazySubjectDataModel getSubjects() {
		return subjects;
	}

}
