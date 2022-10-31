package de.uni_due.s3.jack3.business.microservices.openobjectutils;

import java.util.List;

import org.openmath.OMA;
import org.openmath.OMF;
import org.openmath.OMI;
import org.openmath.OMOBJ;
import org.openmath.OMS;
import org.openmath.OMSTR;

import de.uni_due.s3.openchem.OCF;
import de.uni_due.s3.openchem.OCI;
import de.uni_due.s3.openchem.OCOBJ;
import de.uni_due.s3.openchem.OCS;
import de.uni_due.s3.openchem.OCSTR;
import de.uni_due.s3.openobject.OpenObject;

public class OpenObjectFactory {

	private static final OpenObject OO_OM_TRUE = OpenObject.of(OMOBJ.of(OMS.of("logic1", "true")));
	private static final OpenObject OO_OM_FALSE = OpenObject.of(OMOBJ.of(OMS.of("logic1", "false")));
	private static final OpenObject OO_OC_TRUE = OpenObject.of(OCOBJ.of(OCS.of("logic1", "true")));
	private static final OpenObject OO_OC_FALSE = OpenObject.of(OCOBJ.of(OCS.of("logic1", "false")));

	public static OpenObject createOpenObjectForOpenMathBoolean(boolean value) {
		return value ? OO_OM_TRUE : OO_OM_FALSE;
	}

	public static OpenObject createOpenObjectForOpenMathInteger(int value) {
		return OpenObject.of(OMOBJ.of(OMI.of(value)));
	}

	public static OpenObject createOpenObjectForOpenMathFloat(double value) {
		return OpenObject.of(OMOBJ.of(OMF.of(value)));
	}

	public static OpenObject createOpenObjectForOpenMathString(String value) {
		return OpenObject.of(OMOBJ.of(OMSTR.of(value)));
	}

	public static OpenObject createOpenObjectForOpenMathStringList(List<String> list) {
		return OpenObject
				.of(OMOBJ.of(OMA.of(OMS.of("list1", "list"), list.stream().map(OMSTR::of).toArray(OMSTR[]::new))));
	}

	public static OpenObject createOpenObjectForOpenChemBoolean(boolean value) {
		return value ? OO_OC_TRUE : OO_OC_FALSE;
	}

	public static OpenObject createOpenObjectForOpenChemInteger(int value) {
		return OpenObject.of(OCOBJ.of(OCI.of(value)));
	}

	public static OpenObject createOpenObjectForOpenChemFloat(double value) {
		return OpenObject.of(OCOBJ.of(OCF.of(value)));
	}

	public static OpenObject createOpenObjectForOpenChemString(String value) {
		return OpenObject.of(OCOBJ.of(OCSTR.of(value)));
	}
}
