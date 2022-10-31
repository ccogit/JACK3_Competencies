package de.uni_due.s3.jack3.comparators;

import java.util.Comparator;

import org.primefaces.model.TreeNode;

import de.uni_due.s3.jack3.beans.data.UserRightsData;

public class UserRightsTreeOrder implements Comparator<TreeNode> {

	@Override
	public int compare(TreeNode node0, TreeNode node1) {
		final UserRightsData arg0 = (UserRightsData) node0.getData();
		final UserRightsData arg1 = (UserRightsData) node1.getData();

		if (arg0.getFolderAlias() == null && arg1.getFolderAlias() == null) {
			return arg0.getFolder().getName().compareTo(arg1.getFolder().getName());
		} else if (arg0.getFolderAlias() != null && arg1.getFolderAlias() != null) {
			return arg0.getFolderAlias().compareTo(arg1.getFolderAlias());
		}

		return 0;
	}

}
