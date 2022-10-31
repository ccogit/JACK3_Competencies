package de.uni_due.s3.jack3.business.exceptions;

public class FolderException extends ActionNotAllowedException {

	private static final long serialVersionUID = -5504068721967963006L;

	private final EType type;
	
	public FolderException(EType type) {
		super();
		this.type = type;
	}

	public EType getType() {
		return type;
	}

	@Override
	public String getMessage() {
		return type == null ? null : type.toString();
	}

	public enum EType {
		/** Action will cause a recursion */
		RECURSION,
		/** A folder is a personal Folder */
		PERSONAL_FOLDER,
		/** A folder is the root */
		ROOT;
	}

}
