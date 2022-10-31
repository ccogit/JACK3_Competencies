package de.uni_due.s3.jack3.tests.business;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.FirstTimeSetupBusiness;
import de.uni_due.s3.jack3.services.FolderService;
import de.uni_due.s3.jack3.tests.utils.AbstractBusinessTest;

/**
 * Tests for FirstTimeSetupBusiness
 * 
 * @author lukas.glaser
 *
 */
class FirstTimeSetupBusinessTest extends AbstractBusinessTest {

	@Inject
	private FirstTimeSetupBusiness firstTimeSetupBusiness;

	@Inject
	private FolderService folderService;

	/**
	 * Check that the database has content and presentation root and a configured SAGE server after FirstTimeSetup.
	 */
	@Test
	void doFirstTimeSetup() {
		firstTimeSetupBusiness.doFirstTimeSetup();

		// Note: Returning type of "get*Root" is NOT an Optional. In case of an empty result, an Exception is thrown.
		assertNotNull(folderService.getContentRoot());
		assertNotNull(folderService.getPresentationRoot());
	}

}
