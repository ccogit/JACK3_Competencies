package de.uni_due.s3.jack3.beans.data;

import java.io.Serializable;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.User;

/**
 * Helper class for course offer personal passwords.
 * 
 * @author lukas.glaser
 */
public class CourseOfferPersonalPasswordEntry implements Serializable {

	private static final long serialVersionUID = 8595104714295737885L;

	public CourseOfferPersonalPasswordEntry(User user, CourseOffer offer, CourseBusiness courseBusiness) {
		super();
		this.user = user;
		this.offer = offer;
		this.courseBusiness = courseBusiness;
	}

	private User user;
	private CourseOffer offer;
	private CourseBusiness courseBusiness;

	public boolean getHasPersonalPassword() {
		return offer.getPersonalPasswords().containsKey(user);
	}

	public void setHasPersonalPassword(boolean hasPersonalPassword) {
		// Don't overwrite existing setting
		if (hasPersonalPassword && getHasPersonalPassword()) {
			return;
		}

		if (hasPersonalPassword) {
			courseBusiness.addPersonalPasswordEntryToCourseOffer(offer, user);
		} else {
			offer.removePersonalPassword(user);
		}
	}

	public String getPersonalPassword() {
		return offer.getPersonalPasswords().get(user);
	}

	public String getUsername() {
		return user.getLoginName();
	}

}
