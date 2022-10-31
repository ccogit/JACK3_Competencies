package de.uni_due.s3.jack3.business.exceptions;

/**
 * This Exception indicates that an Drag and Drop action performed by a user is not allowed.
 */
public class DragDropException extends ActionNotAllowedException {

	private static final long serialVersionUID = -6405030390873976006L;
	
	private final EType type;
	
	public DragDropException(EType type) {
		super();
		this.type = type;
	}
	
	public EType getType() {
		return type;
	}
	
	public enum EType{
		/** The Target to drop into is not a Folder */
		TARGET_IS_NOT_FOLDER,
	}

}
