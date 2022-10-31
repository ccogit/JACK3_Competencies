package de.uni_due.s3.jack3.business.microservices.openobjectutils;

import static de.uni_due.s3.jack3.business.microservices.openobjectutils.TestOpenObjectData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.Test;

class OpenObjectConverterTest {

	@Test
	void assertEquals_convertingOpenObjectToXmlString() throws JAXBException {
		assertEquals(XML_OO_ONE, OpenObjectConverter.toXmlString(OO_ONE));
		assertEquals(XML_OO_TEXT, OpenObjectConverter.toXmlString(OO_TEXT));
		assertEquals(XML_OO_VARIABLE, OpenObjectConverter.toXmlString(OO_VARIABLE));
		assertEquals(XML_OO_TRUE, OpenObjectConverter.toXmlString(OO_TRUE));
	}

	@Test
	void assertEquals_convertingXmlStringsToOpenObject() throws JAXBException {
		assertEquals(OO_ONE, OpenObjectConverter.fromXmlString(XML_OM_ONE));
		assertEquals(OO_ONE, OpenObjectConverter.fromXmlString(XML_OM_ONE_XMLNS));
		assertEquals(OO_ONE, OpenObjectConverter.fromXmlString(XML_OO_ONE));

		assertEquals(OO_TEXT, OpenObjectConverter.fromXmlString(XML_OM_TEXT));
		assertEquals(OO_TEXT, OpenObjectConverter.fromXmlString(XML_OM_TEXT_XMLNS));
		assertEquals(OO_TEXT, OpenObjectConverter.fromXmlString(XML_OO_TEXT));

		assertEquals(OO_VARIABLE, OpenObjectConverter.fromXmlString(XML_OM_VARIABLE));
		assertEquals(OO_VARIABLE, OpenObjectConverter.fromXmlString(XML_OM_VARIABLE_XMLNS));
		assertEquals(OO_VARIABLE, OpenObjectConverter.fromXmlString(XML_OO_VARIABLE));

		assertEquals(OO_TRUE, OpenObjectConverter.fromXmlString(XML_OM_TRUE));
		assertEquals(OO_TRUE, OpenObjectConverter.fromXmlString(XML_OM_TRUE_XMLNS));
		assertEquals(OO_TRUE, OpenObjectConverter.fromXmlString(XML_OO_TRUE));
	}
}
