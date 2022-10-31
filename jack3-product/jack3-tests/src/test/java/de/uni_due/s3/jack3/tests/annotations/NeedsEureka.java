package de.uni_due.s3.jack3.tests.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that a test class needs an Eureka configuration.
 */
@Retention(RUNTIME)
@Target(TYPE)
@Inherited
public @interface NeedsEureka {

}
