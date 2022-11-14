package de.uni_due.s3.jack3.beans.administration;

import de.uni_due.s3.jack3.beans.AbstractView;
import de.uni_due.s3.jack3.business.CompetenceBusiness;
import de.uni_due.s3.jack3.business.SubjectBusiness;
import de.uni_due.s3.jack3.business.exceptions.CompetenceException;
import de.uni_due.s3.jack3.business.exceptions.SubjectException;
import de.uni_due.s3.jack3.entities.enums.ECompetenceDimension;
import de.uni_due.s3.jack3.entities.tenant.Competence;
import de.uni_due.s3.jack3.entities.tenant.Subject;
import de.uni_due.s3.jack3.utils.JackStringUtils;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ViewScoped
@Named
public class CompetenciesView extends AbstractView implements Serializable {

    private static final long serialVersionUID = 8292917095452268891L;
    @Inject
    CompetenceBusiness competenceBusiness;
    @Inject
    SubjectBusiness subjectBusiness;
    private List<Competence> competencies = new ArrayList<>();
    private List<Competence> subcompetencies = new ArrayList<>();
    private Subject selectedSubject;
    private ECompetenceDimension selectedCompetenceDimension;
    private Competence selectedCompetence;
    private String newCompetenceName;
    private String newSubcompetenceName;

    @PostConstruct
    private void init() {

    }


    // Lists
    public void onChange() {
        selectedCompetence=null;
        if (selectedSubject != null && selectedCompetenceDimension != null) {
            updateListOfCompetencies();
            updateListOfSubcompetencies();
        }
    }

    public void updateListOfCompetencies() {
        competencies = competenceBusiness.getAllCompetenciesByDimensionAndSubject(selectedCompetenceDimension, selectedSubject);
        selectedCompetence = null;
    }

    public void updateListOfSubcompetencies() {
        subcompetencies = competenceBusiness.getSubcompetenciesByCompetence(selectedCompetence);
    }

    // CRUD
    public void addCompetence() throws CompetenceException {
        if (competenceBusiness.addNewCompetence(new Competence(newCompetenceName, selectedSubject, selectedCompetenceDimension))) {
            addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.save", "global.success");
            updateListOfCompetencies();
            newCompetenceName = null;
        } else {
            addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.save", "global.error");
        }
    }

    public void removeCompetence(Competence competence) {
        if (competencies.contains(competence)) {
            if (competenceBusiness.removeCompetence(competence)) {
                addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.delete", "global.success");
                updateListOfCompetencies();
            } else {
                getLogger().warn("Could not delete Competence: '" + competence
                        + "' because there exist references from Courses and/or Exercises!");
                addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.delete", "global.error");
            }
        } else {
            getLogger().warn("Could not delete Competence: '" + competence + "' since it was not stored in the view!");
            addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.delete", "global.error");
        }
    }

    public void updateCompetence(Competence competence) throws CompetenceException {
        if (competencies.contains(competence)) {
            if (competenceBusiness.updateCompetence(competence)) {
                addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.save", "global.success");
                updateListOfCompetencies();
            } else {
                getLogger().warn("Could not update Competence: '" + competence
                        + "' because there exist references from Courses and/or Exercises!");
                addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.save", "global.error");
            }
        } else {
            getLogger().warn("Could not update Competence: '" + competence + "' since it was not stored in the view!");
            addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.save", "global.error");
        }
    }

    public void addSubcompetence() throws CompetenceException {
        if (competenceBusiness.addNewSubcompetence(new Competence(
                newSubcompetenceName,
                selectedSubject,
                selectedCompetenceDimension,
                selectedCompetence))) {
            addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.save", "global.success");
            updateListOfSubcompetencies();
            newSubcompetenceName = null;
        } else {
            addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.save", "global.error");
        }
    }

    public void removeSubcompetence(Competence subcompetence) {
        if (subcompetencies.contains(subcompetence)) {
            if (competenceBusiness.removeSubcompetence(subcompetence)) {
                addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.delete", "global.success");
                updateListOfSubcompetencies();
            } else {
                getLogger().warn("Could not delete Subcompetence: '" + subcompetence
                        + "' because there exist references from Courses and/or Exercises!");
                addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.delete", "global.error");
            }
        } else {
            getLogger().warn("Could not delete Subcompetence: '" + subcompetence + "' since it was not stored in the view!");
            addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.delete", "global.error");
        }
    }

    public void updateSubcompetence(Competence subcompetence) throws CompetenceException {
        if (subcompetencies.contains(subcompetence)) {
            if (competenceBusiness.updateSubcompetence(subcompetence)) {
                addGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "global.save", "global.success");
            } else {
                getLogger().warn("Could not update Subcompetence: '" + subcompetence
                        + "' because there exist references from Courses and/or Exercises!");
                addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.save", "global.error");
            }
        } else {
            getLogger().warn("Could not update Subcompetence: '" + subcompetence + "' since it was not stored in the view!");
            addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "global.save", "global.error");
        }
    }


    // Getter & Setter
    public List<Competence> getCompetencies() {
        return competencies;
    }

    public void setCompetencies(List<Competence> competencies) {
        this.competencies = competencies;
    }

    public List<Competence> getSubcompetencies() {
        return subcompetencies;
    }

    public void setSubcompetencies(List<Competence> subcompetencies) {
        this.subcompetencies = subcompetencies;
    }

    public String getNewCompetenceName() {
        return newCompetenceName;
    }

    public void setNewCompetenceName(String newCompetenceName) {
        this.newCompetenceName = newCompetenceName;
    }

    public String getNewSubcompetenceName() {
        return newSubcompetenceName;
    }

    public void setNewSubcompetenceName(String newSubcompetenceName) {
        this.newSubcompetenceName = newSubcompetenceName;
    }

    public SubjectBusiness getSubjectBusiness() {
        return subjectBusiness;
    }

    public CompetenceBusiness getCompetenceBusiness() {
        return competenceBusiness;
    }

    public Subject getSelectedSubject() {
        return selectedSubject;
    }

    public void setSelectedSubject(Subject selectedSubject) {
        this.selectedSubject = selectedSubject;
    }

    public Competence getSelectedCompetence() {
        return selectedCompetence;
    }

    public void setSelectedCompetence(Competence selectedCompetence) {
        this.selectedCompetence = selectedCompetence;
        updateListOfSubcompetencies();
    }

    public ECompetenceDimension getSelectedCompetenceDimension() {
        return selectedCompetenceDimension;
    }

    public void setSelectedCompetenceDimension(ECompetenceDimension selectedCompetenceDimension) {
        this.selectedCompetenceDimension = selectedCompetenceDimension;
    }

    // Indicators
    public boolean noCompetenceSelected() {
        return JackStringUtils.isBlank(String.valueOf(this.selectedCompetence));
    }

}
