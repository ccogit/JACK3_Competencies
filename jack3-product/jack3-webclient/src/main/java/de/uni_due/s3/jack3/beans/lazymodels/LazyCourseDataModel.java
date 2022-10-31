package de.uni_due.s3.jack3.beans.lazymodels;

import java.util.List;
import java.util.Map;

import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.multitenancy.LoggerProvider;
import de.uni_due.s3.jack3.utils.StopWatch;

/**
 *
 * @author Benjamin.Otto
 *
 */
// REVIEW lg - Diese Klasse wird für die Revisionen genutzt und sollte dementsprechend auch so heißen.
public class LazyCourseDataModel extends LazyDataModel<Course> {

	private static final long serialVersionUID = 1L;

	private CourseBusiness courseBusiness;

	private Course course;

	public LazyCourseDataModel(Course course, CourseBusiness courseBusiness) {
		this.course = course;
		this.courseBusiness = courseBusiness;
	}

	@Override
	public List<Course> load(int first, int pageSize, String sortField, SortOrder sortOrder,
			Map<String, FilterMeta> filterBy) {

		String sortOrderString = "ASC";
		if (sortOrder == SortOrder.DESCENDING) {
			sortOrderString = "DSC";
		}

		StopWatch stopWatch = new StopWatch().start();

		// TODO: respect "filterBy"
		List<Course> filteredRevisionsForCourseWithLazyData = courseBusiness.getFilteredRevisionsForCourse(course,
				first, pageSize, sortField, sortOrderString);

		setRowCount(courseBusiness.getNumberOfRevisions(course));

		LoggerProvider.get(getClass())
				.debug("Lazy loading course-revisions took " + stopWatch.stop().getElapsedMilliseconds());

		return filteredRevisionsForCourseWithLazyData;

	}
}
