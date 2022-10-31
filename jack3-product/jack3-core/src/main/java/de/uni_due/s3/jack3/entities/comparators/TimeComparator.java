package de.uni_due.s3.jack3.entities.comparators;

import java.io.Serializable;
import java.util.Comparator;

import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.interfaces.TimeComparable;

/**
 * Comparator for sorting entitys over time. Entitys need to implement
 * AbstractEntity (getId()) and TimeComparable (getTimestamp())
 *
 * @author Benjamin.Otto
 */
public class TimeComparator<E extends AbstractEntity & TimeComparable> implements Comparator<E>, Serializable {

	private static final long serialVersionUID = 7402051100258760318L;

	@Override
	public int compare(E first, E second) {
		if ((first == null) || (second == null)) {
			throw new NullPointerException("Tried to compare null-objects. First: " + first + ", Second " + second);
		}

		final int compareToResult = first.getTimestamp().compareTo(second.getTimestamp());
		if (compareToResult == 0) {
			return (int) (first.getId() - second.getId());
		}
		return compareToResult;
	}

}
