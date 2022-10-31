package de.uni_due.s3.jack3.tests.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.tenant.Config;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.tests.utils.AbstractTest;
import de.uni_due.s3.jack3.utils.EntityReflectionHelper;

class EntityReflectionHelperTest extends AbstractTest {

	@Test
	void generateToStringTest() {
		Config config = new Config("myKey", "myValue");
		String configToString = EntityReflectionHelper.generateToString(config);
		String configRegex = "Config#\\d+ -> \\{\\s+key:myKey\\s+value:myValue\\s+\\}";
		assertTrue(configToString.matches(configRegex));
	}

	@Test
	void generateToStringTest2() {
		ContentFolder folder = new ContentFolder();
		folder.setName("the Folders Name!");
		String folderToString = EntityReflectionHelper.generateToString(folder);
		// For debugging: System.out.println(folderToString);
		String folderRegex = "ContentFolder#\\d+ -> \\{\\s+name:the Folders Name!\\s+\\}";
		assertTrue(folderToString.matches(folderRegex));
	}

	@Test
	void castListTest() {
		List<Object> list = new java.util.ArrayList<>(4);
		list.add("A");
		list.add("B");
		list.add("C");
		list.add("D");

		List<String> result = EntityReflectionHelper.castList(String.class, list);

		for (int i = 0; i < result.size(); i++) {
			assertEquals(list.get(i), result.get(i));
		}
	}

	@Test
	void allSuperClassesOfTest() {
		Config config = new Config("myKey", "myValue");
		assertEquals("tenant.Config -> AbstractEntity -> java.lang.Object -> null",
				EntityReflectionHelper.allSuperClassesOf(config).replace("de.uni_due.s3.jack3.entities.", ""));
	}
}
