package de.uni_due.s3.jack3.converters;

import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.WeakHashMap;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

import de.uni_due.s3.jack3.entities.tenant.User;

/**
 * Converter for user entities based on randomly saved IDs.
 */
@FacesConverter(value = "userConverter")
public class UserConverter implements Converter<User> {

	private static Map<User, String> entities = new WeakHashMap<>();

	@Override
	public User getAsObject(FacesContext context, UIComponent component, String uuid) {
		for (Entry<User, String> entry : entities.entrySet()) {
			if (entry.getValue().equals(uuid)) {
				return entry.getKey();
			}
		}
		return null;
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, User entity) {
		if (entity == null) {
			return "";
		}
		synchronized (entities) {
			if (!entities.containsKey(entity)) {
				String uuid = UUID.randomUUID().toString();
				entities.put(entity, uuid);
				return uuid;
			} else {
				return entities.get(entity);
			}
		}
	}

}
