package de.uni_due.s3.jack3.business;

import static de.uni_due.s3.jack3.utils.JackFileUtils.urlEncodeEnsureUnique;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;

import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.business.xstream.converter.ExerciseResourceConverter;
import de.uni_due.s3.jack3.entities.stagetypes.fillin.FillInStage;
import de.uni_due.s3.jack3.entities.stagetypes.java.JavaStage;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.stagetypes.molecule.MoleculeStage;
import de.uni_due.s3.jack3.entities.stagetypes.python.PythonStage;
import de.uni_due.s3.jack3.entities.stagetypes.r.DynamicRTestCase;
import de.uni_due.s3.jack3.entities.stagetypes.r.RStage;
import de.uni_due.s3.jack3.entities.stagetypes.r.StaticRTestCase;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.ExerciseResource;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.FrozenExercise;
import de.uni_due.s3.jack3.entities.tenant.StageResource;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.VariableDeclaration;
import de.uni_due.s3.jack3.utils.JackStringUtils;

/**
 * This business provides serialization and deserialization (SerDe) to and from XML (and JSON).
 *
 * @author Benjamin.Otto
 */

@RequestScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class SerDeBusiness extends AbstractBusiness {
	public static final String JACK3_INDICATOR_FILE_NAME = "Jack3Export_readme.txt";

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	/**
	 * Recursivly creates a directory on disk with xml-exports of exercises contained in the current given Folder. The
	 * parent-directory must exist first (usually this will be: System.getProperty("java.io.tmpdir"))
	 *
	 * @param contentFolder
	 *            ContentFolder to be exported (will be called recursively)
	 * @param tmpDir
	 *            Path to directory where the current-exercises should be saved (parent must exist!)
	 * @param createDir TODO
	 * @throws IOException
	 */
	public void exportContentFolderToDir(ContentFolder contentFolder, Path tmpDir, Boolean createDir)
			throws IOException {
		contentFolder = folderBusiness.getContentFolderWithLazyData(contentFolder);

		if (createDir) {
			Files.createDirectory(tmpDir);
		}

		Set<Folder> childrenFolder = contentFolder.getChildrenFolder();
		if (!childrenFolder.isEmpty()) {
			for (Folder currentFolder : childrenFolder) {
				String filteredName = urlEncodeEnsureUnique(currentFolder.getName(), currentFolder.getId());
				// recursively call this function for all subfolders
				exportContentFolderToDir((ContentFolder) currentFolder, tmpDir.resolve(filteredName), true);
			}
		}

		for (AbstractExercise currentExercise : contentFolder.getChildrenExercises()) {
			String fileName = urlEncodeEnsureUnique(currentExercise.getName(), currentExercise.getId()) + ".xml";

			Path tmpExercise = tmpDir.resolve(fileName);
			String exerAsString = exerciseToXml((Exercise) currentExercise);

			tmpExercise = Files.createFile(tmpExercise);
			Files.write(tmpExercise, exerAsString.getBytes(StandardCharsets.UTF_8));
			getLogger().debug("Exported: " + tmpExercise);
		}
	}

	public String exerciseToJson(Exercise exercise) {
		XStream xstream = newXstream(false);
		return serialize(exercise, xstream);
	}

	public String exerciseToXml(Exercise exercise) {
		XStream xstream = newXstream(true);
		return serialize(exercise, xstream);
	}

	private String serialize(Exercise exercise, XStream xstream) {
		/**
		 * We use the deep-copy-constructor of FrozenExercise, to construct a "clean" copy of the exercise to be
		 * serialized. Otherwise we would have hibernate specific extensions of collections throughout our json export.
		 * We can still deserialize into a regular Exercise (see: {@link toExerciseFromJsonWithXstream(String)} to see
		 * how we do that).
		 *
		 * @see <a
		 *      href="https://docs.jboss.org/hibernate/orm/3.3/api/org/hibernate/collection/PersistentSet.html">PersistentSet</a>
		 */
		FrozenExercise frozen = new FrozenExercise(exercise);
		return xstream.toXML(frozen);
	}

	public Exercise toExerciseFromJson(String json) {
		XStream xstream = newXstream(false);
		return deserialize(json, xstream);
	}

	public Exercise toExerciseFromXML(String xml) {
		XStream xstream = newXstream(true);
		return deserialize(xml, xstream);
	}

	private Exercise deserialize(String serializedExercise, XStream xstream) {
		/**
		 * This is kind of a hack: We serialize only FrozenExercises (see {@link toJsonWithXstream(AbstractExercise)},
		 * on why we do this), so when deserializing we set the "ExportedExercise"-alias, which is annotated on the
		 * FrozenExercise class to the Exercise class and deserialize into that. This works, because both entitys
		 * inherit from AbstractExercise and all fields not found in AbstractExercise are omitted from serialization!
		 */
		xstream.alias("ExportedExercise", Exercise.class);

		Exercise deserializedExercise = (Exercise) xstream.fromXML(serializedExercise);
		deserializedExercise.generateSuffixWeights(); // See #937
		return deserializedExercise;
	}

	private XStream newXstream(boolean xml) {
		XStream xstream;
		if (xml) {
			xstream = provideXmlXstreamIgnoreMissingAttributes();
		} else {
			xstream = provideJsonXstreamIgnoreMissingAttributes();
		}

		// Human readable Ids for entity-references
		xstream.setMode(XStream.ID_REFERENCES);

		processAnnotations(xstream);

		// clear out existing permissions, we'll allowlist the others
		xstream.addPermission(NoTypePermission.NONE);

		allowlistAllowedEntitys(xstream);

		return xstream;
	}

	/**
	 * Processes annotated aliases and ommissions
	 *
	 * @param xstream
	 *            Xstream object to set annotation processing for
	 */
	private void processAnnotations(XStream xstream) {
		xstream.processAnnotations(FrozenExercise.class);
		xstream.processAnnotations(FillInStage.class);
		xstream.processAnnotations(MCStage.class);
		xstream.processAnnotations(JavaStage.class);
		xstream.processAnnotations(RStage.class);
		xstream.processAnnotations(PythonStage.class);
		xstream.processAnnotations(MoleculeStage.class);
		xstream.processAnnotations(VariableDeclaration.class);
		xstream.processAnnotations(DynamicRTestCase.class);
		xstream.processAnnotations(StaticRTestCase.class);
		xstream.processAnnotations(ExerciseResource.class);
		xstream.processAnnotations(StageResource.class);
	}

	private XStream provideJsonXstreamIgnoreMissingAttributes() {
		// Only JettisonMappedXmlDriver allows for deserializing, so not using the provided
		// JsonHierarchicalStreamDriver here.
		return new XStream(new JettisonMappedXmlDriver()) {
			// We skip deserializing non existing fields here by overwriting "shouldSerializeMember" to prevent
			// an Exceptions being thrown while importing old exports with removed fields.
			// TODO: This should probably be removed when the first non-alpha version is released to implement a
			// "fail fast" strategy.
			@Override
			protected MapperWrapper wrapMapper(MapperWrapper next) {
				return new MapperWrapper(next) {
					@SuppressWarnings("rawtypes")
					@Override
					public boolean shouldSerializeMember(Class definedIn, String fieldName) {
						return definedIn != //
								Object.class ? super.shouldSerializeMember(definedIn, fieldName) : false;
					}
				};
			}
		};
	}

	private XStream provideXmlXstreamIgnoreMissingAttributes() {
		// XML is the default
		return new XStream() {
			// We skip deserializing non existing fields here by overwriting "shouldSerializeMember" to prevent
			// an Exceptions being thrown while importing old exports with removed fields.
			// TODO: This should probably be removed when the first non-alpha version is released to implement a
			// "fail fast" strategy.
			@Override
			protected MapperWrapper wrapMapper(MapperWrapper next) {
				return new MapperWrapper(next) {
					@SuppressWarnings("rawtypes")
					@Override
					public boolean shouldSerializeMember(Class definedIn, String fieldName) {
						return definedIn != //
								Object.class ? super.shouldSerializeMember(definedIn, fieldName) : false;
					}
				};
			}
		};
	}

	/**
	 * Security: Allowlist allowed entitys to deserialize into: https://x-stream.github.io/security.html
	 **/
	private void allowlistAllowedEntitys(XStream xstream) {
		// allow some basics
		xstream.addPermission(NullPermission.NULL);
		xstream.addPermission(PrimitiveTypePermission.PRIMITIVES);
		xstream.allowTypes(new String[] { //
				HashSet.class.getName(), //
				TreeMap.class.getName(), //
				String.class.getName(), //
				Set.class.getName(), //
				LinkedList.class.getName()
		});

		xstream.registerConverter(new ExerciseResourceConverter());

		// allow any type from {enums, providers, fillin, mc}-packages
		xstream.allowTypesByWildcard(new String[] { "de.uni_due.s3.jack3.entities.enums.**" });
		xstream.allowTypesByWildcard(new String[] { "de.uni_due.s3.jack3.entities.providers.**" });
		xstream.allowTypesByWildcard(new String[] { "de.uni_due.s3.jack3.entities.stagetypes.fillin.**" });
		xstream.allowTypesByWildcard(new String[] { "de.uni_due.s3.jack3.entities.stagetypes.java.**" });
		xstream.allowTypesByWildcard(new String[] { "de.uni_due.s3.jack3.entities.stagetypes.mc.**" });
		xstream.allowTypesByWildcard(new String[] { "de.uni_due.s3.jack3.entities.stagetypes.molecule.**" });
		xstream.allowTypesByWildcard(new String[] { "de.uni_due.s3.jack3.entities.stagetypes.python.**" });
		xstream.allowTypesByWildcard(new String[] { "de.uni_due.s3.jack3.entities.stagetypes.r.**" });
		xstream.allowTypesByWildcard(new String[] { "de.uni_due.s3.jack3.entities.stagetypes.uml.**" });

		// **BEWARE** Allow only specific tentant entities, otherwise an attacker could just deserialize a admin user
		// or other security relevant entitys
		xstream.allowTypes(new String[] {
				// Bash command: for f in *.java; do printf '%s\n' "de.uni_due.s3.jack3.entities.tenant.${f%.java}"; done
				// Don't allow AbstractCourse
				"de.uni_due.s3.jack3.entities.tenant.AbstractExercise", //
				"de.uni_due.s3.jack3.entities.tenant.CheckerConfiguration", //
				"de.uni_due.s3.jack3.entities.tenant.Comment", //
				// Don't allow Config
				// Don't allow ContentFolder
				// Don't allow Course
				// Don't allow CourseEntry
				// Don't allow CourseOffer
				// Don't allow CourseRecord
				// Don't allow CourseResource
				// Don't allow ErrorRecord
				"de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression", //
				"de.uni_due.s3.jack3.entities.tenant.Exercise", //
				"de.uni_due.s3.jack3.entities.tenant.ExerciseResource", //
				// Don't allow Folder
				// Don't allow FrozenCourse
				// Don't allow FrozenExercise
				// Don't allow IdentityProfileField
				// Don't allow Job
				// Don't allow LDAPProfileField
				"de.uni_due.s3.jack3.entities.tenant.MinilogEntry", //
				// Don't allow Password
				// Don't allow PresentationFolder
				// Don't allow ProfileField
				"de.uni_due.s3.jack3.entities.tenant.Resource", //
				"de.uni_due.s3.jack3.entities.tenant.Result", //
				"de.uni_due.s3.jack3.entities.tenant.ResultFeedbackMapping", //
				// Don't allow SelectProfileField
				// Don't allow Submission
				// Don't allow SubmissionAttribute
				// Don't allow SubmissionLogEntry
				// Don't allow SubmissionResource
				"de.uni_due.s3.jack3.entities.tenant.Stage", //
				"de.uni_due.s3.jack3.entities.tenant.StageHint", //
				"de.uni_due.s3.jack3.entities.tenant.StageResource", //
				"de.uni_due.s3.jack3.entities.tenant.StageSubmission", //
				"de.uni_due.s3.jack3.entities.tenant.StageTransition", //
				"de.uni_due.s3.jack3.entities.tenant.Tag", //
				// Don't allow TextProfileField
				// Don't allow User
				// Don't allow UserExerciseFilter
				// Don't allow UserGroup
				"de.uni_due.s3.jack3.entities.tenant.JSXGraph",
				"de.uni_due.s3.jack3.entities.tenant.VariableDeclaration", //
				"de.uni_due.s3.jack3.entities.tenant.VariableUpdate", //
				"de.uni_due.s3.jack3.entities.tenant.VariableValue", });
	}

	/**
	 *
	 * @param zipAsBytes
	 *            The Jack3-export we want to import as a byte array
	 * @param folder
	 *            The folder where the export will deserialized into
	 * @return number of imported exercises
	 * @throws IOException
	 * @throws ActionNotAllowedException
	 */
	public int importJack3zip(byte[] zipAsBytes, ContentFolder folder, String suffix, User author)
			throws IOException, ActionNotAllowedException {
		int exerciseCount = 0;
		Map<String, ContentFolder> nameToFolderMap = createContentFoldersIfNeeded(zipAsBytes, folder, author);

		try (ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(zipAsBytes))) {
			ZipEntry entry = zipStream.getNextEntry();
			while (entry != null) {
				if (entry.getName().equals(SerDeBusiness.JACK3_INDICATOR_FILE_NAME) || entry.isDirectory()) {
					entry = nextEntry(zipStream);
					continue;
				}

				ByteArrayOutputStream byteStream = getFileAsByteArrayOutputStream(zipStream);
				String currentXml = new String(byteStream.toByteArray(), StandardCharsets.UTF_8);
				Exercise currentExercise = toExerciseFromXML(currentXml);

				if (!suffix.isEmpty()) {
					String name = currentExercise.getName();
					currentExercise.setName(name + suffix);
				}

				var folderForName = getFolderForName(entry.getName(), nameToFolderMap, folder);
				exerciseBusiness.persistImportedExercise(currentExercise, folderForName);
				exerciseCount++;
				entry = nextEntry(zipStream);
			}
		}
		return exerciseCount;
	}

	private ContentFolder getFolderForName(String name, Map<String, ContentFolder> nameToFolderMap,
			ContentFolder parent) {
		if (!name.contains("\\")) {
			return parent;
		}
		String basename = name.split("\\\\(?:.(?!\\\\))+$")[0];
		//decode basename to get same format as in map
		basename = URLDecoder.decode(basename, StandardCharsets.UTF_8);
		return nameToFolderMap.get(basename);
	}

	private Map<String, ContentFolder> createContentFoldersIfNeeded(byte[] zipAsBytes, ContentFolder folder, User user)
			throws IOException, ActionNotAllowedException {

		Map<String, ContentFolder> result = new HashMap<>();
		Set<String> neededDirs = getNeededDirNames(zipAsBytes);
		for (String dir : neededDirs) {
			result.putAll(folderBusiness.createContentFolderHierarchy(dir, folder, user));
		}
		return result;
	}

	private Set<String> getNeededDirNames(byte[] zipAsBytes) throws IOException {

		List<String> fileList = new ArrayList<>();
		Set<String> dirSet = new HashSet<>();

		try (ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(zipAsBytes))) {
			ZipEntry entry = zipStream.getNextEntry();
			while (entry != null) {
				// To preserve special chars that are allowded in our folder names but aren't allowed in the file
				// system / as zipentry we use URL (en/de)coder for the filenames
				fileList.add(URLDecoder.decode(entry.getName(), StandardCharsets.UTF_8));
				entry = nextEntry(zipStream);
			}
		}

		for (String relativePath : fileList) {
			// The spec only permits forward slashes, but the library seems to convert them to backslashes (i hope this
			// is not OS-dependent). So split using backslashes here.
			String[] hierarchy = relativePath.split("\\\\");
			StringJoiner hierarchyAsStringJoiner = new StringJoiner("\\");
			for (int i = 0; i < (hierarchy.length - 1); i++) {
				if (JackStringUtils.isNotBlank(hierarchy[i])) {
					hierarchyAsStringJoiner.add(hierarchy[i]);
					dirSet.add(hierarchyAsStringJoiner.toString());
				}
			}
		}
		return dirSet;
	}

	private ZipEntry nextEntry(ZipInputStream zipStream) throws IOException {
		zipStream.closeEntry();
		return zipStream.getNextEntry();
	}

	private ByteArrayOutputStream getFileAsByteArrayOutputStream(ZipInputStream zipStream) throws IOException {

		try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[1024];
			int length;
			while ((length = zipStream.read(buffer)) > 0) {
				byteStream.write(buffer, 0, length);
			}
			return byteStream;
		}
	}
}
