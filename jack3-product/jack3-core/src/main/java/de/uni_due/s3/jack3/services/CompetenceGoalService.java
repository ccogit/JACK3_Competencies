package de.uni_due.s3.jack3.services;

import de.uni_due.s3.jack3.entities.enums.ECompetenceDimension;
import de.uni_due.s3.jack3.entities.tenant.*;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

import static de.uni_due.s3.jack3.services.utils.DBHelper.getOneOrZeroRemovingDuplicates;

/**
 * Service for managing {@link CompetenceGoal} entities.
 */

@Stateless
public class CompetenceGoalService extends AbstractServiceBean {

    @Inject
    BaseService baseService;

    /**
     * Returns the competenceGoal by id.
     *
     * @return Found competenceGoal
     */
    public Optional<CompetenceGoal> getCompetenceGoalById(long competenceGoalId) {
        final EntityManager em = getEntityManager();
        final TypedQuery<CompetenceGoal> query = em.createNamedQuery(CompetenceGoal.COMPETENCEGOAL_BY_ID, CompetenceGoal.class);
        query.setParameter("id", competenceGoalId);

        return getOneOrZeroRemovingDuplicates(query);
    }

    /**
     * Returns the competenceGoals of a given course.
     *
     * @return Found competenceGoals
     * @param course
     */
    public List<CompetenceGoal> getCompetenceGoalsByCourse(Course course) {
        final EntityManager em = getEntityManager();
        final TypedQuery<CompetenceGoal> query = em.createNamedQuery(CompetenceGoal.COMPETENCEGOALS_BY_COURSE, CompetenceGoal.class);
        query.setParameter("course", course);

        return query.getResultList();
    }

    public boolean persistCompetenceGoal(CompetenceGoal competenceGoal) {
        try {
            baseService.persist(competenceGoal);
            return true;
        } catch (Exception e) {
            getLogger().error("Error while trying to persist " + competenceGoal, e);
            return false;
        }
    }

    public boolean mergeCompetenceGoal(CompetenceGoal competenceGoal) {
        try {
            baseService.merge(competenceGoal);
            return true;
        } catch (Exception e) {
            getLogger().error("Error while trying to update " + competenceGoal, e);
            return false;
        }
    }

    public void deleteCompetenceGoal(CompetenceGoal competenceGoal) {
        baseService.deleteEntity(competenceGoal);
    }

}
