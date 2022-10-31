package de.uni_due.s3.jack3.entities.providers;

import java.util.List;
import java.util.Map;

import javax.persistence.Column;

import org.hibernate.annotations.Type;

import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;

// REVIEW lg - Wird (noch?) nirgends benutzt. Derzeit werden nur FolderExerciseProvider und FixedListExerciseProvider genutzt
public class RuleBasedExerciseProvider extends FolderExerciseProvider {

	private static final long serialVersionUID = 1L;

	@Column(nullable = false)
	@Type(type = "text")
	String tagRule;

	@Column(nullable = false)
	@Type(type = "text")
	List<String> languages;

	@Column(nullable = false)
	int minDifficulty;

	@Column(nullable = false)
	int maxDifficulty;

	public RuleBasedExerciseProvider() {
		// Empty constructor for Hibernate
	}

	public RuleBasedExerciseProvider(Map<ContentFolder, Integer> folders) {
		super(folders);
	}

	@Override
	public RuleBasedExerciseProvider deepCopy() {
		throw new UnsupportedOperationException(
				"Deep copying of " + this.getClass().getSimpleName() + " is not yet implemented");
	}

	// TODO: bo move courseEntries to fixedListExerciseProvider only
	@Override
	public List<CourseEntry> getCourseEntries() {
		// TODO Auto-generated method stub
		return null;
	}

}
