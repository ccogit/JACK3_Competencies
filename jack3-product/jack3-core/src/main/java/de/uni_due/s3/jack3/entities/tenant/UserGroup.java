package de.uni_due.s3.jack3.entities.tenant;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.AbstractEntity;

@Audited
@NamedQuery(
		name = UserGroup.USERGROUP_BY_NAME, //
		query = "SELECT ug FROM UserGroup ug WHERE ug.name = :name")
@NamedQuery(
		name = UserGroup.ALL_USERGROUPS,
		query = "SELECT DISTINCT u FROM UserGroup u " //
		+ "LEFT JOIN FETCH u.memberUsers " //
		+ "LEFT JOIN FETCH u.memberGroups " //
		+ "ORDER BY u.name ASC")
@NamedQuery(
		name = UserGroup.USERGROUP_WITH_LAZY_DATA,
		query = "SELECT DISTINCT ug FROM UserGroup ug " //
		+ "LEFT JOIN FETCH ug.memberUsers " //
		+ "LEFT JOIN FETCH ug.memberGroups " //
		+ "WHERE ug.id = :id")
@NamedQuery(
		name = UserGroup.USERGROUP_WITH_MEMBER_USERS,
		query = "SELECT ug FROM UserGroup ug " //
		+ "LEFT JOIN FETCH ug.memberUsers " //
		+ "WHERE ug.id = :id")
@NamedQuery(
		name = UserGroup.USERGROUP_WITH_MEMBER_GROUPS,
		query = "SELECT ug FROM UserGroup ug " //
		+ "LEFT JOIN FETCH ug.memberGroups " //
		+ "WHERE ug.id = :id")
@Entity
public class UserGroup extends AbstractEntity implements Comparable<UserGroup> {

	private static final long serialVersionUID = 7498575957761796440L;

	public static final String USERGROUP_BY_NAME = "UserGroup.userGroupByName";

	public static final String ALL_USERGROUPS = "UserGroup.allUserGroups";

	public static final String USERGROUP_WITH_MEMBER_USERS = "UserGroup.userGroupWithMemberUsers";

	public static final String USERGROUP_WITH_LAZY_DATA = "UserGroup.usergroupWithLazyData";

	/** Name of the query that returns the user group with the member groups. */
	public static final String USERGROUP_WITH_MEMBER_GROUPS = "UserGroup.userGroupWithMemberGroups";

	@Column
	@Type(type = "text")
	String name;

	@Column
	@Type(type = "text")
	String description;

	@ManyToMany
	private Set<User> memberUsers = new HashSet<>();

	@ManyToMany
	private Set<UserGroup> memberGroups = new HashSet<>();

	public UserGroup() {
		super();
	}

	public UserGroup(String name, String description) {
		setName(name);
		this.description = description;
	}

	public void addMemberUser(User user) {
		memberUsers.add(user);
	}

	public void addMemberGroup(UserGroup group) {
		memberGroups.add(group);
	}

	public String getDescription() {
		return description;
	}

	/*
	 * @return unmodifiableSet of memberGroups
	 */
	public Set<UserGroup> getMemberGroups() {
		return Collections.unmodifiableSet(memberGroups);
	}

	/*
	 * @return unmodifiableSet of memberUsers
	 */
	public Set<User> getMemberUsers() {
		return Collections.unmodifiableSet(memberUsers);
	}

	public String getName() {
		return name;
	}

	public int getNumberOfMemberGroups() {
		return memberGroups.size();
	}

	public int getNumberOfMemberUsers() {
		return memberUsers.size();
	}

	public boolean containsUserAsDirectMember(User u) {
		return memberUsers.contains(u);
	}

	public boolean containsGroupAsDirectMember(UserGroup u) {
		return memberGroups.contains(u);
	}

	public boolean containsUserAsAnyMember(User u) {
		if (memberUsers.contains(u)) {
			return true;
		}

		for (final UserGroup g : memberGroups) {
			if (g.containsUserAsAnyMember(u)) {
				return true;
			}
		}

		return false;
	}

	public boolean containsGroupAsAnyMember(UserGroup u) {
		if (memberGroups.contains(u)) {
			return true;
		}

		for (final UserGroup g : memberGroups) {
			if (g.containsGroupAsAnyMember(u)) {
				return true;
			}
		}

		return false;
	}

	public void removeMemberUser(User user) {
		memberUsers.remove(user);
	}

	public void removeMemberGroup(UserGroup group) {
		memberGroups.remove(group);
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setName(String name) {
		this.name = requireIdentifier(name, "The groups name must be non-empty string.");
	}

	@Override
	public int compareTo(UserGroup other) {
		if (other == null) {
			return -1;
		}
		return getName().compareTo(other.getName());
	}
}
