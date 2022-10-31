package de.uni_due.s3.jack3.entities.comparators;

import java.io.Serializable;
import java.util.Comparator;

import de.uni_due.s3.jack3.entities.tenant.Stage;

/**
 * Comparator for sorting two stages using their order indices.
 */
public class StageOrderIndexComparator implements Comparator<Stage>, Serializable {

	private static final long serialVersionUID = -3586684914057873283L;

	@Override
	public int compare(Stage first, Stage second) {
		if (first.getOrderIndex() > second.getOrderIndex()) {
			return 1;
		}
		if (first.getOrderIndex() < second.getOrderIndex()) {
			return -1;
		}

		throw new IllegalStateException(
				"Both stages have the same order index, but this is an illegal state for the owning exercise! "
						+ "First stage was: " + first + " with first.getOrderIndex(): " + first.getOrderIndex()
						+ ", second stage was: " + second + " with second.getOrderIndex(): " + second.getOrderIndex());
	}
}
