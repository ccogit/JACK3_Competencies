package de.uni_due.s3.jack3.business.microservices.openobjectutils;

import org.openmath.OMI;
import org.openmath.OMOBJ;
import org.openmath.OMS;
import org.openmath.OMSTR;
import org.openmath.OMV;

import de.uni_due.s3.openobject.OpenObject;

final class TestOpenObjectData {

	static final OMOBJ OM_ONE = OMOBJ.of(OMI.of(1));
	static final String XML_OM_ONE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><om:OMOBJ xmlns:om=\"http://www.openmath.org/OpenMath\"><om:OMI>1</om:OMI></om:OMOBJ>";
	static final String XML_OM_ONE_XMLNS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><OMOBJ xmlns=\"http://www.openmath.org/OpenMath\"><OMI>1</OMI></OMOBJ>";
	static final OpenObject OO_ONE = OpenObject.of(OM_ONE);
	static final String XML_OO_ONE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><oo:OpenObject xmlns:oo=\"http://s3.uni-due.de/OpenObject\" xmlns:oc=\"http://s3.uni-due.de/OpenChem\" xmlns:om=\"http://www.openmath.org/OpenMath\"><om:OMOBJ><om:OMI>1</om:OMI></om:OMOBJ></oo:OpenObject>";

	static final OMOBJ OM_TEXT = OMOBJ.of(OMSTR.of("Hello World!"));
	static final String XML_OM_TEXT = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><om:OMOBJ xmlns:om=\"http://www.openmath.org/OpenMath\"><om:OMSTR>Hello World!</om:OMSTR></om:OMOBJ>";
	static final String XML_OM_TEXT_XMLNS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><OMOBJ xmlns=\"http://www.openmath.org/OpenMath\"><OMSTR>Hello World!</OMSTR></OMOBJ>";
	static final OpenObject OO_TEXT = OpenObject.of(OM_TEXT);
	static final String XML_OO_TEXT = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><oo:OpenObject xmlns:oo=\"http://s3.uni-due.de/OpenObject\" xmlns:oc=\"http://s3.uni-due.de/OpenChem\" xmlns:om=\"http://www.openmath.org/OpenMath\"><om:OMOBJ><om:OMSTR>Hello World!</om:OMSTR></om:OMOBJ></oo:OpenObject>";

	static final OMOBJ OM_VARIABLE = OMOBJ.of(OMV.of("a"));
	static final String XML_OM_VARIABLE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><om:OMOBJ xmlns:om=\"http://www.openmath.org/OpenMath\"><om:OMV name=\"a\"/></om:OMOBJ>";
	static final String XML_OM_VARIABLE_XMLNS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><OMOBJ xmlns=\"http://www.openmath.org/OpenMath\"><OMV name=\"a\"/></OMOBJ>";
	static final OpenObject OO_VARIABLE = OpenObject.of(OM_VARIABLE);
	static final String XML_OO_VARIABLE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><oo:OpenObject xmlns:oo=\"http://s3.uni-due.de/OpenObject\" xmlns:oc=\"http://s3.uni-due.de/OpenChem\" xmlns:om=\"http://www.openmath.org/OpenMath\"><om:OMOBJ><om:OMV name=\"a\"/></om:OMOBJ></oo:OpenObject>";

	static final OMOBJ OM_TRUE = OMOBJ.of(OMS.of("logic1", "true"));
	static final String XML_OM_TRUE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><om:OMOBJ xmlns:om=\"http://www.openmath.org/OpenMath\"><om:OMS name=\"true\" cd=\"logic1\"/></om:OMOBJ>";
	static final String XML_OM_TRUE_XMLNS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><OMOBJ xmlns=\"http://www.openmath.org/OpenMath\"><OMS name=\"true\" cd=\"logic1\"/></OMOBJ>";
	static final OpenObject OO_TRUE = OpenObject.of(OM_TRUE);
	static final String XML_OO_TRUE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><oo:OpenObject xmlns:oo=\"http://s3.uni-due.de/OpenObject\" xmlns:oc=\"http://s3.uni-due.de/OpenChem\" xmlns:om=\"http://www.openmath.org/OpenMath\"><om:OMOBJ><om:OMS name=\"true\" cd=\"logic1\"/></om:OMOBJ></oo:OpenObject>";

}
