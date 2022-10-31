package de.uni_due.s3.jack3.converters;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

import org.primefaces.component.picklist.PickList;
import org.primefaces.model.DualListModel;

import de.uni_due.s3.jack3.entities.tenant.User;

/**
 * Converts a user into a String value that represents the entity ID (and vice versa). This converter is needed for
 * PrimeFaces PickList elements in combination with a DualListModel.
 */
@FacesConverter(value = "userPickListConverter")
public class UserPickListConverter implements Converter<User> {

	/**
	 * Lookups the entity ID in the PickList and returns the user for the given ID.
	 * 
	 * @return null if the element was not found.
	 */
	@Override
	public User getAsObject(FacesContext context, UIComponent component, String value) {
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
		final DualListModel<User> listModel = (DualListModel<User>) dualList;

		// Search for the user in the sources of the dual list model
		for (final User user : listModel.getSource()) {
			if (user.getId() == id) {
				return user;
			}
		}
		// Search for the user in the targets of the dual list model
		for (final User user : listModel.getTarget()) {
			if (user.getId() == id) {
				return user;
			}
		}
		throw new ConverterException("Unknown user id: " + id);
	}

	/**
	 * Returns the entity ID of the user.
	 */
	@Override
	public String getAsString(FacesContext context, UIComponent component, User value) {
		if (value == null) {
			return "";
		}
		return Long.toString(value.getId());
	}
}
