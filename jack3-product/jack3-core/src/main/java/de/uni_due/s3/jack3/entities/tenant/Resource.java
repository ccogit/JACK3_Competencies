package de.uni_due.s3.jack3.entities.tenant;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.Arrays;

import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

import de.uni_due.s3.jack3.annotations.DeepCopyOmitField;
import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;
import de.uni_due.s3.jack3.utils.ByteCount;
import de.uni_due.s3.jack3.utils.DeepCopyHelper;

/**
 * Abstract Superclass for all Types of Files, which can be attached to Exercises, Submissions or Courses.
 */
@Audited
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Resource extends AbstractEntity implements DeepCopyable<Resource>, Comparable<Resource> {

	private static final long serialVersionUID = 1828885637753965334L;

	@Column(nullable = false)
	@Lob
	protected byte[] content;

	@Column(nullable = false)
	protected LocalDateTime uploadTimestamp;

	// REVIEW bo: wozu brauchen wir das hier, ist das nicht redundant? Das führt zB beim Import zu dem Problem,
	// dass man im XStream.converter nicht sinnvoll den currentUser rausbekommt. An sich wäre "null = system" ja auch
	// irgendwo zu vertreten, aber schön ist das m.E. nicht
	@XStreamOmitField
	@ManyToOne(optional = true)
	@DeepCopyOmitField(
			copyTheReference = true,
			reason = "We must not deep copy referenced users")
	protected User lastEditor; // [0..1] null for System

	@ToString
	@Column(nullable = false)
	@Type(type = "text")
	protected String filename;

	@Column
	@Type(type = "text")
	@ColumnDefault("'description'")
	protected String description;

	public Resource() {
		super();
	}

	public Resource(String filename, byte[] content, User lastEditor, String description) {
		this.filename = requireIdentifier(filename, "You must specify a non-empty filename.");
		this.content = Arrays.copyOf(content, content.length);
		uploadTimestamp = LocalDateTime.now();
		this.lastEditor = lastEditor;
		this.description = description;
	}

	public byte[] getContent() {
		// REVIEW: warum wird hier immer erst eine Kopie erzeugt?
		return Arrays.copyOf(content, content.length);
	}

	public void setContent(byte[] content) {
		this.content = content;
	}

	public User getLastEditor() {
		return lastEditor;
	}

	public LocalDateTime getUploadTimestamp() {
		return uploadTimestamp;
	}

	protected void deepCopyResourceVars(Resource resourceToCloneFrom) {
		if (resourceToCloneFrom.content != null) {
			byte[] contentDeepCopy = new byte[resourceToCloneFrom.content.length];
			System.arraycopy(resourceToCloneFrom.content, 0, contentDeepCopy, 0, resourceToCloneFrom.content.length);
			content = contentDeepCopy;
		}

		// LocalDateTime is immutable, so no need to deepCopy explicitly
		uploadTimestamp = resourceToCloneFrom.uploadTimestamp;
		lastEditor = DeepCopyHelper.getCorrespondingUserFromMainDb(resourceToCloneFrom.getLastEditor()).orElse(null);

		this.filename = resourceToCloneFrom.getFilename();
		this.description = resourceToCloneFrom.getDescription();
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * Returns an educated guess of the attachments mime type according to its filename. E.g. for a
	 * filename of "myfile.jpg" this method will return "image/jpeg".
	 *
	 * @return The attachments mime type.
	 * @see MimetypesFileTypeMap
	 */
	public String getMimeType() {
		final FileTypeMap ftm = MimetypesFileTypeMap.getDefaultFileTypeMap();
		return ftm.getContentType(filename);
	}

	/**
	 * Returns the attachments mediatype, i.e. the first part of the MIME type.
	 *
	 * @return The attachments mediatype, i.e. the first part of the MIME type.
	 * @see #getMimeType()
	 */
	public String getMediaType() {
		final String mimeType = getMimeType();
		return mimeType.substring(0, mimeType.indexOf('/'));
	}

	/**
	 * Returns the attachments size, i.e. the number of bytes in its content.
	 *
	 * @return The attachments size.
	 */
	public int getSize() {
		return content.length;
	}

	/**
	 * Returns the attachments size in a human readable string
	 *
	 * @return
	 *         the attachments size in a human readable string defining the given number of bytes with
	 *         SI prefixes.
	 */
	public String getSizeHumanReadable() {
		return ByteCount.toSIString(getSize());
	}

	/**
	 * Writes the attachments content to the outputstream {@code out}. This is far more effective
	 * than getting the content by {@link #getContent()} and writing it manually as this method
	 * doesn't need to create a defensive copy.
	 *
	 * @param out
	 *            The outputstream the attachments content should be written to.
	 * @throws IOException
	 *             If there is an {@link IOException} while writing to
	 *             the stream.
	 */
	public void writeToStream(final OutputStream out) throws IOException {
		out.write(content);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public int compareTo(Resource other) {
		return filename.compareTo(other.filename);
	}

}
