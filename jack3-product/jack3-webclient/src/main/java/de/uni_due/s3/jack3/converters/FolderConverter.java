package de.uni_due.s3.jack3.converters;

import javax.faces.convert.FacesConverter;

import de.uni_due.s3.jack3.entities.tenant.ContentFolder;

@FacesConverter("folderConverter")
public class FolderConverter extends AbstractEntityConverter<ContentFolder> {

	public FolderConverter() {
		super(ContentFolder.class);
	}
}
