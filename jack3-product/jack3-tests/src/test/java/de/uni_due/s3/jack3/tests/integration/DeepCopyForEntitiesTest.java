package de.uni_due.s3.jack3.tests.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.Transient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.reflections.Reflections;

import de.uni_due.s3.jack3.annotations.DeepCopyOmitField;
import de.uni_due.s3.jack3.business.CourseBusiness;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.providers.AbstractExerciseProvider;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.entities.stagetypes.java.AbstractJavaCheckerConfiguration;
import de.uni_due.s3.jack3.entities.stagetypes.java.TracingGradingConfig;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.stagetypes.python.AbstractPythonCheckerConfiguration;
import de.uni_due.s3.jack3.entities.stagetypes.python.TracingPythonGradingConfig;
import de.uni_due.s3.jack3.entities.stagetypes.r.AbstractTestCase;
import de.uni_due.s3.jack3.entities.stagetypes.r.StaticRTestCase;
import de.uni_due.s3.jack3.entities.tenant.AbstractCourse;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseEntry;
import de.uni_due.s3.jack3.entities.tenant.CourseResource;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.IdentityProfileField;
import de.uni_due.s3.jack3.entities.tenant.ProfileField;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.Tag;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.interfaces.DeepCopyable;
import de.uni_due.s3.jack3.tests.utils.AbstractBasicTest;

public class DeepCopyForEntitiesTest extends AbstractBasicTest {

	private Map<Class<?>, Object> nonDefaultValuesMap = new HashMap<Class<?>, Object>();
	private Map<Class<?>, Class<?>> implementationOfAbstractClassesMap = new HashMap<>();
	private List<Field> JustCopyReferenceOfFieldsList = new ArrayList<>();

	{
		nonDefaultValuesMap.put(boolean.class, true);
		nonDefaultValuesMap.put(char.class, 'v');
		nonDefaultValuesMap.put(byte.class, (byte) 4);
		nonDefaultValuesMap.put(Byte.class, Byte.valueOf((byte) 4));
		nonDefaultValuesMap.put(short.class, (short) 8);
		nonDefaultValuesMap.put(Short.class, Short.valueOf((short) 4));
		nonDefaultValuesMap.put(int.class, 42);
		nonDefaultValuesMap.put(Integer.class, Integer.valueOf(42));
		nonDefaultValuesMap.put(long.class, 12345L);
		nonDefaultValuesMap.put(Long.class, Long.valueOf(12345L));
		nonDefaultValuesMap.put(float.class, 3.1415f);
		nonDefaultValuesMap.put(Float.class, Float.valueOf(3.1415f));
		nonDefaultValuesMap.put(double.class, 2.7d);
		nonDefaultValuesMap.put(Double.class, Double.valueOf(2.7d));
		nonDefaultValuesMap.put(String.class, "example");
		nonDefaultValuesMap.put(Map.class, new HashMap<>());
		nonDefaultValuesMap.put(List.class, new ArrayList<>());
		nonDefaultValuesMap.put(Set.class, new HashSet<>());
		nonDefaultValuesMap.put(LocalDateTime.class, LocalDateTime.of(2021, 5, 12, 11, 00));
		nonDefaultValuesMap.put(Duration.class, Duration.ofMinutes(25));
		nonDefaultValuesMap.put(Tag.class, new Tag("Example Tag")); //we always use this tag, because this tag is persisted in the db.
		nonDefaultValuesMap.put(Stage.class, new MCStage()); //we always use this mcStage as stage, because if we always create a new Stage for every stage field, the exercise class has trouble with setting a startStage.

		implementationOfAbstractClassesMap.put(Stage.class, MCStage.class);
		implementationOfAbstractClassesMap.put(AbstractCourse.class, Course.class);
		implementationOfAbstractClassesMap.put(AbstractExercise.class, Exercise.class);
		implementationOfAbstractClassesMap.put(AbstractExerciseProvider.class, FolderExerciseProvider.class);
		implementationOfAbstractClassesMap.put(ProfileField.class, IdentityProfileField.class);
		implementationOfAbstractClassesMap.put(AbstractEntity.class, User.class);
		implementationOfAbstractClassesMap.put(AbstractTestCase.class, StaticRTestCase.class);
		implementationOfAbstractClassesMap.put(AbstractJavaCheckerConfiguration.class, TracingGradingConfig.class);
		implementationOfAbstractClassesMap.put(AbstractPythonCheckerConfiguration.class,
				TracingPythonGradingConfig.class);
	}

	@Inject
	private CourseBusiness courseBusiness;

	private Field failedField = null; // we use this for a better error message if the test fails.

