package de.uni_due.s3.jack3.business.microservices.openobjectutils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.openmath.OMOBJ;

import de.uni_due.s3.jack3.exceptions.JackRuntimeException;
import de.uni_due.s3.openchem.OCOBJ;
import de.uni_due.s3.openobject.OpenObject;

class OpenObjectJAXBContext {

	static OpenObjectJAXBContext INSTANCE;

	private JAXBContext context;

	private OpenObjectJAXBContext() throws JAXBException {
		context = JAXBContext.newInstance(OpenObject.class, OMOBJ.class, OCOBJ.class);
	}

	static {
		try {
			INSTANCE = new OpenObjectJAXBContext();
		} catch (JAXBException e) {
			throw new JackRuntimeException("Could not create OpenObject XML Converter.", e);
		}
	}

	JAXBContext getJAXBContext() {
		return context;
	}
}
