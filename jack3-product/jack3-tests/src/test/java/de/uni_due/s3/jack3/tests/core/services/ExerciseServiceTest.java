package de.uni_due.s3.jack3.tests.core.services;

import static de.uni_due.s3.jack3.entities.AccessRight.READ;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;

import de.uni_due.s3.jack3.entities.tenant.*;
import de.uni_due.s3.jack3.services.SubjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.services.ExerciseService;
import de.uni_due.s3.jack3.services.TagService;
import de.uni_due.s3.jack3.tests.utils.AbstractContentTest;
import de.uni_due.s3.jack3.tests.utils.TestDataFactory;

class ExerciseServiceTest extends AbstractContentTest {

    private User lecturer = TestDataFactory.getUser("Lecturer", true, true);
    private User student = TestDataFactory.getUser("Student", false, false);

    @Inject
    private TagService tagService;

    @Inject
    private ExerciseService exerciseService;

    @Inject
    private SubjectService subjectService;

    @BeforeEach
    @Override
    protected void doBeforeTest() {
        userService.persistUser(lecturer);
        userService.persistUser(student);
        persistFolder();
    }

    /**
     * Create new exercise and put in folder
     */
    private Exercise addNewExercise(String exerciseName) {
        Exercise exercise = new Exercise(exerciseName, TestDataFactory.getDefaultLanguage());
        folder.addChildExercise(exercise);
        exerciseService.persistExercise(exercise);
        folder = folderService.mergeContentFolder(folder);
        folder = folderService.getContentFolderWithLazyData(folder);
        return exercise;
    }

    /**
     * Create new exercise with subject and put in folder
     */
    private Exercise addNewExerciseWithSubject(String exerciseName, Subject subject) {
        Exercise exercise = new Exercise(exerciseName, TestDataFactory.getDefaultLanguage());
        exercise.setSubject(subject);
        folder.addChildExercise(exercise);
        exerciseService.persistExercise(exercise);
        folder = folderService.mergeContentFolder(folder);
        folder = folderService.getContentFolderWithLazyData(folder);
        return exercise;
    }


    /**
     * Create new subject
     */
    private Subject addNewSubject(String subjectName) {
        Subject newSubject = new Subject(subjectName);
        subjectService.persistSubject(newSubject);
        return newSubject;
    }

    /**
     * Tests empty database
     */
    @Test
    void testEmptyDatabase() {

        // there should be no exercises
        assertTrue(exerciseService.getAllExercisesForUser(lecturer).isEmpty());
        assertTrue(exerciseService.getAllExercisesForUser(student).isEmpty());
    }

    /**
     * Delete exercise
     */
    @Test
    void deleteExercise() {

        // persist exercise
        Exercise exercise = addNewExercise("Exercise");

        // delete exercise
        folder.removeChildExercise(exercise);
        exerciseService.deleteExercise(exercise);

        // exercise should be deleted from database & folder
        assertTrue(exerciseService.getAllExercisesForUser(lecturer).isEmpty());
        assertTrue(exerciseService.getAllExercisesForUser(student).isEmpty());
        assertTrue(exerciseService.getAllExercisesForContentFolder(folder).isEmpty());
        assertTrue(exerciseService.getAllExercisesForContentFolderList(Arrays.asList(folder)).isEmpty());
        assertTrue(exerciseService.getAllExercisesForContentFolderListBySubject(Arrays.asList(folder), null).isEmpty());
    }

    /**
     * Get exercise with lazy data
     */
    @Test
    void getExerciseWithLazyData() {

        // persist exercise
        AbstractExercise exercise = addNewExercise("Exercise");

        // get exercise with lazy data
        long exerciseID = exercise.getId();
        AbstractExercise exerciseWithLazyData = exerciseService.getExerciseByIdWithLazyData(exerciseID)
                .orElseThrow(AssertionError::new);
        assertEquals(exercise, exerciseWithLazyData);
        assertEquals(exercise,
                exerciseService.getExerciseByIdWithLazyData(exerciseID).orElseThrow(AssertionError::new));

        // get fields
        assertTrue(exerciseWithLazyData.getStages().isEmpty());
        assertTrue(exerciseWithLazyData.getTags().isEmpty());
        assertTrue(exerciseWithLazyData.getExerciseResources().isEmpty());
        assertTrue(exerciseWithLazyData.getVariableDeclarations().isEmpty());
        assertTrue(exerciseWithLazyData.getResultFeedbackMappings().isEmpty());
    }

