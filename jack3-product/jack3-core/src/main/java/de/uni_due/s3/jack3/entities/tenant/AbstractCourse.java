package de.uni_due.s3.jack3.entities.tenant;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.CDI;
import javax.persistence.*;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.hibernate.proxy.HibernateProxy;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.enums.ECourseExercisesOrder;
import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.enums.EDeepCopyExceptionErrorCode;
import de.uni_due.s3.jack3.entities.providers.AbstractExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.exceptions.DeepCloningException;
import de.uni_due.s3.jack3.interfaces.Namable;
import de.uni_due.s3.jack3.multitenancy.LoggerProvider;
import de.uni_due.s3.jack3.services.CourseService;
import de.uni_due.s3.jack3.services.ExerciseService;

@NamedQuery(
	name = AbstractCourse.ABSTRACTCOURSES_REFERENCING_EXERCISE_BY_EXERCISE_PROVIDER, //
	query = "SELECT abstractC FROM AbstractCourse abstractC " //
			+ "INNER JOIN abstractC.contentProvider as contentP " //
			+ "INNER JOIN contentP.courseEntries as courseE " //
			+ "WHERE courseE.exercise.id=:abstractExerciseId")
@NamedQuery(
	name = AbstractCourse.ABSTRACTCOURSES_REFERENCING_FOLDER_BY_FOLDER_PROVIDER, //
	query = "SELECT abstractC FROM AbstractCourse abstractC " //
			+ "INNER JOIN abstractC.contentProvider as folderProvider " //
			+ "INNER JOIN folderProvider.folders as contentFolders " //
			+ "WHERE KEY(contentFolders) IN (:folderList)")
@Audited
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Entity
public abstract class AbstractCourse extends AbstractEntity implements Namable {

	private static final long serialVersionUID = 4190837048053004812L;

	/**
	 * Name of the query that returns all abstract courses, which have an folderexerciseProvider attached and reference
	 * at least one of the given folders
	 */
	public static final String ABSTRACTCOURSES_REFERENCING_FOLDER_BY_FOLDER_PROVIDER = "AbstractCourse.courseReferencingExerciseByFolderProvider";
	/**
	 * Name of the query that returns all abstract courses, which have an exerciseProvider attached and contain the
	 * given exercise
	 */
	public static final String ABSTRACTCOURSES_REFERENCING_EXERCISE_BY_EXERCISE_PROVIDER = "AbstractCourse.courseReferencingExerciseByExerciseProvider";

	// Adding new fields here requires to update the constructor of FrozenCourse!

	@Column
	@Type(type = "text")
	protected String externalDescription;

	@ToString
	@OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
	protected AbstractExerciseProvider contentProvider;

	/**
	 * Order in which the exercises are presented to the user initially.
	 */
	@Enumerated(EnumType.STRING)
	protected ECourseExercisesOrder exerciseOrder;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	protected Set<CourseResource> courseResources = new HashSet<>();

	@Column
	@Type(type = "text")
	protected String internalDescription;

	@Column
	protected boolean isValid;

	@Column
	@Type(type = "text")
	protected String language;

	@ToString
	@Column(nullable = false)
	@Type(type = "text")
	protected String name;

	@ManyToMany(cascade = CascadeType.ALL)
	protected Set<ResultFeedbackMapping> resultFeedbackMappings = new HashSet<>();

	@ManyToOne
	protected Subject subject;

	@Enumerated(EnumType.STRING)
	protected ECourseScoring scoringMode;

	@Transient
	protected boolean isFromEnvers = false;

	protected AbstractCourse() {

	}

	public abstract void addCourseResource(CourseResource courseResource);

	public abstract void addResultFeedbackMapping(ResultFeedbackMapping resultFeedbackMapping);

	public abstract void removeResultFeedbackMapping(ResultFeedbackMapping resultFeedbackMapping);

	public abstract AbstractExerciseProvider getContentProvider();

	public abstract ECourseExercisesOrder getExerciseOrder();

	/**
	 * @return unmodifiableList of courseResources
	 */
	public abstract Set<CourseResource> getCourseResources();

	public abstract String getInternalDescription();

	/**
	 * @return unmodifiableSet of resultFeedbackMappings
	 */
	public abstract Set<ResultFeedbackMapping> getResultFeedbackMappings();

	public abstract boolean isValid();

	public abstract void removeCourseResource(CourseResource courseResource);

	public abstract void setContentProvider(AbstractExerciseProvider contentProvider);

	public abstract void setExerciseOrder(ECourseExercisesOrder exerciseOrder);

	// This method needs to be package private
	abstract void setFolder(ContentFolder folder);

	public abstract void setInternalDescription(String internalDescription);

	public abstract void setName(String name);

	public abstract void setValid(boolean isValid);

	public abstract boolean isFrozen();

	public abstract ECourseScoring getScoringMode();

	public abstract long getRealCourseId();

	public abstract boolean isFromEnvers();

	public abstract String getLanguage();

	public abstract void setLanguage(String language);

	public abstract String getExternalDescription();

	public abstract void setExternalDescription(String externalDescription);

	public abstract Subject getSubject();

	public abstract void setSubject(Subject subject);

