package de.uni_due.s3.jack3.entities.tenant;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.enums.EExerciseOrder;

// REVIEW lg - Obsolet? Weder Getter&Setter noch irgendeine sinnvolle Benutzung
/**
 * A Filter for limiting the Display of Exercises. The Selection can be limited by Tags, Difficulty and Language.
 */
@Audited
@Entity
public class UserExerciseFilter extends AbstractEntity {

	private static final long serialVersionUID = -762673540737410946L;

	public UserExerciseFilter() {
		super();
	}

	@Column
	@Type(type = "text")
	private String tagRule;

	@ElementCollection
	@Type(type = "text")
	private List<String> languages;

	@Column
	private boolean hideCompleted;

	@Column
	private boolean hideUnstarted;

	@Column
	private int minDifficulty;

	@Column
	private int maxDifficulty;

	@Enumerated(EnumType.STRING)
	private EExerciseOrder exerciseOrder;
}
