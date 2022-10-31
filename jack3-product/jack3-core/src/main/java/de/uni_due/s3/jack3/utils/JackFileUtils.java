package de.uni_due.s3.jack3.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.base.VerifyException;

public class JackFileUtils {

	private JackFileUtils() {
		throw new IllegalStateException("Static utility class");
	}

	public static void deleteDirIfExists(Path tmpDir) {
		if (Files.exists(tmpDir)) {
			try (Stream<Path> fileObjects = Files.walk(tmpDir)) {
				fileObjects //
				.map(Path::toFile) //
				.sorted((o1, o2) -> -o1.compareTo(o2)) // reverse order so deleting works correctly
				.forEach(File::delete);
			} catch (IOException e) {
				throw new VerifyException(e);
			}
		}
	}

	public static String filterNonAlphNumCharsAddIdIfChanged(String filename, Number id) {
		Objects.requireNonNull(filename);
		Objects.requireNonNull(id);

		final String before = filename;

		String result = filterNonAlphNumChars(filename);
		result = addIdIfChanged(id, before, result);

		return result;
	}

	public static String filterNonAlphNumChars(String filename) {
		Objects.requireNonNull(filename);
		String result = replaceUmlauts(filename).replaceAll("[^a-zA-Z0-9\\.\\- ]", "_"); // Only alphanumeric chars
		return result;
	}

	public static String addIdIfChanged(Number id, final String before, String after) {
		Objects.requireNonNull(id);
		Objects.requireNonNull(before);
		Objects.requireNonNull(after);

		// If result was changed, this could potentially lead to duplicate names
		if (!before.equals(after)) {
			return ensureUniqueString(after, id); // thats why we add the id at the end
		}
		return after;
	}

	public static String urlEncode(String name) {
		Objects.requireNonNull(name);
		return URLEncoder.encode(name, StandardCharsets.UTF_8);
	}

	public static String urlEncodeEnsureUnique(String name, Number id) {
		Objects.requireNonNull(id);
		Objects.requireNonNull(name);
		return urlEncode(ensureUniqueString(name, id));
	}

	public static String ensureUniqueString(String name, Number id) {
		Objects.requireNonNull(id);
		Objects.requireNonNull(name);

		return name + " (id_" + id + ")";
	}

	// https://stackoverflow.com/a/32696479
	private static String replaceUmlauts(String input) {

		// replace all lower Umlauts
		String output = input //
				.replace("ü", "ue") //
				.replace("ö", "oe") //
				.replace("ä", "ae") //
				.replace("ß", "ss");

		// first replace all capital umlaute in a non-capitalized context (e.g. Übung)
		output = output //
				.replaceAll("Ü(?=[a-zäöüß ])", "Ue") //
				.replaceAll("Ö(?=[a-zäöüß ])", "Oe") //
				.replaceAll("Ä(?=[a-zäöüß ])", "Ae");

		// now replace all the other capital umlaute
		output = output //
				.replace("Ü", "UE") //
				.replace("Ö", "OE") //
				.replace("Ä", "AE");

		return output;
	}

	public static InputStream zipFolder(Path folderToZip) {
		try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				ZipOutputStream zipStream = new ZipOutputStream(byteArrayOutputStream)) {
			Files.walkFileTree(folderToZip, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
					zipStream.putNextEntry(new ZipEntry(folderToZip.relativize(file).toString()));
					Files.copy(file, zipStream);
					zipStream.closeEntry();
					return FileVisitResult.CONTINUE;
				}
			});
			zipStream.close();
			return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
		} catch (IOException e) {
			throw new VerifyException(e);
		}
	}

	/**
	 * Returns a String <strong>not</strong> containing characters that are illegal in file names. All illegal
	 * characters are replaced with an underscore
	 */
	public static String filterIllegalWindowsFileCharacters(String original) {
		// https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file
		return original.replaceAll("[\\\\/:*?\"<>|]", "_");
	}

}
