package de.uni_due.s3.jack3.entities.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.enums.ECourseExercisesOrder;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;

/**
 * Provides a list of exercises that were selected manually by the creator.
 */
@Audited
@Entity
public class FixedListExerciseProvider extends AbstractExerciseProvider
		implements DeepCopyable<FixedListExerciseProvider> {

	private static final long serialVersionUID = 1261774886005658488L;

	/**
	 * List with all exercise entries containing the exercise and a point weight.
	 */
	@ToString
	@OneToMany(
			mappedBy = "abstractExerciseProvider",
			cascade = CascadeType.ALL,
			fetch = FetchType.EAGER,
			orphanRemoval = true)
	@OrderColumn(name = "courseEntry_order")
	@AuditMappedBy(mappedBy = "abstractExerciseProvider", positionMappedBy = "courseEntry_order") // fixes #316
	private List<CourseEntry> courseEntries = new ArrayList<>();

	public FixedListExerciseProvider() {
		// Empty constructor for Hibernate
	}

	@Override
	public List<CourseEntry> getCourseEntries() {
		return Collections.unmodifiableList(courseEntries);
	}

	public List<CourseEntry> getCourseEntriesForReorder() {
		return new ArrayList<>(courseEntries);
	}
	
	public void sortCourseEntries(Comparator<CourseEntry> comparator) {
		courseEntries.sort(comparator);
	}

	public void addCourseEntry(CourseEntry courseEntry) {
		if (courseEntry == null) {
			return;
		}
		courseEntry.setAbstractExerciseProvider(this);
		courseEntries.add(courseEntry);
	}

	public void removeCourseEntry(CourseEntry courseEntry) {
		if (courseEntry == null) {
			return;
		}
		courseEntries.remove(courseEntry);
		courseEntry.setAbstractExerciseProvider(null);
	}

	/**
	 * If the passed Exercise was not already inserted, adds a new Course Entry with 1 point.
	 */
	public void addExerciseIfNotPresent(Exercise exercise) {
		if (courseEntries.stream().noneMatch(ce -> ce.getExercise().equals(exercise))) {
			var newEntry = new CourseEntry(exercise, 1);
			newEntry.setAbstractExerciseProvider(this);
			courseEntries.add(newEntry);
		}
	}

	/**
	 * Removes all Course Entries that are linked to the passed Exercise.
	 */
	public void removeExercise(Exercise exercise) {
		final var foundEntries = courseEntries.stream()
				.filter(ce -> ce.getExercise().equals(exercise))
				.collect(Collectors.toList());
		for (CourseEntry courseEntry : foundEntries) {
			courseEntries.remove(courseEntry);
			courseEntry.setAbstractExerciseProvider(null);
		}
	}

	public void reorderCourseEntry(int from, int to) {
		courseEntries.add(to, courseEntries.remove(from));
	}

	/**
	 * Returns the average difficulty (empty Optional if no course entry exists).
	 */
	public OptionalDouble getAverageDifficulty() {
		return courseEntries.stream().map(CourseEntry::getExercise).mapToInt(Exercise::getDifficulty).average();
	}

	/**
	 * Returns the sum of all point weights (empty Optional if no course entry exists).
	 */
	public OptionalInt getPointSum() {
		if (courseEntries.isEmpty()) {
			return OptionalInt.empty();
		}
		int sum = 0;
		for (CourseEntry entry : courseEntries) {
			sum += entry.getPoints();
		}
		return OptionalInt.of(sum);
	}

	@Override
	public FixedListExerciseProvider deepCopy() {
		FixedListExerciseProvider copy = new FixedListExerciseProvider();
		getCourseEntries().stream() //
				.forEach(oldCourseEntry -> copy.addCourseEntry( //
						new CourseEntry( //
								oldCourseEntry.getExercise(), // 
								oldCourseEntry.getFrozenExercise(), //
								oldCourseEntry.getPoints() //
						)));
		return copy;
	}

	@Override
	public boolean isExerciseOrderSupported(ECourseExercisesOrder order) {
		return order != null;
	}
}
