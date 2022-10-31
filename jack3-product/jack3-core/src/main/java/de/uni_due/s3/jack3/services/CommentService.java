package de.uni_due.s3.jack3.services;

import javax.ejb.Stateless;

import de.uni_due.s3.jack3.entities.tenant.Comment;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.Exercise;

/**
 * Service for managing {@link Comment} entities.
 */
@Stateless
public class CommentService extends AbstractServiceBean {

	/**
	 * Counts unread comments for all course records that belong to a course offer.
	 */
	public long countUnreadComments(CourseOffer courseOffer) {
		return getEntityManager()
				.createNamedQuery(Comment.COUNT_UNREAD_COMMENTS_FOR_COURSEOFFER, Long.class)
				.setParameter("courseOffer", courseOffer)
				.getSingleResult();
	}

	/**
	 * Counts unread comments for all non-testing course records that belong to a course, including all frozen versions
	 * of the course.
	 */
	public long countNontestingUnreadComments(Course course) {
		return getEntityManager()
				.createNamedQuery(Comment.COUNT_NONTESTING_UNREAD_COMMENTS_FOR_COURSE_INCLUDING_FROZENREVISIONS,
						Long.class)
				.setParameter("courseId", course.getId())
				.getSingleResult();
	}

	/**
	 * Counts unread comments for all non-testing course records that belong to a course, including all frozen versions
	 * of the course.
	 */
	public long countNontestingUnreadComments(Exercise exercise) {
		return getEntityManager()
				.createNamedQuery(Comment.COUNT_NONTESTING_UNREAD_COMMENTS_FOR_EXERCISE_INCLUDING_FROZENREVISIONS, Long.class)
				.setParameter("exerciseId", exercise.getId())
				.getSingleResult();
	}
}
