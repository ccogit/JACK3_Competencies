package de.uni_due.s3.jack3.business.microservices.variableutils;

import java.util.List;

import de.uni_due.s3.jack3.business.microservices.openobjectutils.OpenObjectConverter;
import de.uni_due.s3.jack3.business.microservices.openobjectutils.OpenObjectFactory;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.openobject.OpenObject;

public class VariableValueFactory {

	private static final String OO_OM_TRUE = toXmlString(OpenObjectFactory.createOpenObjectForOpenMathBoolean(true));
	private static final String OO_OM_FALSE = toXmlString(OpenObjectFactory.createOpenObjectForOpenMathBoolean(false));
	private static final String OO_OC_TRUE = toXmlString(OpenObjectFactory.createOpenObjectForOpenChemBoolean(true));
	private static final String OO_OC_FALSE = toXmlString(OpenObjectFactory.createOpenObjectForOpenChemBoolean(false));

	public static VariableValue createVariableValueForOpenMathBoolean(boolean value) {
		return value ? instantiateVariableValue(OO_OM_TRUE) : instantiateVariableValue(OO_OM_FALSE);
	}

	public static VariableValue createVariableValueForOpenMathInteger(int value) {
		return createVariableValue(OpenObjectFactory.createOpenObjectForOpenMathInteger(value));
	}

	public static VariableValue createVariableValueForOpenMathFloat(double value) {
		return createVariableValue(OpenObjectFactory.createOpenObjectForOpenMathFloat(value));
	}

	public static VariableValue createVariableValueForOpenMathString(String value) {
		return createVariableValue(OpenObjectFactory.createOpenObjectForOpenMathString(value));
	}

	public static VariableValue createVariableValueForOpenMathStringList(List<String> list) {
		return createVariableValue(OpenObjectFactory.createOpenObjectForOpenMathStringList(list));
	}

	public static VariableValue createVariableValueForOpenChemBoolean(boolean value) {
		return value ? instantiateVariableValue(OO_OC_TRUE) : instantiateVariableValue(OO_OC_FALSE);
	}

	public static VariableValue createVariableValueForOpenChemInteger(int value) {
		return createVariableValue(OpenObjectFactory.createOpenObjectForOpenChemInteger(value));
	}

	public static VariableValue createVariableValueForOpenChemFloat(double value) {
		return createVariableValue(OpenObjectFactory.createOpenObjectForOpenChemFloat(value));
	}

	public static VariableValue createVariableValueForOpenChemString(String value) {
		return createVariableValue(OpenObjectFactory.createOpenObjectForOpenChemString(value));
	}

	public static VariableValue createVariableValue(OpenObject openObject) {
		return instantiateVariableValue(toXmlString(openObject));
	}

	private static String toXmlString(OpenObject openObject) {
		return OpenObjectConverter.toXmlString(openObject);
	}

	private static VariableValue instantiateVariableValue(String xmlString) {
		VariableValue var = new VariableValue();
		var.setContent(xmlString);
		return var;
	}

}
