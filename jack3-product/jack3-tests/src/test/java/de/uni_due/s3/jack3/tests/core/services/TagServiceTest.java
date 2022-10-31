package de.uni_due.s3.jack3.tests.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.ejb.EJBTransactionRolledbackException;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.Tag;
import de.uni_due.s3.jack3.services.TagService;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

@NeedsExercise
class TagServiceTest extends AbstractContentTest {

	@Inject
	private TagService tagService;

	/**
	 * Adds a tag to an exercise and return merged exercise with lazy data
	 */
	private AbstractExercise addTagToExercise(AbstractExercise exercise, String name) {
		exercise.addTag(tagService.getOrCreateByName(name));
		exercise = baseService.merge(exercise);
		return exercise;
	}

	/**
	 * Get empty tag list
	 */
	@Test
	void getEmptyTagLists() {
		assertTrue(tagService.getAllTags().isEmpty());
		assertTrue(tagService.getAllTagsAsStrings().isEmpty());
	}

	/**
	 * Add tag to an exercise
	 */
	@Test
	void addTagToExercise() {
		exercise = addTagToExercise(exercise, "foobar");

		Tag tag = new ArrayList<>(exercise.getTags()).get(0);
		Collection<Tag> allTagsFromDB = tagService.getAllTags();

		assertEquals(1, allTagsFromDB.size());
		assertTrue(allTagsFromDB.contains(tag));
		assertTrue(tagService.getAllTagsAsStrings().contains("foobar"));
		assertEquals(tag, tagService.getTagByName("foobar").get());
	}

	/**
	 * Add tags to an exercise and get all tags
	 */
	@Test
	void getAllTags() {
		exercise = addTagToExercise(exercise, "foobar1");
		exercise = addTagToExercise(exercise, "foobar2");

		Collection<Tag> tagList = new ArrayList<>(exercise.getTags());
		Collection<Tag> tagListFromDB = tagService.getAllTags();

		assertEquals(tagList.size(), tagListFromDB.size());
		assertTrue(tagListFromDB.containsAll(tagList));
		assertTrue(tagList.containsAll(tagListFromDB));
	}

	/**
	 * Add tags to an exercise and get all tags as string
	 */
	@Test
	void getAllTagsAsStrings() {
		exercise = addTagToExercise(exercise, "foobar1");
		exercise = addTagToExercise(exercise, "foobar2");

		Collection<String> stringListFromDB = tagService.getAllTags().stream().map(Tag::getName)
				.collect(Collectors.toList());
		Collection<String> stringList = Arrays.asList("foobar1", "foobar2");

		assertEquals(stringList.size(), stringListFromDB.size());
		assertTrue(stringListFromDB.containsAll(stringList));
		assertTrue(stringList.containsAll(stringListFromDB));
	}

	/**
	 * Get available and not available tag by name
	 */
	@Test
	void getTagByString() {
		// should be an empty optional because the tag "foobar" doesn't exist
		assertFalse(tagService.getTagByName("foobar").isPresent());

		// add foobar tag to exercise
		exercise = addTagToExercise(exercise, "foobar");

		// tag from database should be equal to tag from exercise
		assertTrue(tagService.getTagByName("foobar").isPresent());
	}

	/**
	 * Get and create a tag via the "getOrCreate" method
	 */
	@Test
	void getOrCreate() {
		// should NOT exist
		assertFalse(tagService.getTagByName("jackie").isPresent());
		assertEquals(0, baseService.countAll(Tag.class));

		var firstCreated = tagService.getOrCreateByName("jackie");
		assertNotNull(firstCreated);
		assertEquals("jackie", firstCreated.getName());
		assertEquals(1, baseService.countAll(Tag.class));

		var secondCallTag = tagService.getOrCreateByName("jackie");
		assertNotNull(secondCallTag);
		assertEquals("jackie", secondCallTag.getName());
		assertEquals(1, baseService.countAll(Tag.class));
		assertEquals(firstCreated, secondCallTag);
	}

	/**
	 * Persist the same tag twice (->Rollback)
	 */
	@Test
	void persistTagTwice() {
		var persistedTag = new Tag("hello-world");
		baseService.persist(persistedTag);
		assertTrue(tagService.getTagByName("hello-world").isPresent());

		final var existingTag = new Tag("hello-world");
		assertThrows(EJBTransactionRolledbackException.class, () -> {
			baseService.persist(existingTag);
		});

		assertEquals(persistedTag.getId(), tagService.getOrCreateByName("hello-world").getId());
	}
}
