package de.uni_due.s3.jack3.entities;

import java.io.Serializable;
import java.util.Set;
import java.util.StringJoiner;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.google.common.collect.ImmutableSet;

import de.uni_due.s3.jack3.entities.tenant.CourseOffer;

/**
 * <p>
 * This immutable class based on bit flags saves the different rights users can have on folders. Note that some flags
 * contain other flags, e.g. {@link #WRITE} contains {@link #READ}.
 * </p>
 * 
 * <h2>Usage</h2>
 * <p>
 * Use the flag constants and {@link #getFromFlags(int...)} for getting an access right granting the specified flags:
 * </p>
 * 
 * <pre>
 * AccessRight read = AccessRight.getFromFlags(AccessRight.READ);
 * AccessRight readWrite = AccessRight.getFromFlags(AccessRight.READ, AccessRight.WRITE);
 * </pre>
 * 
 * <p>
 * There are two special rights:
 * </p>
 * <ul>
 * <li>The <code>NONE</code> right contains no flag. It indicates that a user has no rights on the folder. This means
 * that the user cannot read settings and submissions for content in this folder. Users who don't have rights for
 * {@linkplain CourseOffer}s can interact with them as students, e.g. they enroll and submit exercises. This right is
 * only used as a returning value in Business methods, not stored in database. You can get this right via
 * {@link #getNone()}.</li>
 * <li>The <code>FULL</code> right combines all flags, a user who has this right has full access to the folder. You can
 * get it via {@link #getFull()}.</li>
 * </ul>
 */
@Immutable
public final class AccessRight implements Serializable {
	
	private static final long serialVersionUID = -6506353538000298475L;

	/*- H I N T : Steps for adding new flags:
	 * 1. Increment COUNT_FLAGS
	 * 2. Add a leading zero to all existing bit constants so that all constants have the new length.
	 * 3. Add a new integer constant with a leading 1, followed by zeros with the length of MAX_BITFlAG_LENGTH.
	 *    If the new right requires READ (or other rights): Add "| READ" (or other right) to the Integer constant
	 * 5. Add the constant to ALL_FLAGS.
	 * 6. Implement "is<Flag>()" method
	 * 7. Extend "toString()"
	 * 8. The following actions on the UI-side must be performed:
	 *    1. Add the new right and translations to the "AccessRight" key in the i18n file
	 *    2. Update "refreshShownAccessRights" method in AbstractContentTreeView
	 *    3. Update userRightsDialog.xhtml: Add 2 new columns, one for user rights, one for group rights
	 *       (you can copy an existing column and rename all references to the new right)
	 *    4. UserRightsData: Add 2 new fields, one for direct rights, one for inherited rights, add the new right in the
	 *       "getRights()" and "updateData()" method, add getters and setters and update "is...Immutable" methods if
	 *       this right requires another.
	 *    5. Update UserRightsDialogView: Add actions to updateUserRights method if the right e.g. need READ right
	 *    6. userManagement.xhtml: Add new columns to the existing rights tables
	 *       (ids: columnTenant*Rights, userGroupRightsData*Rights)
	 */

	/**
	 * Number of flags that are currently supported.
	 */
	public static final short COUNT_FLAGS = 5;

	// Bit flags in the format GRADE (G) - WRITE (W) - EXTENDED_READ (E) - READ (R)
	// Add new flags on the left side of the bit pattern
	/**
	 * Right to read exercise or course (offer) configurations, can use exercises in courses and courses in course
	 * offers, can export exercise and course, can see pseudonymous submissions, can test exercise or course and see own
	 * test submissions.
	 */
	public static final int READ = 0b00001;
	/**
	 * Right to read and export all submissions for exercises, courses and course offers without pseudonymization. This
	 * includes the {@link #READ} right.
	 */
	public static final int EXTENDED_READ = 0b00010 | READ;
	/**
	 * Right to edit and create new exercises, courses and course offers, including delete and move content and sub
	 * folders. Data can be moved only if there is no change in their rights. This includes the {@link #READ} right.
	 */
	public static final int WRITE = 0b00100 | READ;
	/**
	 * Right to perform actions on submissions that affect the score, e.g. manual feedback. This includes the
	 * {@link #READ} right.
	 */
	public static final int GRADE = 0b01000 | READ;
	/**
	 * Right to edit rights for folders and move content, even if there are right changes. This includes {@link #READ},
	 * {@link #EXTENDED_READ}, {@link #WRITE} and {@link #GRADE}.
	 */
	public static final int MANAGE = 0b10000 | READ | EXTENDED_READ | WRITE | GRADE;

	private static final Set<Integer> ALL_FLAGS = ImmutableSet.of(READ, EXTENDED_READ, WRITE, GRADE, MANAGE);

	/**
	 * The flags in bit representation: WRITE - EXTENDED_READ - READ
	 */
	private final int bitFlag;