	/**
	 * Persist a tag, a user and a Course.
	 * We need a tag in the DB for calling deep copy of Exercise and Course
	 * Also deep coping a course only works if the course is persisted in the DB (we also need a persisted user for
	 * this)
	 *
	 * @throws ActionNotAllowedException
	 */
	@BeforeEach
	private void prepareTest() throws ActionNotAllowedException {
		baseService.persist(new Tag("Example Tag"));

		user = getLecturer("testLecturer");
		nonDefaultValuesMap.put(User.class, user);

		Course course = courseBusiness.createCourse("Example Course", user, user.getPersonalFolder());
		nonDefaultValuesMap.put(Course.class, course);

		nonDefaultValuesMap.put(ContentFolder.class, user.getPersonalFolder());

		//we need this predefined CourseResource, because otherwise we can't merge the course later on. We would get a rolledBackException if the courseResource wouldn't have valid values
		nonDefaultValuesMap.put(CourseResource.class, new CourseResource("Example fileName", new byte[0],
				(AbstractCourse) nonDefaultValuesMap.get(Course.class), user));
	}

	@Test
	public void deepCopyTest() {
		//List of all classes which shall be tested
		List<Class<?>> classesToTest = getDeepCopyClasses();

		// Log some information about the classes that are being tested
		getLogger().info("The DeepCopyForEntitiesTest tests " + classesToTest.size() + " different classes.");
		getLogger().info("The following classes will be tested: "
				+ classesToTest.stream().map(clazz -> clazz.getSimpleName()).collect(Collectors.toList()));

		for (Class<?> clazz : classesToTest) {
			//get all relevant fields and make them accessible
			JustCopyReferenceOfFieldsList.clear();
			List<Field> allFields = getAllNotOmittedFields(new ArrayList<Field>(), clazz);
			allFields.forEach(field -> field.setAccessible(true));

			try {
				//get deep copy method
				Method deepCopyMethod = clazz.getMethod("deepCopy");
				//create instance of the current class.
				Object original = clazz.getDeclaredConstructor().newInstance();

				// if the class is a Course we need to persist it later,
				// because of this we have to ensure some fields have valid values and we take
				// a template Course Object from the nonDefaultValueMap.
				if (clazz.equals(Course.class)) {
					original = nonDefaultValuesMap.get(Course.class);
				}

				setNonDefaultValues(original, allFields);

				// if the Class is a Course we need to merge it into the DB (otherwise DeepCopy doesn't work)
				if (clazz.equals(Course.class)) {
					original = courseBusiness.updateCourse((AbstractCourse) original);
				}

				//check every field has none default values
				Object tmp = clazz.getDeclaredConstructor().newInstance();
				for (Field field : allFields) {
					assertNotEquals(field.get(tmp), field.get(original));
				}

				//create deep copy
				Object copy;
				try {
					copy = deepCopyMethod.invoke(original);
				} catch (InvocationTargetException e) {
					if (e.getCause() instanceof UnsupportedOperationException) {
						getLogger().info("The Class '" + original.getClass().getSimpleName()
								+ "' threw an UnsupportedOperationException. The DeepCopy Method will be ignored, because the method isn't implemented yet");
						continue;
					} else {
						throw e;
					}
				}

				// check deep copy has the same none default values as the original
				try {
					CheckThatTheFieldsGotDeepCopied(original, copy, allFields);
				} catch (AssertionFailedError e) {
					throw new AssertionFailedError(
							String.format("The field '%s' from the class '%s' wasn't copied correctly.",
									failedField.getName(), clazz.getSimpleName()),
							e);
				}
			} catch (NoSuchMethodException | IllegalAccessException | InstantiationException
					| InvocationTargetException | NoSuchFieldException e) {
				e.printStackTrace();
				throw new AssertionError(
						"exception while using reflections. The problem accured while testing the class '"
								+ clazz.getSimpleName() + "'.",
								e);
			}
		}
	}

	/**
	 * returns all classes which implements the DeepCopy Interface and are not abstract
	 */
	private List<Class<?>> getDeepCopyClasses() {
		Reflections reflections = new Reflections("de.uni_due.s3.jack3");

		return reflections.getSubTypesOf(DeepCopyable.class).stream()
				.filter(copyAbleClass -> !java.lang.reflect.Modifier.isAbstract(copyAbleClass.getModifiers()))
				.collect(Collectors.toList());
	}

