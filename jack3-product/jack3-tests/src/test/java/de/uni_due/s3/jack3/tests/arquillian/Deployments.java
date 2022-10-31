package de.uni_due.s3.jack3.tests.arquillian;

import java.io.File;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

/**
 * Utility class for generating deployments
 *
 * @author lukas.glaser
 */
public class Deployments {

	/**
	 * Default deployment for using as extended deployment
	 */
	@Deployment
	public static WebArchive createMultiDeployment() {
		return createDeployment("multi-test.war");
	}

	public static WebArchive createSingleDeployment() {
		return createDeployment("single-test.war");
	}

	private static WebArchive createDeployment(String filename) {

		// Load all maven dependencies ignoring jack3 artefacts because they are already added via "addPackages"
		File[] files = Maven

				// Resolve dependencies from local repository instead of downloading it
				.configureResolver().workOffline() //

				// Import Maven runtime dependencies
				.loadPomFromFile("pom.xml") //
				.importRuntimeAndTestDependencies() //
				.resolve() //
				.withTransitivity() //
				.asList(File.class) //

				// filter jack3 projects from dependencies because they are already added
				.stream() //
				.filter(currFile -> (!currFile.getName().contains("jack3-core") //
						&& !currFile.getName().contains("jack3-business") //
						&& !currFile.getName().contains("jack3-webclient"))) //
				.toArray(File[]::new);

		// Show deploy structure, for debugging only
		// System.out.println(war.toString(true));

		return ShrinkWrap.create(WebArchive.class, filename) //
				.addPackages(true, "de.uni_due.s3.jack3") //
				.addAsResource("persistence.xml", "META-INF/persistence.xml") //
				// create empty beans.xml to activate CDI
				.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml") //
				// Add testdata
				.addAsResource("testdata", "testdata") //
				.addAsResource(
						new FileAsset(
								new File("../jack3-core/src/main/resources/META-INF/microprofile-config.properties")),
						"META-INF/microprofile-config.properties")
				.addAsLibraries(files);
	}

}
