package de.uni_due.s3.jack3.business;

import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.hibernate.Hibernate;

import de.uni_due.s3.jack3.business.microservices.CalculatorBusiness;
import de.uni_due.s3.jack3.business.microservices.ConverterBusiness;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.InternalErrorEvaluatorException;
import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.entities.tenant.CourseRecord;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression.EDomain;
import de.uni_due.s3.jack3.entities.tenant.FrozenCourse;
import de.uni_due.s3.jack3.entities.tenant.ResultFeedbackMapping;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;
import de.uni_due.s3.jack3.services.CourseRecordService;
import de.uni_due.s3.jack3.services.CourseService;
import de.uni_due.s3.jack3.services.SubmissionService;
import de.uni_due.s3.jack3.utils.StopWatch;

@ApplicationScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class CoursePlayerBusiness extends AbstractBusiness {

	@Inject
	private CourseService courseService;

	@Inject
	private CourseRecordService courseRecordService;

	@Inject
	private SubmissionService submissionService;

	@Inject
	private CalculatorBusiness evaluatorBusiness;

	@Inject
	private ConverterBusiness converterBusiness;

	public CourseRecord updateCourseResult(CourseRecord courseRecord) {
		AbstractCourse course = courseRecord.getCourse();
		if (!course.isFrozen()) {
			// Make sure we use the latest version in case of non-frozen courses
			course = courseService.getCourseWithLazyDataByCourseID(course.getId())
					.orElseThrow(NoSuchJackEntityException::new);
		}

		if (course.getContentProvider() instanceof FixedListExerciseProvider) {
			return updateCourseResultForFixedList(courseRecord,
					(FixedListExerciseProvider) course.getContentProvider());
		}
		if (course.getContentProvider() instanceof FolderExerciseProvider) {
			return updateCourseResultForFolders(courseRecord, (FolderExerciseProvider) course.getContentProvider());
		}

		return courseRecord;
	}

	private CourseRecord updateCourseResultForFolders(CourseRecord courseRecord,
			final FolderExerciseProvider folderExerciseProvider) {
		final var sw = new StopWatch().start();
		if (!Hibernate.isInitialized(courseRecord.getExercises())) {
			courseRecord = courseRecordService.getCourseRecordWithExercises(courseRecord.getId())
					.orElseThrow(NoSuchJackEntityException::new);
		}
		getLogger().debugf("Loading course record exercises took %s", sw.stop().getElapsedMilliseconds());
		Optional<Submission> submission = Optional.empty();
		final AbstractCourse course = courseRecord.getCourse();
		final Set<AbstractExercise> exercises = courseRecord.getExercises();

		double pointSum = 0.0;
		double exerciseCount = exercises.size();

		for (AbstractExercise exercise : exercises) {
			if (course.getScoringMode() == ECourseScoring.LAST) {
				submission = submissionService.getLatestSubmissionForCourseRecordAndExercise(courseRecord, exercise);
			} else if (course.getScoringMode() == ECourseScoring.BEST) {
				submission = submissionService.getBestSubmissionForCourseRecordAndExercise(courseRecord, exercise);
			}
			if (submission.isPresent()) {
				pointSum += submission.get().getResultPoints();
			}
		}

		if (exerciseCount > 0) {
			courseRecord.setResultPoints((int) Math.round(pointSum / exerciseCount));
			return updateCourseRecord(courseRecord);
		}

		return courseRecord;
	}

	private CourseRecord updateCourseResultForFixedList(CourseRecord courseRecord,
			final FixedListExerciseProvider fixedListExerciseProvider) {
		Optional<Submission> submission = Optional.empty();
		final AbstractCourse course = courseRecord.getCourse();

		int pointSum = 0;
		double weightSum = 0.0;

		for (final CourseEntry courseEntry : fixedListExerciseProvider.getCourseEntries()) {
			weightSum += courseEntry.getPoints();

			AbstractExercise exercise = courseEntry.getFrozenExercise();
			if (exercise == null) {
				exercise = courseEntry.getExercise();
			}

			if (course.getScoringMode() == ECourseScoring.LAST) {
				submission = submissionService.getLatestSubmissionForCourseRecordAndExercise(courseRecord, exercise);
			} else if (course.getScoringMode() == ECourseScoring.BEST) {
				submission = submissionService.getBestSubmissionForCourseRecordAndExercise(courseRecord, exercise);
			}
			if (submission.isPresent()) {
				pointSum += submission.get().getResultPoints() * courseEntry.getPoints();
			}
		}

		if (weightSum > 0) {
			courseRecord.setResultPoints((int) Math.round(pointSum / weightSum));
			return updateCourseRecord(courseRecord);
		}

		return courseRecord;
	}

	public CourseRecord updateCourseRecord(CourseRecord courseRecord) {
		AbstractCourse abstractCourse = courseRecord.getCourse();
		Set<ResultFeedbackMapping> resultFeedbackMappings;

		long courseId = abstractCourse.getId();
		if (abstractCourse.isFrozen()) {
			FrozenCourse frozen = courseService.getFrozenCourse(courseId).orElseThrow(NoSuchJackEntityException::new);
			resultFeedbackMappings = frozen.getResultFeedbackMappings();
		} else {
			Course course = courseService.getCourseWithLazyDataByCourseID(courseId)
					.orElseThrow(NoSuchJackEntityException::new);
			resultFeedbackMappings = course.getResultFeedbackMappings();
		}

		StringBuilder feedback = new StringBuilder();
		EvaluatorMaps evaluatorMaps = new EvaluatorMaps();
		evaluatorMaps.addMetaVariable("currentResult", courseRecord.getResultPoints());

		// TODO ms: Add more meta variables here?
		for (ResultFeedbackMapping feedbackMapping : resultFeedbackMappings) {
			EvaluatorExpression expression = new EvaluatorExpression(feedbackMapping.getExpression());
			expression.setDomain(EDomain.MATH);
			try {
				if (evaluatorBusiness.calculateToBoolean(expression, evaluatorMaps)) {
					if (!feedbackMapping.getTitle().isEmpty()) {
						feedback.append("<p><b>");
						feedback.append(converterBusiness.replaceVariablesByVariableName(feedbackMapping.getTitle(),
								evaluatorMaps));
						feedback.append("</b></p>");
					}
					feedback.append("<p>");
					feedback.append(
							converterBusiness.replaceVariablesByVariableName(feedbackMapping.getText(), evaluatorMaps));
					feedback.append("</p>");
				}
			} catch (InternalErrorEvaluatorException iee) {
				getLogger().warnf(iee,
						"Cannot evaluate feedback expression %s in course %s. Feedback may thus be incorrect!",
						expression, abstractCourse);
			}
		}

		courseRecord.setCourseFeedback(feedback.toString());
		return courseRecordService.mergeCourseRecord(courseRecord);
	}

}
