package de.uni_due.s3.jack3.entities.converters;

import javax.persistence.AttributeConverter;

import org.jboss.logging.Logger;

import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.multitenancy.LoggerProvider;

/**
 * This bidirectional converter makes it possible to store flags described by {@link AccessRight} instances directly as
 * integers into the database.
 */
public class AccessRightConverter implements AttributeConverter<AccessRight, Integer> {

	private Logger logger = LoggerProvider.get(AccessRightConverter.class);
	private static boolean warnMessageShown = false;

	@Override
	public Integer convertToDatabaseColumn(AccessRight attribute) {
		return attribute.getBitFlag();
	}

	@Override
	public AccessRight convertToEntityAttribute(Integer dbData) {
		if (dbData == null)
			return AccessRight.getNone();

		int maxFlagValue = 1 << AccessRight.COUNT_FLAGS; // same as 2 ^ AccessRight.COUNT_FLAGS
		if (dbData > maxFlagValue) {
			// dbData contains more flags than allowed
			if (!warnMessageShown) {
				logger.warnf("Bit flag '%s' (%s) contains more bits than allowed (%s), leading bits are ignored.",
						Integer.toBinaryString(dbData), dbData, AccessRight.COUNT_FLAGS);
				warnMessageShown = true;
			}
			dbData = dbData % maxFlagValue;
		}
		return AccessRight.getFromBitFlag(dbData);
	}

}
