package de.uni_due.s3.jack3.entities.tenant;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.enterprise.inject.spi.CDI;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;

import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.DeepCopyOmitField;
import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.enums.ECourseExercisesOrder;
import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.providers.AbstractExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;
import de.uni_due.s3.jack3.services.RevisionService;

@Audited
@Entity
// REVIEW lg - An dieser Stelle wird "c.folder" (f) nur unvollständig geladen, weil aus dem LEFT-JOIN-FETCH-Ergebnis
// alle (inherited)ManagingUsers-Einträge, die nicht zum angefragten Benutzer passen, wieder entfernt werden.
// Dementsprechend fehlen in 'f' die Rechte aller anderen Benutzer und es könnte beim Abspeichern des so geladenen
// Ordners zum gleichen Fehler wie JACK/jack3-core#467 kommen. Um das zu beheben, müsste man entweder das "FETCH"
// entfernen (Nebeneffekte?), sodass der Ordner gar nicht mehr geladen wird oder eine Unterabfrage (wie z.B. in
// "ContentFolder" eingeführt werden.
// Es sollte sichergestellt werden, dass "c.folder" aus der Abfrage nicht irgendwo wieder abgespeichert wird.
// In "Exercise" gibt es die gleiche Abfrage, nur für Aufgaben - allerdings ohne FETCH. Dies könnte eine Lösung sein.
@NamedQuery(
	name = Course.ALL_COURSES_FOR_USER,
	query = "SELECT c FROM Course c " + "LEFT JOIN FETCH c.folder as f " //
			+ "LEFT JOIN FETCH f.managingUsers mu " //
			+ "LEFT JOIN FETCH f.inheritedManagingUsers imu " //
			+ "WHERE (index(mu) = :user OR index(imu) = :user) " //
			+ "ORDER BY c.name ASC")
@NamedQuery(
	name = Course.ALL_COURSES_FOR_USERGROUP,
	query = "SELECT c FROM Course c " + "LEFT JOIN FETCH c.folder as f " //
			+ "LEFT JOIN FETCH f.managingUserGroups mug " //
			+ "LEFT JOIN FETCH f.inheritedManagingUserGroups imug " //
			+ "WHERE (index(mug) = :group OR index(imug) = :group) " //
			+ "ORDER BY c.name ASC")
@NamedQuery(
	name = Course.COURSE_WITH_LAZY_DATA_BY_COURSE_ID, //
	query = "SELECT DISTINCT c FROM Course c " //
			+ "LEFT JOIN FETCH c.contentProvider " //
			+ "LEFT JOIN FETCH c.resultFeedbackMappings " //
			+ "LEFT JOIN FETCH c.courseResources " //
			+ "WHERE c.id=:id")
@NamedQuery(
	name = Course.ALL_COURSES_FOR_CONTENT_FOLDER_LIST, //
	query = "SELECT c FROM Course c " //
			+ "LEFT JOIN FETCH c.folder AS f " //
			+ "WHERE f IN (:folderList) ORDER BY c.name ASC")

@NamedQuery(
	name = Course.COURSES_REFERENCING_EXERCISE_PROVIDER, //
	query = "SELECT course FROM Course course " //
			+ "INNER JOIN course.contentProvider as contentP " //
			+ "INNER JOIN contentP.courseEntries as courseE " //
			+ "WHERE courseE.exercise.id=:abstractExerciseId")

@NamedQuery(
	name = Course.COURSES_REFERENCING_FOLDER_BY_FOLDER_PROVIDER, //
	query = "SELECT course FROM Course course " //
			+ "INNER JOIN course.contentProvider as folderProvider " //
			+ "INNER JOIN folderProvider.folders as contentFolders " //
			+ "WHERE KEY(contentFolders) IN (:folderList)")

@NamedQuery(
		name = Course.COURSES_REFERENCING_SUBJECT, //
		query = "SELECT course FROM Course course " //
				+ "WHERE course.subject=:subject")
public class Course extends AbstractCourse implements DeepCopyable<Course> {

	/**
	 * Name of the query that returns all courses that are children of a folder in the given folder list.
	 */
	public static final String ALL_COURSES_FOR_CONTENT_FOLDER_LIST = "Course.allCoursesForContentFolderList";

	/**
	 * Name of the query that returns all courses, where the given user has rights on the parent folder, ordered
	 * alphabetically by name.
	 */
	public static final String ALL_COURSES_FOR_USER = "Course.allCoursesForUser";
	/**
	 * Name of the query that returns all courses where the given user group has rights on the parent folder, ordered
	 * alphabetically by name
	 */
	public static final String ALL_COURSES_FOR_USERGROUP = "Course.allCoursesForUserGroup";

	/**
	 * Name of the query that returns the course with lazy data by the given id.
	 */
	public static final String COURSE_WITH_LAZY_DATA_BY_COURSE_ID = "Course.courseWithLazyDataByCourseId";

	/**
	 * Name of the query that returns all courses, which have an exerciseProvider attached and contain the given
	 * exercise
	 */
	public static final String COURSES_REFERENCING_EXERCISE_PROVIDER = "Course.coursesReferencingExerciseProvider";

