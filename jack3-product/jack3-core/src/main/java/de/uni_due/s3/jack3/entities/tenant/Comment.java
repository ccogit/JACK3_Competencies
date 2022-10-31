package de.uni_due.s3.jack3.entities.tenant;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.enums.ECommentThumb;

/**
 * Representation of a comment as a message to authors, which can be attached to the course, exercise or submission.
 */

@NamedQuery(
		name = Comment.COUNT_COMMENTS_FOR_COURSERECORD,
		query = "SELECT COUNT(c) FROM Submission s " //
		+ "LEFT JOIN s.comments as c " //
		+ "WHERE s.courseRecord = :courseRecord") //
@NamedQuery(
		name = Comment.COUNT_UNREAD_COMMENTS_FOR_COURSERECORD,
		query = "SELECT COUNT(c) FROM Submission s " //
		+ "LEFT JOIN s.comments as c " //
		+ "WHERE s.courseRecord = :courseRecord " //
		+ "AND c.isRead = false") //
@NamedQuery(
	name = Comment.COUNT_UNREAD_COMMENTS_FOR_COURSEOFFER,
	query = "SELECT COUNT(c) FROM Submission s " //
			+ "LEFT JOIN s.comments as c " //
			+ "WHERE s.courseRecord.courseOffer = :courseOffer " //
			+ "AND c.isRead = false") //
@NamedQuery(
	name = Comment.COUNT_NONTESTING_UNREAD_COMMENTS_FOR_COURSE_INCLUDING_FROZENREVISIONS,
	query = "SELECT COUNT(c) FROM Submission s " //
			+ "LEFT JOIN s.comments as c " //
			+ "WHERE (s.courseRecord.course.id = :courseId OR s.courseRecord.course.id IN (SELECT fc.id FROM FrozenCourse fc WHERE fc.proxiedCourseId = :courseId)) " //
			+ "AND s.courseRecord.isTestSubmission IS FALSE " //
			+ "AND c.isRead = false") //
@NamedQuery(
	name = Comment.COUNT_NONTESTING_UNREAD_COMMENTS_FOR_EXERCISE_INCLUDING_FROZENREVISIONS,
	query = "SELECT COUNT(c) FROM Submission s " //
			+ "LEFT JOIN s.comments as c " //
			+ "WHERE (s.exercise.id = :exerciseId OR s.exercise.id IN (SELECT fe.id FROM FrozenExercise fe WHERE fe.proxiedExerciseId = :exerciseId)) " //
			+ "AND s.isTestSubmission IS FALSE " //
			+ "AND c.isRead = false") //
@Audited
@Entity
public class Comment extends AbstractEntity {

	private static final long serialVersionUID = 6126993250485643809L;

	public static final String COUNT_COMMENTS_FOR_COURSERECORD = "Comment.countCommentsForCourseRecord";

	public static final String COUNT_UNREAD_COMMENTS_FOR_COURSERECORD = "Comment.countUnreadCommentsForCourseRecord";

	public static final String COUNT_UNREAD_COMMENTS_FOR_COURSEOFFER = "Comment.countUnreadCommentsForCourseOffer";

	public static final String COUNT_NONTESTING_UNREAD_COMMENTS_FOR_COURSE_INCLUDING_FROZENREVISIONS = "Comment.countNonTestingUnreadCommentsForCourseIncludingFrozenRevisions";
	public static final String COUNT_NONTESTING_UNREAD_COMMENTS_FOR_EXERCISE_INCLUDING_FROZENREVISIONS = "Comment.countNonTestingUnreadCommentsForExerciseIncludingFrozenRevisions";

	@Column
	@Type(type = "text")
	private String text;

	// REVIEW lg - Dieses Attribut wird nicht benutzt, zusammen mit der Klasse "ECommentThumb"
	@Enumerated(EnumType.STRING)
	private ECommentThumb commentThump;

	@Column
	private LocalDateTime timestamp;

	@Column
	private boolean isRead;

	@ManyToOne
	private User commentAuthor;

	@Column
	private boolean showEmail;

	Comment() {
	}

	public Comment(User commentAuthor, String text, boolean emailVisible) {
		this.commentAuthor = commentAuthor;
		this.text = text;
		timestamp = LocalDateTime.now();
		isRead = false;
		showEmail = emailVisible;
	}

	public User getCommentAuthor() {
		return commentAuthor;
	}

	public void setCommentAuthor(User commentAuthor) {
		this.commentAuthor = commentAuthor;
	}

	public String getText() {
		return text;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public boolean isRead() {
		return isRead;
	}

	public void setRead(boolean isRead) {
		this.isRead = isRead;
	}

	public boolean isShowEmail() {
		return showEmail;
	}

	public void setShowEmail(boolean showEmail) {
		this.showEmail = showEmail;
	}
}
