package de.uni_due.s3.jack3.tests.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.services.CourseOfferService;
import de.uni_due.s3.jack3.tests.annotations.NeedsCourse;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;

@NeedsCourse
class CourseOfferServiceTest extends AbstractContentTest {

	@Inject
	private CourseOfferService offerService;

	/**
	 * Tests getting an empty list of course offers
	 */
	@Test
	void testEmptyDatabase() {
		assertTrue(offerService.getAllCourseOffers().isEmpty());
	}

	/**
	 * Tests adding courseOffers
	 */
	@Test
	void getAllCourseOffers() {
		Collection<CourseOffer> offers = new ArrayList<>(3);
		offers.add(new CourseOffer("Course Offer 1", course));
		offers.add(new CourseOffer("Course Offer 2", course));
		offers.add(new CourseOffer("Course Offer 3", course));

		for (CourseOffer courseOffer : offers) {
			offerService.persistCourseOffer(courseOffer);
		}

		// offer list should be equal to getAllCourseOffers
		Collection<CourseOffer> getAllCourseOffersFromDatabase = offerService.getAllCourseOffers();
		assertTrue(getAllCourseOffersFromDatabase.containsAll(offers));
		assertTrue(offers.containsAll(getAllCourseOffersFromDatabase));

		Collection<CourseOffer> getCourseOffersReferencingCourse = offerService
				.getCourseOffersReferencingCourse(course);
		assertTrue(getCourseOffersReferencingCourse.containsAll(offers));
		assertTrue(offers.containsAll(getCourseOffersReferencingCourse));
	}

	/**
	 * Tests if course offer is found by id
	 */
	@Test
	void getCourseOfferById() {
		CourseOffer offer = new CourseOffer("Course Offer", course);
		offerService.persistCourseOffer(offer);

		assertEquals(offer, offerService.getCourseOfferById(offer.getId())
												.orElseThrow(AssertionError::new));
	}

	/**
	 * Tests if course offer could be deleted
	 */
	@Test
	void deleteCourseOffer() {

		CourseOffer offer = new CourseOffer("Course Offer", course);
		offerService.persistCourseOffer(offer);
		assertFalse(offerService.getAllCourseOffers().isEmpty());

		offerService.deleteCourseOffer(offer);
		assertTrue(offerService.getAllCourseOffers().isEmpty());
	}

}