	/**
	 * we consider only fields which:
	 * 1. are not static
	 * 2. don't have the DeepCopyOmitField annotation
	 * 3. don't have the Transient annotation or the Transient keyWord
	 */
	private List<Field> getAllNotOmittedFields(List<Field> fields, Class<?> type) {
		for (Field field : type.getDeclaredFields()) {
			final boolean isStatic = java.lang.reflect.Modifier.isStatic(field.getModifiers());
			final boolean hasDeepCopyOmitFieldAnnotationAndShouldBeIgnoredTotaly = field
					.getAnnotation(DeepCopyOmitField.class) != null
					&& !field.getAnnotation(DeepCopyOmitField.class).copyTheReference();
			final boolean isTransient = field.getAnnotation(Transient.class) != null
					|| java.lang.reflect.Modifier.isTransient(field.getModifiers());

			if (!(isStatic || hasDeepCopyOmitFieldAnnotationAndShouldBeIgnoredTotaly || isTransient)) {
				fields.add(field);
				if (field.getAnnotation(DeepCopyOmitField.class) != null) {
					// this fields have @DeepCopyOmitField annotation but with the parameter "copyTheReference=true"
					// because of this we won't ignore this fields completely but will only check
					// that the reference got copied and not the whole object
					JustCopyReferenceOfFieldsList.add(field);
				}
			}
		}

		Class<?> superClass = type.getSuperclass();
		if (superClass != null && !superClass.equals(AbstractEntity.class)) {
			getAllNotOmittedFields(fields, superClass);
		}
		return fields;
	}

	private void CheckThatTheFieldsGotDeepCopied(Object original, Object copy, List<Field> fields)
			throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {

		for (Field field : fields) {
			failedField = field;
			Class<?> fieldType = field.getType();

			// All fields got nonDefault values. Because of this the copy shouldn't have null values as well.
			assertNotNull(field.get(copy));

			// Check if the value of the field is correct
			if (AbstractEntity.class.isAssignableFrom(fieldType)) {
				// The type of the field is an entity
				assertEqualsForEntities(field, field.get(original), field.get(copy));
			} else if (Collection.class.isAssignableFrom(fieldType)) {
				// The field is a collection. We check that the Collection from the original and from the copy has the same size.
				// Also we check that the element which was added to the collection is correctly copied.
				Method size = Collection.class.getDeclaredMethod("size", new Class[] {});
				Method getIterator = Collection.class.getDeclaredMethod("iterator", new Class[] {});

				//both collections should have exactly one element
				assertEquals(1, size.invoke(field.get(original)));
				assertEquals(size.invoke(field.get(original)), size.invoke(field.get(copy)));

				Class<?> actualTypeArguments = (Class<?>) ((ParameterizedType) field.getGenericType())
						.getActualTypeArguments()[0];

				if (AbstractEntity.class.isAssignableFrom(actualTypeArguments)) {
					//the elements in the collection are entities
					assertEqualsForEntities(field, ((Iterator<?>) getIterator.invoke(field.get(original))).next(),
							((Iterator<?>) getIterator.invoke(field.get(copy))).next());
				} else {
					//the elements aren't entities and should be simply equal
					assertEquals(((Iterator<?>) getIterator.invoke(field.get(original))).next(),
							((Iterator<?>) getIterator.invoke(field.get(copy))).next());
				}

			} else if (fieldType.isArray()) {
				if (AbstractEntity.class.isAssignableFrom(fieldType.getComponentType())) {
					//the elements of the array are entities
					assertEqualsForEntities(field, Array.get(field.get(original), 0), Array.get(field.get(copy), 0));
				} else {
					//the elements of the array aren't entities and should be equal
					assertEquals(Array.get(field.get(original), 0), Array.get(field.get(copy), 0));
				}
			} else {
				// the values aren't entities or maps or arrays
				// we just check that the values are equals
				assertEquals(field.get(original), field.get(copy));
			}
		}
	}

	private void setNonDefaultValues(Object obj, List<Field> fields)
			throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException,
			NoSuchFieldException {

		for (Field field : fields) {
			final Class<?> fieldType = field.getType();

			if (fieldType.equals(boolean.class)) {
				//if the field is a boolean just change its value
				field.set(obj, !(boolean) field.get(obj));
				continue;
			}

			if (fieldType.isEnum()) {
				setValueForEnum(obj, field);
				continue;
			}

			if (Map.class.isAssignableFrom(fieldType)) {
				//if the field is a map, be sure the field is not null and add a value
				if (field.get(obj) == null) {
					field.set(obj, new HashMap<>());
				}
				putKeyValuePairInMap(obj, field); //add value to the map
				continue;
			}

			if (Collection.class.isAssignableFrom(fieldType)) {
				//if the field is a Collection, be sure the field is not null and add a value
				if (field.get(obj) == null) {
					if (Set.class.isAssignableFrom(fieldType)) {
						field.set(obj, new HashSet<>());
					} else {
						field.set(obj, new ArrayList<>());
					}
				}
				addValueInCollection(obj, field); //add value to the Collection
				continue;
			}

			if (fieldType.isArray()) {
				//create an array with the length of 1
				Object newArray = Array.newInstance(fieldType.getComponentType(), 1);
				// set a element at the index 0
				Array.set(newArray, 0, createInstanceOfObject(fieldType.getComponentType()));
				//set the array to the field
				field.set(obj, newArray);
				continue;
			}

			if (nonDefaultValuesMap.containsKey(fieldType)) {
				//set a non default value
				field.set(obj, nonDefaultValuesMap.get(fieldType));
				continue;
			}

			if (AbstractEntity.class.isAssignableFrom(fieldType)) {
				//if we have a subclass of AbstractEntity check if it is an abstract class
				if (implementationOfAbstractClassesMap.containsKey(fieldType)) {
					//it is an abstract class. Use one of the not abstract subclasses to create an instance
					Constructor<?> constructor = implementationOfAbstractClassesMap.get(fieldType)
							.getDeclaredConstructor();
					constructor.setAccessible(true);

					field.set(obj, constructor.newInstance());
					continue;
				} else {
					//it is not an abstract class. Just create an instance of the class
					field.set(obj, fieldType.getDeclaredConstructor().newInstance());
					continue;
				}
			}

			throw new AssertionError("No none default Value could be assigned to The Field '" + field.getName()
			+ "' from the Class '" + field.getDeclaringClass() + "'.");
		}
	}

