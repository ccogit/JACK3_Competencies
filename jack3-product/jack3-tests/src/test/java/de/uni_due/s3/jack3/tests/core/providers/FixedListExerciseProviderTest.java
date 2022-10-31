package de.uni_due.s3.jack3.tests.core.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.tests.annotations.NeedsCourse;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

@NeedsExercise
@NeedsCourse
class FixedListExerciseProviderTest extends AbstractContentTest {

	private FixedListExerciseProvider provider;

	/**
	 * Creates some test course entries
	 * <ul>
	 * <li>Exercise A - 3 points - difficulty 10</li>
	 * <li>Exercise B - 2 points - difficulty 5</li>
	 * <li>Exercise C - 8 points - difficulty 70</li>
	 * <li>Exercise D - 1 points - difficulty 20</li>
	 * </ul>
	 */
	private List<CourseEntry> createTestCourseEntries() {

		List<CourseEntry> entries = new ArrayList<>(4);
		entries.add(new CourseEntry(new ExerciseBuilder("A").withDifficulty(10).create(), 3));
		entries.add(new CourseEntry(new ExerciseBuilder("B").withDifficulty(5).create(), 2));
		entries.add(new CourseEntry(new ExerciseBuilder("C").withDifficulty(70).create(), 8));
		entries.add(new CourseEntry(new ExerciseBuilder("D").withDifficulty(20).create(), 1));

		entries.stream().map(CourseEntry::getExercise).forEach(baseService::persist);

		return entries;
	}

	private FixedListExerciseProvider merge() {
		course.setContentProvider(provider);
		course = baseService.merge(course);
		return (FixedListExerciseProvider) course.getContentProvider();
	}

	private List<CourseEntry> sortList(FixedListExerciseProvider provider, Comparator<CourseEntry> comparator) {
		List<CourseEntry> entries = new ArrayList<>(provider.getCourseEntries());
		entries.sort(comparator);
		return entries;
	}

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();
		provider = new FixedListExerciseProvider();
		provider = merge();
	}

	@Test
	void getEmptyEntryList() {
		assertTrue(provider.getCourseEntries().isEmpty());
	}

	@Test
	void addCourseEntry() {
		provider.addCourseEntry(new CourseEntry(exercise, 100));
		provider = merge();
		assertEquals(1, provider.getCourseEntries().size());
		assertEquals(exercise, provider.getCourseEntries().get(0).getExercise());
		assertEquals(100, provider.getCourseEntries().get(0).getPoints());
	}

	@Test
	void removeCourseEntry() {
		provider.addCourseEntry(new CourseEntry(exercise, 100));
		provider = merge();
		assertEquals(1, provider.getCourseEntries().size());

		provider.removeCourseEntry(provider.getCourseEntries().get(0));
		provider = merge();
		assertTrue(provider.getCourseEntries().isEmpty());
	}

	@Test
	void calculateAvgDifficulty() {
		
		// Empty provider
		assertTrue(provider.getAverageDifficulty().isEmpty());
		
		// Only one entry
		exercise.setDifficulty(15);
		provider = new FixedListExerciseProvider();
		provider.addCourseEntry(new CourseEntry(exercise, 27));
		provider = merge();
		assertEquals(15, provider.getAverageDifficulty().getAsDouble(), 0.0001);
		
		// Test course entries => 26,25 average difficulty
		provider = new FixedListExerciseProvider();
		createTestCourseEntries().stream().forEach(entry -> provider.addCourseEntry(entry));
		provider = merge();
		assertEquals(26.25, provider.getAverageDifficulty().getAsDouble(), 0.0001);
	}

	@Test
	void calculatePointSum() {

		// Empty provider
		assertTrue(provider.getPointSum().isEmpty());

		// Only one entry
		provider = new FixedListExerciseProvider();
		provider.addCourseEntry(new CourseEntry(exercise, 27));
		provider = merge();
		assertEquals(27, provider.getPointSum().getAsInt());

		// Test course entries => 14 poins
		provider = new FixedListExerciseProvider();
		createTestCourseEntries().stream().forEach(entry -> provider.addCourseEntry(entry));
		assertEquals(14, provider.getPointSum().getAsInt());
	}

	@Test
	void sortCourseEntriesManually() {
		Exercise[] exercises = new Exercise[4];
		exercises[0] = new ExerciseBuilder("Exercise A").create();
		exercises[1] = new ExerciseBuilder("Exercise B").create();
		exercises[2] = new ExerciseBuilder("Exercise C").create();
		exercises[3] = new ExerciseBuilder("Exercise D").create();
		for (Exercise exercise : exercises) {
			baseService.persist(exercise);
		}

		// Add exercise: (C) -> (C,A) -> (C,A,B) -> (C,A,B,D)
		provider.addCourseEntry(new CourseEntry(exercises[2], 1));
		provider.addCourseEntry(new CourseEntry(exercises[0], 1));
		provider.addCourseEntry(new CourseEntry(exercises[1], 1));
		provider.addCourseEntry(new CourseEntry(exercises[3], 1));
		provider = merge();

		// Reorder: (C,A,B,D) -> (A,C,B,D) -> (A,B,C,D)
		provider.reorderCourseEntry(1, 0);
		provider.reorderCourseEntry(1, 2);

		// Check correct order
		assertEquals(
				Arrays.asList(exercises),
				provider.getCourseEntries().stream().map(CourseEntry::getExercise).collect(Collectors.toList()));
	}
}
