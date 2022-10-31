package de.uni_due.s3.jack3.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import de.uni_due.s3.jack3.interfaces.DeepCopyable;

/**
 * Specifies that a field is omitted in the deepCopy method and because of this doesn't get deepCopied.
 * {@link DeepCopyable}.
 */
@Retention(RetentionPolicy.RUNTIME) // available for reflection
@Target(ElementType.FIELD)
public @interface DeepCopyOmitField {

	/**
	 * set copyTheReference to false, if the field shall totally be ignored by the deepCopy method.
	 * It should only be set to true if it is an Entity and, in addition, the entity shall not be deep copied, but
	 * instead the same reference shall be used.
	 * 
	 * For example the Class Course has the field 'folder'. Deep coping the Course doesn't mean to also copy the entire
	 * folder. But the field 'folder' should not be totally ignored by the deep copy method. The copy of the course
	 * should have a reference to the same folder as the original course. In this case we would set copyTheRefernce to
	 * true.
	 */
	public boolean copyTheReference() default false;

	/**
	 * Describe the reason why this field is omitted.
	 */
	public String reason();
}