package de.uni_due.s3.jack3.converters;

import javax.faces.convert.FacesConverter;

import de.uni_due.s3.jack3.entities.tenant.Course;

@FacesConverter("courseConverter")
public class CourseConverter extends AbstractEntityConverter<Course> {

	public CourseConverter() {
		super(Course.class);
	}
}
