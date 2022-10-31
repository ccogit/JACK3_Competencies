package de.uni_due.s3.jack3.business.microservices.variableutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.business.microservices.openobjectutils.OpenObjectConverter;
import de.uni_due.s3.jack3.business.microservices.openobjectutils.OpenObjectFactory;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.openobject.OpenObject;

class VariableValueFactoryTest {

	@Test
	void asserEqualsContent_afterInstanciateVariableValueForOpenObjectMathInteger() throws Exception {
		OpenObject ooOmi1 = OpenObjectFactory.createOpenObjectForOpenMathInteger(1);
		VariableValue actual = VariableValueFactory.createVariableValue(ooOmi1);
		VariableValue expected = new VariableValue();
		expected.setContent(OpenObjectConverter.toXmlString(ooOmi1));
		assertEquals(expected.getContent(), actual.getContent());
	}

	@Test
	void asserEqualsContent_afterInstanciateVariableValueForOpenObjectChemInteger() throws Exception {
		OpenObject ooOci1 = OpenObjectFactory.createOpenObjectForOpenChemInteger(1);
		VariableValue actual = VariableValueFactory.createVariableValue(ooOci1);
		VariableValue expected = new VariableValue();
		expected.setContent(OpenObjectConverter.toXmlString(ooOci1));
		assertEquals(expected.getContent(), actual.getContent());
	}

	@Test
	void assertThrowsException_afterInstanciateVariableValueForNull() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			VariableValueFactory.createVariableValue(null);
		});
	}

}
