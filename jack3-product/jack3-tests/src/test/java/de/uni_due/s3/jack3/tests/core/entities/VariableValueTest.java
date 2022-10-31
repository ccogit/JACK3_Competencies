package de.uni_due.s3.jack3.tests.core.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmath.OMI;
import org.openmath.OMOBJ;

import de.uni_due.s3.jack3.business.microservices.openobjectutils.OpenObjectConverter;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;
import de.uni_due.s3.openobject.OpenObject;

/**
 * Test class for {@linkplain VariableValue} with sample OpenObjects.
 * 
 * @author lukas.glaser
 *
 */
class VariableValueTest extends AbstractBasicTest {

	private VariableValue varValue = new VariableValue();

	/**
	 * Persist default Variable Value
	 */
	@BeforeEach
	@Override
	protected void doBeforeTest() {
		super.doBeforeTest();
		baseService.persist(varValue);
	}

	/**
	 * Set and get a sample Integer with value 42
	 */
	@Test
	void testSampleVariableValue() {
		// Set OpenMathInteger
		OpenObject oo = OpenObject.of(OMOBJ.of(OMI.of(42)));
		varValue.setContent(OpenObjectConverter.toXmlString(oo));

		varValue = baseService.merge(varValue);

		// Expected XML representation of an OpenMathInteger with the value 42

		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
				+ "<oo:OpenObject xmlns:oo=\"http://s3.uni-due.de/OpenObject\" xmlns:oc=\"http://s3.uni-due.de/OpenChem\" xmlns:om=\"http://www.openmath.org/OpenMath\">"
				+ "<om:OMOBJ>" + "<om:OMI>42</om:OMI>" + "</om:OMOBJ>" + "</oo:OpenObject>";
		assertEquals(expected, varValue.getContent());
	}

	/**
	 * Set and get an empty OpenObject
	 */
	@Test
	void testEmptyVariableValue() {
		varValue.setContent(OpenObjectConverter.toXmlString(new OpenObject()));
		varValue = baseService.merge(varValue);

		// Expected XML representation of an empty OpenObject
		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
				+ "<oo:OpenObject xmlns:oo=\"http://s3.uni-due.de/OpenObject\" xmlns:oc=\"http://s3.uni-due.de/OpenChem\" xmlns:om=\"http://www.openmath.org/OpenMath\"/>";

		assertEquals(expected, varValue.getContent());
	}

	/**
	 * Get a non-specified OpenObject (default value), NPE is thrown
	 */
	@Test
	void testDefaultVariableValue() throws JAXBException {
		assertThrows(IllegalArgumentException.class, () -> {
			OpenObjectConverter.toXmlString(null);
		});
	}

}
