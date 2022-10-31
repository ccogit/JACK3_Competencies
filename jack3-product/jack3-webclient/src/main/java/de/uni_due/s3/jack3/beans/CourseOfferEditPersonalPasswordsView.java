package de.uni_due.s3.jack3.beans;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.faces.application.FacesMessage;
import javax.inject.Inject;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import de.uni_due.s3.jack3.beans.data.CourseOfferPersonalPasswordEntry;
import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.UserBusiness;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;

/**
 * This bean handles the personal password mode in course offer editor.
 * 
 * @author lukas.glaser
 */
@Dependent
public class CourseOfferEditPersonalPasswordsView extends AbstractView implements Serializable {

	private static final long serialVersionUID = 4749676792287132507L;

	private CourseOffer courseOffer;
	private List<CourseOfferPersonalPasswordEntry> entries;
	private List<CourseOfferPersonalPasswordEntry> filteredEntries;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private UserBusiness userBusiness;
	
	@Inject
	private AuthorizationBusiness authorizationBusiness;

	/**
	 * Initialization is called from CourseOfferEditView
	 */
	public void initialize(CourseOffer courseOffer) {
		this.courseOffer = courseOffer;
		entries = new ArrayList<>();
		userBusiness.getAllUsers()
				.forEach(user -> entries.add(new CourseOfferPersonalPasswordEntry(user, courseOffer, courseBusiness)));
	}

	public List<CourseOfferPersonalPasswordEntry> getEntries() {
		return entries;
	}

	/**
	 * Handle upload of a CSV file with personal passwords
	 */
	public void uploadFile(FileUploadEvent event) {
		if (!authorizationBusiness.isAllowedToEditFolder(getCurrentUser(), courseOffer.getFolder())) {
			getLogger().warn("User " +getCurrentUser().getLoginName()+
					" tried to upload csv file with passwords in "
					+courseOffer+
					" while not having write permission! Users should not be able to even call this function without manipulation of the UI");
			
			return;
		}
		try {
			courseBusiness.uploadPersonalPasswordsFile(courseOffer, event.getFile().getInputStream());
		} catch (Exception e) {
			addFacesMessage("courseOfferPersonalPasswordsUpload", FacesMessage.SEVERITY_ERROR, "global.error",
					"courseOfferEdit.personalPasswords.uploadError");
		}
	}

	/**
	 * Handle download of a CSV file with personal passwords
	 */
	public StreamedContent getFile() {
		InputStream stream = courseBusiness.downloadPersonalPasswordsFile(courseOffer);
		return DefaultStreamedContent.builder()
				.stream(() -> stream)
				.contentType("text/csv")
				.name(courseOffer.getName() + ".csv")
				.contentEncoding("UTF-8")
				.build();
	}

	public void clearPersonalPasswordList() {
		if (!authorizationBusiness.isAllowedToEditFolder(getCurrentUser(), courseOffer.getFolder())) {
			getLogger().warn("User " +getCurrentUser().getLoginName()+
					" tried to clear personal-password-list in "
					+courseOffer+
					" while not having write permission! Users should not be able to even call this function without manipulation of the UI");
			
			return;
		}

		courseOffer.clearAllPersonalPasswords();
	}

	public List<CourseOfferPersonalPasswordEntry> getFilteredEntries() {
		return filteredEntries;
	}

	public void setFilteredEntries(List<CourseOfferPersonalPasswordEntry> filteredEntries) {
		this.filteredEntries = filteredEntries;
	}

}
