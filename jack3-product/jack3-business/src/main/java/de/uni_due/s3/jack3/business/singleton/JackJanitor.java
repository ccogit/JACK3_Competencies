package de.uni_due.s3.jack3.business.singleton;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.inject.Inject;

import de.uni_due.s3.jack3.entities.maintenance.TempDir;
import de.uni_due.s3.jack3.services.AbstractServiceBean;
import de.uni_due.s3.jack3.services.BaseService;
import de.uni_due.s3.jack3.utils.JackFileUtils;

/**
 * This class can be used to execute background maintainance-tasks
 *
 * @author Benjamin Otto
 *
 */
@Singleton
public class JackJanitor extends AbstractServiceBean {

	@Inject
	BaseService baseService;

	/**
	 * When exporting a folder structure the directories can't be deleted immediately while they are streamed to the
	 * user. We look every hour if there are dirs to be deleted and delete them if they are at least 5 minutes old
	 */
	@Schedule(hour = "*", minute = "1", second = "*", persistent = true)
	public void cleanTempDirs() {
		baseService.findAll(TempDir.class) //
		.stream() //
		.filter(this::atLeastFiveMinutesOld) //
		.forEach(dir -> {
			getLogger().info("Deleting " + dir);
			JackFileUtils.deleteDirIfExists(dir.getPath());
			baseService.deleteEntity(dir);
		});
	}

	private boolean atLeastFiveMinutesOld(TempDir dir) {
		return dir.getCreated().until(LocalDateTime.now(), ChronoUnit.MINUTES) >= 5;
	}
}
