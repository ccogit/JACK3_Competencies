package de.uni_due.s3.jack3.entities.tenant;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;

/**
 * This class represents a content folder, i.e. a folder that is capable of holding exercises, courses and other content
 * folders.
 *
 * @see PresentationFolder
 */
// Note: We have to use subqueries here because a simple LEFT JOIN FETCH only includes the folder and the right for the
// specific user. This turns into a problem when we store the user fetched in this way in the database again.
// See JACK/jack3-core#467 for explanation.
@NamedQuery(
		name = ContentFolder.ALL_CONTENT_FOLDERS_FOR_USER, //
		query = "SELECT DISTINCT cf FROM ContentFolder cf " //
		+ "LEFT JOIN FETCH cf.managingUsers " //
		+ "LEFT JOIN FETCH cf.inheritedManagingUsers " //
		+ "LEFT JOIN FETCH cf.managingUserGroups " //
		+ "LEFT JOIN FETCH cf.inheritedManagingUserGroups " //
		+ "WHERE cf.id IN (" //
		+ "SELECT cf2.id FROM ContentFolder cf2 " //
		+ "LEFT JOIN cf2.managingUsers mu " //
		+ "LEFT JOIN cf2.inheritedManagingUsers imu " //
		+ "WHERE (index(mu) = :user OR index(imu) = :user)) " //
		+ "ORDER BY cf.name ASC")
@NamedQuery(
		name = ContentFolder.ALL_CONTENT_FOLDERS_FOR_USERGROUP,
		query = "SELECT DISTINCT cf FROM ContentFolder cf " //
		+ "LEFT JOIN FETCH cf.managingUsers " //
		+ "LEFT JOIN FETCH cf.inheritedManagingUsers " //
		+ "LEFT JOIN FETCH cf.managingUserGroups " //
		+ "LEFT JOIN FETCH cf.inheritedManagingUserGroups " //
		+ "WHERE cf.id IN (" //
		+ "SELECT cf2.id FROM ContentFolder cf2 " //
		+ "LEFT JOIN cf2.managingUserGroups mu " //
		+ "LEFT JOIN cf2.inheritedManagingUserGroups imu " //
		+ "WHERE (index(mu) = :usergroup OR index(imu) = :usergroup)) " //
		+ "ORDER BY cf.name ASC")
@NamedQuery(
		name = ContentFolder.CONTENT_FOLDER_BY_ID,
		query = "SELECT cf FROM ContentFolder cf " //
		+ "WHERE cf.id = :id")
@NamedQuery(
		name = ContentFolder.CONTENT_FOLDER_WITH_LAZY_DATA,
		query = "SELECT cf FROM ContentFolder cf " //
		+ "LEFT JOIN FETCH cf.childrenFolder " //
		+ "LEFT JOIN FETCH cf.childrenCourses " //
		+ "LEFT JOIN FETCH cf.childrenExercises " //
		+ "LEFT JOIN FETCH cf.managingUsers " //
		+ "LEFT JOIN FETCH cf.inheritedManagingUsers " //
		+ "LEFT JOIN FETCH cf.managingUserGroups " //
		+ "LEFT JOIN FETCH cf.inheritedManagingUserGroups " //
		+ "WHERE cf.id = :id")
@NamedQuery(
		name = ContentFolder.QUERY_COUNT, //
		query = "SELECT COUNT (cf) " //
		+ "FROM ContentFolder cf")
@NamedQuery(
		name = ContentFolder.QUERY_ROOT, //
		query = "SELECT c FROM ContentFolder c " //
		+ "WHERE c.parentFolder is null")
@NamedQuery(
	name = ContentFolder.ALL_CHILDREN,
	query = "SELECT c FROM ContentFolder c WHERE c.parentFolder = :parent")
@NamedQuery(
	name = ContentFolder.CONTENTFOLDER_BY_EXERCISE, //
	query = "SELECT folder FROM Exercise e WHERE e = :exercise")

@Audited
@Entity
public class ContentFolder extends Folder {

	/** Name of a personal folder */
	public static final String PERSONAL_FOLDER_NAME = "personalFolder";

	/** Generated serial version UID. */
	private static final long serialVersionUID = 8349626561224559671L;

	/** Name of the query that returns all content folders for a given user. */
	public static final String ALL_CONTENT_FOLDERS_FOR_USER = "ContentFolder.allContentFoldersForUser";

	/**
	 * Name of the query that returns all content folders for the given user group.
	 */
	public static final String ALL_CONTENT_FOLDERS_FOR_USERGROUP = "ContentFolder.allContentFoldersForUserGroup";

	public static final String CONTENT_FOLDER_BY_ID = "ContentFolder.contentFolderById";

	/**
	 * Name of the query that returns the content folder with lazy data of the given folder.
	 */
	public static final String CONTENT_FOLDER_WITH_LAZY_DATA = "ContentFolder.contentFolderWithLazyData";

	public static final String QUERY_COUNT = "ContentFolder.queryCount";

	/** This is the name of the query that returns the root content folder. */
	public static final String QUERY_ROOT = "ContentFolder.queryRoot";

	/** Name of the query that returns all children folder of the given folder. */
	public static final String ALL_CHILDREN = "ContentFolder.allChildren";

	public static final String CONTENTFOLDER_BY_EXERCISE = "ContentFolder.contentFolderByExercise";

	// It seems we are running into a limitation of Hibernate here. For our specific case targetEntity should work,
	// since FrozenCourses will never be in a ContentFolder anyway. See:
	// https://stackoverflow.com/a/31876253
	@OneToMany(mappedBy = "folder", fetch = FetchType.LAZY, targetEntity = Course.class)
	private Set<AbstractCourse> childrenCourses = new HashSet<>();

	//
	@OneToMany(mappedBy = "folder", fetch = FetchType.LAZY, targetEntity = Exercise.class)
	private Set<AbstractExercise> childrenExercises = new HashSet<>();

	public ContentFolder() {
		super();
	}

	public ContentFolder(String name) {
		super(name);
	}

	public void addChildCourse(AbstractCourse course) {
		if (childrenCourses.contains(course)) {
			return;
		}
		childrenCourses.add(course);
		course.setFolder(this);
	}

	public void removeChildCourse(AbstractCourse course) {
		if (!childrenCourses.contains(course)) {
			return;
		}
		childrenCourses.remove(course);
		course.setFolder(null);
	}

	public void addChildExercise(AbstractExercise exercise) {
		if (childrenExercises.contains(exercise)) {
			return;
		}
		childrenExercises.add(exercise);
		exercise.setFolder(this);
	}

	public void removeChildExercise(AbstractExercise exercise) {
		if (!childrenExercises.contains(exercise)) {
			return;
		}
		childrenExercises.remove(exercise);
		exercise.setFolder(null);
	}

	/*
	 * @return unmodifiableSet of childrenExercises
	 */
	public Set<AbstractExercise> getChildrenExercises() {
		return Collections.unmodifiableSet(childrenExercises);
	}

	/*
	 * * @return unmodifiableSet of childrenCourses
	 */
	public Set<AbstractCourse> getChildrenCourses() {
		return Collections.unmodifiableSet(childrenCourses);
	}

}
