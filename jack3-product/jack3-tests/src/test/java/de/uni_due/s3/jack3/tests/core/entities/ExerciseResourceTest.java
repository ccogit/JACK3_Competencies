package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.ExerciseResource;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

@NeedsExercise
class ExerciseResourceTest extends AbstractContentTest {

	/**
	 * The size of the original resource content.
	 */
	private static final int ORIGIN_RESOURCE_CONTENT_SIZE = 5;

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();
		exercise.addExerciseResource(new ExerciseResource("Filename", "Content".getBytes(), user, "", false));
		exercise = baseService.merge(exercise);
	}

	/**
	 * Get the only resource in the exercise
	 */
	private ExerciseResource getResource() {
		return exercise.getExerciseResources().iterator().next();
	}

	@Test
	void changeFilename() {
		getResource().setFilename("File.pdf");
		exercise = baseService.merge(exercise);

		assertEquals("File.pdf", getResource().getFilename());
	}

	@Test
	void changeDescription() {
		getResource().setDescription("Description text");
		exercise = baseService.merge(exercise);

		assertEquals("Description text", getResource().getDescription());
	}

	@Test
	void getMimeType() {
		// No file extension -> "application/octet-stream" expected (default value)
		assertEquals("application/octet-stream", getResource().getMimeType());

		// Setting the filename to *.jpg -> "image/jpeg" expected
		getResource().setFilename("File.jpg");
		exercise = baseService.merge(exercise);
		assertEquals("image/jpeg", getResource().getMimeType());

		// Setting the filename to *.txt -> "text/plain" expected
		getResource().setFilename("File.txt");
		exercise = baseService.merge(exercise);
		assertEquals("text/plain", getResource().getMimeType());
	}

	@Test
	void getMediaType() {
		// No file extension ->"application" expected (default value)
		assertEquals("application", getResource().getMediaType());

		// Setting the filename to *.jpg -> "image" expected
		getResource().setFilename("File.jpg");
		exercise = baseService.merge(exercise);
		assertEquals("image", getResource().getMediaType());

		// Setting the filename to *.txt -> "text" expected
		getResource().setFilename("File.txt");
		exercise = baseService.merge(exercise);
		assertEquals("text", getResource().getMediaType());
	}

	/**
	 * This test checks the deep copy of exercise resource.
	 */
	@Test
	void deepCopyOfExerciseResource() {
		ExerciseResource originResource;
		ExerciseResource deepCopyOfResource;
		ExerciseResource tempResource;
		byte[] originContent = new byte[ORIGIN_RESOURCE_CONTENT_SIZE];

		// fill byte to init origin content
		for (int i = 0; i < ORIGIN_RESOURCE_CONTENT_SIZE; i++) {
			originContent[i] = (byte) (i + 2);
		}

		originResource = new ExerciseResource("original file", originContent, user,
				"Deep copy test of " + "exercise resource.", false);

		exercise.addExerciseResource(originResource);
		exercise = baseService.merge(exercise);

		// get and copy last exercise resource of exercise
		tempResource = exercise.getExerciseResources().stream().filter(resource -> resource.equals(originResource))
				.findAny().orElseThrow(() -> new AssertionError("ExerciseResource could not be found"));
		deepCopyOfResource = tempResource.deepCopy();

		assertNotEquals(originResource, deepCopyOfResource, "The deep copy is the origin exercise resource itself.");
		assertEquals("original file", deepCopyOfResource.getFilename(), "The filename of resource is different.");
		assertEquals("Deep copy test of exercise resource.", deepCopyOfResource.getDescription(),
				"The description of resource is different.");
		assertEquals("User@foobar.com", deepCopyOfResource.getLastEditor().getEmail(),
				"The last editor of resource is different.");
		assertEquals(ORIGIN_RESOURCE_CONTENT_SIZE, deepCopyOfResource.getContent().length,
				"The size of resource content is different.");
		// check the content byte by byte
		for (int i = 0; i < deepCopyOfResource.getContent().length; i++) {
			assertEquals(originContent[i], deepCopyOfResource.getContent()[i],
					"The byte " + i + "of the resource content is different.");
		}
	}

	/**
	 * This test checks the deep copy of an empty exercise resource.
	 * The empty resource is defines as a byte[] of zero size.
	 *
	 */
	@Test
	void deepCopyOfExerciseResourceWithEmptyContent() {
		ExerciseResource originResource;
		ExerciseResource deepCopyOfResource;
		ExerciseResource tempResource;
		byte[] originContent = new byte[0];

		originResource = new ExerciseResource("original empty file", originContent, user,
				"Deep copy test of " + "exercise resource.", false);

		exercise.addExerciseResource(originResource);
		exercise = baseService.merge(exercise);

		// get and copy last exercise resource of exercise
		tempResource = exercise.getExerciseResources().stream()
				.filter(resource -> resource.equals(originResource)).findAny()
				.orElseThrow(() -> new AssertionError("ExerciseResource could not be found"));
		deepCopyOfResource = tempResource.deepCopy();

		assertNotEquals(originResource, deepCopyOfResource, "The deep copy is the origin exercise resource itself.");
		assertEquals("original empty file", deepCopyOfResource.getFilename(), "The filename of resource is different.");
		assertEquals("Deep copy test of exercise resource.", deepCopyOfResource.getDescription(),
				"The description of resource is different.");
		assertEquals("User@foobar.com", deepCopyOfResource.getLastEditor().getEmail(),
				"The last editor of resource is different.");
		assertEquals(0, deepCopyOfResource.getContent().length, "The size of resource content is different.");
	}

}
