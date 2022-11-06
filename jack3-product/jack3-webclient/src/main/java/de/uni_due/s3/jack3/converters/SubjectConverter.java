package de.uni_due.s3.jack3.converters;

import de.uni_due.s3.jack3.entities.tenant.Subject;

import javax.faces.convert.FacesConverter;

@FacesConverter("subjectConverter")
public class SubjectConverter extends AbstractEntityConverter<Subject> {

	public SubjectConverter() {
		super(Subject.class);
	}
}
