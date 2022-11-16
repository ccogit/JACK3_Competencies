package de.uni_due.s3.jack3.business;

import de.uni_due.s3.jack3.business.exceptions.CompetenceException;
import de.uni_due.s3.jack3.business.exceptions.CompetenceGoalException;
import de.uni_due.s3.jack3.entities.enums.ECompetenceDimension;
import de.uni_due.s3.jack3.entities.tenant.*;
import de.uni_due.s3.jack3.services.*;
import de.uni_due.s3.jack3.utils.JackStringUtils;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.List;

@RequestScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class CompetenceGoalBusiness extends AbstractBusiness {

    @Inject
    private CompetenceGoalService competenceGoalService;

    @Inject
    private CompetenceService competenceService;

    @Inject CompetenceBusiness competenceBusiness;

    /**
     * Returns all competencies that belong to the same subject as the given course,
     * and that have not been added as a competenceGoal already.
     *
     * @param course
     */
    public List<Competence> getAllCompetenciesOfCourseSubjectNotAllocatedAsGoal(AbstractCourse course) {
        List<Competence> competenciesNotAllocated = competenceBusiness.getAllSubcompetenciesBySubject(course.getSubject());
        for (CompetenceGoal competenceGoal : competenceGoalService.getCompetenceGoalsByCourse((Course) course)) {
            competenciesNotAllocated.remove(competenceGoal.getCompetence());
        }
        return competenciesNotAllocated;
    }

    /**
     * Returns all competenceGoals of a given course.
     *
     * @param course
     */
    public List<CompetenceGoal> getCompetenceGoalsByCourse(AbstractCourse course) {
        return competenceGoalService.getCompetenceGoalsByCourse((Course) course);
    }

    /**
     * Removes a competence goal.
     *
     * @param competenceGoal
     */
    public boolean removeCompetenceGoal(CompetenceGoal competenceGoal) {
        competenceGoalService.deleteCompetenceGoal(competenceGoal);
        return competenceGoalService.getCompetenceGoalById(competenceGoal.getId()).isEmpty();
    }

    /**
     * Adds a new competenceGoal to a course and persists it to the database.
     * Returns persisted competenceGoal if that was successful.
     * <p>
     * Prerequisites:
     * - there exists no competenceGoal referencing the same competence.
     * - level specified
     * - intensity specified
     * <p>
     * * @param competenceGoal
     */
    public boolean addNewCompetenceGoalToCourse(CompetenceGoal competenceGoal) throws CompetenceGoalException {
        if (competenceGoalReferencingEqualCompetenceExistsForCourse(competenceGoal)) {
            throw new CompetenceGoalException(CompetenceGoalException.EType.COMPETENCEGOAL_ALREADY_EXISTS);
        }
        checkSpecOfCompetenceGoal(competenceGoal);
        return competenceGoalService.persistCompetenceGoal(competenceGoal);
    }

    /**
     * Adds a new competenceGoal to an exercise and persists it to the database.
     * Returns persisted competenceGoal if that was successful.
     * <p>
     * Prerequisites:
     * - there exists no competenceGoal referencing the same competence.
     * - competenceGoal fully specified
     * <p>
     * * @param competenceGoal
     */
    public boolean addNewCompetenceGoalToExercise(CompetenceGoal competenceGoal) throws CompetenceGoalException {
        if (competenceGoalReferencingEqualCompetenceExistsForExercise(competenceGoal)) {
            throw new CompetenceGoalException(CompetenceGoalException.EType.COMPETENCEGOAL_ALREADY_EXISTS);
        }
        checkSpecOfCompetenceGoal(competenceGoal);
        return competenceGoalService.persistCompetenceGoal(competenceGoal);
    }

    /**
     * Checks completeness of the specification of a competenceGoal.
     * <p>
     * A complete specification requires:
     * - specification of addressed competence.
     * - target level
     * - target intensity
     * <p>
     * * @param competenceGoal
     */

    public boolean checkSpecOfCompetenceGoal(CompetenceGoal competenceGoal) {
        if (JackStringUtils.isBlank(String.valueOf(competenceGoal.getCompetence()))) {
            throw new IllegalArgumentException("You must specify a competence.");
        }
        if (JackStringUtils.isBlank(String.valueOf(competenceGoal.getLevel()))) {
            throw new IllegalArgumentException("You must specify a level.");
        }
        if (JackStringUtils.isBlank(String.valueOf(competenceGoal.getIntensity()))) {
            throw new IllegalArgumentException("You must specify an intensity.");
        }
        return true;
    }

    /**
     * Updates a competenceGoal. Returns true if competence was updated and false otherwise.
     *
     * @param competenceGoal
     */
    public boolean updateCompetenceGoal(CompetenceGoal competenceGoal) {
        checkSpecOfCompetenceGoal(competenceGoal);
        competenceGoalService.mergeCompetenceGoal(competenceGoal);
        return true;
    }

    /**
     * Checks for a course if a competenceGoal exists that is referencing the same competence.
     *
     * @param competenceGoal
     */
    public boolean competenceGoalReferencingEqualCompetenceExistsForCourse(CompetenceGoal competenceGoal) {
        boolean competenceWithEqualNameExists = false;
        for (CompetenceGoal existingCompetenceGoal : competenceGoal.getCourse().getCompetenceGoals()) {
            if (existingCompetenceGoal.getCompetence().equals(competenceGoal.getCompetence())) {
                competenceWithEqualNameExists = true;
            }
        }
        return competenceWithEqualNameExists;
    }

    /**
     * Checks for an exercise if a competenceGoal exists that is referencing the same competence.
     *
     * @param competenceGoal
     */
    public boolean competenceGoalReferencingEqualCompetenceExistsForExercise(CompetenceGoal competenceGoal) {
        boolean competenceWithEqualNameExists = false;
        for (CompetenceGoal existingCompetenceGoal : competenceGoal.getExercise().getCompetenceGoals()) {
            if (existingCompetenceGoal.getCompetence().equals(competenceGoal.getCompetence())) {
                competenceWithEqualNameExists = true;
            }
        }
        return competenceWithEqualNameExists;
    }

}