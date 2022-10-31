package de.uni_due.s3.jack3.converters;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.FacesConverter;

import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.services.utils.RepeatStage;

@FacesConverter("stageSelectOneMenuConverter")
public class StageSelectOneMenuConverter extends AbstractEntityConverter<Stage> {

	private static final String REPEAT_KEY = "repeat";

	public StageSelectOneMenuConverter() {
		super(Stage.class);
	}

	@Override
	public Stage getAsObject(FacesContext context, UIComponent uiComponent, String value) {
		if (REPEAT_KEY.equals(value)) {
			return new RepeatStage();
		}
		return super.getAsObject(context, uiComponent, value);
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Stage value) {
		if (value instanceof RepeatStage) {
			return REPEAT_KEY;
		}
		return super.getAsString(context,component,value);
	}
}
