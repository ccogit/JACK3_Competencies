package de.uni_due.s3.jack3.entities.tenant;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;

/**
 * This class represents a presentation folder, i.e. a folder that can contain course offers and other presentation
 * folders.
 *
 * @see ContentFolder
 */
@NamedQuery(
		name = PresentationFolder.ALL_PRESENTATION_FOLDERS,
		query = "SELECT pf FROM PresentationFolder pf " //
		+ "ORDER BY pf.name ASC")
// Note: We have to use subqueries here because a simple LEFT JOIN FETCH only includes the folder and the right for the
// specific user. This turns into a problem when we store the user fetched in this way in the database again.
// See JACK/jack3-core#467 for explanation.
@NamedQuery(
		name = PresentationFolder.ALL_PRESENTATION_FOLDERS_FOR_USER,
		query = "SELECT DISTINCT p FROM PresentationFolder p " //
		+ "LEFT JOIN FETCH p.managingUsers " //
		+ "LEFT JOIN FETCH p.inheritedManagingUsers " //
		+ "LEFT JOIN FETCH p.managingUserGroups " //
		+ "LEFT JOIN FETCH p.inheritedManagingUserGroups " //
		+ "WHERE p.id IN (" //
		+ "SELECT p2.id FROM PresentationFolder p2 " //
		+ "LEFT JOIN p2.managingUsers mu " //
		+ "LEFT JOIN p2.inheritedManagingUsers imu " //
		+ "WHERE (index(mu) = :user OR index(imu) = :user)) " //
		+ "ORDER BY p.name ASC")
@NamedQuery(
		name = PresentationFolder.ALL_PRESENTATION_FOLDERS_FOR_USERGROUP,
		query = "SELECT DISTINCT p FROM PresentationFolder p " //
		+ "LEFT JOIN FETCH p.managingUsers " //
		+ "LEFT JOIN FETCH p.inheritedManagingUsers " //
		+ "LEFT JOIN FETCH p.managingUserGroups " //
		+ "LEFT JOIN FETCH p.inheritedManagingUserGroups " //
		+ "WHERE p.id IN (" //
		+ "SELECT p2.id FROM PresentationFolder p2 " //
		+ "LEFT JOIN p2.managingUserGroups mu " //
		+ "LEFT JOIN p2.inheritedManagingUserGroups imu " //
		+ "WHERE (index(mu) = :usergroup OR index(imu) = :usergroup)) " //
		+ "ORDER BY p.name ASC")
@NamedQuery(
		name = PresentationFolder.PRESENTATION_FOLDER_BY_ID,
		query = "SELECT pf FROM PresentationFolder pf " //
		+ "WHERE pf.id = :id")
@NamedQuery(
		name = PresentationFolder.PRESENTATION_FOLDER_WITH_LAZY_DATA,
		query = "SELECT pf FROM PresentationFolder pf " //
		+ "LEFT JOIN FETCH pf.childrenFolder " //
		+ "LEFT JOIN FETCH pf.childrenCourseOffer " //
		+ "LEFT JOIN FETCH pf.managingUsers " //
		+ "LEFT JOIN FETCH pf.inheritedManagingUsers " //
		+ "LEFT JOIN FETCH pf.managingUserGroups " //
		+ "LEFT JOIN FETCH pf.inheritedManagingUserGroups " //
		+ "WHERE pf.id = :id")
@NamedQuery(
		name = PresentationFolder.QUERY_COUNT, //
		query = "SELECT COUNT (pf) FROM PresentationFolder pf")
@NamedQuery(
		name = PresentationFolder.QUERY_ROOT,
		query = "SELECT p FROM PresentationFolder p WHERE p.parentFolder is null")
@NamedQuery(
	name = PresentationFolder.ALL_CHILDREN,
	query = "SELECT pf FROM PresentationFolder pf WHERE pf.parentFolder = :parent")
@Audited
@Entity
public class PresentationFolder extends Folder {

	/** Generated serial version UID. */
	private static final long serialVersionUID = 3531067552257356446L;

	/** Name of the query that returns all presentation folders. */
	public static final String ALL_PRESENTATION_FOLDERS = "PresentationFolder.allPresentationFolders";

	/**
	 * Name of the query that returns all presentation folders for given user.
	 */
	public static final String ALL_PRESENTATION_FOLDERS_FOR_USER = "PresentationFolder.allPresentationFoldersForUser";

	public static final String ALL_PRESENTATION_FOLDERS_FOR_USERGROUP = "PresentationFolder.allPresentationFoldersForUserGroup";

	public static final String PRESENTATION_FOLDER_BY_ID = "PresentationFolder.presentationFolderById";

	public static final String PRESENTATION_FOLDER_WITH_LAZY_DATA = "PresentationFolder.contentFolderWithLazyData";

	public static final String QUERY_COUNT = "PresentationFolder.queryCount";

	public static final String QUERY_ROOT = "PresentationFolder.queryRoot";

	public static final String ALL_CHILDREN = "PresentationFolder.allChildren";

	@Column(nullable = false, columnDefinition = "boolean default false")
	private boolean containsLinkedCourses;

	@OneToMany(mappedBy = "folder", fetch = FetchType.LAZY)
	private Set<CourseOffer> childrenCourseOffer = new HashSet<>();

	public PresentationFolder() {
		super();
	}

	public PresentationFolder(String name) {
		super(name);
	}

	public void addChildCourseOffer(CourseOffer courseOffer) {
		childrenCourseOffer.add(courseOffer);
		courseOffer.setFolder(this);
	}

	public void removeCourseOffer(CourseOffer courseOffer) {
		childrenCourseOffer.remove(courseOffer);
		courseOffer.setFolder(null);
	}

	public Set<CourseOffer> getChildrenCourseOffer() {
		return Collections.unmodifiableSet(childrenCourseOffer);
	}

	public boolean isContainsLinkedCourses() {
		return containsLinkedCourses;
	}

	public void setContainsLinkedCourses(boolean containsLinkedCourses) {
		this.containsLinkedCourses = containsLinkedCourses;
	}
}
