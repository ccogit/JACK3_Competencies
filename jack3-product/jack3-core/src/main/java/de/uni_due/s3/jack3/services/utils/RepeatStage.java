package de.uni_due.s3.jack3.services.utils;

import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;

/**
 * <p>
 * Pseudo class used in conjunction with {@link StageTransition} in order to distinguish between transitions that force
 * users to repeat the current stage and transitions that lead to a new instance of the very same stage.
 * </p>
 *
 * <p>
 * Instances of {@link RepeatStage} are not intended to be stored in the database. The class is not marked to be an
 * entity and its instances will always return 0 as their id.
 * </p>
 *
 */
public class RepeatStage extends Stage {

	private static final long serialVersionUID = 6900862596586018573L;

	@Override
	public long getId() {
		return 0;
	}

	@Override
	public RepeatStage deepCopy() {
		throw new UnsupportedOperationException(
				"Deep copying of " + this.getClass().getSimpleName() + " is not yet implemented");
	}

	@Override
	public boolean mustWaitForPendingJobs() {
		throw new UnsupportedOperationException();
	}
}
