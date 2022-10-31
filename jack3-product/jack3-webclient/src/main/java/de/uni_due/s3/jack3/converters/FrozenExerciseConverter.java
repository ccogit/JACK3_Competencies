package de.uni_due.s3.jack3.converters;

import javax.enterprise.inject.spi.CDI;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.entities.tenant.FrozenExercise;

@FacesConverter("frozenExerciseConverter")
public class FrozenExerciseConverter extends AbstractEntityConverter<FrozenExercise> {

	public FrozenExerciseConverter() {
		super(FrozenExercise.class);
	}

	/**
	 * Tries to convert the value to a long and returns the FrozenExercise entity
	 * from the database with that id (if it exists) or null otherwise
	 */
	@Override
	public FrozenExercise getAsObject(FacesContext context, UIComponent component, String value) {
		if (value == null) {
			return null;
		}

		final ExerciseBusiness exerciseBusiness = CDI.current().select(ExerciseBusiness.class).get();
		long id = Long.valueOf(value);
		return exerciseBusiness.getFrozenExerciseWithLazyDataById(id);
	}
}
