package de.uni_due.s3.jack3.beans;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.ProfileField;
import de.uni_due.s3.jack3.utils.JackStringUtils;

@Dependent
public class CourseOfferEditUserFilterView extends AbstractView implements Serializable {

	private static final long serialVersionUID = -1559173152161504139L;

	@Inject
	private UserBusiness userBusiness;

	private CourseOffer courseOffer;

	private String filterText;

	public void initialize(CourseOffer courseOffer) {
		this.courseOffer = courseOffer;
		updateFilterList();
	}

	public void updateFilterList() {
		filterText = courseOffer.getUserFilter().stream().sorted().collect(Collectors.joining("\n"));
	}

	public void saveFilterList() {
		final List<String> list = JackStringUtils.splitAndStripLines(filterText);
		courseOffer.setUserFilter(new HashSet<>(list));
	}

	public String getFilterText() {
		return filterText;
	}

	public void setFilterText(String filterText) {
		this.filterText = filterText;
		saveFilterList();
	}

	public List<ProfileField> getProfileFields() {
		List<ProfileField> allProfileFields = userBusiness.getAllPublicProfileFields(getCurrentUser(),
				courseOffer.getFolder());
		allProfileFields.sort(Comparator.comparing(ProfileField::toString));
		return allProfileFields;
	}

	public Set<ProfileField> getProfileFieldFilter() {
		return new HashSet<>(courseOffer.getProfileFieldFilter());
	}

	public void setProfileFieldFilter(Set<ProfileField> profileFieldFilter) {
		courseOffer.setProfileFieldFilter(profileFieldFilter);
	}

}
