package de.uni_due.s3.jack3.entities.tenant;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.enums.ECompetenceDimension;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Representation of a goal related to a competence. A goal is either related to a course or to an exercise.
 * Therefore, one of these two properties will be null in each entry.
 */

@Entity
@Audited
@XStreamAlias("CompetenceGoal")
@NamedQuery(
        name = CompetenceGoal.COMPETENCEGOAL_BY_ID,
        query = "select cg FROM CompetenceGoal cg " //
                + "WHERE cg.id = :id")
@NamedQuery(
        name = CompetenceGoal.COMPETENCEGOALS_BY_COURSE,
        query = "select cg FROM CompetenceGoal cg " //
                + "JOIN Competence c on cg.competence=c.id " //
                + "WHERE cg.course = :course")
public class CompetenceGoal extends AbstractEntity {
    private static final long serialVersionUID = 5524587832823182702L;

    /**
     * Name of the query that gets a competenceGoal by id.
     */
    public static final String COMPETENCEGOAL_BY_ID = "CompetenceGoal.competenceGoalById";

    /**
     * Name of the query that gets all competenceGoals of a given course.
     */
    public static final String COMPETENCEGOALS_BY_COURSE = "CompetenceGoal.competenceGoalsByCourse";

    @ManyToOne
    private Competence competence;

    @ManyToOne
    private Course course;

    @ManyToOne
    private Exercise exercise;

    private int intensity;

    private int level;

    public CompetenceGoal() {
    }

    public CompetenceGoal(Competence competence, Course course, int intensity, int level) {
        this.competence = competence;
        this.course = course;
        this.intensity = intensity;
        this.level = level;
    }

    public Competence getCompetence() {
        return competence;
    }

    public void setCompetence(Competence competence) {
        this.competence = competence;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
    }

    public int getIntensity() {
        return intensity;
    }

    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
