package de.uni_due.s3.jack3.beans;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import com.thoughtworks.xstream.mapper.CannotResolveClassException;

import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.business.ExerciseBusiness;
import de.uni_due.s3.jack3.business.FolderBusiness;
import de.uni_due.s3.jack3.business.Jack2ImportBusiness;
import de.uni_due.s3.jack3.business.SerDeBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.exceptions.JackSecurityException;
import de.uni_due.s3.jack3.services.ExerciseService;

@ViewScoped
@Named
public class ImportExerciseDialogView extends AbstractView implements Serializable {
	private static final long serialVersionUID = -7188411733863426138L;

	private ContentFolder currentFolder;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private SerDeBusiness serDeBusiness;

	@Inject
	private Jack2ImportBusiness jack2ImportBusiness;

	@Inject
	private AuthorizationBusiness authorizationBusiness;

	@Inject
	private ExerciseService exerciseService;


	private List<FacesMessage> errorMessages = new ArrayList<>();

	private int exerciseCount;
	private int actualFileCount;
	private String suffix = "";


	public void setCurrentFolder(ContentFolder folder) {
		currentFolder = folder;
	}

	public void handleFileUpload(FileUploadEvent event) throws IOException, ActionNotAllowedException {
		if (!authorizationBusiness.isAllowedToEditFolder(getCurrentUser(), currentFolder)) {
			throw new JackSecurityException(getCurrentUser() + " is not allowed to write to " + currentFolder);
		}

		UploadedFile uploadedFile = event.getFile();
		String contentType = uploadedFile.getContentType();
		Exercise exercise = null;
		boolean isJson = contentType.equals("application/json") && uploadedFile.getFileName().endsWith(".json");
		boolean isXml = contentType.equals("text/xml") && uploadedFile.getFileName().endsWith(".xml");
		boolean isZip = (contentType.equals("application/x-zip-compressed") || contentType.equals("application/zip"))
				&& uploadedFile.getFileName().endsWith(".zip");
		actualFileCount--;



		if ((!isJson && !isXml && !isZip)) {
			errorMessages.add(new FacesMessage(FacesMessage.SEVERITY_ERROR,
					formatLocalizedMessage("startContentTabView.import.wrongFormat", new Object[] { contentType }),
					""));
		}

		else if (isZip) {
			byte[] zipAsBytes = uploadedFile.getContent();
			if (hasJack3Indicator(zipAsBytes)) {
				try {
					int count = serDeBusiness.importJack3zip(zipAsBytes, currentFolder, suffix, getCurrentUser());
					exerciseCount += count - 1; // otherwise the folder is counted as an exercise
				} catch (Exception e) {
					getLogger().error("Exception while importing a zipped JACK3 exercise.", e);
					errorMessages.add(new FacesMessage(FacesMessage.SEVERITY_ERROR, formatLocalizedMessage(
							"startContentTabView.import.exceptionMsg", new Object[] { uploadedFile.getFileName() }),
							""));
				}
			} else {
				try {
					exercise = handleJack2ZipUpload(zipAsBytes);
					addSuffixToExerciseName(exercise);
					/*
					 * exercise could only be merged here,
					 * when the exercise would be persisted the included tags , which are persistent, would be also
					 * persisted
					 * that leads to an unique key error
					 */
					exerciseService.mergeExercise(exercise);

					if (exercise.getInternalNotes().contains("ERROR")) {
						errorMessages.add(new FacesMessage(FacesMessage.SEVERITY_WARN, formatLocalizedMessage(
							"startContentTabView.import.failureMsg", new Object[] { exercise.getName() }), ""));

					}
				} catch (Exception e) {
					getLogger().error("Exception while importing a zipped JACK2 exercise.", e);
					//show error message instead of exception
					errorMessages.add(new FacesMessage(FacesMessage.SEVERITY_ERROR, formatLocalizedMessage(
						"startContentTabView.import.exceptionMsg", new Object[] { uploadedFile.getFileName() }), "")

					);

				}
			}
		} else {
			try {
				exercise = handleXmlOrJsonUpload(uploadedFile, isJson, isXml);
				addSuffixToExerciseName(exercise);
				currentFolder = folderBusiness.getContentFolderWithLazyData(currentFolder); //otherwise we get a lazyInitializationException
				exerciseBusiness.persistImportedExercise(exercise, currentFolder);
			} catch (CannotResolveClassException e) {
				// ConversionException are handled through an Primefaces ajaxExceptionHandler!
				errorMessages.add(new FacesMessage(FacesMessage.SEVERITY_ERROR, formatLocalizedMessage(
						"startContentTabView.import.failedMsg", new Object[] { uploadedFile.getFileName() }), ""));
			}
		}

		exerciseCount++;

		if (actualFileCount == 0) {
			// show import summary after uploading last file
			generateImportSummary();
		}

	}

