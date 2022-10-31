package de.uni_due.s3.jack3.tests.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.jboss.arquillian.junit5.ArquillianExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import de.uni_due.s3.jack3.business.FirstTimeSetupBusiness;
import de.uni_due.s3.jack3.business.SerDeBusiness;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.services.DevelopmentService;
import de.uni_due.s3.jack3.services.DevelopmentService.EDatabaseType;

/**
 * Super class for all business tests working with Arquillian.
 *
 * @author lukas.glaser
 *
 */
@ExtendWith(ArquillianExtension.class)
public abstract class AbstractBusinessTest extends AbstractTest {

	/**
	 * Injected {@linkplain DevelopmentService} for clearing the database after testing
	 */
	@Inject
	protected DevelopmentService devService;

	/**
	 * Injected {@linkplain FirstTimeSetupBusiness} for first time setup after clearing database
	 */
	@Inject
	protected FirstTimeSetupBusiness firstTimeSetupBusiness;

	@Inject
	private SerDeBusiness serDeBusiness;

	/**
	 * First time setup
	 */
	@BeforeEach
	protected void beforeTest() {
		firstTimeSetupBusiness.doFirstTimeSetup();
	}

	/**
	 * Clear database after testing
	 */
	@AfterEach
	final void afterTest() {
		devService.deleteTenantDatabase(EDatabaseType.H2);
	}

	/*
	 *
	 */
	@Nonnull
	protected Exercise importSampleExercise(String resourcePathToExerciseAsXML) throws Exception {
		// We use an exported sample exercise
		final ClassLoader classLoader = this.getClass().getClassLoader();
		final InputStream input = classLoader.getResourceAsStream(resourcePathToExerciseAsXML);
		final InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
		final String xml = new BufferedReader(reader).lines().collect(Collectors.joining());
		return serDeBusiness.toExerciseFromXML(xml);
	}

}
