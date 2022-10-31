package de.uni_due.s3.jack3.services;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import de.uni_due.s3.jack3.multitenancy.TenantIdentifier;

/**
 * <p>
 * This bean offers a simple way to send mails by offering a single method that returns
 * a builder object allowing to configure the new mail and finally sending it.
 * </p>
 * <p>
 * Sample usage:
 * </p>
 *
 * <pre>
 * {@code
 * mailService.createMail().withSender("JACK Support <jack@paluno.uni-due.de>")
 *   .withRecipients("SkaLa Support <skala@uni-due.de>")
 *   .withSubject("Test")
 *   .withHtml("Hallo <b>SkaLa</b> Support!")
 *   .send();
 * }
 * </pre>
 *
 * @see #createMail()
 * @see Mail
 */
@Stateless
public class EmailService extends AbstractServiceBean implements Serializable {

	private static final long serialVersionUID = 5701815270105458633L;

	/**
	 * This is a builder class for mails. You can send an email by first calling the class' setters
	 * and the calling the {@link #send()} method.
	 */
	public static final class Mail {

		/** The underlying MimeMessage that is wrapped by this instance. */
		private final MimeMessage message;

		/** The charset we use to encode the mail's content. */
		private final String charset;

		/**
		 * Creates a new Mail instance wrapping the given {@link MimeMessage} instance. This
		 * constructor is private to hide it from client code. Clients should call {@link
		 * EmailService#createMail()} instead.
		 *
		 * @param message
		 *            The MimeMessage instance to be wrapped.
		 * @throws MessagingException
		 *             If initializing the new mail failed. See the exception's error message for details.
		 * @see EmailService#createMail()
		 */
		private Mail(MimeMessage message, final Charset charset) throws MessagingException {
			this.message = message;
			this.charset = charset.name();
			message.setFrom();
		}

		/**
		 * Adds the given recipient to the mail's recipients. This methods behaves exactly as calling
		 * {@link #withRecipients(RecipientType, String)} with {@link RecipientType#TO}.
		 *
		 * @param addresses
		 *            The recipients' addresses.
		 * @return A reference to this object so you can chain commands.
		 * @throws MessagingException
		 *             If adding the new recipient failed. Please see the exception's message for details.
		 * @see #withRecipients(RecipientType, String)
		 */
		public final Mail withRecipients(final String addresses) throws MessagingException {
			return withRecipients(RecipientType.TO, addresses);
		}

		/**
		 * Adds the given recipient to the mail's recipients. The addresses must be a comma separated list of
		 * addresses specified in <a href="https://tools.ietf.org/html/rfc822">RFC822</a> syntax.
		 *
		 * @param addresses
		 *            The new recipient's address.
		 * @return A reference to this object so you can chain commands.
		 * @throws MessagingException
		 *             If adding the new recipient failed. Please see the exception's message for details.
		 */
		public final Mail withRecipients(final RecipientType recipientType, final String addresses)
				throws MessagingException {
			message.addRecipients(recipientType, InternetAddress.parse(addresses));
			return this;
		}

		/**
		 * Sets the mail's sender to the given address. The address must be a single internet address
		 * specified in <a href="https://tools.ietf.org/html/rfc822">RFC822</a> syntax.
		 *
		 * @param address
		 *            The address of the mail's sender
		 * @return A reference to this object so you can chain commands.
		 * @throws MessagingException
		 *             If setting the mail's sender failed. Please see the exception's message for details.
		 */
		public final Mail withSender(final String address) throws MessagingException {
			message.setFrom(address);
			return this;
		}

		/**
		 * Set the mail's subject to the given String.
		 *
		 * @param subject
		 *            The message's new subject.
		 * @return A reference to this object so you can chain commands.
		 * @throws MessagingException
		 *             If setting the subject failed. Please see the exception's message for details.
		 */
		public final Mail withSubject(final String subject) throws MessagingException {
			message.setSubject(subject, charset);
			return this;
		}

		/**
		 * Sets the mail's content to result of formatting the given pattern with the arguments and the mail's MIME type
		 * to "text/plain". This method uses MessageFormat#format(String,Object...) to format its input.
		 *
		 * @param pattern
		 *            The mail's new plaintext as a message format pattern.
		 * @param arguments
		 *            The arguments to format the pattern with.
		 * @return A reference to this object so you can chain commands.
		 * @throws MessagingException
		 *             If setting the mail's content failed. Please see the exception's message for details.
		 */
		public final Mail withPlainText(final String pattern, final Object... arguments) throws MessagingException {
			return withPlainText(MessageFormat.format(pattern, arguments));
		}

