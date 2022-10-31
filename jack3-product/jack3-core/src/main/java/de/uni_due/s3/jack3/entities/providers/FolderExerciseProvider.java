package de.uni_due.s3.jack3.entities.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.enterprise.inject.spi.CDI;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OrderColumn;

import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.enums.ECourseExercisesOrder;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;
import de.uni_due.s3.jack3.services.FolderService;

/**
 * Provides the exercise list for a course based on selected folders. All exercises in the listed folders with all
 * subfolders (recursive) are to be included in the course.
 */
@Audited
@Entity
public class FolderExerciseProvider extends AbstractExerciseProvider implements DeepCopyable<FolderExerciseProvider> {

	private static final long serialVersionUID = 7389463644619217364L;

	/** Use all exercises from a folder and not only a specified number of exercises */
	public static final int ALL_EXERCISES = 0;

	/**
	 * Map with all folders whose exercises should be included in the course and the number of exercises which should be
	 * used from the folder.
	 * The keys are the contentFolders and the values are the number of exercises which should be used in the course
	 */
	@ToString
	@ElementCollection(fetch = FetchType.EAGER)
	@OrderColumn
	private Map<ContentFolder, Integer> folders = new LinkedHashMap<>();

	public FolderExerciseProvider() {
		// Empty constructor for Hibernate
	}

	public FolderExerciseProvider(Map<ContentFolder, Integer> folders) {
		this.folders.putAll(folders);
	}

	// TODO: bo move courseEntries to fixedListExerciseProvider only (or just return the exercises, that are contained
	// in one of the referenced folders?
	@Override
	public List<CourseEntry> getCourseEntries() {
		// We need the folder service to prevent a LazyInitializationException (see #1144)
		final FolderService folderService = CDI.current().select(FolderService.class).get();

		return folders.keySet().stream()
			.map(folderService::getContentFolderWithLazyData)
			.flatMap(f -> f.getChildrenExercises().stream())
			.map(e -> new CourseEntry(e,0))
			.collect(Collectors.toList());
	}

	/**
	 * @return unmodifiableList of folders
	 */
	public List<ContentFolder> getFolders() {
		return Collections.unmodifiableList(new ArrayList<>(folders.keySet()));
	}

	public Map<ContentFolder, Integer> getFoldersMap() {
		return folders;
	}

	public void addFolder(ContentFolder folder) {
		folders.put(folder, ALL_EXERCISES);
	}

	public void addFolder(ContentFolder folder, int numberOfUsedExercises) {
		folders.put(folder, numberOfUsedExercises);
	}

	public void removeFolder(ContentFolder folder) {
		folders.remove(folder);
	}

	/**
	 * If the given folder should currently use all of the childExercises, change it so that the folder now uses a
	 * concrete number of the childExercises.
	 *
	 * If the given folder should currently use a concrete number of the childExercises, change it so the folder now
	 * uses all of the childExercises
	 */
	public void switchAllExerciseSetting(ContentFolder folder, int maxNumber) {
		if (!folders.containsKey(folder))
			throw new IllegalArgumentException("Folder is expected to be listet in the content provider.");

		if (folders.get(folder) == ALL_EXERCISES) {
			folders.replace(folder, maxNumber);
		} else {
			folders.replace(folder, ALL_EXERCISES);
		}
	}

	@Override
	public FolderExerciseProvider deepCopy() {
		FolderExerciseProvider copy = new FolderExerciseProvider();
		for (Entry<ContentFolder, Integer> entry : folders.entrySet()) {
			copy.addFolder(entry.getKey(), entry.getValue().intValue());
		}

		return copy;
	}

	@Override
	public boolean isExerciseOrderSupported(ECourseExercisesOrder order) {
		if (order == null) {
			return false;
		}
		if (order == ECourseExercisesOrder.POINTS_ASCENDING || order == ECourseExercisesOrder.POINTS_DESCENDING) {
			return false;
		}
		return order != ECourseExercisesOrder.MANUAL;
	}
}