	private AccessRight(int bitFlag) {
		if (bitFlag < 0) {
			throw new IllegalArgumentException("Bit flag must not be negative.");
		}
		if (Integer.toBinaryString(bitFlag).length() > COUNT_FLAGS) {
			throw new IllegalArgumentException(bitFlag + " is too big, maximum is " + COUNT_FLAGS + " bits.");
		}

		this.bitFlag = bitFlag;
	}

	/**
	 * Constructs a right object from the given flags.
	 * 
	 * @param flags
	 *            An array of flags. Each flag must be a flag value specified in this class.
	 * @return A right object that contains the flags. If no flags are passed, the returned right object has no flags,
	 *         i.e. no rights are granted.
	 */
	@Nonnull
	public static AccessRight getFromFlags(int... flags) {
		int bitFlag = 0;
		for (final int i : flags) {
			// Check if the flag is a valid flag, e.g. we don't want to allow WRITE without READ
			if (!ALL_FLAGS.contains(i)) {
				throw new IllegalArgumentException(i + "(" + Integer.toBinaryString(i) + ") is no valid flag.");
			}
			bitFlag |= i;
		}
		return new AccessRight(bitFlag);
	}

	/**
	 * Constructs a right object from a given flag value.
	 */
	// Called from the converter.
	@Nonnull
	public static AccessRight getFromBitFlag(int bitFlag) {
		return new AccessRight(bitFlag);
	}

	/**
	 * Returns access right object with no flags, i.e. no rights are granted to the user.
	 */
	@Nonnull
	public static AccessRight getNone() {
		return new AccessRight(0);
	}

	/**
	 * Returns access right object with all flags set.
	 */
	@Nonnull
	public static AccessRight getFull() {
		int bitFlag = 0;
		for (int flag : ALL_FLAGS) {
			bitFlag |= flag;
		}
		return new AccessRight(bitFlag);
	}

	/**
	 * Returns the union of both rights, not changing the object's value. The parameter may be {@code null}.
	 */
	@Nonnull
	public AccessRight add(AccessRight otherRight) {
		if (otherRight == null) {
			return new AccessRight(bitFlag);
		} else {
			return new AccessRight(bitFlag | otherRight.bitFlag);
		}
	}

	/**
	 * Returns the combination of the right with a flag.
	 * 
	 * @param flag
	 *            Must be a valid flag value specified in this class.
	 * @return Union of the right and the right the given flag grants.
	 */
	@Nonnull
	public AccessRight add(int flag) {
		// Check if the flag is valid
		if (!ALL_FLAGS.contains(flag)) {
			throw new IllegalArgumentException(flag + "(" + Integer.toBinaryString(flag) + ") is no valid flag.");
		}
		return new AccessRight(bitFlag | flag);
	}

	/**
	 * Returns an integer whose bit representation holds the flags.
	 */
	public int getBitFlag() {
		return bitFlag;
	}

	/**
	 * Returns wether no rights are granted.
	 */
	public boolean isNone() {
		return bitFlag == 0;
	}

	/**
	 * Returns wether the access right grants {@link #READ} access.
	 */
	public boolean isRead() {
		return is(READ);
	}

	/**
	 * Returns wether the access right grants {@link #EXTENDED_READ} access.
	 */
	public boolean isExtendedRead() {
		return is(EXTENDED_READ);
	}

	/**
	 * Returns wether the access right grants {@link #WRITE} access.
	 */
	public boolean isWrite() {
		return is(WRITE);
	}

	/**
	 * Returns wether the access right grants {@link #GRADE} access.
	 */
	public boolean isGrade() {
		return is(GRADE);
	}

	/**
	 * Returns wether the access right grants {@link #MANAGE} access.
	 */
	public boolean isManage() {
		return is(MANAGE);
	}

	private boolean is(int flag) {
		return (bitFlag & flag) == flag;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof AccessRight))
			return false;

		final AccessRight other = (AccessRight) obj;
		return bitFlag == other.bitFlag;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(bitFlag);
	}

	/**
	 * <p>
	 * Returns 'AccessRight' together with all rights or <code>'AccessRight(NONE)'</code>.
	 * </p>
	 * 
	 * <p>
	 * Example: <code>AccessRight(READ,WRITE)</code>
	 * </p>
	 */
	@Override
	public String toString() {
		if (isNone())
			return "AccessRight(NONE)";

		StringJoiner joiner = new StringJoiner(",", "AccessRight(", ")");
		if (isRead())
			joiner.add("READ");
		if (isExtendedRead())
			joiner.add("EXTENDED_READ");
		if (isWrite())
			joiner.add("WRITE");
		if (isGrade())
			joiner.add("GRADE");
		if (isManage())
			joiner.add("MANAGE");
		return joiner.toString();
	}
}
