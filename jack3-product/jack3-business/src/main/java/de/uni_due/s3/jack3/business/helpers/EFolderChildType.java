package de.uni_due.s3.jack3.business.helpers;

import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;

public enum EFolderChildType {

	EXERCISE, COURSE, COURSEOFFER, CONTENT_FOLDER, PRESENTATION_FOLDER;

	public static EFolderChildType fromObject(AbstractEntity entity) {
		if (entity instanceof Exercise) {
			return EXERCISE;
		} else if (entity instanceof Course) {
			return COURSE;
		} else if (entity instanceof CourseOffer) {
			return COURSEOFFER;
		} else if (entity instanceof ContentFolder) {
			return CONTENT_FOLDER;
		} else if (entity instanceof PresentationFolder) {
			return PRESENTATION_FOLDER;
		} else {
			return null;
		}
	}

	public String getClassName() {
		switch (this) {
		case CONTENT_FOLDER:
			return "ContentFolder";
		case COURSE:
			return "Course";
		case COURSEOFFER:
			return "CourseOffer";
		case EXERCISE:
			return "Exercise";
		case PRESENTATION_FOLDER:
			return "PresentationFolder";
		default:
			return null;
		}
	}

}
