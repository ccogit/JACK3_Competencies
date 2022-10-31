package de.uni_due.s3.jack3.entities.tenant;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.converters.AccessRightConverter;
import de.uni_due.s3.jack3.interfaces.Namable;

/**
 * Abstract Superclass for both Foldertypes.
 */
@Audited
@NamedQuery(
	name = Folder.ALL_CHILDREN,
	query = "SELECT f FROM Folder f WHERE f.parentFolder = :parent")
@NamedQuery(
		name = Folder.FOLDER_WITH_MANAGING_RIGHS_BY_ID,
		query = "SELECT f from Folder f " //
		+ "LEFT JOIN FETCH f.managingUsers " //
		+ "LEFT JOIN FETCH f.inheritedManagingUsers " //
		+ "LEFT JOIN FETCH f.managingUserGroups " //
		+ "LEFT JOIN FETCH f.inheritedManagingUserGroups " //
		+ "WHERE f.id=:id")
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Folder extends AbstractEntity implements Namable {

	private static final long serialVersionUID = -2919380403725743628L;

	public static final String ALL_CHILDREN = "Folder.allChildren";

	public static final String FOLDER_WITH_MANAGING_RIGHS_BY_ID = "Folder.folderWithManagingRights";

	@ToString
	@Column
	@Type(type = "text")
	private String name;

	@ManyToOne(fetch = FetchType.EAGER)
	private Folder parentFolder;

	/**
	 * The Users who have concrete Rights on the Folder
	 */
	@ElementCollection(fetch = FetchType.LAZY)
	@Convert(converter = AccessRightConverter.class, attributeName = "value")
	private Map<User, AccessRight> managingUsers = new HashMap<>();

	/**
	 * The UserGroups who have concrete Rights on the Folder
	 */
	@ElementCollection(fetch = FetchType.LAZY)
	@Convert(converter = AccessRightConverter.class, attributeName = "value")
	private Map<UserGroup, AccessRight> managingUserGroups = new HashMap<>();

	/**
	 * The Users who have rights on the parent folders and therefore inherit these rights
	 */
	@ElementCollection(fetch = FetchType.LAZY)
	@Convert(converter = AccessRightConverter.class, attributeName = "value")
	private Map<User, AccessRight> inheritedManagingUsers = new HashMap<>();

	/**
	 * The UserGroups who have rights on the parent folders and therefore inherit these rights
	 */
	@ElementCollection(fetch = FetchType.LAZY)
	@Convert(converter = AccessRightConverter.class, attributeName = "value")
	private Map<UserGroup, AccessRight> inheritedManagingUserGroups = new HashMap<>();

	@OneToMany(mappedBy = "parentFolder", fetch = FetchType.LAZY)
	protected Set<Folder> childrenFolder;

	public Folder() {

	}

	public Folder(final String name) {
		this.name = requireIdentifier(name, "You must specify an non-empty name.");
		childrenFolder = new HashSet<>();
	}

	public void addInheritedUserGroupRight(UserGroup userGroup, AccessRight right) {
		if (right.isNone()) {
			inheritedManagingUserGroups.remove(userGroup);
		} else {
			inheritedManagingUserGroups.put(userGroup, right);
		}
	}

	public void addInheritedUserGroupRights(Map<UserGroup, AccessRight> rights) {
		for (Map.Entry<UserGroup, AccessRight> entry : rights.entrySet()) {
			if (entry.getValue().isNone()) {
				inheritedManagingUserGroups.remove(entry.getKey());
			} else {
				inheritedManagingUserGroups.put(entry.getKey(), entry.getValue());
			}
		}
	}

	public void addInheritedUserRight(User user, AccessRight right) {
		if (right.isNone()) {
			inheritedManagingUsers.remove(user);
		} else {
			inheritedManagingUsers.put(user, right);
		}
	}

	public void addInheritedUserRights(Map<User, AccessRight> rights) {
		for (Map.Entry<User, AccessRight> entry : rights.entrySet()) {
			if (entry.getValue().isNone()) {
				inheritedManagingUsers.remove(entry.getKey());
			} else {
				inheritedManagingUsers.put(entry.getKey(), entry.getValue());
			}
		}
	}

	public void addUserGroupRight(UserGroup userGroup, AccessRight right) {
		if (right.isNone()) {
			managingUserGroups.remove(userGroup);
		} else {
			managingUserGroups.put(userGroup, right);
		}
	}

	public void removeUserGroupRight(UserGroup userGroup) {
		managingUserGroups.remove(userGroup);
	}

	public void addUserRight(User user, AccessRight right) {
		if (right.isNone()) {
			managingUsers.remove(user);
		} else {
			managingUsers.put(user, right);
		}
	}

	public void removeUserRight(User user) {
		managingUsers.remove(user);
	}

	public void deleteAllInheritedRights() {
		inheritedManagingUsers.clear();
		inheritedManagingUserGroups.clear();
	}

	public void deleteAllUserRights(User user) {
		managingUsers.remove(user);
		inheritedManagingUsers.remove(user);
	}


	public List<Folder> getBreadcrumb() {
		// Workaround to not have to lazy-load Folders for old revisions
		if (!Hibernate.isInitialized(parentFolder)) {
			return new LinkedList<>();
		}
		if (parentFolder != null) {
			final List<Folder> b;
			if((parentFolder.getParentFolder() != null)) {
				b = parentFolder.getBreadcrumb();
			}else {
				b = new LinkedList<>();
			}
			b.add(parentFolder);
			return b;
		} else {
			return new LinkedList<>();
		}
	}

	/*
	 * @return unmodifiableMap of inheritedManagingUserGroups
	 */
	public Map<UserGroup, AccessRight> getInheritedManagingUserGroups() {
		return Collections.unmodifiableMap(inheritedManagingUserGroups);
	}

	/*
	 * @return unmodifiableMap of inheritedManagingUsers
	 */
	public Map<User, AccessRight> getInheritedManagingUsers() {
		return Collections.unmodifiableMap(inheritedManagingUsers);
	}

	/*
	 * @return unmodifiableMap of managingUserGroups
	 */
	public Map<UserGroup, AccessRight> getManagingUserGroups() {
		return Collections.unmodifiableMap(managingUserGroups);
	}

	/*
	 * @return unmodifiableMap of managingUsers
	 */
	public Map<User, AccessRight> getManagingUsers() {
		return Collections.unmodifiableMap(managingUsers);
	}

	@Override
	@Nonnull
	public String getName() {
		return name;
	}

	public Folder getParentFolder() {
		return parentFolder;
	}

	public boolean isChildOf(Folder folder) {
		if (folder == null) {
			return false;
		}

		if (folder.equals(this) || folder.equals(parentFolder)) {
			return true;
		}

		if (parentFolder == null) {
			return false;
		}

		return parentFolder.isChildOf(folder);
	}

	public void setName(String name) {
		this.name = name;
	}

	private void setParentFolder(Folder parentFolder) {
		this.parentFolder = parentFolder;
	}

	public void addChildFolder(Folder childFolder) {
		childrenFolder.add(childFolder);
		childFolder.setParentFolder(this);
	}

	public void removeChildFolder(Folder childFolder) {
		childrenFolder.remove(childFolder);
		childFolder.setParentFolder(null);
	}

	public Set<Folder> getChildrenFolder() {
		return childrenFolder;
	}

	public void setChildrenFolder(Set<Folder> childrenFolder) {
		this.childrenFolder = childrenFolder;
	}

	/**
	 * Returns wether the folder has no parent folder.
	 */
	public boolean isRoot() {
		return parentFolder == null;
	}

}
