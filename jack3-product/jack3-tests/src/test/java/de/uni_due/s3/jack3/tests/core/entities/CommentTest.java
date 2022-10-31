package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.Comment;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.tests.annotations.NeedsExercise;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

/**
 * Test class for {@linkplain Comment} based on a sample exercise.
 *
 * @author lukas.glaser
 *
 */
@NeedsExercise
class CommentTest extends AbstractContentTest {

	private Comment comment = new Comment(user, "Comment Text", false);

	@Test
	void changeCommentAuthor() {
		assertEquals(user, comment.getCommentAuthor());

		User newUser = TestDataFactory.getUser("NewUser");
		userService.persistUser(newUser);

		comment.setCommentAuthor(newUser);

		assertNotEquals(user, comment.getCommentAuthor());
		assertEquals(newUser, comment.getCommentAuthor());
	}

	@Test
	void changeIsRead() {
		assertFalse(comment.isRead());
		comment.setRead(true);
		assertTrue(comment.isRead());
	}

	@Test
	void changeShowEmail() {
		assertFalse(comment.isShowEmail());
		comment.setShowEmail(true);
		assertTrue(comment.isShowEmail());
	}

	@Test
	void createCommentWithMessage() {
		Comment comment = new Comment(user, "Hello, World!", false);
		assertEquals("Hello, World!", comment.getText());
	}

	@Test
	void getTimestamp() {
		Comment comment = new Comment(user, "Hello, World!", false);
		//the comment is less than a minute old
		assertTrue(LocalDateTime.now().minusMinutes(1).isBefore(comment.getTimestamp()));
		//the comment was created in the past and not in the future
		assertTrue(comment.getTimestamp().isBefore(LocalDateTime.now())
				|| comment.getTimestamp().isEqual(LocalDateTime.now()));
	}
}