		/**
		 * Sets the mail's content to the given text and the mail's MIME type to "text/plain".
		 *
		 * @param text
		 *            The mail's new plaintext content.
		 * @return A reference to this object so you can chain commands.
		 * @throws MessagingException
		 *             If setting the mail's content failed. Please see the exception's message for details.
		 */
		public final Mail withPlainText(final String text) throws MessagingException {
			message.setText(text, charset);
			return this;
		}

		/**
		 * Sets the mail's content to the result of formatting the give pattern with the arguments assuming the result
		 * is HTML. This method uses MessageFormat#format(String,Object...) to format its input.
		 *
		 * @param pattern
		 *            The mail's new HTML as a message format pattern.
		 * @param arguments
		 *            The arguments to format the pattern with.
		 * @return A reference to this object so you can chain commands.
		 * @throws MessagingException
		 *             If setting the mail's content failed. Please see the exception's message for details.
		 */
		public final Mail withHtml(final String pattern, final Object... arguments) throws MessagingException {
			return withHtml(MessageFormat.format(pattern, arguments));
		}

		/**
		 * Sets the mail's content to the given text assuming it contains HTML.
		 *
		 * @param html
		 *            The mail's new HTML content.
		 * @return A reference to this object so you can chain commands.
		 * @throws MessagingException
		 *             If setting the mail's content failed. Please see the exception's message for details.
		 */
		public final Mail withHtml(final String html) throws MessagingException {
			message.setContent(html, "text/html; charset=" + charset);
			return this;
		}

		/**
		 * Attempts to immediately send this mail using the information set so far.
		 * Also the mail's sent date is set to the current time.
		 *
		 * @throws MessagingException
		 *             If sending the mail failed. Please see the exception's message for further details.
		 */
		public void send() throws MessagingException {
			message.setSentDate(new Date());
			Transport.send(message);
		}
	}

	/** The mail session used to send mails. */
	@Resource(name = "java:jboss/mail/jack")
	private Session session;

	@PostConstruct
	private void checkForTenantSpecificMailSession() {
		try {
			// We attempt to lookup a tenant specific mail session
			final String tenantId = TenantIdentifier.get();
			this.session = InitialContext.doLookup("java:jboss/mail/jack-" + tenantId);
			getLogger().infof("Using tenant specific mail session for tenant \"%s\".", tenantId);
		} catch (final NameNotFoundException e) {
			// If the name is not found we just use the globally configured one.
			getLogger().infof("There is no tenant specific mail session configured.");
		} catch (final NamingException e) {
			// The name was found but initialization failed. We do not use the globally configured
			// mail session in this case because the administrator willingly created a tenant specific one.
			this.session = null;
			getLogger().error("Failed to initialize tenant specific mail session.", e);
		}

		// We log an error message is no mail session is available at this point.
		if (session == null) {
			getLogger().error("No mail session is configured. The email service will be unavailable.");
		}
	}

	/**
	 * Returns a new Mail object which is basically a builder class for mails.
	 *
	 * @return a new Mail object.
	 * @throws MessagingException
	 *             If preparing the new mail failed. Please refer to the exception's message for details.
	 * @throws IllegalStateException
	 *             If the mail session is currently unavailable, most likely due
	 *             to no session being available because of a server misconfiguration.
	 */
	public Mail createMail() throws MessagingException {
		if (session == null)
			throw new MessagingException("No mail session is available.");

		return new Mail(new MimeMessage(session), StandardCharsets.UTF_8);
	}

	/**
	 * Returns {@code true} in case the session is connected or can be connected to,
	 * {@code false} otherwise. This method may execute rather slow as it can make a
	 * connection attempt.
	 * @return {@code true} in case the session is connected or can be connected to,
	 * {@code false} otherwise.
	 */
	public boolean isReady() {
		if (session == null)
			return false;

		try {
			if (session.getTransport().isConnected())
				return true;
		}
		catch (final NoSuchProviderException e) {
			throw new AssertionError(e);
		}

		try {
			session.getTransport().connect();
			return true;
		}
		catch (final MessagingException e) {
			getLogger().warn("Failed to test mail service connectivity.",e);
			return false;
		}
	}
}