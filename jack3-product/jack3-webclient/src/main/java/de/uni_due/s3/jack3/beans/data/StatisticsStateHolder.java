package de.uni_due.s3.jack3.beans.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.primefaces.event.ToggleEvent;
import org.primefaces.model.Visibility;

import de.uni_due.s3.jack3.entities.tenant.ProfileField;

/**
 * Holds the toggle state of the panels in the statistics pages (CourseOfferParticipantsView, CourseStatisticsView,
 * ExerciseSubmissionsView).
 */
public class StatisticsStateHolder implements Serializable {
	
	private static final long serialVersionUID = -4830423691470286530L;

	private boolean toggleKeyFigures = false;
	private boolean toggleUserStatus = false;
	private boolean toggleDisplaySettings = false;
	private boolean groupSubmissionsByVersion = false;
	private List<ProfileField> selectedProfileFields = new ArrayList<>();

	public boolean isToggleKeyFigures() {
		return toggleKeyFigures;
	}

	public void toggleKeyFigures(ToggleEvent event) {
		this.toggleKeyFigures = event.getVisibility() == Visibility.HIDDEN;
	}

	public boolean isToggleUserStatus() {
		return toggleUserStatus;
	}

	public void toggleUserStatus(ToggleEvent event) {
		this.toggleUserStatus = event.getVisibility() == Visibility.HIDDEN;
	}

	public boolean isToggleDisplaySettings() {
		return toggleDisplaySettings;
	}

	public void toggleDisplaySettings(ToggleEvent event) {
		this.toggleDisplaySettings = event.getVisibility() == Visibility.HIDDEN;
	}

	public boolean isGroupSubmissionsByVersion() {
		return groupSubmissionsByVersion;
	}

	public void setGroupSubmissionsByVersion(boolean groupSubmissionsByVersion) {
		this.groupSubmissionsByVersion = groupSubmissionsByVersion;
	}

	public List<ProfileField> getSelectedProfileFields() {
		return selectedProfileFields;
	}

	public void setSelectedProfileFields(List<ProfileField> selectedProfileFields) {
		this.selectedProfileFields = selectedProfileFields;
	}

}
