package de.uni_due.s3.jack3.entities.tenant;

import java.io.Serializable;
import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

/**
 * This class represents a bcrypt password consisting of a salt, a hash and a rounds parameter.
 * @author Björn Zurmaar
 */
@Embeddable
public class Password implements Serializable {

	private static final long serialVersionUID = -7700337616269774329L;

	/** The number of rounds as specified by the bcrypt standard. */
	@Min(value=4)
	@Max(value=31)
	@Column(nullable=true)
	private byte rounds;

	/** The password's salt which is always 128 bits long. */
	@Size(min=16, max=16)
	private byte[] salt;

	/** The password's hash which is always 184 bits long. */
	@Size(min=23, max=23)
	private byte[] hash;

	public Password() {}

	/**
	 * Creates a new bcrypt password with the given parameters.
	 * @param rounds The password's round parameter which must be between 4 and 31.
	 * @param salt The passwords's salt which must be 16 of length corresponding to 128 bits.
	 * @param hash The password's hash which must be 23 of length corresponding to 184 bits.
	 */
	public Password(final int rounds,final byte[] salt,final byte[] hash) {

		if ((rounds < 4) || (rounds > 31)) {
			throw new IllegalArgumentException("Illegal round number: " + rounds);
		}
		if (salt.length != 16) {
			throw new IllegalArgumentException("128 bit salt required.");
		}
		if (hash.length != 23) {
			throw new IllegalArgumentException("184 bit hash required.");
		}

		this.rounds = (byte)rounds;
		this.salt = Arrays.copyOf(salt,salt.length);
		this.hash = Arrays.copyOf(hash,hash.length);
	}

	public final byte getRounds() {
		return rounds;
	}

	public final byte[] getSalt() {
		return Arrays.copyOf(salt,salt.length);
	}

	public final byte[] getHash() {
		return Arrays.copyOf(hash,hash.length);
	}

	@Override
	public int hashCode() {
		return (Arrays.hashCode(hash) * 23)
			  + (Arrays.hashCode(salt) * 23)
			  + rounds;
	}

	@Override
	public boolean equals(final Object obj)	{
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof Password)) {
			return false;
		}

		final Password p = (Password)obj;
		return (p.rounds == rounds)
			&& Arrays.equals(p.salt,salt)
			&& Arrays.equals(p.hash,hash);
	}
}
