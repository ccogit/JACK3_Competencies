package de.uni_due.s3.jack3.converters;

import javax.enterprise.inject.spi.CDI;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.entities.tenant.FrozenCourse;

@FacesConverter("frozenCourseConverter")
public class FrozenCourseConverter extends AbstractEntityConverter<FrozenCourse> {

	public FrozenCourseConverter() {
		super(FrozenCourse.class);
	}

	/**
	 * Tries to convert the value to a long and returns the FrozenCourse entity from the database
	 * with that id (if it exists) or {@code null} otherwise.
	 */
	@Override
	public FrozenCourse getAsObject(FacesContext context, UIComponent component, String value) {
		if (value == null) {
			return null;
		}

		final CourseBusiness courseBusiness = CDI.current().select(CourseBusiness.class).get();
		final long id = Long.valueOf(value);
		return courseBusiness.getFrozenCourse(id);
	}
}