	/**
	 * Name of the query that returns all courses, which have an folderexerciseProvider attached and contain at least
	 * one of the given
	 * folders
	 */
	public static final String COURSES_REFERENCING_FOLDER_BY_FOLDER_PROVIDER = "Course.coursesReferencingFolderByFolderProvider";

	/**
	 * Name of the query that returns all courses, which have a reference to the given subject
	 */
	public static final String COURSES_REFERENCING_SUBJECT = "Course.coursesReferencingSubject";

	private static final long serialVersionUID = 1L;

	// DO NOT ADD ANY FIELDS HERE (unless it explicitly makes sense to only have in Course, like folder below)!
	// Fields for Course are declared in AbstractCourse.

	@ToString
	@ManyToOne
	@DeepCopyOmitField(copyTheReference = true, reason = "Copying a Course doesn't mean copying the entire folder")
	protected ContentFolder folder;

	public Course() {
		// Only for Hibernate
	}

	/**
	 * (Deep-)Copy constructor
	 *
	 * @param course
	 * @param revisionIndex
	 */
	public Course(Course course, int revisionIndex) {
		super.deepCopyCourseVars(course, revisionIndex);
	}

	public Course(String name) {
		this.name = requireIdentifier(name, "You must specify a non-empty name.");
	}

	@Override
	public void addCourseResource(CourseResource courseResource) {
		courseResources.add(courseResource);
	}

	@Override
	public void addResultFeedbackMapping(ResultFeedbackMapping resultFeedbackMapping) {
		resultFeedbackMappings.add(resultFeedbackMapping);
	}

	@Override
	public void removeResultFeedbackMapping(ResultFeedbackMapping resultFeedbackMapping) {
		resultFeedbackMappings.remove(resultFeedbackMapping);
	}

	@Override
	public AbstractExerciseProvider getContentProvider() {
		return contentProvider;
	}

	@Override
	public ECourseExercisesOrder getExerciseOrder() {
		return exerciseOrder;
	}

	/**
	 * @return unmodifiableList of courseResources
	 */
	@Override
	public Set<CourseResource> getCourseResources() {
		return Collections.unmodifiableSet(courseResources);
	}

	public ContentFolder getFolder() {
		return folder;
	}

	@Override
	public String getExternalDescription() {
		return externalDescription;
	}

	@Override
	public void setExternalDescription(String externalDescription) {
		this.externalDescription = externalDescription;
	}

	@Override
	public String getInternalDescription() {
		return internalDescription;
	}

	@Override
	@Nonnull
	public String getName() {
		return name;
	}

	@Override
	public long getRealCourseId() {
		// We are in a non frozen course, so just returning the id here!
		return getId();
	}

	/**
	 * @return unmodifiableSet of resultFeedbackMappings
	 */
	@Override
	public Set<ResultFeedbackMapping> getResultFeedbackMappings() {
		return Collections.unmodifiableSet(resultFeedbackMappings);
	}

	@Override
	public ECourseScoring getScoringMode() {
		return scoringMode;
	}

	@Override
	public boolean isFrozen() {
		return false;
	}

	@Override
	public boolean isValid() {
		return isValid;
	}

	@Override
	public void removeCourseResource(CourseResource courseResource) {
		courseResources.remove(courseResource);

	}

	@Override
	public void setContentProvider(AbstractExerciseProvider contentProvider) {
		this.contentProvider = contentProvider;
	}

	@Override
	public void setExerciseOrder(ECourseExercisesOrder exerciseOrder) {
		this.exerciseOrder = exerciseOrder;
	}

	// This method needs to be package private
	@Override
	void setFolder(ContentFolder folder) {
		this.folder = folder;
	}

	@Override
	public void setInternalDescription(String internalDescription) {
		this.internalDescription = internalDescription;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	public void setScoringMode(ECourseScoring scoringMode) {
		this.scoringMode = scoringMode;
	}

	@Override
	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	@Override
	public boolean isFromEnvers() {
		return isFromEnvers;
	}

	public void setFromEnvers(boolean isFromEnvers) {
		this.isFromEnvers = isFromEnvers;
	}

	@Override
	public String getLanguage() {
		return language;
	}

	@Override
	public void setLanguage(String language) {
		this.language = language;
	}

	@Override
	public Subject getSubject() {
		return subject;
	}

	@Override
	public void setSubject(Subject subject) {
		this.subject = subject;
	}

	@Override
	public Course deepCopy() {
		RevisionService revisionService = CDI.current().select(RevisionService.class).get();
		Course copy = new Course(this, revisionService.getProxiedOrLastPersistedRevisionId(this));

		if (getContentProvider() instanceof FixedListExerciseProvider) {
			FixedListExerciseProvider fixedListExerciseProvider = (FixedListExerciseProvider) getContentProvider();
			copy.setContentProvider(fixedListExerciseProvider.deepCopy());
		} else if (getContentProvider() instanceof FolderExerciseProvider) {
			FolderExerciseProvider folderExerciseProvider = (FolderExerciseProvider) getContentProvider();
			copy.setContentProvider(folderExerciseProvider.deepCopy());
		} else if (getContentProvider() == null) {
			copy.setContentProvider(null);
		} else {
			throw new UnsupportedOperationException();
		}
		return copy;
	}

}