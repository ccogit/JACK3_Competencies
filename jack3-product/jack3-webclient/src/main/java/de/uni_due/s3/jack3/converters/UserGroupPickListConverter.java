package de.uni_due.s3.jack3.converters;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import org.primefaces.component.picklist.PickList;
import org.primefaces.model.DualListModel;

import de.uni_due.s3.jack3.entities.tenant.UserGroup;

/**
 * Converts a user group into a String value that represents the entity ID (and vice versa). This converter is needed
 * for PrimeFaces PickList elements in combination with a DualListModel.
 */
@FacesConverter(value = "userGroupPickListConverter")
public class UserGroupPickListConverter implements Converter<UserGroup> {

	/**
	 * Lookups the entity ID in the PickList and returns the user group for the given ID.
	 * 
	 * @return null if the element was not found.
	 */
	@Override
	public UserGroup getAsObject(FacesContext context, UIComponent component, String value) {
		if (value == null) {
			return null;
		}

		long id = 0;
		try {
			id = Long.parseLong(value);
		} catch (NumberFormatException e) {
			throw new ConverterException(e);
		}

		if (!(component instanceof PickList)) {
			throw new ConverterException("Unexpected component: " + component);
		}

		final Object dualList = ((PickList) component).getValue();
		@SuppressWarnings("unchecked") // Inside a picklist the value is always a DualListModel
		final DualListModel<UserGroup> listModel = (DualListModel<UserGroup>) dualList;

		// Search for the user group in the sources of the dual list model
		for (final UserGroup group : listModel.getSource()) {
			if (group.getId() == id) {
				return group;
			}
		}
		// Search for the user group in the targets of the dual list model
		for (final UserGroup group : listModel.getTarget()) {
			if (group.getId() == id) {
				return group;
			}
		}
		throw new ConverterException("Unknown user group id: " + id);
	}

	/**
	 * Returns the entity ID of the user group.
	 */
	@Override
	public String getAsString(FacesContext context, UIComponent component, UserGroup value) {
		if (value == null)
			return "";

		return Long.toString(value.getId());
	}
}
