package de.uni_due.s3.jack3.services;

import static de.uni_due.s3.jack3.services.utils.DBHelper.getOneOrZeroRemovingDuplicates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import de.uni_due.s3.jack3.entities.tenant.*;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.services.utils.DBHelper;
import de.uni_due.s3.jack3.utils.EntityReflectionHelper;

/**
 * Service for managaging entities derived from {@link AbstractExercise}.
 */
@Stateless
public class ExerciseService extends AbstractServiceBean {

    @Inject
    private RevisionService revisionService;

    @Inject
    private BaseService baseService;

    /**
     * Returns all exercises from the database where the user has rights on the parent folder of the exercise.
     *
     * @return Exercise list without lazy data, alphabetically ordered
     */
    public List<Exercise> getAllExercisesForUser(User user) {
        return getEntityManager() //
                .createNamedQuery(Exercise.ALL_EXERCISES_FOR_USER, Exercise.class) //
                .setParameter("user", user) //
                .getResultList();
    }

    /**
     * Returns all exercises that are direct children of a folder in the folderList
     *
     * @param folderList The list of folders where the exercises need to be in
     * @return Exercise list without lazy data, alphabetically ordered
     */
    public List<Exercise> getAllExercisesForContentFolderList(List<ContentFolder> folderList) {
        if (folderList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> idsOfFolderList = folderList.stream().map(ContentFolder::getId).collect(Collectors.toList());

        final EntityManager em = getEntityManager();
        final TypedQuery<Exercise> query = em.createNamedQuery(Exercise.ALL_EXERCISES_FOR_CONTENTFOLDER_LIST,
                Exercise.class);
        query.setParameter("idsFolderList", idsOfFolderList);
        return query.getResultList();
    }

    /**
     * Returns all exercises that are direct children of a folder in the folderList.
     * In case a subject has been passed, only those exercises are returned whose subject corresponds to the given subject
     *
     * @param folderList The list of folders where the exercises need to be in
     * @param subject    The subject an exercise needs to correspond to
     * @return Exercise list without lazy data, alphabetically ordered
     */
    public List<Exercise> getAllExercisesForContentFolderListBySubject(List<ContentFolder> folderList, Subject subject) {
        if (folderList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> idsOfFolderList = folderList.stream().map(ContentFolder::getId).collect(Collectors.toList());

        final EntityManager em = getEntityManager();

        final TypedQuery<Exercise> query;

        if (subject == null) {
            query = em.createNamedQuery(Exercise.ALL_EXERCISES_FOR_CONTENTFOLDER_LIST, Exercise.class);
            query.setParameter("idsFolderList", idsOfFolderList);
        } else {
            query = em.createNamedQuery(Exercise.ALL_EXERCISES_FOR_CONTENTFOLDER_LIST_BY_SUBJECT, Exercise.class);
            query.setParameter("idsFolderList", idsOfFolderList);
            query.setParameter("subject", subject);
        }
        return query.getResultList();
    }

    /**
     * Returns all exercises tagged with a specific tag from the database. Exercises are ordered alphabetically by name.
     *
     * @return Exercise list without lazy data
     */
    public List<Exercise> getAllExercisesForThisTag(Tag tag) {
        final EntityManager em = getEntityManager();
        final TypedQuery<Exercise> query = em.createNamedQuery(Exercise.ALL_EXERCISES_FOR_TAG, Exercise.class);
        query.setParameter("id", tag.getId());
        return query.getResultList();
    }

    /**
     * Returns all exercises in a specific folder.
     *
     * @return Exercise list without lazy data
     */
    public List<AbstractExercise> getAllExercisesForContentFolder(ContentFolder folder) {
        final EntityManager em = getEntityManager();
        final TypedQuery<AbstractExercise> query = em.createNamedQuery(Exercise.ALL_EXERCISES_FOR_CONTENTFOLDER,
                AbstractExercise.class);
        query.setParameter("folder", folder);
        return query.getResultList();
    }

    /**
     * Returns all exercises in a specific folder. BEWARE: This is potentially slow!
     *
     * @return Exercise list eagerly (with lazy data)
     */
    public List<AbstractExercise> getAllExercisesForContentFolderEagerly(ContentFolder folder) {
        final EntityManager em = getEntityManager();
        final TypedQuery<AbstractExercise> query = em.createNamedQuery(Exercise.ALL_EXERCISES_FOR_CONTENTFOLDER_EAGERLY,
                AbstractExercise.class);
        query.setParameter("folder", folder);
        return query.getResultList();
    }

    /**
     * Returns the exercise with lazy data by id.
     *
     * @return Found exercise with lazy data
     */
    public Optional<Exercise> getExerciseByIdWithLazyData(long exerciseID) {
        final EntityManager em = getEntityManager();
        final TypedQuery<Exercise> query = em.createNamedQuery(Exercise.EXERCISE_WITH_LAZY_DATA_BY_EXERCISE_ID,
                Exercise.class);
        query.setParameter("id", exerciseID);

        return getOneOrZeroRemovingDuplicates(query);
    }

    /**
     * Returns the exercise by id.
     *
     * @return Found exercise <strong>without</strong> lazy data except the folder and the author of the exercise.
     */
    public Optional<Exercise> getExerciseById(long exerciseID) {
        final EntityManager em = getEntityManager();
        // We need a query here because "author" and "folder" are lazy.
        final TypedQuery<Exercise> query = em.createNamedQuery(Exercise.EXERCISE_BY_ID, Exercise.class);
        query.setParameter("id", exerciseID);

        return getOneOrZeroRemovingDuplicates(query);
    }

    /**
     * Returns all exercises that contain an exercise resource.
     *
     * @return Exercise list <strong>without</strong> lazy data
     */
    // REVIEW lg - Ist es überhaupt möglich, dass eine Ressource zu mehreren Exercises gehört?
    public List<Exercise> getExercisesReferencingExerciseResource(ExerciseResource resource) {
        final EntityManager em = getEntityManager();
        final TypedQuery<Exercise> query = em.createNamedQuery(Exercise.EXERCISES_REFERENCING_EXERCISE_RESOURCE,
                Exercise.class);
        query.setParameter("resource", resource);
        return query.getResultList();
    }

    public void persistExercise(AbstractExercise exercise) {
        baseService.persist(exercise);
    }

    public AbstractExercise mergeExercise(AbstractExercise exercise) {
        return baseService.merge(exercise);
    }

    /**
     * Returns an exercise revision with the given revision ID.
     *
     * @return Found exercise <strong>without</strong> lazy data
     * @see RevisionService#getRevisionOfEntity(AbstractEntity, int)
     */
    public Optional<AbstractExercise> getRevisionOfExercise(AbstractExercise exercise, int revisionId) {
        Optional<AbstractExercise> revisionOfExercise = revisionService.getRevisionOfEntity(exercise, revisionId);

        revisionOfExercise.filter(abstractExercise -> (abstractExercise instanceof Exercise))
                .ifPresent(abstractExercise -> ((Exercise) abstractExercise).setFromEnvers(true));

        return revisionOfExercise;
    }

    /**
     * Returns an exercise revision with the given revision ID.
     *
     * @return Found exercise with lazy data
     * @see RevisionService#getRevisionOfEntityWithLazyData(AbstractEntity, int)
     */
    public Optional<AbstractExercise> getRevisionOfExerciseWithLazyData(AbstractExercise exercise, int revisionId) {
        return getRevisionOfExerciseWithLazyData(exercise.getId(), revisionId);
    }

    public Optional<AbstractExercise> getRevisionOfExerciseWithLazyData(long exerciseId, int revisionId) {
        AuditReader auditReader = AuditReaderFactory.get(getEntityManager());
        Exercise result = auditReader.find(Exercise.class, exerciseId, revisionId);

        if (result == null) {
            return Optional.empty();
        }
        result.setFromEnvers(true);

        EntityReflectionHelper.hibernateInitializeObjectGraph(result);

        return Optional.of(result);
    }

    public AbstractExercise resetToRevision(AbstractExercise exercise, int revisionIndex) {
        return revisionService.resetToRevisionOfEntity(exercise, revisionIndex);
    }

    public void deleteExercise(AbstractExercise exercise) {
        baseService.deleteEntity(exercise);
    }

    /**
     * Get a frozen revision for an exercise.
     *
     * @param realExerciseId            The ID of the proxied ("real") exercise.
     * @param proxiedExerciseRevisionId The real revision ID of the corresponding exercise
     * @return Frozen exercise <strong>without</strong> lazy data
     */
    public Optional<FrozenExercise> getFrozenRevisionForExercise(long realExerciseId, int proxiedExerciseRevisionId) {

        final EntityManager em = getEntityManager();
        final TypedQuery<FrozenExercise> query = em.createNamedQuery( //
                FrozenExercise.FROZEN_REVISION_FOR_EXERCISE, FrozenExercise.class);
        query.setParameter("proxiedExerciseId", realExerciseId);
        query.setParameter("proxiedExerciseRevisionId", proxiedExerciseRevisionId);

        return DBHelper.getOneOrZero(query);
    }

    public FrozenExercise mergeFrozenExercise(FrozenExercise frozenExercise) {
        return baseService.merge(frozenExercise);
    }

    /**
     * Loads a frozen revision for an exercise.
     *
     * @param frozenExerciseId ID of the frozen exercise entity
     * @return Frozen exercise with lazy data
     */
    public Optional<FrozenExercise> getFrozenExerciseWithLazyDataById(long frozenExerciseId) {
        final EntityManager em = getEntityManager();
        final TypedQuery<FrozenExercise> query = em.createNamedQuery( //
                FrozenExercise.FROZEN_REVISION_WITH_LAZY_DATA_BY_ID, FrozenExercise.class);
        query.setParameter("id", frozenExerciseId);

        return getOneOrZeroRemovingDuplicates(query);
    }

    /**
     * Get all frozen exercise revisions for a given exercise by its ID.
     *
     * @param exercise The proxied ("real") exercise.
     * @return Frozen exercise list <strong>without</strong> lazy data.
     */
    public List<FrozenExercise> getFrozenRevisionsForExercise(AbstractExercise exercise) {
        final EntityManager em = getEntityManager();
        final TypedQuery<FrozenExercise> query = em.createNamedQuery( //
                FrozenExercise.FROZEN_REVISIONS_FOR_EXERCISE, FrozenExercise.class);
        query.setParameter("proxiedExerciseId", exercise.getProxiedOrRegularExerciseId());
        return query.getResultList();
    }

    public void removeEntitiesFromStageOrExerciseManually(Set<AbstractEntity> entitiesToRemoveManually) {
        entitiesToRemoveManually.stream()
                .filter(e -> !e.isTransient())
                .forEach(baseService::deleteEntity);
        getEntityManager().flush();
    }

    /**
     * Lists all tag names for an exercise, not ordered.
     */
    public List<String> getTagsForExerciseAsString(Exercise exercise) {
        Objects.requireNonNull(exercise);
        final var list = getEntityManager()
                .createNamedQuery(Exercise.TAGS_FOR_EXERCISE, String.class)
                .setParameter("exercise", exercise)
                .getResultList();
        // If the exercise doesn't have any tag, a list with null element was returned
        if (list.size() == 1 && list.get(0) == null) {
            return Collections.emptyList();
        }
        return list;
    }

    /**
     * Returns all exercises that reference a given {@link de.uni_due.s3.jack3.entities.tenant.Subject}.
     *
     * @return Exercise list
     */
    public List<Exercise> getExercisesReferencingSubject(Subject subject) {
        final EntityManager em = getEntityManager();
        final TypedQuery<Exercise> query = em.createNamedQuery( //
                Exercise.EXERCISES_REFERENCING_SUBJECT, Exercise.class);
        query.setParameter("subject", subject);
        return query.getResultList();
    }

    /**
     * Returns all exercises that reference a given {@link de.uni_due.s3.jack3.entities.tenant.Competence}.
     *
     * @return Exercises list
     */
    public List<Exercise> getExercisesReferencingSubcompetence(Competence subcompetence) {
        final EntityManager em = getEntityManager();
        final TypedQuery<Exercise> query = em.createNamedQuery( //
                Exercise.EXERCISES_REFERENCING_SUBCOMPETENCE, Exercise.class);
        query.setParameter("subcompetence", subcompetence);
        return query.getResultList();
    }

}
