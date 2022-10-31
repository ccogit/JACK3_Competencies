package de.uni_due.s3.jack3.utils;

/**
 * Utility class that converts a number of bytes, e.g. the size of a file, to a
 * human readable string representation with SI or IEC prefixes.
 *
 * @author Björn Zurmaar
 */
public class ByteCount {

	private ByteCount() {
		throw new AssertionError("This class shouldn't be instantiated.");
	}

	/**
	 * Returns a human readable string representation of the given number of
	 * bytes as defined in IEC 60027-2. The unit is selected so that the number
	 * returned is larger than 1 and smaller than 1024. This method uses the
	 * following units:
	 * <ul>
	 * <li>Kibibyte (KiB) = 2<sup>10</sup> Byte</li>
	 * <li>Mebibyte (MiB) = 2<sup>20</sup> Byte</li>
	 * <li>Gibibyte (GiB) = 2<sup>30</sup> Byte</li>
	 * <li>Tebibyte (TiB) = 2<sup>40</sup> Byte</li>
	 * <li>Pebibyte (PiB) = 2<sup>50</sup> Byte</li>
	 * <li>Exbibyte (EiB) = 2<sup>60</sup> Byte</li>
	 * <li>Zebibyte (ZiB) = 2<sup>70</sup> Byte</li>
	 * <li>Yobibyte (YiB) = 2<sup>80</sup> Byte</li>
	 * </ul>
	 *
	 * @param bytes
	 *            The byte number to be converted.
	 * @return A human readable IEC string.
	 */
	public static final String toIECString(final long bytes) {

		if (bytes < 1024) {
			return bytes + " B";
		}

		final int exp = (int) (Math.log(bytes) / Math.log(1024));
		final char prefix = "KMGTPEZY".charAt(exp - 1);

		return String.format("%.1f %ciB", bytes / Math.pow(1024, exp), prefix);
	}

	/**
	 * Returns a human readable string representation of the given number of
	 * bytes using SI prefixes. The prefix is selected so that the resulting
	 * number equal to or larger than 1 and smaller than 1000. This methods uses
	 * the following units:
	 * <ul>
	 * <li>Kilobyte (KB) = 10<sup>3</sup> Byte</li>
	 * <li>Megabyte (MB) = 10<sup>6</sup> Byte</li>
	 * <li>Gigabyte (GB) = 10<sup>9</sup> Byte</li>
	 * <li>Terabyte (TB) = 10<sup>12</sup> Byte</li>
	 * <li>Petabyte (PB) = 10<sup>15</sup> Byte</li>
	 * <li>Exabyte (EB) = 10<sup>18</sup> Byte</li>
	 * <li>Zettabyte (ZB) = 10<sup>21</sup> Byte</li>
	 * <li>Yottabyte (YB) = 10<sup>24</sup> Byte</li>
	 * </ul>
	 *
	 * @param bytes
	 *            The byte number to be converted.
	 * @return A human readable string defining the given number of bytes with
	 *         SI prefixes.
	 */
	public static final String toSIString(final long bytes) {

		if (bytes < 1000) {
			return bytes + " B";
		}

		final int exp = (int) (Math.log(bytes) / Math.log(1000));
		final char prefix = "KMGTPEZY".charAt(exp - 1);

		return String.format("%.1f %cB", bytes / Math.pow(1000, exp), prefix);
	}

	/**
	 * Returns a human readable string representation of the given byte count in
	 * IEC units.
	 * 
	 * @param bytes
	 *            The number of bytes to be converted to a string.
	 * @return A human readable string representation of the given byte count.
	 * @see ByteCount#toIECString(long)
	 */
	public static final String toString(final long bytes) {
		return toIECString(bytes);
	}
}
