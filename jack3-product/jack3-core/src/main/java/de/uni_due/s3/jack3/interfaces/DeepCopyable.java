package de.uni_due.s3.jack3.interfaces;

/**
 * Indicates that an entity can be deep-copied.
 * 
 * @author Benjamin.Otto
 *
 * @param <T>
 */
public interface DeepCopyable<T> {
	/**
	 * You must supply a deepCopy()-Method that deepcopys the whole object and referenced objects!
	 * This will amongst other things be called, when a frozen revision of an entity is created.
	 * Usually this should look roughly like this:
	 * 
	 * <pre>
	 *	Entity entityDeepCopy = new Entity();
	 *
	 *  // Deepcopy all class fields of the superclass (if any). In this method you have to think
	 *  // "the other way around", because you give it a reference to your current Entity and you need to deepcopy the
	 *  // variables from "this" to your fields, i.e. fieldFromSuperClass = "this".fieldFromSuperClass;
	 *	entityDeepCopy.deepCopySuperVars(this);
	 *
	 * 	// For primitive types:
	 * 	entityDeepCopy.field = field;
	 *
	 * 	// For Object-Types that are allowed to be null:
	 * 	entityDeepCopy.fieldAllowNull = DeepCopyHelper.deepCopyOrNull(fieldAllowNull);
	 *
	 *  // For Object-Types that are not allowed to be null:
	 *  entityDeepCopy.fieldNotAllowNull = fieldNotAllowNull.deepCopy();
	 *
	 * 	// For a collection of entitys, that also need to be deepcopyed.
	 *  // We assume that collections dont contain null!
	 *	for (EntityEntry entityEntry : collectionOfEntitys) {
	 *		entityDeepCopy.collectionOfEntitys.add(entityEntry.deepCopy());
	 *	}
	 *	...
	 * </pre>
	 * 
	 * @return a new instance of T with also deep copied fields
	 */
	T deepCopy();
}
