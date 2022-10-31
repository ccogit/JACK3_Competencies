package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Iterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseResource;
import de.uni_due.s3.jack3.entities.tenant.Password;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.tests.annotations.NeedsCourse;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

@NeedsCourse
class CourseResourceTest extends AbstractContentTest {

	/**
	 * The size of the original resource content.
	 */
	private static final int ORIGIN_RESOURCE_CONTENT_SIZE = 5;

	private CourseResource resource = new CourseResource("Filename", "Content".getBytes(), course, null);

	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();

		course.addCourseResource(resource);
		course = baseService.merge(course);
		resource = course.getCourseResources().iterator().next();
	}

	@Test
	void changeFilename() {
		assertEquals("Filename", resource.getFilename());

		resource.setFilename("New Filename");
		course = baseService.merge(course);

		assertEquals("New Filename", resource.getFilename());
	}

	@Test
	void changeCourse() {
		assertEquals(course, resource.getCourse());

		Course newCourse = new Course("New Course");
		baseService.persist(newCourse);

		resource.setCourse(newCourse);
		course.removeCourseResource(resource);
		newCourse.addCourseResource(resource);
		course = baseService.merge(course);
		newCourse = baseService.merge(newCourse);

		assertNotEquals(course, resource.getCourse());
		assertEquals(newCourse, resource.getCourse());
	}

	/**
	 * This test checks the deep copy of course resource.
	 */
	@Test
	void deepCopyOfCourseResource() {
		CourseResource originResource;
		CourseResource deepCopyOfResource;
		CourseResource tempResource = null;
		byte[] originContent = new byte[ORIGIN_RESOURCE_CONTENT_SIZE];
		Course originCourse;
		User courseUser;
		User resourceUser;

		// fill byte to init origin content
		for (int i = 0; i < ORIGIN_RESOURCE_CONTENT_SIZE; i++) {
			originContent[i] = (byte) (i + 2);
		}

		courseUser = new User("Max", "Muster", new Password(), "Max@example.com", true, true);
		userService.persistUser(courseUser);
		originCourse = new Course("Course for deep copy test of course resource");
		baseService.persist(originCourse);
		resourceUser = new User("Tom", "mot", new Password(), "Tom@example.com", true, true);
		userService.persistUser(resourceUser);
		originResource = new CourseResource("original file", originContent, originCourse, resourceUser);
		originResource.setDescription("Deep copy test of course resource.");
		originCourse.addCourseResource(originResource);
		originCourse = baseService.merge(originCourse);

		// get and copy last course resource of original course
		Iterator<CourseResource> it = originCourse.getCourseResources().iterator();
		while (it.hasNext()) {
			tempResource = it.next();
		}
		deepCopyOfResource = tempResource.deepCopy();

		assertNotEquals(originResource, deepCopyOfResource, "The deep copy is the origin course resource itself.");
		assertEquals("original file", deepCopyOfResource.getFilename(), "The filename of resource is different.");
		assertEquals("Deep copy test of course resource.", deepCopyOfResource.getDescription(),
				"The description of resource is different.");
		/*
		 * The course must be null because the reference is set by the
		 * caller and not by the deep copy method.
		 */
		assertNull(deepCopyOfResource.getCourse(), "The course of resource is set.");
		assertEquals("Tom@example.com", deepCopyOfResource.getLastEditor().getEmail(),
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
	 * This test checks the deep copy of an empty course resource.
	 * The empty resource is defines as a byte[] of zero size.
	 */
	@Test
	void deepCopyOfCourseResourceWithEmptyContent() {
		CourseResource originResource;
		CourseResource deepCopyOfResource;
		CourseResource tempResource = null;
		byte[] originContent = new byte[0];
		Course originCourse;
		User courseUser;
		User resourceUser;

		courseUser = new User("Max", "Muster", new Password(), "Max@example.com", true, true);
		userService.persistUser(courseUser);
		originCourse = new Course("Course for deep copy test of empty " + "course resource");
		baseService.persist(originCourse);
		resourceUser = new User("Tom", "mot", new Password(), "Tom@example.com", true, true);
		userService.persistUser(resourceUser);
		originResource = new CourseResource("original empty file", originContent, originCourse, resourceUser);
		originResource.setDescription("Deep copy test of course resource.");
		originCourse.addCourseResource(originResource);
		originCourse = baseService.merge(originCourse);

		// get and copy last exercise resource of exercise
		Iterator<CourseResource> it = originCourse.getCourseResources().iterator();
		while (it.hasNext()) {
			tempResource = it.next();
		}
		deepCopyOfResource = tempResource.deepCopy();

		assertNotEquals(originResource, deepCopyOfResource, "The deep copy is the origin course resource itself.");
		assertEquals("original empty file", deepCopyOfResource.getFilename(), "The filename of resource is different.");
		assertEquals("Deep copy test of course resource.", deepCopyOfResource.getDescription(),
				"The description of resource is different.");
		/*
		 * The course must be null because the reference is set by the
		 * caller and not by the deep copy method.
		 */
		assertNull(deepCopyOfResource.getCourse(), "The course of resource is set.");
		assertEquals("Tom@example.com", deepCopyOfResource.getLastEditor().getEmail(),
				"The last editor of resource is different.");
		assertEquals(0, deepCopyOfResource.getContent().length, "The size of resource content is different.");
	}

}
