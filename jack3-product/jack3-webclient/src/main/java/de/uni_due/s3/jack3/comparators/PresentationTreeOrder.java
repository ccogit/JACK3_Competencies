package de.uni_due.s3.jack3.comparators;

import java.io.Serializable;
import java.util.Comparator;

import org.primefaces.model.TreeNode;

import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;

public class PresentationTreeOrder implements Comparator<TreeNode>, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 526039767784597308L;

	@Override
	public int compare(TreeNode node0, TreeNode node1) {
		final Object arg0 = node0.getData();
		final Object arg1 = node1.getData();
		if (arg0 instanceof PresentationFolder) {
			if (arg1 instanceof CourseOffer) {
				return -1;
			} else if (arg1 instanceof PresentationFolder) {
				return ((PresentationFolder) arg0).getName().compareTo(((PresentationFolder) arg1).getName());
			}
		}
		if (arg0 instanceof CourseOffer) {
			if (arg1 instanceof PresentationFolder) {
				return 1;
			} else if (arg1 instanceof CourseOffer) {
				return ((CourseOffer) arg0).getName().compareTo(((CourseOffer) arg1).getName());
			}
		}
		return 0;
	}

}
