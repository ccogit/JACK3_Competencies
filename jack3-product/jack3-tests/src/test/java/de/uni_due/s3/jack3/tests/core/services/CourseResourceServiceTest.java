package de.uni_due.s3.jack3.tests.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRolledbackException;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.CourseResource;
import de.uni_due.s3.jack3.services.CourseResourceService;
import de.uni_due.s3.jack3.tests.annotations.NeedsCourse;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

@NeedsCourse
class CourseResourceServiceTest extends AbstractContentTest {

    @Inject
    private CourseResourceService resourceService;

    /**
     * Create and persist 3 course resources
     */
    private CourseResource[] createCourseResources() {

        CourseResource[] allCourseResources = new CourseResource[] {
                new CourseResource("Resource 1.pdf", "Content".getBytes(), course, null),
                new CourseResource("Resource 2.pdf", "Content".getBytes(), course, null),
                new CourseResource("Resource 3.pdf", "Content".getBytes(), course, null) };

        // persist course resources
        for (CourseResource resource : allCourseResources) {
			baseService.persist(resource);
            course.addCourseResource(resource);
			course = baseService.merge(course);
        }

        return allCourseResources;
    }

    /**
     * Tests if a new course has no resources
     */
    @Test
    void getEmptyResourceList() {

        // there should be no resources at all
		assertTrue(resourceService.getAllCourseResourcesForCourse(course).isEmpty());
		assertTrue(course.getCourseResources().isEmpty());
    }

    /**
     * Tests if resource is found by course
     */
    @Test
    void getResourceForCourse() {

        CourseResource resource = new CourseResource("Resource.pdf", "Content".getBytes(), course, null);
		baseService.persist(resource);

        course.addCourseResource(resource);
		course = baseService.merge(course);

        // Resource should be found from resourceService
		assertEquals(Arrays.asList(resource), resourceService.getAllCourseResourcesForCourse(course));

        // Resource should be found from course
		assertEquals(resource, course.getCourseResources().iterator().next());
    }

    /**
     * Tests if all resources are found by course
     */
    @Test
    void getAllResourcesForCourse() {

        // create List of resources and persist each item
        CourseResource[] resources = createCourseResources();

        // All resources should be persisted and found
        Collection<CourseResource> getCourseResourcesFromService = resourceService
                .getAllCourseResourcesForCourse(course);
        Collection<CourseResource> getCourseResourcesFromCourse = course.getCourseResources();

        // List from service should be correct
		assertTrue(getCourseResourcesFromService.containsAll(Arrays.asList(resources)));
		assertTrue(Arrays.asList(resources).containsAll(getCourseResourcesFromService));

        // Lists from service and from course should be equal
		assertTrue(getCourseResourcesFromService.containsAll(getCourseResourcesFromCourse));
		assertTrue(getCourseResourcesFromCourse.containsAll(getCourseResourcesFromService));
    }

    /**
     * Find resources by name
     */
    @Test
    void getCourseResourceByFilename() {

        // create List of resources and persist each item
        CourseResource[] resources = createCourseResources();

        // find all resources
        for (CourseResource courseResource : resources) {
			var res = resourceService.getCourseResourceForCourseByFilename(courseResource.getFilename(),
					course.getId());
			assertTrue(res.isPresent());
			assertEquals(courseResource, res.get());
        }
    }

    /**
     * Remove resource
     */
    @Test
    void removeCourseResource() {

        CourseResource resource = new CourseResource("Resource.pdf", "Content".getBytes(), course, null);

        course.addCourseResource(resource);
		course = baseService.merge(course);

        // remove resource
        course.removeCourseResource(resource);
		course = baseService.merge(course);

		// The resource is already deleted.
		assertThrows(EJBException.class, () -> baseService.deleteEntity(resource));

		assertTrue(
				resourceService.getCourseResourceForCourseByFilename("Resource.pdf", course.getId()).isEmpty());
		assertTrue(resourceService.getAllCourseResourcesForCourse(course).isEmpty());
		assertTrue(course.getCourseResources().isEmpty());
    }

    /**
     * Create duplicate resources -> EJBTransactionRolledbackException
     */
    @Test
    void createDuplicateResources() {

        CourseResource resource = new CourseResource("Resource.pdf", "Content".getBytes(), course, null);
		baseService.persist(resource);

        course.addCourseResource(resource);
		course = baseService.merge(course);

        resource = new CourseResource("Resource.pdf", "Content".getBytes(), course, null);

		// Dummy for lambda expression
		final CourseResource resourceDummy = resource;

        // adding the resource twice should roll back
		assertThrows(EJBTransactionRolledbackException.class, () -> {
			baseService.persist(resourceDummy);
		});

    }

}
