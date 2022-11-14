package de.uni_due.s3.jack3.entities.tenant;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.persistence.Entity;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.enums.ECourseExercisesOrder;
import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.providers.AbstractExerciseProvider;

@Entity
@NamedQuery( //
        name = FrozenCourse.FROZEN_REVISIONS_FOR_COURSE, //
        query = "SELECT c FROM FrozenCourse c WHERE c.proxiedCourseId=:proxiedCourseId")
@NamedQuery( // Gets a FrozenCourse by querying for the proxied ids
        name = FrozenCourse.FROZEN_COURSE_BY_PROXIED_IDS_WITH_LAZY_DATA, //
        query = "SELECT c FROM FrozenCourse c " //
                + "LEFT JOIN FETCH c.contentProvider " //
                + "LEFT JOIN FETCH c.resultFeedbackMappings " //
                + "LEFT JOIN FETCH c.courseResources " //
                + "WHERE c.proxiedCourseId=:proxiedCourseId " //
                + "AND c.proxiedCourseRevisionId=:proxiedCourseRevisionId")
@NamedQuery( // Gets a FrozenCourse by querying for the proxied ids
        name = FrozenCourse.FROZEN_COURSE_BY_PROXIED_IDS, //
        query = "SELECT c FROM FrozenCourse c " //
                + "WHERE c.proxiedCourseId=:proxiedCourseId " //
                + "AND c.proxiedCourseRevisionId=:proxiedCourseRevisionId")
@NamedQuery( // Gets a FrozenCourse by querying for the proxied ids
        name = FrozenCourse.FROZEN_COURSE_BY_ID_WITH_LAZY_DATA, //
        query = "SELECT c FROM FrozenCourse c " //
                + "LEFT JOIN FETCH c.contentProvider " //
                + "LEFT JOIN FETCH c.resultFeedbackMappings " //
                + "LEFT JOIN FETCH c.courseResources " //
                + "WHERE c.id=:id ")
@NamedQuery(// Returns all IDs of Frozen Courses that belong to a course
        name = FrozenCourse.FROZEN_COURSE_IDS_FOR_COURSE, //
        query = "SELECT c.id FROM FrozenCourse c " //
                + "WHERE c.proxiedCourseId = :proxiedCourseId")
@Audited
public class FrozenCourse extends AbstractCourse implements Comparable<FrozenCourse> {

    private static final String MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS = "Must not change state of frozen objects!";

    public static final String FROZEN_COURSE_BY_ID_WITH_LAZY_DATA = "FrozenCourseByIdWithLazyData";
    public static final String FROZEN_COURSE_BY_PROXIED_IDS = "frozenCourseByProxiedIds";
    public static final String FROZEN_COURSE_BY_PROXIED_IDS_WITH_LAZY_DATA = "frozenCourseByProxiedIdsWithLazyData";
    public static final String FROZEN_REVISIONS_FOR_COURSE = "FrozenRevisionsForCourse";
    public static final String FROZEN_COURSE_IDS_FOR_COURSE = "FrozenCourse.frozenCourseIdsForCourse";

    private static final long serialVersionUID = 1812208684854262994L;

    // Only fields specific to a frozen course are declared here!
    // General fields for Course are declared in AbstractCourse.

    @ToString
    @Type(type = "text")
    private String frozenTitle;

    /**
     * ID of the {@link Course} this frozen copy belongs to.
     */
    @ToString
    long proxiedCourseId;

    /**
     * ID of the original revision that has been frozen to create this frozen course.
     */
    // Since Hibernates returns a List that can only be evaluated to Integer, this is an int not a long.
    // See {@link RevisionService#getRevisionNumbersFor}
    @ToString
    int proxiedCourseRevisionId;

    @SuppressWarnings("unused") // Hibernate needs an no-argument constructor, but we dont want to use it.
    private FrozenCourse() {
    }

    /**
     * (Deep) Copy-Constructor for Courses
     *
     * @param course
     * @param proxiedCourseRevisionId
     */
    public FrozenCourse(Course course, int proxiedCourseRevisionId) {

        // REVIEW bo: is copying the author of the Course correct or should we save the
        // user who froze the revision
        // here? Same applys for the course copy-constructor.
        super.deepCopyCourseVars(course, proxiedCourseRevisionId);
    }

    @Override
    public void addCourseResource(CourseResource courseResource) {
        throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
    }

    @Override
    public void addResultFeedbackMapping(ResultFeedbackMapping resultFeedbackMapping) {
        throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
    }

    @Override
    public void removeResultFeedbackMapping(ResultFeedbackMapping resultFeedbackMapping) {
        throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
    }

    @Override
    public int compareTo(FrozenCourse other) {
        if (other == null) {
            return -1;
        }

        if (other.getProxiedCourseId() != getProxiedCourseId()) {
            throw new AssertionError("Comparing of frozen courses only implemented for courses having the same "
                    + "proxiedCourseId! " + this + "!=" + other);
        }

        Integer otherProxiedCourseRevisionId = other.getProxiedCourseRevisionId();
        return ((Integer) getProxiedCourseRevisionId()).compareTo(otherProxiedCourseRevisionId);
    }

    @Override
    public AbstractExerciseProvider getContentProvider() {
        return contentProvider;
    }

    @Override
    public ECourseExercisesOrder getExerciseOrder() {
        return exerciseOrder;
    }

    @Override
    public Set<CourseResource> getCourseResources() {
        return Collections.unmodifiableSet(courseResources);
    }

    @Override
    public String getExternalDescription() {
        return externalDescription;
    }

    public String getFrozenTitle() {
        return frozenTitle;
    }

    @Override
    public String getInternalDescription() {
        return internalDescription;
    }

    @Override
    @Nonnull
    public String getName() {
        return name;
    }

    public long getProxiedCourseId() {
        return proxiedCourseId;
    }

    public int getProxiedCourseRevisionId() {
        return proxiedCourseRevisionId;
    }

    @Override
    public Set<ResultFeedbackMapping> getResultFeedbackMappings() {
        return Collections.unmodifiableSet(resultFeedbackMappings);
    }

    @Override
    public ECourseScoring getScoringMode() {
        return scoringMode;
    }

    @Override
    public boolean isFrozen() {
        return true;
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    public boolean hasSubject() {
        return this.subject != null;
    }

    @Override
    public Subject getSubject() {
        return subject;
    }

    @Override
    public void removeCourseResource(CourseResource courseResource) {
        throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
    }

    @Override
    public void setContentProvider(AbstractExerciseProvider contentProvider) {
        throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
    }

    @Override
    public void setExerciseOrder(ECourseExercisesOrder exerciseOrder) {
        throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
    }

    @Override
    public void setExternalDescription(String externalDescription) {
        throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
    }

    // This method needs to be package private
    // REVIEW bo: why? and it currently isn‘t
    @Override
    public void setFolder(ContentFolder folder) {
        throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
    }

    public void setFrozenTitle(String frozenTitle) {
        this.frozenTitle = frozenTitle;
    }

    @Override
    public void setInternalDescription(String internalDescription) {
        throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
    }

    @Override
    public void setValid(boolean isValid) {
        throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
    }

    @Override
    public void setSubject(Subject subject) {
        throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
    }

    @Override
    public long getRealCourseId() {
        return getProxiedCourseId();
    }

    @Override
    public boolean isFromEnvers() {
        return false;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public void setLanguage(String language) {
        throw new UnsupportedOperationException(MUST_NOT_CHANGE_STATE_OF_FROZEN_OBJECTS);
    }
}
