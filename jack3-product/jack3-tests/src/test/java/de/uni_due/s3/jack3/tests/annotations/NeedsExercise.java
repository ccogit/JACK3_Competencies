package de.uni_due.s3.jack3.tests.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicates that a test class needs an automatic generated exercise.
 */
@Target(ElementType.TYPE)
@Retention(RUNTIME)
@Inherited
public @interface NeedsExercise {

}