	protected void deepCopyCourseVars(Course other, int proxiedCourseRevisionId) {
		if (this instanceof FrozenCourse) {
			((FrozenCourse) this).proxiedCourseId = other.getId();
			((FrozenCourse) this).proxiedCourseRevisionId = proxiedCourseRevisionId;
		}
		CourseService courseService = CDI.current().select(CourseService.class).get();

		Course courseRevisionFromEnvers = (Course) courseService //
				.getRevisionOfCourseWithLazyData(other, proxiedCourseRevisionId) //
				.orElseThrow(IllegalArgumentException::new);

		if (this instanceof FrozenCourse) {
			// When doing a FrozenCopy we need to handle the ExerciseProviders slightly differently than when cloning a
			// regular exercise. Since FrozenExercises must only contain FrozenExercises and a FixedListExerciseProvider.
			// Also we don't just ignore refernces to meanwhile deleted Exercises (Frozenexercises are not deleted atm).
			handleExerciseProviderDeepCopy(courseRevisionFromEnvers);
		} else {
			// We need a folder when deep copying regular Exercises. Since "other" is the course at the latest revision,
			// it is save to use its folder here
			((Course) this).folder = other.getFolder();
		}

		exerciseOrder = courseRevisionFromEnvers.getExerciseOrder();

		for (CourseResource courseResource : courseRevisionFromEnvers.getCourseResources()) {
			CourseResource courseResourceDeepCopy = courseResource.deepCopy();
			courseResourceDeepCopy.setCourse(this);
			courseResources.add(courseResourceDeepCopy);
		}

		externalDescription = courseRevisionFromEnvers.getExternalDescription();
		internalDescription = courseRevisionFromEnvers.getInternalDescription();
		isValid = courseRevisionFromEnvers.isValid();

		name = courseRevisionFromEnvers.getName();

		for (ResultFeedbackMapping resultFeedbackMapping : courseRevisionFromEnvers.getResultFeedbackMappings()) {
			resultFeedbackMappings.add(resultFeedbackMapping.deepCopy());
		}

		scoringMode = courseRevisionFromEnvers.getScoringMode();
		language = courseRevisionFromEnvers.getLanguage();
	}

	private void handleExerciseProviderDeepCopy(Course courseRevisionFromEnvers) {
		// TODO comment and refactor!
		Object abstractExerciseProvider = Hibernate.unproxy(courseRevisionFromEnvers.getContentProvider());

		if (!(abstractExerciseProvider instanceof FixedListExerciseProvider)) {
			throw new DeepCloningException("Only Courses with FixedListExerciseProvider support freezing, '"
					+ (abstractExerciseProvider == null ? "null" : abstractExerciseProvider.getClass().getSimpleName())
					+ "' given!", EDeepCopyExceptionErrorCode.ONLY_FIXEDLIST_EXERCISEPROVIDER_ALLOWED);
		}
		FixedListExerciseProvider fixedListExerciseProvider = (FixedListExerciseProvider) abstractExerciseProvider;
		FixedListExerciseProvider contentProviderDeepCopy = new FixedListExerciseProvider();

		for (CourseEntry currentCourseEntry : fixedListExerciseProvider.getCourseEntries()) {
			handleCourseEntry(contentProviderDeepCopy, currentCourseEntry);
		}

		contentProvider = contentProviderDeepCopy;
	}

	private void handleCourseEntry(FixedListExerciseProvider contentProviderDeepCopy, CourseEntry currentCourseEntry) {
		FrozenExercise frozenExercise = currentCourseEntry.getFrozenExercise();
		if (frozenExercise == null) {
			throw new DeepCloningException("Frozen courses must only contain frozen Exercises, '"
					+ currentCourseEntry.getExercise() + "' given",
					EDeepCopyExceptionErrorCode.ONLY_FROZEN_EXERCISES_IN_FROZENCOURSES_ALLOWED);
		}
		if (frozenExercise instanceof HibernateProxy) {
			frozenExercise = (FrozenExercise) ((HibernateProxy) frozenExercise).getHibernateLazyInitializer()
					.getImplementation();
		}

		int exerciseRevisionId = frozenExercise.getProxiedExerciseRevisionId();

		ExerciseService exerciseService = CDI.current().select(ExerciseService.class).get();

		FrozenExercise frozenMainDB = exerciseService.getFrozenExerciseWithLazyDataById(frozenExercise.getId())
				.orElseGet(() -> {
					LoggerProvider.get(getClass())
					.warn("FrozenExercise has been deleted in" + " the main DB, freeze again");
					FrozenExercise recreatedFrozenExercise = new FrozenExercise(currentCourseEntry.getExercise(),
							exerciseRevisionId);
					recreatedFrozenExercise = exerciseService.mergeFrozenExercise(recreatedFrozenExercise);
					recreatedFrozenExercise.generateSuffixWeights();
					return exerciseService.mergeFrozenExercise(recreatedFrozenExercise);
				});
		Exercise correspondingExercise = exerciseService
				.getExerciseByIdWithLazyData(frozenMainDB.getProxiedOrRegularExerciseId()).orElseThrow();


		CourseEntry courseEntry = new CourseEntry(correspondingExercise, frozenMainDB, currentCourseEntry.getPoints());

		contentProviderDeepCopy.addCourseEntry(courseEntry);
	}

}
