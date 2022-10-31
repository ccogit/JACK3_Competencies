package de.uni_due.s3.jack3.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import javax.persistence.NonUniqueResultException;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.proxy.HibernateProxy;
import org.jboss.logging.Logger;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.multitenancy.LoggerProvider;

/**
 * This class provides static methods to help with traversing unkown objects using reflection
 *
 * @author Benjamin.Otto
 *
 */
public class EntityReflectionHelper {

	private EntityReflectionHelper() {
		throw new IllegalStateException("Class should only be used statically!");
	}

	private static final Logger logger = LoggerProvider.get(EntityReflectionHelper.class);

	/**
	 * Recursively gets all fields of given class and superclasses (excluding Object) using reflection
	 *
	 * @param fields
	 *            List of fields to be populated, calling methods should just supply a new list here
	 * @param type
	 *            Class to get all fields from
	 * @return List of all fields of given class
	 */
	private static List<Field> generateAllFields(List<Field> fields, Class<?> type) {
		fields.addAll(Arrays.asList(type.getDeclaredFields()));

		if (type.getSuperclass() != null) {
			generateAllFields(fields, type.getSuperclass());
		}
		return fields;
	}

	/**
	 * Generates a {@link String} representation of the given object by including only {@link ToString} annotated fields
	 * using reflection.
	 */
	public static String generateToString(Object object) {
		try {
			return generateToString(object, 1);
		} catch (RuntimeException e) {
			return "Exception while trying to generate toString(): " + e.getMessage();
		}

	}

	private static String generateToString(Object object, int indent) {
		List<Field> allFields = new ArrayList<>();

		generateAllFields(allFields, object.getClass());

		final String tabs = "\t".repeat(indent);
		final String tabsForCloseBracket = tabs.substring(1);

		final StringJoiner stringJoiner = new StringJoiner("", " -> {\n", tabsForCloseBracket + "}");
		stringJoiner.setEmptyValue("");

		for (Field field : allFields) {
			indent = addToStringForField(object, field, stringJoiner, indent, tabs);
		}

		String identifier;
		if (object instanceof AbstractEntity) {
			identifier = "#" + ((AbstractEntity) object).getId();
		} else {
			identifier = "@" + Integer.toHexString(object.hashCode());
		}

		final String className = object.getClass().getSimpleName();
		return className + identifier  + stringJoiner.toString();
	}

	private static int addToStringForField(Object object, Field field, StringJoiner toString, int indent, String tabs) {
		if (field.isAnnotationPresent(ToString.class)) {

			Object value = null;
			try {
				field.setAccessible(true); // NOSONAR
				value = field.get(object);
			} catch (IllegalArgumentException | IllegalAccessException | LazyInitializationException e) {
				logger.error("Reflection Exception: " + e);
			}

			if (value instanceof AbstractEntity) {
				// Since it is possible that LazyInitializationExceptions or other RuntimeExceptions are thrown we
				// do the best we can to still display a meaningful string representation of the entity.
				String entityToStringOrExceptionMessage = "";
				try {
					indent++;
					entityToStringOrExceptionMessage = generateToString(value, indent);
				} catch (RuntimeException runtimeException) {
					// It is not useful here to log the entire stacktrace.
					entityToStringOrExceptionMessage = runtimeException.getMessage();
				}
				toString.add(tabs + field.getName() + ":" + entityToStringOrExceptionMessage + "\n");
			} else {
				toString.add(tabs + field.getName() + ":" + value + "\n");
			}
		}
		return indent;
	}

	/**
	 * Traverses the object graph of a given object entity and hibernate-initializes all fields found in the object and
	 * in objects referenced by this object (including maps and collections). But we dont traverse into folder entitys
	 * for performance reasons!
	 *
	 * ********************************************** BEWARE: **********************************************************
	 * This method should be used with caution due to potential performance problems. You have to be aware that all
	 * referenced entitys (with the exception of Folder Entitys) will be loaded from the database, potentially causing
	 * alot of database-calls! But since e.g. Exercises can be arbitraily constructed by the user, there is no way of
	 * knowing all needed Entitys beforehand.
	 * *****************************************************************************************************************
	 *
	 * The better approach would be to obtain an instance from hibernate in JSFs INVOKE_APPLICATION phase and
	 * immediately load the required data from there by e.g. calling size() on a collection that is required later
	 * on. In the RENDER_RESPONSE phase all data must be available because the hibernate session is already closed in
	 * this phase.
	 *
	 * @param object
	 *
	 */
	public static void hibernateInitializeObjectGraph(Object object) {
		if (object == null) {
			return;
		}

		if (object instanceof HibernateProxy) {
			object = Hibernate.unproxy(object);
		}

		// For performance reasons we don't continue to recursively traverse the object graph if we get a
		// Folder instance. If we need to initialize a Folder, we have to write a special method for that in the future.
		if (object instanceof Folder) {
			return;
		}

		List<Field> allFields = new ArrayList<>();
		generateAllFields(allFields, object.getClass());
		for (Field field : allFields) {
			initializeField(object, field);
		}
	}

