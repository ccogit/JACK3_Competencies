package de.uni_due.s3.jack3.beans.administration;

import de.uni_due.s3.jack3.beans.AbstractView;
import de.uni_due.s3.jack3.business.SubjectBusiness;
import de.uni_due.s3.jack3.business.exceptions.SubjectException;
import de.uni_due.s3.jack3.entities.tenant.Subject;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

@ViewScoped @Named public class SubjectsView extends AbstractView implements Serializable {

	private static final long serialVersionUID = 8624156385538278239L;

	@Inject SubjectBusiness subjectBusiness;

	private List<Subject> subjects;

	private String newSubjectName;

	@PostConstruct private void init() {
		subjects = subjectBusiness.getAllSubjects();
	}

	public void addSubject() throws SubjectException {
		if (subjectBusiness.addNewSubject(new Subject(newSubjectName))) {
			addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.save", "global.success");
			subjects = subjectBusiness.getAllSubjects();
			newSubjectName = null;
		} else {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.save", "global.error");
		}
	}

	public void removeSubject(Subject subject) {
		if (subjects.contains(subject)) {
			if(subjectBusiness.removeSubject(subject)){
				addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.delete", "global.success");
				subjects = subjectBusiness.getAllSubjects();
			} else {
				getLogger().warn("Could not delete Subject: '" + subject
						+ "' because there exist references from Courses and/or Exercises!");
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.delete", "global.error");
			}
		} else {
			getLogger().warn("Could not delete Subject: '" + subject + "' since it was not stored in the view!");
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.delete", "global.error");
		}
	}

	public void updateSubject(Subject subject) throws SubjectException {
		if (subjects.contains(subject)) {
			if(subjectBusiness.updateSubject(subject)){
				addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.save", "global.success");
			} else {
				getLogger().warn("Could not update Subject: '" + subject
						+ "' because there exist references from Courses and/or Exercises!");
				addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.save", "global.error");
			}
		} else {
			getLogger().warn("Could not update Subject: '" + subject + "' since it was not stored in the view!");
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.save", "global.error");
		}
	}

	public List<Subject> getSubjects() {
		return subjects;
	}

	public void setSubjects(List<Subject> subjects) {
		this.subjects = subjects;
	}

	public String getNewSubjectName() {
		return newSubjectName;
	}

	public void setNewSubjectName(String newSubjectName) {
		this.newSubjectName = newSubjectName;
	}

}
