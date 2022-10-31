package de.uni_due.s3.jack3.business.microservices.openobjectutils;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBException;

import org.openmath.OMOBJ;

import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.jack3.exceptions.JackRuntimeException;
import de.uni_due.s3.openchem.OCOBJ;
import de.uni_due.s3.openobject.OpenObject;

/**
 * This class provides converting functions for OpenObject and XML. With this class it is possible to convert an
 * OpenObject to String based XML representation and the other way around using the un/marshalling process of
 * JAXBContext.
 * 
 * @author Sebastian Pobel
 */
public class OpenObjectConverter {

	private static OpenObjectJAXBContext holder = OpenObjectJAXBContext.INSTANCE;

	public static String toXmlString(OpenObject openObject) {
		try {
			return marshalOpenObject(openObject);
		} catch (JAXBException e) {
			throw new JackRuntimeException("Could not convert openobject to xml-string, due to JAXBException.", e);
		}
	}

	public static OpenObject fromXmlString(String xml) {
		try {
			return castObjectToOpenObject(unmarshalXmlToObject(xml));
		} catch (JAXBException e) {
			throw new JackRuntimeException("Could not convert xml-string to openobject, due to JAXBException.", e);
		}
	}

	public static OpenObject fromVariableValue(VariableValue variableValue) {
		return OpenObjectConverter.fromXmlString(variableValue.getContent());
	}

	private static OpenObject castObjectToOpenObject(Object object) {
		if (object instanceof OMOBJ) {
			return OpenObject.of((OMOBJ) object);
		} else if (object instanceof OCOBJ) {
			return OpenObject.of((OCOBJ) object);
		} else {
			return (OpenObject) object;
		}
	}

	private static Object unmarshalXmlToObject(String xml) throws JAXBException {
		return holder.getJAXBContext().createUnmarshaller().unmarshal(new StringReader(xml));
	}

	private static String marshalOpenObject(OpenObject openObject) throws JAXBException {
		StringWriter sw = new StringWriter();
		holder.getJAXBContext().createMarshaller().marshal(openObject, sw);
		return sw.toString();
	}

}
