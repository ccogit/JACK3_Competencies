package de.uni_due.s3.jack3.entities.providers;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.enums.ECourseExercisesOrder;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;

/**
 * Abstract superclass for all providers.
 */
@Audited
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class AbstractExerciseProvider extends AbstractEntity {
	private static final long serialVersionUID = -1625234083771118810L;

	public AbstractExerciseProvider() {
	}

	public abstract List<CourseEntry> getCourseEntries();

	/**
	 * @return Wether the passed Exercise order is supported by the provider.
	 */
	public abstract boolean isExerciseOrderSupported(final ECourseExercisesOrder order);
}