    /**
     * Get all exercises for user
     */
    @Test
    void getExerciseForUser() {

        // add exercises
        Exercise exercise1 = addNewExercise("Exercise 1");
        Exercise exercise2 = addNewExercise("Exercise 2");

        // set rights for Lecturer for folder
        folder.addUserRight(lecturer, AccessRight.getFull());
        folder = folderService.mergeContentFolder(folder);

        // there should be 2 exercises for lecturer and 0 for student
        Collection<Exercise> getExercisesForLecturerFromDB = exerciseService.getAllExercisesForUser(lecturer);
        assertEquals(Arrays.asList(exercise1, exercise2), getExercisesForLecturerFromDB);
        assertEquals(0, exerciseService.getAllExercisesForUser(student).size());

        // adding Student to folder
        folder.addUserRight(student, AccessRight.getFromFlags(READ));
        folder = folderService.mergeContentFolder(folder);

        // exercise list for lecturer and students should be equal
        assertEquals(getExercisesForLecturerFromDB, exerciseService.getAllExercisesForUser(student));
    }

    /**
     * Get all exercises for content folder list
     */
    @Test
    void getExerciseForFolderList() {

        // add exercise to folder
        Exercise exercise1 = addNewExercise("Exercise 1");
        Exercise exercise2 = addNewExercise("Exercise 2");

        // set rights for Lecturer for folder to get content folder for user
        folder.addUserRight(lecturer, AccessRight.getFull());
        folder = folderService.mergeContentFolder(folder);

        // 2 exercises should be found
        assertEquals(Arrays.asList(exercise1, exercise2), exerciseService
                .getAllExercisesForContentFolderList(folderService.getAllContentFoldersForUser(lecturer)));
    }

    /**
     * Get all exercises for content folder list whose subject corresponds to the given subject
     */
    @Test
    void getExerciseForFolderListBySubject() {
        // create a subject
        Subject subject1 = addNewSubject("Subject 1");
        Subject subject2 = addNewSubject("Subject 2");

        // add exercise to folder
        Exercise exercise1 = addNewExerciseWithSubject("Exercise 1", subject1);
        Exercise exercise2 = addNewExerciseWithSubject("Exercise 2", subject1);
        Exercise exercise3 = addNewExerciseWithSubject("Exercise 3", subject2);

        // set rights for Lecturer for folder to get content folder for user
        folder.addUserRight(lecturer, AccessRight.getFull());
        folder = folderService.mergeContentFolder(folder);

        // 2 exercises should be found
        assertEquals(Arrays.asList(exercise1, exercise2), exerciseService
                .getAllExercisesForContentFolderListBySubject(
                        folderService.getAllContentFoldersForUser(lecturer), subject1)
        );
    }

    /**
     * Get all exercises for content folder
     */
    @Test
    void getExercisesForFolder() {

        // add exercise to folder
        Exercise exercise1 = addNewExercise("Exercise 1");
        Exercise exercise2 = addNewExercise("Exercise 2");

        // set rights for Lecturer for folder to get content folder for user
        folder.addUserRight(lecturer, AccessRight.getFull());
        folder = folderService.mergeContentFolder(folder);

        // 2 exercises should be found
        assertEquals(Arrays.asList(exercise1, exercise2),
                exerciseService.getAllExercisesForContentFolderEagerly(folder));
    }

    /**
     * Tests if exercises are found by tags
     */
    @Test
    void getExerciseByTag() {

        Exercise exercise = new Exercise("Exercise", TestDataFactory.getDefaultLanguage());
        Tag tag = tagService.getOrCreateByName("blah");
        exercise.addTag(tag);

        folder.addChildExercise(exercise);
        exerciseService.persistExercise(exercise);
        folder = folderService.mergeContentFolder(folder);

        Collection<Exercise> exercisesFromTags = exerciseService.getAllExercisesForThisTag(tag);
        assertEquals(1, exercisesFromTags.size());
        assertTrue(exercisesFromTags.contains(exercise));
    }

}
