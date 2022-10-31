package de.uni_due.s3.jack3.uitests.arquillian;

import java.io.File;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ExplodedImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

/**
 * Utility class for generating deployments
 *
 * @author lukas.glaser
 */
public class Deployments {

	private Deployments() {
	}

	@Deployment
	public static WebArchive createUIDeployment() {

		// Load all maven dependencies ignoring jack3 artifacts because they are already added via "addPackages"
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

		return ShrinkWrap //
				.create(WebArchive.class, "jack3-webclient.war") //
				.addPackages(true, "de.uni_due.s3.jack3") //
				.addAsLibraries(files)

				.addAsWebInfResource(new FileAsset(new File("../jack3-webclient/src/main/webapp/WEB-INF/beans.xml")),
						"beans.xml") //
				.addAsWebInfResource("faces-config.xml") // custom faces config
				.addAsWebInfResource(
						new FileAsset(new File("../jack3-webclient/src/main/webapp/WEB-INF/jboss-web.xml")),
						"jboss-web.xml") //
				.addAsWebInfResource(
						new FileAsset(new File("../jack3-webclient/src/main/webapp/WEB-INF/jack.taglib.xml")),
						"jack.taglib.xml") //
				.addAsResource("persistence.xml", "META-INF/persistence.xml") // custom persistence configuration
				.addAsResource(
						new FileAsset(
								new File("../jack3-core/src/main/resources/META-INF/microprofile-config.properties")),
						"META-INF/microprofile-config.properties")
				.addAsResource(new FileAsset(new File("../jack3-business/src/main/resources/MittelwertBerechnenR.xml")),
						"MittelwertBerechnenR.xml") // example exercise
				.addAsResource(new FileAsset(new File("../jack3-business/src/main/resources/Add+Numbers+(Python).xml")),
						"Add+Numbers+(Python).xml") // example exercise
				.addAsResource(new FileAsset(new File("../jack3-business/src/main/resources/UML+Test.xml")),
						"UML+Test.xml") // example exercise
				.addAsResource(new FileAsset(new File("../jack3-business/src/main/resources/Demoprojekt+1+(Java).xml")),
						"Demoprojekt+1+(Java).xml") // example exercise
				.addAsResource(new FileAsset(new File("../jack3-business/src/main/resources/Essigsäure.xml")),
						"Essigsäure.xml") // example exercise
				.addAsWebInfResource("web.xml") // custom webapp configuration

				// Add resources directory from jack3-webclient project
				.merge(ShrinkWrap.create(GenericArchive.class).as(ExplodedImporter.class) //
						.importDirectory("../jack3-webclient/src/main/webapp/resources").as(GenericArchive.class), //
						"/resources/", Filters.includeAll())

				// Add internationalization
				.merge(ShrinkWrap.create(GenericArchive.class).as(ExplodedImporter.class) //
						.importDirectory("../jack3-webclient/src/main/resources/i18n").as(GenericArchive.class), //
						"/WEB-INF/classes/i18n/", Filters.include(".*\\.properties$"))

				// Add classes
				.merge(ShrinkWrap.create(GenericArchive.class).as(ExplodedImporter.class) //
						.importDirectory("../jack3-webclient/target/classes").as(GenericArchive.class), //
						"/WEB-INF/classes/", Filters.include(".*\\.class$"))

				// Add XHTML files
				.merge(ShrinkWrap.create(GenericArchive.class).as(ExplodedImporter.class) //
						.importDirectory("../jack3-webclient/src/main/webapp").as(GenericArchive.class), //
						"/", Filters.include(".*\\.xhtml$"));
	}

}
