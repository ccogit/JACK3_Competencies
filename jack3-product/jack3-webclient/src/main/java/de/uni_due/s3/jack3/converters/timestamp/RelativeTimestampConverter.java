package de.uni_due.s3.jack3.converters.timestamp;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.Temporal;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

/**
 * Produces a human readable relative date, such as "2 weeks ago", "4 hours ago" and "last week". The output is in HTML,
 * the additional tooltip shows the exact time.
 * 
 * @author lukas.glaser
 */
@FacesConverter(value = "relativeTimestampConverter")
public class RelativeTimestampConverter extends AbstractTimestampConverter {

	public RelativeTimestampConverter() {
		super("timestamp.formatter.short", true);
	}

	@Override
	public LocalDateTime getAsObject(FacesContext context, UIComponent component, String value) {
		throw new ConverterException("Converting String to timestamp is not supported");
	}

	@Override
	protected String getTimestampAsString(Temporal value) {
		if (value == null) {
			return getLocalizedText("global.never");
		}

		final LocalDateTime now = LocalDateTime.now();

		Duration diff = Duration.between(value, now);
		boolean future = diff.isNegative();
		diff = diff.abs();

		if (diff.getSeconds() < 60) {
			// now
			return formatTextToHTML(value, getLocalizedText("timestamp.relative.now"));
		}

		final String outerPattern = getLocalizedText(future ? "timestamp.relative.future" : "timestamp.relative.past");

		final long minutes = diff.toMinutes();
		if (minutes < 60) {
			// x minute(s) ago / in x minute(s)
			return formatTextToHTML(value, outerPattern,
					getLocalizedText(minutes == 1 ? "timestamp.relative.minute" : "timestamp.relative.minutes"),
					minutes);
		}

		if (LocalDate.from(now).minusDays(1).equals(LocalDate.from(value))) {
			// yesterday
			return formatTextToHTML(value, getLocalizedText("timestamp.relative.yesterday"));
		}
		if (LocalDate.from(now).plusDays(1).equals(LocalDate.from(value))) {
			// tomorrow
			return formatTextToHTML(value, getLocalizedText("timestamp.relative.tomorrow"));
		}

		final long hours = diff.toHours();
		if (hours < 24) {
			// 1 hour(s) ago / in 1 hour(s)
			return formatTextToHTML(value, outerPattern,
					getLocalizedText(hours == 1 ? "timestamp.relative.hour" : "timestamp.relative.hours"), hours);
		}

		// From now on we only distinguish between the dates
		Period period = Period.between(LocalDate.from(value), LocalDate.from(now));
		if (period.isNegative()) {
			period = period.negated();
		}

		final long days = period.getDays();
		final long weeks = days / 7;
		final long months = period.getMonths();
		final long years = period.getYears();

		if (days < 7 && months == 0 && years == 0) {
			// x days ago / in x days
			return formatTextToHTML(value, outerPattern, getLocalizedText("timestamp.relative.days"), days);
		}

		if (months == 0 && years == 0) {
			// x week(s) ago / in x week(s)
			return formatTextToHTML(value, outerPattern,
					getLocalizedText(weeks == 1 ? "timestamp.relative.week" : "timestamp.relative.weeks"), weeks);
		}

		if (months < 12 && years == 0) {
			// x month(s) ago / in x month(s)
			return formatTextToHTML(value, outerPattern,
					getLocalizedText(months == 1 ? "timestamp.relative.month" : "timestamp.relative.months"), months);
		}

		return formatTextToHTML(value,
				getLocalizedText(future ? "timestamp.relative.inAWhile" : "timestamp.relative.longAgo"));

	}

	private String formatTextToHTML(Temporal exact, String inner) {
		return "<span title=\"" + super.getTimestampAsString(exact) + "\">" + inner + "</span>";
	}

	private String formatTextToHTML(Temporal exact, String outerPattern, String innerPattern, long value) {
		final String relative = MessageFormat.format(outerPattern, value + " " + innerPattern);
		return "<span title=\"" + super.getTimestampAsString(exact) + "\">" + relative + "</span>";
	}

}
