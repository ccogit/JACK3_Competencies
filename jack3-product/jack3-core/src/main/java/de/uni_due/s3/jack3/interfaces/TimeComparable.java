package de.uni_due.s3.jack3.interfaces;

import java.time.LocalDateTime;

import de.uni_due.s3.jack3.entities.comparators.TimeComparator;

/**
 * Interface for making sure a class is sortable over time. We can sort all entitys implementing this with
 * {@link TimeComparator}.
 *
 * @author Benjamin.Otto
 *
 */
public interface TimeComparable {
	LocalDateTime getTimestamp();
}
