package de.uni_due.s3.jack3.business.microservices.openobjectutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.openobject.OpenObject;

class OpenObjectFactoryTest {

	@Test
	void assertEqualValue_afterInstanciateOpenObjectChemInteger() throws Exception {
		OpenObject ooOci1 = OpenObjectFactory.createOpenObjectForOpenChemInteger(1);
		assertEquals("1", ooOci1.getOCOBJ().getOCI().getValue());
	}

	@Test
	void assertEqualValue_afterInstanciateOpenObjectChemString() throws Exception {
		OpenObject ooOci1 = OpenObjectFactory.createOpenObjectForOpenChemString("Hello World!");
		assertEquals("Hello World!", ooOci1.getOCOBJ().getOCSTR().getContent());
	}

	@Test
	void assertEqualValue_afterInstanciateOpenObjectMathInteger() throws Exception {
		OpenObject ooOmi1 = OpenObjectFactory.createOpenObjectForOpenMathInteger(1);
		assertEquals("1", ooOmi1.getOMOBJ().getOMI().getValue());
	}

	@Test
	void assertEqualValue_afterInstanciateOpenObjectMathString() throws Exception {
		OpenObject ooOmi1 = OpenObjectFactory.createOpenObjectForOpenMathString("Hello World!");
		assertEquals("Hello World!", ooOmi1.getOMOBJ().getOMSTR().getContent());
	}

}
