package de.uni_due.s3.jack3.business;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.enterprise.context.ApplicationScoped;

import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.interfaces.BCryptPassword;
import org.wildfly.security.password.spec.EncryptablePasswordSpec;
import org.wildfly.security.password.spec.IteratedSaltedPasswordAlgorithmSpec;

import de.uni_due.s3.jack3.entities.tenant.Password;

/**
 * This class offers hashing passwords and checking if a given password matches a provided hash
 * using the bcrypt algorithm.
 * @author Björn Zurmaar
 */
@ApplicationScoped
public class BcryptBusiness extends AbstractBusiness {

	// SecureRandom is thread-safe so it's safe to share this instance between threads.
	private final SecureRandom secureRandom;

	/**
	 * Default constructor which just creates a new bcrypt service instance.
	 */
	public BcryptBusiness() {
		this.secureRandom = new SecureRandom();
	}

	/**
	 * Checks if the given plaintext password matches the given bcrypt hashed password. Returns
	 * {@code true} in case the password matches, {@code false} otherwise.
	 * @param password The bcrypt hashed password to check against.
	 * @param plainText The plaintext password to check.
	 * @return {@code true} in case the password matches, {@code false} otherwise.
	 * @throws NullPointerException If {@code plainText} or {@code password} is {@code null}.
	 */
	public boolean matches(final String plainText,final Password password){
		try {
			final BCryptPassword bcryptPassword = BCryptPassword.createRaw(
				BCryptPassword.ALGORITHM_BCRYPT,
				password.getHash(),
				password.getSalt(),
				password.getRounds());

			final PasswordFactory factory = getPasswordFactory();
			return factory.verify(
				factory.translate(bcryptPassword),
				plainText.toCharArray());
		}
		catch (final InvalidKeyException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Hashes the given plaintext password into the bcrypt format using the given number of rounds.
	 * @param plainText The password to be hashed.
	 * @return The result of hashing the given plaintext password.
	 * @throws NullPointerException If {@code plainText} is {@code null}.
	 */
	public Password createPassword(final String plainText) {
		try {
			// FIXME - make number of rounds configurable
			final KeySpec passwordSpec = createPasswordSpecification(plainText,12);
			final PasswordFactory factory = getPasswordFactory();
			BCryptPassword password = (BCryptPassword)factory.generatePassword(passwordSpec);
			return new Password(password.getIterationCount(),password.getSalt(),password.getHash());
		}
		catch (final InvalidKeySpecException e) {
			// If this exception occurs we failed at creating a correct specification.
			// It definitely is a programming error in this class then.
			throw new AssertionError(e);
		}
	}

	private KeySpec createPasswordSpecification(final String plainText,final int rounds) {
		if (rounds < 4 || rounds > 31)
			throw new IllegalArgumentException("Illegal round number: " + rounds);

		final byte[] salt = new byte[BCryptPassword.BCRYPT_SALT_SIZE];
		secureRandom.nextBytes(salt);

		return new EncryptablePasswordSpec(
			plainText.toCharArray(),
			new IteratedSaltedPasswordAlgorithmSpec(rounds,salt));
	}

	private PasswordFactory getPasswordFactory() {
		try {
			return PasswordFactory.getInstance(BCryptPassword.ALGORITHM_BCRYPT);
		}
		catch (final NoSuchAlgorithmException e) {
			// Wildlfy supports the bcrypt algorithm and hence this situation should never occur.
			throw new AssertionError(e);
		}
	}
}