	private void setValueForEnum(Object obj, Field enumField)
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		//Be sure the new value is different to the default value for this object.
		if (enumField.getType().getEnumConstants()[0].equals(enumField.get(obj))) {
			enumField.set(obj, enumField.getType().getEnumConstants()[1]);
		} else {
			enumField.set(obj, enumField.getType().getEnumConstants()[0]);
		}
	}

	private void addValueInCollection(Object obj, Field field) throws NoSuchMethodException, IllegalAccessException,
	InvocationTargetException, InstantiationException, NoSuchFieldException {
		//get generic type of the Collection
		ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
		Class<?> actualTypeArguments = (Class<?>) parameterizedType.getActualTypeArguments()[0];

		//get "add" method
		Method add = Collection.class.getDeclaredMethod("add", Object.class);
		Object toAdd = createInstanceOfObject(actualTypeArguments);

		add.invoke(field.get(obj), toAdd);
	}

	private void putKeyValuePairInMap(Object obj, Field field) throws NoSuchMethodException,
	IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException {
		//get generic types of the map
		ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
		Class<?> firstTypeArgument = (Class<?>) parameterizedType.getActualTypeArguments()[0];
		Class<?> secondTypeArgument = (Class<?>) parameterizedType.getActualTypeArguments()[1];

		//get "put" method
		Method put = Map.class.getDeclaredMethod("put", Object.class, Object.class);

		Object firstArgument = createInstanceOfObject(firstTypeArgument);
		Object secondArgument = createInstanceOfObject(secondTypeArgument);

		//put the new element into the map
		put.invoke(field.get(obj), firstArgument, secondArgument);
	}

	private Object createInstanceOfObject(Class<?> type)
			throws InstantiationException, IllegalAccessException, NoSuchFieldException, InvocationTargetException,
			NoSuchMethodException {
		Object result;

		if (nonDefaultValuesMap.containsKey(type)) {
			//if we have a value saved in the nonDefaultValuesMap we can just use this.
			result = nonDefaultValuesMap.get(type);
		} else {
			if (java.lang.reflect.Modifier.isAbstract(type.getModifiers())) {
				//the typeArgument is an abstract class.
				//use the implementationOfAbstractClassesMap map to get a not abstract subclass.
				if (implementationOfAbstractClassesMap.containsKey(type)) {
					//create an instance of the subclass.
					result = implementationOfAbstractClassesMap.get(type).getDeclaredConstructor().newInstance();
				} else {
					throw new AssertionError("The Class '" + type
							+ "' is abstract, but we dont have a not abstract sub class in the 'implementationOfAbstractClassesMap' map."
							+ "Please add a corresponding sub class which shall be used in this case.");
				}
			} else {
				//the typeArgument is not an abstract class. We can simply create an instance.
				result = type.getDeclaredConstructor().newInstance();

				// TODO we need this to avoid exceptions by calling deepCopy from the FixedListExerciseProvider
				if (type.equals(CourseEntry.class)) {
					//the CourseEntry needs an exercise. If the exercise is null. the DeepCopy Method from FixedListExerciseProvider will throw an exception
					Field exerciseField = CourseEntry.class.getDeclaredField("exercise");
					exerciseField.setAccessible(true);
					exerciseField.set(result, Exercise.class.getDeclaredConstructor().newInstance());
				}
			}
		}
		return result;
	}

	private void assertEqualsForEntities(Field field, Object entity1, Object entity2)
			throws IllegalArgumentException, IllegalAccessException {
		if (JustCopyReferenceOfFieldsList.contains(field)) {
			//the field is an abstract entity. We only copied the reference of the object. Because of this the Object should be the same
			assertEquals(entity1, entity2);
		} else {
			//the field is an abstract entity. The original object and the copy object should not be equals,
			//because the copied entity got a different ID
			assertNotEquals(entity1, entity2);
		}
	}

}
