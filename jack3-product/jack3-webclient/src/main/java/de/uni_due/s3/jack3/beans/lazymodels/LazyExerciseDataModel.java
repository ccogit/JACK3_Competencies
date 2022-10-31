package de.uni_due.s3.jack3.beans.lazymodels;

import java.util.List;
import java.util.Map;

import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.multitenancy.LoggerProvider;
import de.uni_due.s3.jack3.utils.StopWatch;

/**
 *
 * @author Benjamin.Otto
 *
 */
//REVIEW lg - Diese Klasse wird für die Revisionen genutzt und sollte dementsprechend auch so heißen.
public class LazyExerciseDataModel extends LazyDataModel<Exercise> {

	private static final long serialVersionUID = 1L;

	private ExerciseBusiness exerciseBusiness;

	private Exercise exercise;

	public LazyExerciseDataModel(Exercise exercise, ExerciseBusiness exerciseBusiness) {
		this.exerciseBusiness = exerciseBusiness;
		this.exercise = exercise;
	}

	@Override
	public List<Exercise> load(int first, int pageSize, String sortField, SortOrder sortOrder,
			Map<String, FilterMeta> filterBy) {

		String sortOrderString = "ASC";
		if (sortOrder == SortOrder.DESCENDING) {
			sortOrderString = "DSC";
		}

		StopWatch stopWatch = new StopWatch().start();

		// TODO: Respect "filterBy"
		List<Exercise> filteredRevisionsOfExercise = exerciseBusiness.getFilteredRevisionsOfExercise(exercise, first,
				pageSize, sortField, sortOrderString);

		setRowCount(exerciseBusiness.getNumberOfRevisions(exercise));

		LoggerProvider.get(getClass())
				.debug("Lazy loading exercise-revisions took " + stopWatch.stop().getElapsedMilliseconds());

		return filteredRevisionsOfExercise;

	}
}
