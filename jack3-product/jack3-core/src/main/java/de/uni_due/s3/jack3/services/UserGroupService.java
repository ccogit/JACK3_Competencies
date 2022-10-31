package de.uni_due.s3.jack3.services;

import static de.uni_due.s3.jack3.services.utils.DBHelper.getOneOrZero;
import static de.uni_due.s3.jack3.services.utils.DBHelper.getOneOrZeroRemovingDuplicates;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;

/**
 * Service for managing {@link UserGroup} entities.
 */
@Stateless
public class UserGroupService extends AbstractServiceBean {
	@Inject
	private BaseService baseService;

	public Optional<UserGroup> getUserGroupByName(String name) {
		final EntityManager em = getEntityManager();
		final TypedQuery<UserGroup> query = em.createNamedQuery(UserGroup.USERGROUP_BY_NAME, UserGroup.class);
		query.setParameter("name", name);
		return getOneOrZero(query);
	}

	/**
	 * Lists all user groups in which a user is a direct or indirect member.
	 * 
	 * @return Unordered user group list with lazy data (member lists)
	 */
	public List<UserGroup> getUserGroupsForUser(User user) {
		final List<UserGroup> resultGroups = new LinkedList<>();

		final List<UserGroup> allGroups = getAllUserGroups();

		for (final UserGroup u : allGroups) {
			if (u.containsUserAsAnyMember(user)) {
				resultGroups.add(u);
			}
		}

		return resultGroups;
	}

	/**
	 * Lists all user groups.
	 * 
	 * @return User group list with lazy data (member lists), ordered by user group name
	 */
	public List<UserGroup> getAllUserGroups() {
		final EntityManager em = getEntityManager();
		final TypedQuery<UserGroup> q = em.createNamedQuery(UserGroup.ALL_USERGROUPS, UserGroup.class);

		return q.getResultList();
	}

	public void persistUserGroup(UserGroup userGroup) {
		baseService.persist(userGroup);
	}

	public Optional<UserGroup> getUserGroupById(long id) {
		return baseService.findById(UserGroup.class, id, false);
	}

	/**
	 * Fetches member user data for a given user group.
	 * 
	 * @return User group entity with lazy group members.
	 * @see #getUserGroupWithLazyData(UserGroup)
	 * @see #getUserGroupWithMemberGroups(UserGroup)
	 */
	public UserGroup getUserGroupWithMemberUsers(UserGroup userGroup) {
		final EntityManager em = getEntityManager();
		final TypedQuery<UserGroup> query = em.createNamedQuery(UserGroup.USERGROUP_WITH_MEMBER_USERS, UserGroup.class);
		query.setParameter("id", userGroup.getId());

		return getOneOrZeroRemovingDuplicates(query).orElseThrow(
				() -> new IllegalStateException("UserGroup is expected to exist in the database."));
	}

	/**
	 * Fetches all lazy data for a given user group.
	 * 
	 * @return User group entity with all lazy data about member users and user groups.
	 */
	public UserGroup getUserGroupWithLazyData(UserGroup userGroup) {
		final EntityManager em = getEntityManager();
		final TypedQuery<UserGroup> query = em.createNamedQuery(UserGroup.USERGROUP_WITH_LAZY_DATA, UserGroup.class);
		query.setParameter("id", userGroup.getId());

		return getOneOrZeroRemovingDuplicates(query).orElseThrow(
				() -> new IllegalStateException("UserGroup is expected to exist in the database."));
	}

	public Optional<UserGroup> getUserGroupWithLazyData(long id) {
		final var query = getEntityManager()
				.createNamedQuery(UserGroup.USERGROUP_WITH_LAZY_DATA, UserGroup.class)
				.setParameter("id", id);
		return getOneOrZero(query);
	}

	/**
	 * Fetches member group data for a given user group.
	 * 
	 * @return User group entity with lazy member groups.
	 * @see #getUserGroupWithLazyData(UserGroup)
	 * @see #getUserGroupWithMemberUsers(UserGroup)
	 */
	public UserGroup getUserGroupWithMemberGroups(UserGroup userGroup) {
		final EntityManager em = getEntityManager();
		final TypedQuery<UserGroup> query = em.createNamedQuery(UserGroup.USERGROUP_WITH_MEMBER_GROUPS, UserGroup.class);
		query.setParameter("id", userGroup.getId());

		return getOneOrZeroRemovingDuplicates(query).orElseThrow(
				() -> new IllegalStateException("UserGroup is expected to exist in the database."));
	}

	public UserGroup mergeUserGroup(UserGroup parentGroup) {
		return baseService.merge(parentGroup);
	}

	public void removeUserGroup(UserGroup group) {
		baseService.deleteEntity(group);
		getLogger().infof("Deleted User Group with name: %s and id: %s", group.getName(), group.getId());
	}

}
