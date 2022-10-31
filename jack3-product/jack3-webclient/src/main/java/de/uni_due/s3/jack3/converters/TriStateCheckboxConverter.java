package de.uni_due.s3.jack3.converters;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 * One-way converter for converting the value of Primefaces' triStateCheckBox (0 - unselected; 1 - selected; 2 - third
 * selected state, marked by an 'X') to a boolean for filtering in a DataTable. Using of this:
 * 
 * <ol>
 * <li>Add <code>filterBy</code> and <code>filterMatchMode="equals"</code> properties on the column element</li>
 * <li>Add the following snippet in the column element:
 * 
 * <pre>
 * {@code
 * <f:facet name="filter">
 *   <p:triStateCheckbox onchange="PF('*insertWidgetVarHere*').filter()" converter="triStateCheckboxConverter" />
 * </f:facet>
 * }
 * </pre>
 * 
 * </li>
 * </ol>
 * 
 * @author lukas.glaser
 */
@FacesConverter(value = "triStateCheckboxConverter")
public class TriStateCheckboxConverter implements Converter<Boolean> {

	@Override
	public Boolean getAsObject(FacesContext context, UIComponent component, String value) {
		if (value.equals("1")) {
			return Boolean.TRUE;
		} else if (value.equals("2")) {
			return Boolean.FALSE;
		} else {
			return null; // NOSONAR 'null' is a valid filter value and means 'no filtering'
		}
	}

	/**
	 * Don't needed
	 */
	@Override
	public String getAsString(FacesContext context, UIComponent component, Boolean value) {
		return "";
	}

}
