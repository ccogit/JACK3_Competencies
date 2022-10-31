package de.uni_due.s3.jack3.business;

import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import de.uni_due.s3.jack3.business.microservices.EurekaBusiness;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.services.FolderService;

/**
 * This service performs a first time setup and creates a tenant admin and root
 * folders if they have not been created yet.
 */
@ApplicationScoped
@Startup
public class FirstTimeSetupBusiness extends AbstractBusiness {

	/** The default name of content and presentation root folders. */
	private static final String ROOT_FOLDER_NAME = "root";

	/**
	 * The folder service is used to create the presentation and content root
	 * folders.
	 */
	@Inject
	private FolderService folderService;

	@Inject
	private EurekaBusiness eurekaBusiness;

	/*
	 * This class originally was a service and had a @Startup annotation so that
	 * this method was called when the application started. Unfortunately there is
	 * no CDI complement to @Startup. Adding a parameter to this method that is
	 * initialized in application scope is a workaround for this problem. The
	 * argument passed in is willingly ignored as it is just a expedient to get the
	 * method called automatically.
	 */
	private void doFirstTimeSetup(@Observes @Initialized(ApplicationScoped.class) Object ignored) {
		createContentRootFolder();
		createPresentationRootFolder();
		eurekaBusiness.touch();
	}

	/**
	 * Performs the first time setup routine. This include three steps:
	 * <ol>
	 * <li>Ensuring there is a tenant administrator account.</li>
	 * <li>Ensuring there is a content root folder.</li>
	 * <li>Ensuring there is a presentation root folder.</li>
	 * </ol>
	 */
	// REVIEW bo: where does it ensure a tenant admin exists?
	public void doFirstTimeSetup() {
		doFirstTimeSetup(null);
	}

	/**
	 * Ensures there is a root content folder available. If there is no root content
	 * folder present this method creates it.
	 */
	private void createContentRootFolder() {
		if (folderService.getNoOfContentFolders() < 1) {
			ContentFolder root = new ContentFolder(ROOT_FOLDER_NAME);
			folderService.persistFolder(root);
			getLogger().debugf("Created Content root folder \"%s\".", ROOT_FOLDER_NAME);
		}
	}

	/**
	 * Ensures there is a root presentation folder available. If there is no root
	 * presentation folder present this methods creates it.
	 */
	private void createPresentationRootFolder() {
		if (folderService.getNoOfPresentationFolder() < 1) {
			PresentationFolder presentationFolder = new PresentationFolder(ROOT_FOLDER_NAME);
			folderService.persistFolder(presentationFolder);
			getLogger().debugf("Created Presentation root folder \"%s\".", ROOT_FOLDER_NAME);
		}
	}
}
