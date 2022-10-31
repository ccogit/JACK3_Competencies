package de.uni_due.s3.jack3.utils;

import java.util.Optional;

import javax.enterprise.inject.spi.CDI;

import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;
import de.uni_due.s3.jack3.services.UserService;

/**
 * Helper class for creating deep copies.
 * 
 * @author Benjamin.Otto
 */
public class DeepCopyHelper {

	private DeepCopyHelper() {
		throw new IllegalStateException("Static utility class");
	}

	public static <T extends DeepCopyable<T>> T deepCopyOrNull(T deepCopyableEntity) {
		if (deepCopyableEntity == null) {
			return null;
		} else {
			return deepCopyableEntity.deepCopy();
		}
	}

	public static Optional<User> getCorrespondingUserFromMainDb(User user) {
		if (user == null) {
			return Optional.empty();
		}
		UserService userService = CDI.current().select(UserService.class).get();

		return userService.getUserById(user.getId());
	}

}
