package de.uni_due.s3.jack3.comparators;

import java.io.Serializable;
import java.util.Comparator;

import org.primefaces.model.TreeNode;

import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.enums.ETreeNodeType;

public class ContentTreeOrder implements Comparator<TreeNode>, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -4328220709004256370L;
	
	private FolderBusiness folderBusiness;
	
	public ContentTreeOrder(FolderBusiness folderBusiness){
		this.folderBusiness = folderBusiness;
	}

	@Override
	public int compare(TreeNode node0, TreeNode node1) {
		if (!(isUserRootNode(node0) && isUserRootNode(node1))) {
			final Object arg0 = node0.getData();
			final Object arg1 = node1.getData();
			if (arg0 instanceof ContentFolder) {
				if (arg1 instanceof Course) {
					return -1;
				} else if (arg1 instanceof Exercise) {
					return -1;
				} else if (arg1 instanceof ContentFolder) {
					return ((ContentFolder) arg0).getName().compareTo(((ContentFolder) arg1).getName());
				}
			}
			if (arg0 instanceof Course) {
				if (arg1 instanceof ContentFolder) {
					return 1;
				} else if (arg1 instanceof Exercise) {
					return -1;
				} else if (arg1 instanceof Course) {
					return ((Course) arg0).getName().compareTo(((Course) arg1).getName());
				}
			}
			if (arg0 instanceof Exercise) {
				if (arg1 instanceof ContentFolder) {
					return 1;
				} else if (arg1 instanceof Course) {
					return 1;
				} else if (arg1 instanceof Exercise) {
					return ((Exercise) arg0).getName().compareTo(((Exercise) arg1).getName());
				}
			}
		} else {
			if (node0.getType().contentEquals(ETreeNodeType.PERSONAL_FOLDER_TYPE.getName())) {
				return -1;
			} else if (node1.getType().contentEquals(ETreeNodeType.PERSONAL_FOLDER_TYPE.getName())) {
				return 1;
			} else if (((ContentFolder) node0.getData()).getParentFolder() == null
					&& ((ContentFolder) node1.getData()).getParentFolder() == null) {
				return ((ContentFolder) node0.getData()).getName()
						.compareTo(((ContentFolder) node1.getData()).getName());
			} else {
				return folderBusiness.getOwnerOfContentFolder((ContentFolder) node0.getData()).getLoginName()
						.compareTo(folderBusiness.getOwnerOfContentFolder((ContentFolder) node1.getData()).getLoginName());
			}

		}
		return 0;
	}

	private boolean isUserRootNode(TreeNode node) {
		return node.getType().contentEquals(ETreeNodeType.PERSONAL_FOLDER_TYPE.getName())
				|| node.getType().contentEquals(ETreeNodeType.SHARED_FOLDER_TYPE.getName());
	}

}