	private void addSuffixToExerciseName(Exercise exercise) {
		if (!suffix.isEmpty()) {
			String name = exercise.getName();
			exercise.setName(name + suffix);
		}
	}

	private boolean hasJack3Indicator(byte[] zipAsBytes) throws IOException {
		try (ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(zipAsBytes))) {
			ZipEntry entry = zipStream.getNextEntry();
			while (entry != null) {
				if (entry.getName().equals(SerDeBusiness.JACK3_INDICATOR_FILE_NAME)) {
					return true;
				}
				entry = zipStream.getNextEntry();
			}
			return false;
		}
	}

	private Exercise handleXmlOrJsonUpload(UploadedFile uploadedFile, boolean isJson, boolean isXml) {
		Exercise exercise;
		if (isJson) {
			String json = new String(uploadedFile.getContent(), StandardCharsets.UTF_8);
			exercise = serDeBusiness.toExerciseFromJson(json);
		} else if (isXml) {
			String xml = new String(uploadedFile.getContent(), StandardCharsets.UTF_8);
			exercise = serDeBusiness.toExerciseFromXML(xml);
		} else {
			throw new IllegalStateException();
		}
		return exercise;
	}

	private Exercise handleJack2ZipUpload(byte[] zipAsBytes) throws ActionNotAllowedException {
		Exercise exercise;
		final String language = getUserLanguage().toLanguageTag();
		exercise = jack2ImportBusiness.importJack2Excercise(getCurrentUser(), language, currentFolder, zipAsBytes,
				getRequest().getContextPath());
		String errors = jack2ImportBusiness.getConverterErrorsAsString();
		if (!errors.isEmpty()) {
			String internalNotes = exercise.getInternalNotes();
			exercise.setInternalNotes(
					"ERROR{ " + errors + " }" + System.lineSeparator() + internalNotes);
		}
		return exercise;
	}

	/**
	 * Retrieves the number of files to upload.
	 * This is later used, to detect when the last file is uploaded.
	 * 
	 */
	public void initialiseUpload() {
		errorMessages = new ArrayList<>();
		Map<String, String[]> requestParameterValuesMap = FacesContext.getCurrentInstance().getExternalContext()
				.getRequestParameterValuesMap();

		actualFileCount = Integer.parseInt(requestParameterValuesMap.get("size")[0]);
		exerciseCount = 0;

	}

	/**
	 * Generates a summary containing all error messages which occur while uploading.
	 * If more than four errors occur, the single detail messages are not shown.
	 */
	public void generateImportSummary() {
		//shows if errors occur
		int errorCount = errorMessages.size();
		String importDetails = "importDetails";
		if (errorCount == 0) {
			//no errors
			String msg = formatLocalizedMessage("startContentTabView.import.successSummary",
					new Object[] { exerciseCount });
			FacesContext.getCurrentInstance().addMessage(importDetails,
					new FacesMessage(FacesMessage.SEVERITY_INFO, msg, ""));
		} else if (errorCount > 4) {
			//too many errors
			String message = getLocalizedMessage("startContentTabView.import.errorSummary");
			FacesContext.getCurrentInstance().addMessage(importDetails, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					MessageFormat.format(message, errorCount, exerciseCount), ""));
		} else {
			String message = getLocalizedMessage("startContentTabView.import.errorDetail");
			FacesContext.getCurrentInstance().addMessage(importDetails, new FacesMessage(FacesMessage.SEVERITY_ERROR,
					MessageFormat.format(message, errorCount, exerciseCount), ""));

			for (int i = 0; i < errorMessages.size(); i++) {
				FacesContext.getCurrentInstance().addMessage(importDetails, errorMessages.get(i));

			}

		}
		PrimeFaces.current().ajax().update("importForm:messages");

	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

}