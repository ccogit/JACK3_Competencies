package de.uni_due.s3.jack3.converters;

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItem;
import javax.faces.component.UISelectItems;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

import de.uni_due.s3.jack3.entities.AbstractEntity;

/**
 * Converts from an Entity to its ID and vice versa for use in selectOneMenus and alike in the UI. The converter accepts
 * menus with both "selectItem" and "selectItems" as content, but assumes that there is only one element of type
 * "selectItems" and that it is the last element in the menu.
 *
 * @author striewe
 */
public abstract class AbstractEntityConverter<T extends AbstractEntity> implements Converter<T> {

	private final Class<T> expectedType;

	protected AbstractEntityConverter(Class<T> expectedType) {
		this.expectedType = expectedType;
	}

	@Override
	public T getAsObject(FacesContext context, UIComponent uiComponent, String value) {
		if (uiComponent == null || value == null) {
			return null;
		}

		return handleUIcomponent(uiComponent, value);
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, T entity) {
		return entity == null ? "" : Long.toString(entity.getId());
	}

	protected T handleUIcomponent(UIComponent uiComponent, String providedId) {
		final List<UIComponent> children = uiComponent.getChildren();
		// Let's see what the UIComponent contains
		for (UIComponent child : children) {
			if (child instanceof UISelectItems) {
				// We have an extra method for complete sets of select items
				return handleItems((UISelectItems) child, providedId);
			} else if (child instanceof UISelectItem) {
				final UISelectItem item = (UISelectItem) child;
				final Object itemValue = item.getItemValue();
				// Let's see whether the item has the correct type
				if (expectedType.isInstance(itemValue)) {
					// If it also has the right id, we return it
					final T entity = expectedType.cast(itemValue);
					if (providedId.equals(Long.toString(entity.getId()))) {
						return entity;
					}
					// ... otherwise the loop continues
				}
			}
		}

		return null;
	}

	protected T handleItems(UISelectItems items, String providedId) {
		@SuppressWarnings("unchecked")
		List<AbstractEntity> values = (List<AbstractEntity>) items.getValue();

		// Iterate over all elements of the item set
		for (AbstractEntity itemValue : values) {
			// If an element has the right type and id we return it.
			if (expectedType.isInstance(itemValue) && providedId.equals(Long.toString(itemValue.getId()))) {
				return expectedType.cast(itemValue);
			}
		}

		// If we haven't found a matching entity we can return null.
		return null;
	}
}
