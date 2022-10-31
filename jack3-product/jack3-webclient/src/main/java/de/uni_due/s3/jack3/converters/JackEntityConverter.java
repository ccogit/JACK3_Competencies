package de.uni_due.s3.jack3.converters;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import de.uni_due.s3.jack3.entities.AbstractEntity;

/**
 * Converter for all entities based on randomly saved IDs.
 */
@FacesConverter(value = "jackEntityConverter")
public class JackEntityConverter implements Converter<AbstractEntity> {

	private static Map<AbstractEntity, String> entities = new WeakHashMap<>();

	@Override
	public AbstractEntity getAsObject(FacesContext context, UIComponent component, String uuid) {
		if (component == null || uuid == null || uuid.isBlank()) {
			// If UIComponent or ID is missing, we cannot do anything useful
			return null;
		}

		for (var entry : entities.entrySet()) {
			if (entry.getValue().equals(uuid)) {
				return entry.getKey();
			}
		}
		return null;
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, AbstractEntity entity) {
		if (entity == null) {
			return "";
		}

		synchronized (entities) {
			return entities.computeIfAbsent(entity, enrollment -> UUID.randomUUID().toString());
		}
	}

}
