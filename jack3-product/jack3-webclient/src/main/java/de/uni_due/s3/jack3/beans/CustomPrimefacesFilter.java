package de.uni_due.s3.jack3.beans;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.uni_due.s3.jack3.utils.JackStringUtils;

@ViewScoped
@Named
public class CustomPrimefacesFilter extends AbstractView implements Serializable {

	private DateTimeFormatter formatter;

	@PostConstruct
	public void init() {
		formatter = DateTimeFormatter.ofPattern(getLocalizedMessage("timestamp.formatter.short"), getCurrentLocale());
	}

	private static final long serialVersionUID = -5448328172597313650L;

	/**
	 * Custom filter function for filtering LocalDateTime columns in DataTables. Implements a "contains" strategy.
	 * You can use this by adding filterFunction="#{customPrimefacesFilter.filterByDate}" to a DataTable-Column.
	 * 
	 * @param currentDateObj
	 *            The LocalDateTime we want to check if our filter applys to. Given as an Object.
	 * @param filterStringObj
	 *            The string entered by the user to check against. Given as an Object.
	 * @param locale
	 *            Ignored here, because we use the same date format for all languages
	 * @return
	 */
	public boolean filterByDate(Object currentDateObj, Object filterStringObj, Locale locale) {
		String filter = (String) filterStringObj;
		if (JackStringUtils.isBlank(filter)) {
			// No filter set, so match everything
			return true;
		}

		LocalDateTime currentDate = (LocalDateTime) currentDateObj;
		if (currentDate == null) {
			// Filter is set (or we wouldn't be here) but the currentDate is not, so no match.
			return false;
		}

		// Format our date and match the string
		String currentDateFormatted = formatter.format(currentDate);
		return currentDateFormatted.contains(filter);
	}
}
