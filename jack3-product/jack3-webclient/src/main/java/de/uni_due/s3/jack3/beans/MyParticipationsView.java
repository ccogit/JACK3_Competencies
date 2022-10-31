package de.uni_due.s3.jack3.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import de.uni_due.s3.jack3.business.EnrollmentBusiness;
import de.uni_due.s3.jack3.business.helpers.CourseParticipation;
import de.uni_due.s3.jack3.enums.ECourseOrder;
import de.uni_due.s3.jack3.utils.JackStringUtils;

@ViewScoped
@Named
public class MyParticipationsView extends AbstractView implements Serializable {

	private static final long serialVersionUID = -1157514754914067744L;
	private List<CourseParticipation> allParticipations;
	private List<CourseParticipation> filteredParticipations;
	private String searchString;
	private ECourseOrder openSubmissionsOrder;

	private static final Comparator<CourseParticipation> nameComparator = Comparator
			.comparing(CourseParticipation::getCourseOfferName);
	private static final Comparator<CourseParticipation> startTimeComparator = Comparator
			.comparing(CourseParticipation::getStartTime, Comparator.nullsLast(Comparator.reverseOrder()))
			.thenComparing(nameComparator);
	private static final Comparator<CourseParticipation> deadlineComparator = Comparator
			.comparing(CourseParticipation::getDeadline, Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(nameComparator);
	private static final Comparator<CourseParticipation> lastVisitComparator = Comparator
			.comparing(CourseParticipation::getLastVisit, Comparator.nullsLast(Comparator.reverseOrder()))
			.thenComparing(nameComparator);

	@Inject
	private EnrollmentBusiness enrollmentBusiness;
	
	@PostConstruct
	public void init() {
		allParticipations = enrollmentBusiness.getVisibleParticipationsForUser(getCurrentUser());
		filteredParticipations = allParticipations;
		openSubmissionsOrder = ECourseOrder.NAME_ASC;
	}

	public void filterCourses() {
		if (JackStringUtils.isBlank(searchString)) {
			// Empty filter
			filteredParticipations = allParticipations;
		} else {
			filteredParticipations = new ArrayList<>();
			// Compare without case sensitivity
			String query = searchString.strip().toLowerCase();
			for (CourseParticipation participation : allParticipations) {
				String name = participation.getCourseOfferName().toLowerCase();
				String breadcrumb = getPathAsString(participation.getCourseOffer()).toLowerCase();
				if (name.contains(query) || breadcrumb.contains(query)) {
					filteredParticipations.add(participation);
				}
			}
		}
	}

	public void sortCourses() {
		switch (openSubmissionsOrder) {
		case DEADLINE_ASC:
			allParticipations.sort(deadlineComparator);
			filteredParticipations.sort(deadlineComparator);
			break;
		case LAST_VISIT_DESC:
			allParticipations.sort(lastVisitComparator);
			filteredParticipations.sort(lastVisitComparator);
			break;
		case START_TIME_DESC:
			allParticipations.sort(startTimeComparator);
			filteredParticipations.sort(startTimeComparator);
			break;
		case NAME_ASC:
		default:
			allParticipations.sort(nameComparator);
			filteredParticipations.sort(nameComparator);
			break;
		}
	}

	public String getSearchString() {
		return searchString;
	}

	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}

	public ECourseOrder[] getAvailableSorting() {
		return ECourseOrder.values();
	}

	public ECourseOrder getOpenSubmissionsOrder() {
		return openSubmissionsOrder;
	}

	public void setOpenSubmissionsOrder(ECourseOrder openSubmissionsOrder) {
		this.openSubmissionsOrder = openSubmissionsOrder;
	}

	public List<CourseParticipation> getAllParticipations() {
		return allParticipations;
	}

	public List<CourseParticipation> getFilteredParticipations() {
		return filteredParticipations;
	}

}
