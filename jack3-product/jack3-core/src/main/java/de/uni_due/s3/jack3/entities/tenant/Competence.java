package de.uni_due.s3.jack3.entities.tenant;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.enums.ECompetenceDimension;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Representation of a competence or subcompetence. Each subcompetence has a parent competence.
 * Many subcompetencies can have the same parent competence.
 * A competence / subcompetence can be of type "process" or "content".
 */

@Entity
@Audited
@XStreamAlias("Competence")
@NamedQuery( //
        name = Competence.ALL_COMPETENCIES_BY_DIMENSION_AND_SUBJECT, //
        query = "SELECT comp FROM Competence comp " //
                + "WHERE comp.subject=:subject " //
                + "AND comp.parentCompetence is null " //
                + "AND comp.competenceDimension =:dimension")
@NamedQuery( //
        name = Competence.ALL_SUBCOMPETENCIES_BY_DIMENSION_AND_SUBJECT, //
        query = "SELECT subcomp FROM Competence subcomp " //
                + "join Competence comp on subcomp.parentCompetence=comp.id " //
                + "WHERE comp.subject=:subject " //
                + "AND comp.competenceDimension =:dimension")
@NamedQuery( //
        name = Competence.ALL_SUBCOMPETENCIES_BY_COMPETENCE, //
        query = "SELECT comp FROM Competence comp " //
                + "WHERE comp.parentCompetence=:competence") //
@NamedQuery( //
        name = Competence.ALL_SUBCOMPETENCIES_BY_SUBJECT, //
        query = "SELECT c FROM Competence c " //
                + "WHERE c.subject=:subject " //
                + "AND NOT c.parentCompetence is null") //
public class Competence extends AbstractEntity {

    private static final long serialVersionUID = -5154989747417668114L;
    public static final String ALL_SUBCOMPETENCIES_BY_SUBJECT = "Competence.allSubcompetenciesBySubject";
    public static final String ALL_SUBCOMPETENCIES_BY_COMPETENCE = "Competence.allSubcompetenciesByCompetence";
    public static final String ALL_COMPETENCIES_BY_DIMENSION_AND_SUBJECT = "Competence.allCompetenciesByDimensionAndSubject";
    public static final String ALL_SUBCOMPETENCIES_BY_DIMENSION_AND_SUBJECT = "Competence.allSubcompetenciesByDimensionAndSubject";

    @Column
    @Type(type = "text")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    private Competence parentCompetence;
    @Column(name = "COMPETENCE_DIMENSION")
    @Enumerated(EnumType.STRING)
    private ECompetenceDimension competenceDimension;

    @OneToMany(mappedBy = "parentCompetence", cascade = CascadeType.ALL)
    private Set<Competence> childCompetencies = new HashSet<>();

    public Competence() {
    }

    // Constructor for new competence (i.e. has no parent competence)
    public Competence(String name, Subject subject, ECompetenceDimension competenceDimension) {
        this.name = name;
        this.subject = subject;
        this.competenceDimension = competenceDimension;
    }

    // Constructor for new subcompetence (i.e. has parent competence)
    public Competence(String name, Subject subject, ECompetenceDimension competenceDimension, Competence parentCompetence) {
        this.name = name;
        this.subject = subject;
        this.competenceDimension = competenceDimension;
        this.parentCompetence=parentCompetence;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Competence getParentCompetence() {
        return parentCompetence;
    }

    public void setParentCompetence(Competence parentCompetence) {
        this.parentCompetence = parentCompetence;
    }

    public ECompetenceDimension getCompetenceDimension() {
        return competenceDimension;
    }

    public void setCompetenceDimension(ECompetenceDimension competenceDimension) {
        this.competenceDimension = competenceDimension;
    }

    public Set<Competence> getChildCompetencies() {
        return childCompetencies;
    }

    public void setChildCompetencies(Set<Competence> childCompetencies) {
        this.childCompetencies = childCompetencies;
    }

    @Override
    public String toString() {
        return this.name;
    }

}
