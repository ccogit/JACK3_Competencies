package de.uni_due.s3.jack3.services;

import de.uni_due.s3.jack3.entities.enums.ECompetenceDimension;
import de.uni_due.s3.jack3.entities.tenant.Competence;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.Subject;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Service for managing {@link de.uni_due.s3.jack3.entities.tenant.Competence} entities.
 */

@Stateless
public class CompetenceService extends AbstractServiceBean {

    @Inject
    BaseService baseService;

    public boolean persistCompetence(Competence competence) {
        try {
            baseService.persist(competence);
            return true;
        } catch (Exception e) {
            getLogger().error("Error while trying to persist " + competence, e);
            return false;
        }
    }

    public boolean mergeCompetence(Competence competence) {
        try {
            baseService.merge(competence);
            return true;
        } catch (Exception e) {
            getLogger().error("Error while trying to update " + competence, e);
            return false;
        }
    }

    public void deleteCompetence(Competence competence) {
        baseService.deleteEntity(competence);
    }

    /**
     * Returns all subcompetencies that are children of a given {@link de.uni_due.s3.jack3.entities.tenant.Competence}.
     *
     * @return Subcompetencies list
     */
    public List<Competence> getSubcompetenciesByCompetence(Competence competence) {
        final EntityManager em = getEntityManager();
        final TypedQuery<Competence> query = em.createNamedQuery( //
                Competence.ALL_SUBCOMPETENCIES_BY_COMPETENCE, Competence.class);
        query.setParameter("competence", competence);
        return query.getResultList();
    }

//    /**
//     * Returns all content-competencies that are related to a given {@link de.uni_due.s3.jack3.entities.tenant.Subject}.
//     *
//     * @return Competencies list
//     */
//    public List<Competence> getContentCompetenciesBySubject(Subject subject) {
//        final EntityManager em = getEntityManager();
//        final TypedQuery<Competence> query = em.createNamedQuery( //
//                Competence.ALL_CONTENT_COMPETENCIES_BY_SUBJECT, Competence.class);
//        query.setParameter("subject", subject);
//        return query.getResultList();
//    }

    /**
     * Returns all competencies that are of a given {@link de.uni_due.s3.jack3.entities.enums.ECompetenceDimension}
     * and that are related to a given {@link de.uni_due.s3.jack3.entities.tenant.Subject}.
     *
     * @return Competencies list
     */
    public List<Competence> getCompetenciesByDimensionAndSubject(ECompetenceDimension dimension, Subject subject) {
        final EntityManager em = getEntityManager();
        final TypedQuery<Competence> query = em.createNamedQuery( //
                Competence.ALL_COMPETENCIES_BY_DIMENSION_AND_SUBJECT, Competence.class);
        query.setParameter("subject", subject);
        query.setParameter("dimension", dimension);
        return query.getResultList();
    }

    /**
     * Returns all subcompetencies that are of a given {@link de.uni_due.s3.jack3.entities.enums.ECompetenceDimension}
     * and that are related to a given {@link de.uni_due.s3.jack3.entities.tenant.Subject}.
     *
     * @return Competencies list
     */
    public List<Competence> getSubcompetenciesByDimensionAndSubject(ECompetenceDimension dimension, Subject subject) {
        final EntityManager em = getEntityManager();
        final TypedQuery<Competence> query = em.createNamedQuery( //
                Competence.ALL_SUBCOMPETENCIES_BY_DIMENSION_AND_SUBJECT, Competence.class);
        query.setParameter("subject", subject);
        query.setParameter("dimension", dimension);
        return query.getResultList();
    }


//    /**
//     * Returns all process-competencies that are related to a given {@link de.uni_due.s3.jack3.entities.tenant.Subject}.
//     *
//     * @return Competencies list
//     */
//    public List<Competence> getProcessCompetenciesBySubject(Subject subject) {
//        final EntityManager em = getEntityManager();
//        final TypedQuery<Competence> query = em.createNamedQuery( //
//                Competence.ALL_PROCESS_COMPETENCIES_BY_SUBJECT, Competence.class);
//        query.setParameter("subject", subject);
//        return query.getResultList();
//    }

}
