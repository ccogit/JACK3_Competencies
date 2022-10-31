package de.uni_due.s3.jack3.tests.arquillian;

/**
 * Defines strategies for deployment and undeployment.
 * 
 * <code>SINGLE</code> defines that the test class must need a single deployment.
 * 
 * <code>MANAGED</code> specifies that the test class does not need a single deployment.
 * 
 * @author lukas.glaser
 *
 */
public enum EDeploymentType {

	/**
	 * Defines that the test class must need a single deployment.
	 */
	SINGLE,

	/**
	 * Specifies that the test class does not need a single deployment.
	 */
	MULTI

}
