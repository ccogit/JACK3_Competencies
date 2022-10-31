package de.uni_due.s3.jack3.entities.stagetypes.python;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.uni_due.s3.jack3.annotations.DeepCopyOmitField;
import de.uni_due.s3.jack3.entities.tenant.CheckerConfiguration;
import de.uni_due.s3.jack3.entities.tenant.ExerciseResource;

@Audited
@Entity
@XStreamAlias("AbstractPythonCheckerConfiguration")
public abstract class AbstractPythonCheckerConfiguration extends CheckerConfiguration {

	private static final long serialVersionUID = 3554548545764055138L;

	@ElementCollection(fetch = FetchType.EAGER)
	protected Set<String> fileNames = new HashSet<>();

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
		name = "abstractpythoncheckerconfiguration_sourcefiles",
		joinColumns = @JoinColumn(name = "abstractpythoncheckerconfiguration_id"),
			inverseJoinColumns = @JoinColumn(name = "exerciseresource_id"))
	@DeepCopyOmitField(
			reason = "The refernces for the exerciseResources must be set by the caller of the deepCopy method")
	protected Set<ExerciseResource> sourceFiles = new HashSet<>();

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
		name = "abstractpythoncheckerconfiguration_libraryfiles",
		joinColumns = @JoinColumn(name = "abstractpythoncheckerconfiguration_id"),
			inverseJoinColumns = @JoinColumn(name = "exerciseresource_id"))
	@DeepCopyOmitField(
			reason = "The refernces for the exerciseResources must be set by the caller of the deepCopy method")
	protected Set<ExerciseResource> libraryFiles = new HashSet<>();

	public String getFileNames() {
		return fileNames.stream().collect(Collectors.joining("\n"));
	}

	public Set<String> getFileNamesAsSet() {
		return fileNames;
	}

	public void setFileNames(String fileNames) {
		this.fileNames.clear();
		this.fileNames.addAll(Arrays.asList(fileNames.split("\n")));
	}

	public Set<ExerciseResource> getSourceFiles() {
		return sourceFiles;
	}

	public List<ExerciseResource> getSourceFilesAsSortedList() {
		List<ExerciseResource> sourceFilesAsList = new ArrayList<>(sourceFiles);
		Collections.sort(sourceFilesAsList);
		return sourceFilesAsList;
	}

	public void setSourceFiles(Set<ExerciseResource> sourceFiles) {
		this.sourceFiles = sourceFiles;
	}

	public void setSourceFilesAsSortedList(List<ExerciseResource> sourceFiles) {
		this.sourceFiles.clear();
		if (sourceFiles != null) {
			this.sourceFiles.addAll(sourceFiles);
		}
	}

	public Set<ExerciseResource> getLibraryFiles() {
		return libraryFiles;
	}

	public List<ExerciseResource> getLibraryFilesAsSortedList() {
		List<ExerciseResource> libraryFilesAsList = new ArrayList<>(libraryFiles);
		Collections.sort(libraryFilesAsList);
		return libraryFilesAsList;
	}

	public void setLibraryFiles(Set<ExerciseResource> libraryFiles) {
		this.libraryFiles = libraryFiles;
	}

	public void setLibraryFilesAsSortedList(List<ExerciseResource> libraryFiles) {
		this.libraryFiles.clear();
		if (libraryFiles != null) {
			this.libraryFiles.addAll(libraryFiles);
		}
	}

	protected void copyFrom(AbstractPythonCheckerConfiguration original) {
		this.fileNames.addAll(original.fileNames);
		this.sourceFiles.addAll(original.sourceFiles);
		this.libraryFiles.addAll(original.libraryFiles);

		// fields from super class
		setName(original.getName());
		setResultLabel(original.getResultLabel());
		setActive(original.isActive());
		setHasVisibleResult(original.isHasVisibleResult());
		setHasVisibleFeedback(original.isHasVisibleFeedback());
		setAsync(original.isAsync());
		weight = original.weight;
	}

	public void updateResourceReferences(Map<ExerciseResource, ExerciseResource> referenceMap) {
		Set<ExerciseResource> newSourceFiles = new HashSet<>();
		for (ExerciseResource oldResource : sourceFiles) {
			newSourceFiles.add(referenceMap.get(oldResource));
		}
		sourceFiles = newSourceFiles;

		Set<ExerciseResource> newLibraryFiles = new HashSet<>();
		for (ExerciseResource oldResource : libraryFiles) {
			newLibraryFiles.add(referenceMap.get(oldResource));
		}
		libraryFiles = newLibraryFiles;
	}

}