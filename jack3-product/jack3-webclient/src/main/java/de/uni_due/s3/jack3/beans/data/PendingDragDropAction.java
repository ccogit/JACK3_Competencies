package de.uni_due.s3.jack3.beans.data;

import java.io.Serializable;
import java.util.Objects;

import javax.annotation.concurrent.Immutable;

import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.tenant.Folder;

/**
 * A pending drag drop action that will move one entity into an other folder.
 *
 * @param <F>
 *            Type of the target and source folder.
 * @param <O>
 *            Type of the entity that moves to a new folder
 */
@Immutable
public class PendingDragDropAction<F extends Folder, O extends AbstractEntity> implements Serializable {
	private static final long serialVersionUID = 6963438779736231933L;

	public PendingDragDropAction(String confirmText, F targetFolder, O objectToMove) {
		super();
		this.confirmText = confirmText;
		this.targetFolder = Objects.requireNonNull(targetFolder);
		this.objectToMove = Objects.requireNonNull(objectToMove);
	}

	private final String confirmText;
	private final F targetFolder;
	private final O objectToMove;

	public String getConfirmText() {
		return confirmText;
	}

	public F getTargetFolder() {
		return targetFolder;
	}

	public O getObjectToMove() {
		return objectToMove;
	}

}
