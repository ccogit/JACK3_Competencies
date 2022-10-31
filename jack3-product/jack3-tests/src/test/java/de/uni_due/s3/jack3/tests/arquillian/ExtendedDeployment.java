package de.uni_due.s3.jack3.tests.arquillian;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Use this annotation to activate extended deployment.
 * 
 * @author lukas.glaser
 */
@Target(ElementType.TYPE)
@Retention(RUNTIME)
@Inherited
public @interface ExtendedDeployment {

	EDeploymentType value();

}