	@SuppressWarnings("rawtypes")
	private static void initializeField(Object object, Field field) {
		Object fieldAsObject = null;
		// doesn't need to be unset!
		field.setAccessible(true); // NOSONAR
		try {
			// primitive types are wrapped
			fieldAsObject = field.get(object);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("Reflection Exception: ", e);
		}

		if (fieldAsObject instanceof Collection) {
			initializeCollection((Collection) fieldAsObject);
		} else if (fieldAsObject instanceof Map) {
			initializeMap((Map) fieldAsObject);
		} else {
			initializeGeneralField(fieldAsObject);
		}
	}

	private static void initializeGeneralField(Object fieldAsObject) {
		if ((fieldAsObject != null) //
				&& !fieldAsObject.getClass().isPrimitive() //
				&& !Hibernate.isInitialized(fieldAsObject)) {
			try {
				Hibernate.initialize(fieldAsObject);
			} catch (NonUniqueResultException e) {
				// TODO: Hotfix for #1082, we should investigate the root cause!
				logger.error("Error while calling Hibernate.initialize() on field: " + fieldAsObject, e);
			}

			hibernateInitializeObjectGraph(fieldAsObject);
		}
	}

	@SuppressWarnings("rawtypes")
	private static void initializeMap(Map map) {
		logger.debug("Initialising map: " + map.getClass().getName());
		// Calling size on a (envers) map seems to be sufficient to initialize the collection
		// https://stackoverflow.com/a/10030134
		map.size(); // NOSONAR
		for (Object mapValueObject : map.values()) {
			if (!mapValueObject.getClass().isPrimitive()) {
				hibernateInitializeObjectGraph(mapValueObject);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private static void initializeCollection(Collection collection) {
		logger.debug("Initialising collection: " + collection.getClass().getName());
		// Calling size on a (envers) collection seems to be sufficient to initialize the collection
		// https://stackoverflow.com/a/10030134
		collection.size(); // NOSONAR

		for (Object collectionObject : collection) {
			if ((collectionObject != null) && !collectionObject.getClass().isPrimitive()) {
				hibernateInitializeObjectGraph(collectionObject);
			}
		}
	}

	/**
	 * Casts a list of objects to a list the given class "clazz"
	 *
	 * @param clazz
	 * @param collection
	 * @return
	 */
	public static <T> List<T> castList(Class<? extends T> clazz, Collection<?> collection) {
		List<T> arrayList = new ArrayList<>(collection.size());
		for (Object object : collection) {
			arrayList.add(clazz.cast(object));
		}
		return arrayList;
	}

	public static String allSuperClassesOf(Object object) {
		StringBuilder classHierarchy = new StringBuilder();
		if (object == null) {
			return "null!";
		}

		@SuppressWarnings("rawtypes")
		Class clazz = object.getClass();
		while (clazz != null) {
			classHierarchy.append(clazz.getName() + " -> ");
			clazz = clazz.getSuperclass();
		}
		classHierarchy.append("null");

		return classHierarchy.toString();
	}

	public static void printObject(Object object) {
		ReflectionToStringBuilder.toString(object, ToStringStyle.MULTI_LINE_STYLE, false, true);
	}

	// REVIEW: This method seems to be only used in test code and hence should be moved to the test project
	public static <T> void setPerReflection(T entity, String field, Object value) {
		try {
			Field checkerConfigurationsReflection = entity.getClass().getDeclaredField(field);
			checkerConfigurationsReflection.setAccessible(true); // NOSONAR
			checkerConfigurationsReflection.set(entity, value); // NOSONAR
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			logger.error(e);
		}
	}

}
