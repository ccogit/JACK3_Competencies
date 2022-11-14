package de.uni_due.s3.jack3.business;

import de.uni_due.s3.jack3.business.exceptions.CompetenceException;
import de.uni_due.s3.jack3.business.exceptions.SubjectException;
import de.uni_due.s3.jack3.entities.enums.ECompetenceDimension;
import de.uni_due.s3.jack3.entities.tenant.*;
import de.uni_due.s3.jack3.services.*;
import de.uni_due.s3.jack3.utils.JackStringUtils;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RequestScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class CompetenceBusiness extends AbstractBusiness {

    @Inject
    private CompetenceService competenceService;

    @Inject
    private CourseService courseService;

    @Inject
    private ExerciseService exerciseService;

    @Inject
    private BaseService baseService;

    /**
     * Returns all competencies for a given competence dimension for a given subject.
     * Returns an empty list if no competencies exist.
     *
     * @param subject
     * @param dimension
     * @return
     */
    public List<Competence> getAllCompetenciesByDimensionAndSubject(ECompetenceDimension dimension, Subject subject) {
        return competenceService.getCompetenciesByDimensionAndSubject(dimension, subject);
    }

    /**
     * Returns all subcompetencies including their parent competencies for a give competence dimension of a give subject.
     * Returns an empty list if no subcompetencies exist.
     *
     * @param subject
     * @param dimension
     * @return
     */
    public List<Competence> getAllSubcompetenciesByDimensionAndSubject(ECompetenceDimension dimension, Subject subject) {
        return competenceService.getSubcompetenciesByDimensionAndSubject(dimension, subject);
    }

    /**
     * Returns all subcompetencies including their parent competencies for a give competence.
     * Returns an empty list if no subcompetencies exist.
     *
     * @param competence
     * @return
     */
    public List<Competence> getSubcompetenciesByCompetence(Competence competence) {
        return competenceService.getSubcompetenciesByCompetence(competence);
    }

    public boolean isSubcompetence(Competence competence) {
        return competence.getParentCompetence() != null;
    }

    public Competence generateNewSubcompetence(Competence competence) {
        Competence newSubcompetence =
                new Competence(
                        "New Subcompetence",
                        competence.getSubject(),
                        competence.getCompetenceDimension());
        newSubcompetence.setParentCompetence(competence);
        newSubcompetence.setName("New Subcompetence");
        competence.getChildCompetencies().add(newSubcompetence);
        return newSubcompetence;
    }

    /**
     * Removes a subcompetence. Prerequisite: there are no references from courses or exercises.
     *
     * @param subcompetence
     */
    public boolean removeSubcompetence(Competence subcompetence) {
        boolean hasBeenRemoved = false;
        List<Course> referencingCourses = courseService.getCoursesReferencingSubcompetence(subcompetence);
        List<Exercise> referencingExercises = exerciseService.getExercisesReferencingSubcompetence(subcompetence);
        if (referencingCourses.isEmpty() && referencingExercises.isEmpty()) {
            competenceService.deleteCompetence(subcompetence);
            hasBeenRemoved = true;
        }
        return hasBeenRemoved;
    }

    /**
     * Removes a competence and all related subcompetencies.
     * Prerequisite: there are no references from courses or exercises
     * to any of the subcompetencies that are children of a given competence.
     *
     * @param competence
     */
    public boolean removeCompetence(Competence competence) {
        boolean hasBeenRemoved = false;
        boolean referenceExists = false;
        List<Competence> subcompetencies = competenceService.getSubcompetenciesByCompetence(competence);
        for (Competence sc : subcompetencies) {
            if (!courseService.getCoursesReferencingSubcompetence(sc).isEmpty() ||
                    !exerciseService.getExercisesReferencingSubcompetence(sc).isEmpty()) {
                referenceExists = true;
            }
        }
        if (!referenceExists) {
            for (Competence sc : subcompetencies) {
                competenceService.deleteCompetence(sc);
            }
            competenceService.deleteCompetence(competence);
            hasBeenRemoved = true;
        }
        return hasBeenRemoved;
    }

    /**
     * Adds a new competence to the database. Returns persisted competence if that was successful.
     * <p>
     * Prerequisites: there exists no competence with equal name in case of relevant subject and name is not empty.
     * I.e. duplicate name for competencies possible if they belong to different subjects.
     * Iterates over competencies to make sure that there exists no competence with same name.
     * <p>
     * The method does NOT update any cached values.
     * <p>
     * * @param competence
     */
    public boolean addNewCompetence(Competence competence) throws CompetenceException {
        if (competenceWithEqualNameExists(competence)) {
            throw new CompetenceException(CompetenceException.EType.COMPETENCE_ALREADY_EXISTS);
        }
        if (JackStringUtils.isBlank(competence.getName())) {
            throw new IllegalArgumentException("You must specify a non-emtpy competence name.");
        }
        return competenceService.persistCompetence(competence);
    }

    /**
     * Adds a new subcompetence to the database. Returns persisted subcompetence if that was successful.
     * <p>
     * Prerequisites: there exists no subcompetence with equal name in case of relevant competence and name is not empty.
     * I.e. duplicate name for subcompetencies possible if they belong to different competencies.
     * <p>
     * The method does NOT update any cached values.
     * <p>
     * * @param subcompetence
     */
    public boolean addNewSubcompetence(Competence subcompetence) throws CompetenceException {
        if (subcompetenceWithEqualNameExists(subcompetence)) {
            throw new CompetenceException(CompetenceException.EType.COMPETENCE_ALREADY_EXISTS);
        }
        if (JackStringUtils.isBlank(subcompetence.getName())) {
            throw new IllegalArgumentException("You must specify a non-emtpy subcompetence name.");
        }
        return competenceService.persistCompetence(subcompetence);
    }

    /**
     * Updates a competence. Returns true if competence was updated and false otherwise.
     * <p>
     * Prerequisites: there exists no competence with equal name in case of relevant subject and name is not empty.
     * I.e. duplicate name for competencies possible if they belong to different subjects.
     * Iterates over competencies to make sure that there exists no competence with same name.
     * <p>
     *
     * @param competence
     */
    public boolean updateCompetence(Competence competence) throws CompetenceException {
        if (competenceWithEqualNameExists(competence)) {
            throw new CompetenceException(CompetenceException.EType.COMPETENCE_ALREADY_EXISTS);
        }
        if (JackStringUtils.isBlank(competence.getName())) {
            throw new IllegalArgumentException("You must specify a non-emtpy competence name.");
        }
        competenceService.mergeCompetence(competence);
        return true;
    }

    /**
     * Updates a subcompetence. Returns true if subcompetence was updated and false otherwise.
     * <p>
     * Prerequisites: there exists no subcompetence with equal name in case of relevant competence and name is not empty.
     * I.e. duplicate name for subcompetencies possible if they belong to different competencies.
     * <p>
     *
     * @param subcompetence
     */
    public boolean updateSubcompetence(Competence subcompetence) throws CompetenceException {
        if (subcompetenceWithEqualNameExists(subcompetence)) {
            throw new CompetenceException(CompetenceException.EType.COMPETENCE_ALREADY_EXISTS);
        }
        if (JackStringUtils.isBlank(subcompetence.getName())) {
            throw new IllegalArgumentException("You must specify a non-emtpy subcompetence name.");
        }
        competenceService.mergeCompetence(subcompetence);
        return true;
    }

    /**
     * Checks for a subject if there already exists a competence with same name.
     *
     * @param competence
     */
    public boolean competenceWithEqualNameExists(Competence competence) {
        List<Competence> contentCompetencies =
                competenceService.getCompetenciesByDimensionAndSubject(ECompetenceDimension.CONTENT,competence.getSubject());
        List<Competence> processCompetencies =
                competenceService.getCompetenciesByDimensionAndSubject(ECompetenceDimension.PROCESS,competence.getSubject());
        boolean competenceWithEqualNameExists = false;
        for (Competence existingContentCompetence : contentCompetencies) {
            if (existingContentCompetence.getName().equals(competence.getName())) {
                competenceWithEqualNameExists = true;
            }
        }
        for (Competence existingProcessCompetence : processCompetencies) {
            if (existingProcessCompetence.getName().equals(competence.getName())) {
                competenceWithEqualNameExists = true;
            }
        }
        return competenceWithEqualNameExists;
    }

    /**
     * Checks for a competence if there already exists a subcompetence with same name.
     *
     * @param subcompetence
     */
    public boolean subcompetenceWithEqualNameExists(Competence subcompetence) {
        List<Competence> subCompetencies = competenceService.getSubcompetenciesByCompetence(subcompetence.getParentCompetence());
        boolean subcompetenceWithEqualNameExists = false;
        for (Competence existingSubcompetence : subCompetencies) {
            if (existingSubcompetence.getName().equals(subcompetence.getName())) {
                subcompetenceWithEqualNameExists = true;
            }
        }
        return subcompetenceWithEqualNameExists;
    }

}