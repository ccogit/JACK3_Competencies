package de.uni_due.s3.jack3.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.utils.EntityReflectionHelper;

/**
 * Specifies that a field appears in the {@link String} representation of an {@link AbstractEntity}.
 * 
 * @see EntityReflectionHelper#generateToString(Object)
 * @see AbstractEntity#toString()
 */
@Retention(RetentionPolicy.RUNTIME) // available for reflection
@Target(ElementType.FIELD)
public @interface ToString {

}
