package de.uni_due.s3.jack3.enums;

/**
 * Holds every Type of Treenode and the corresponding name for *.xhtml
 */

public enum ETreeNodeType {
	
	//Folder
	PLAIN_FOLDER_TYPE("folder"),
	EMPTY_FOLDER_TYPE("emptyFolder"), 
	NO_ACTION_FOLDER_TYPE("noActionFolder"),
	// A folder where only the content may be changed, not the folder itself. 
	NO_CHANGE_FOLDER_TYPE("noChangeFolder"),
	PERSONAL_FOLDER_TYPE("personalFolder"),
	READ_RIGHTS_FOLDER("readRightsFolder"),
	SHARED_FOLDER_TYPE("sharedFolder"),
	//Probably unused
	ONLY_ADD_FOLDER_TYPE("onlyAddFolder"),
	NEW_FOLDER_TYPE("newFolder"),
	
	//CourseOffer
	STUDENT_OFFER_TYPE("studentOffer"),
	EDIT_RIGHTS_OFFER_TYPE("editRightsOffer"),
	READ_RIGHTS_OFFER_TYPE("readRightsOffer"),
	NO_RIGHTS_OFFER_TYPE("noRightsOffer"),
	
	//Course
	COURSE_TYPE("course"),
	NO_DELETE_COURSE_TYPE("noDeleteCourse"),
	NEW_COURSE("newCourse"),
	
	//Exercise
	EXERCISE_TYPE("exercise"),
	NO_DELETE_EXERCISE_TYPE("noDeleteExercise"),
	NEW_EXERCISE("newExercise");
	


	private final String name;

	ETreeNodeType(String name) {
		this.name = name;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
}
