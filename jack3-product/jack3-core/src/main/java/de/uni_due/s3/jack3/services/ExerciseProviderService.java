package de.uni_due.s3.jack3.services;

import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;

import de.uni_due.s3.jack3.entities.providers.AbstractExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FixedListExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;

/**
 * Service for managing entities derived from {@link AbstractExerciseProvider}.
 * 
 * @author nils.schwinning
 */
@Stateless
public class ExerciseProviderService extends AbstractServiceBean {

	@Inject
	private BaseService baseService;

	public Optional<FixedListExerciseProvider> getFixedListExerciseProviderByID(long id) {
		return baseService.findById(FixedListExerciseProvider.class, id, false);
	}

	public Optional<FolderExerciseProvider> getFolderExerciseProviderByID(long id) {
		return baseService.findById(FolderExerciseProvider.class, id, false);
	}

}
