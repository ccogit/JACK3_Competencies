package de.uni_due.s3.jack3.beans;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import de.uni_due.s3.jack3.beans.data.Link;
import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.services.CourseOfferService;

@Named
@ViewScoped
public class QuickIdView extends AbstractView implements Serializable {

	private static final long serialVersionUID = 4058657655168126885L;
	private long exerciseId;
	private Link linkToTestpage;
	private List<Link> possibleCourseLinks = new ArrayList<>();
	private List<Link> possibleCourseOfferLinks = new ArrayList<>();

	private Exercise exercise;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private AuthorizationBusiness authorizationBusiness;

	@Inject
	private CourseOfferService courseOfferService;

	@Inject
	private CourseBusiness courseBusiness;

	public void initView() throws IOException {
		//check for valid id
		Optional<Exercise> maybeExercise = exerciseBusiness.getExerciseById(exerciseId);


		if (maybeExercise.isPresent()) {
			exercise = maybeExercise.get();
			generatePossibleLinks();

		} else {
			//invalid ExerciseId
			sendErrorResponse(400, getLocalizedMessage("quickIdView.noExerciseExists"));
		}

	}

	public void generatePossibleLinks() {
		//test page of exercise
		//user only needs read right
		if (authorizationBusiness.isAllowedToTestExercise(getCurrentUser(), exercise)) {
			this.generateLinkForExerciseTestpage();
		}

			//test page for course
			// user has read rights on course
			//course references exercise
			List<Course> coursesReferencingEx = courseBusiness.getAllCoursesContainingExercise(exercise);
			List<CourseOffer> courseOffersReferencingEx = new ArrayList<>();
			
			for (Course course : coursesReferencingEx) {
				//course test page
				if (authorizationBusiness.isAllowedToTestCourse(getCurrentUser(), course)) {
					String url = viewId.getCourseEditor().withParam(Course.class, course.getId()).toActionUrl();
					String value = course.getName();
					this.addPossibleCourseLink(new Link(url, value));
				}

				//check for courseoffers referencing course
				courseOffersReferencingEx.addAll(courseOfferService.getCourseOffersReferencingCourse(course));

			}
			//remove all duplicates
			List<CourseOffer> courseOffersWithoutDuplicates = courseOffersReferencingEx.stream().distinct()
					.collect(Collectors.toList());
			//create links for courseOffers
			generateCourseOfferLinks(courseOffersWithoutDuplicates);

	}

	public void generateLinkForExerciseTestpage() {
		String url = viewId.getExerciseEditor().withParam(Exercise.class, exerciseId).toActionUrl();
		String value = exercise.getName();
		this.linkToTestpage = new Link(url, value);
	}

	/**
	 * Generates links for all given courseOffers
	 * Links are only created, if the coruseOffer is visible for the user.
	 * 
	 * @param courseOfferRefEx
	 */
	public void generateCourseOfferLinks(List<CourseOffer> courseOfferRefEx) {
		for(CourseOffer offer: courseOfferRefEx) {
			if (authorizationBusiness.isCourseOfferVisibleForUserAsStudent(getCurrentUser(), offer)) {
				String url = viewId.getCourseMainMenu().withParam(offer).withParam("redirect", true)
					.withParam("exerciseId", exerciseId).toActionUrl();
				String value = offer.getName();
				this.addPossibleCourseOfferLink(new Link(url, value));
			}
		}
	}

	/**
	 * The warn message that real courseOffer records are generated and places are taken,
	 * is only shown for users with edit-rights.
	 * 
	 * @return
	 */
	public boolean isShowCourseOfferWarnMessage() {
		return (!this.possibleCourseOfferLinks.isEmpty()) && getCurrentUser().isHasEditRights();
	}

	public long getExerciseId() {
		return exerciseId;
	}

	public void setExerciseId(long exerciseId) {
		this.exerciseId = exerciseId;
	}

	public List<Link> getPossibleCourseLinks() {
		possibleCourseLinks.sort((link1, link2) -> link1.getValue().compareTo(link2.getValue()));
		return possibleCourseLinks;
	}

	public void addPossibleCourseLink(Link link) {
		this.possibleCourseLinks.add(link);
	}

	public Exercise getExercise() {
		return exercise;
	}

	public void setExercise(Exercise exercise) {
		this.exercise = exercise;
	}

	public boolean noLinkForExerciseExists() {
		return this.linkToTestpage == null && this.possibleCourseLinks.isEmpty()
				&& this.possibleCourseOfferLinks.isEmpty();
	}

	public List<Link> getPossibleCourseOfferLinks() {
		possibleCourseOfferLinks.sort((link1, link2) -> link1.getValue().compareTo(link2.getValue()));
		return possibleCourseOfferLinks;
	}

	public void addPossibleCourseOfferLink(Link link) {
		this.possibleCourseOfferLinks.add(link);
	}

	public Link getLinkToTestpage() {
		return linkToTestpage;
	}

}
