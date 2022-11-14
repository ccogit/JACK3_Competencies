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
 * Representation of a goal related to a competence. A goal either references a course or an exercise.
 * Therefore, one of these properties will be null in each entry.
 */

@Entity
@Audited
@XStreamAlias("CompetenceGoal")
public class CompetenceGoal extends AbstractEntity {
    private static final long serialVersionUID = 5524587832823182702L;

    @ManyToOne
    private Competence competence;

    @ManyToOne
    private Course course;

    @ManyToOne
    private Exercise exercise;

}
