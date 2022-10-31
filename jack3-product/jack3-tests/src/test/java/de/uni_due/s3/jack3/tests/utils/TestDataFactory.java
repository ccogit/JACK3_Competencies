package de.uni_due.s3.jack3.tests.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Password;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;

/**
 * Helper class for creating default test objects
 *
 * @author lukas.glaser
 *
 */
public final class TestDataFactory {

	/*
	 * Don't let anyone instantiate this class.
	 */
	private TestDataFactory() {
	}

	/**
	 * Returns a sample password hash.
	 */
	public static Password getEmptyPassword() {
		return new Password();
	}

	/**
	 * Get a default user with admin and edit rights.
	 */
	public static User getUser(String name) {
		return getUser(name, true, true);
	}

	private static final AtomicLong pseudonymIndex = new AtomicLong();

	/**
	 * Get a default user with specified rights.
	 */
	public static User getUser(String name, boolean hasAdminRights, boolean hasEditRights) {
		return new User(name, "pseudonym" + pseudonymIndex.incrementAndGet(), getEmptyPassword(), name.replace(' ', '.') + "@foobar.com",
				hasAdminRights, hasEditRights);
	}

	/**
	 * Get a user group.
	 */
	public static UserGroup getUserGroup(String name) {
		return new UserGroup(name, "Description of " + name);
	}

	/**
	 * Get a user group with member users.
	 */
	public static UserGroup getUserGroup(String name, User... users) {
		UserGroup userGroup = new UserGroup(name, "Description of " + name);
		for (User user : users) {
			userGroup.addMemberUser(user);
		}
		return userGroup;
	}

	/**
	 * Get a user group with member groups.
	 */
	public static UserGroup getUserGroup(String name, UserGroup... groups) {
		UserGroup userGroup = new UserGroup(name, "Description of " + name);
		for (UserGroup group : groups) {
			userGroup.addMemberGroup(group);
		}
		return userGroup;
	}

	/**
	 * Get a presentation folder with optional parent.
	 */
	public static PresentationFolder getPresentationFolder(String name, PresentationFolder parent) {
		PresentationFolder presentationFolder = new PresentationFolder(name);
		if (parent != null) {
			parent.addChildFolder(presentationFolder);
		}
		return presentationFolder;
	}

	/**
	 * Get a content folder with optional parent.
	 */
	public static ContentFolder getContentFolder(String name, ContentFolder parent) {
		ContentFolder contentFolder = new ContentFolder(name);
		if (parent != null) {
			parent.addChildFolder(contentFolder);
		}
		return contentFolder;
	}

	/**
	 * Get a content folder with managing user.
	 */
	public static ContentFolder getContentFolder(String folderName, ContentFolder parent, User user) {
		ContentFolder folder = getContentFolder(folderName, parent);
		folder.addUserRight(user, AccessRight.getFull());
		return folder;
	}

	/**
	 * Get default German language string
	 */
	public static String getDefaultLanguage() {
		return "de_DE";
	}

	/**
	 * Get default DateTime (2018-01-01 10:00)
	 */
	public static LocalDateTime getDateTime() {
		return LocalDateTime.of(2018, 1, 1, 10, 0);
	}

	/**
	 * Get specific Date/Time from YYYY-MM-DD and HH:MM format
	 */
	public static LocalDateTime getDateTime(String date, String time) {
		return LocalDateTime.of(
				LocalDate.parse(date, DateTimeFormatter.ISO_DATE),
				LocalTime.parse(time, DateTimeFormatter.ISO_TIME));
	}

	/**
	 * Get specific Date from YYYY-MM-DD format
	 */
	public static LocalDateTime getDateTime(String date) {
		return LocalDateTime.parse(date, DateTimeFormatter.ISO_DATE);
	}
}
