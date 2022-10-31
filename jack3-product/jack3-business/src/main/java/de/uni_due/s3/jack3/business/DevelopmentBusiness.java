package de.uni_due.s3.jack3.business;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.google.common.base.VerifyException;

import de.uni_due.s3.jack3.builders.ExerciseBuilder;
import de.uni_due.s3.jack3.business.exceptions.ActionNotAllowedException;
import de.uni_due.s3.jack3.business.microservices.placeholderutils.PlaceholderPatternProducer;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.enums.ECourseExercisesOrder;
import de.uni_due.s3.jack3.entities.enums.ECourseScoring;
import de.uni_due.s3.jack3.entities.enums.EFillInEditorType;
import de.uni_due.s3.jack3.entities.enums.EFormularEditorPalette;
import de.uni_due.s3.jack3.entities.providers.FolderExerciseProvider;
import de.uni_due.s3.jack3.entities.stagetypes.mc.MCStage;
import de.uni_due.s3.jack3.entities.tenant.AbstractExercise;
import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Course;
import de.uni_due.s3.jack3.entities.tenant.CourseOffer;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.ResultFeedbackMapping;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageTransition;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.services.BaseService;
import de.uni_due.s3.jack3.services.DevelopmentService;
import de.uni_due.s3.jack3.services.DevelopmentService.EDatabaseType;
import de.uni_due.s3.jack3.services.ExerciseService;
import de.uni_due.s3.jack3.services.FolderService;
import de.uni_due.s3.jack3.services.UserService;
import de.uni_due.s3.jack3.services.utils.RepeatStage;

@RequestScoped
@Transactional(value = Transactional.TxType.REQUIRED)
public class DevelopmentBusiness extends AbstractBusiness {

	private static final String MITTELWERT_BERECHNEN_XML = "/MittelwertBerechnenR.xml";
	private static final String UML_TEST_XML = "/UML+Test.xml";
	private static final String DEMOPREOJEKT_JAVA_XML = "/Demoprojekt+1+(Java).xml";
	private static final String ADD_NUMBERS_PYTHON_XML = "/Add+Numbers+(Python).xml";
	private static final String ESSIGSAEURE_XML = "/Essigsäure.xml";
	private static final String FEEDBACK_CORRECT = "Das ist korrekt.";
	private static final String FEEDBACK_NOT_CORRECT = "Das ist leider <u>nicht</u> korrekt.";
	private static final String RANDOM_INT_1_10 = "randomIntegerBetween(1,10)";
	private static final String RANDOM_INT_10_20 = "randomIntegerBetween(10,20)";

	public static final String FILLIN_FIELD_PREFIX = "fillInField";

	private static final String HTML_TABLE = "<table border=\"1\" cellpadding=\"0\" cellspacing=\"0\"style=\"height:120px;width:300px;\">";
	private static final String HTML_TABLE_E = "</table>";
	private static final String HTML_THEAD = "<thead>";
	private static final String HTML_THEAD_E = "</thead>";
	private static final String HTML_TBODY = "<tbody>";
	private static final String HTML_TBODY_E = "</tbody>";
	private static final String HTML_TR = "<tr>";
	private static final String HTML_TR_E = "</tr>";
	private static final String HTML_TD = "<td style=\"text-align: center;\">";
	private static final String HTML_TD_E = "</td>";

	@Inject
	private ExerciseService exerciseService;

	@Inject
	private FolderService folderService;

	@Inject
	private UserService userService;

	@Inject
	private DevelopmentService developmentService;

	@Inject
	private ExerciseBusiness exerciseBusiness;

	@Inject
	private FolderBusiness folderBusiness;

	@Inject
	private UserBusiness userBusiness;

	@Inject
	private CourseBusiness courseBusiness;

	@Inject
	private SerDeBusiness serDeBusiness;

	@Inject
	private BaseService baseService;

	/**
	 * Removes all entities from tables (Postgres database)
	 */
	public void deleteTenantDatabase() {
		developmentService.deleteTenantDatabase(EDatabaseType.POSTGRES);
		getLogger().info("Database deleted (Postgres)");
	}

	/**
	 * Removes all entities from tables (H2 database)
	 */
	public void deleteTenantDatabaseH2() {
		developmentService.deleteTenantDatabase(EDatabaseType.H2);
		getLogger().info("Database deleted (H2)");
	}

	/**
	 * Inserts a created exercise from an {@link ExerciseBuilder} to the content
	 * folder of the given user and persist/merge it to the database.
	 *
	 * @param exercise
	 *            that was retrieved from an {@link ExerciseBuilder}
	 * @param user
	 *            the user who should get the exercise
	 * @return Persisted exercise entity
	 */
	private Exercise insertExerciseIntoPersonalFolder(Exercise exercise, User user) {
		ContentFolder folder = folderService.getContentFolderById(user.getPersonalFolder().getId())
				.orElseThrow(VerifyException::new);
		folder.addChildExercise(exercise);
		folderService.mergeContentFolder(folder);
		exerciseService.persistExercise(exercise);
		return exercise;
	}

	/**
	 * Get the user for adding sample content.
	 *
	 * @return First occurency of a user called "Lecturer" or "lecturer", otherwise
	 *         empty Optional.
	 */
	public Optional<User> lookupLecturer() {
		return Optional.ofNullable(
				userService.getUserByName("Lecturer").orElse(userService.getUserByName("lecturer").orElse(null)));
	}

	/**
	 * Creates a sample exercise
	 */
	public AbstractExercise createSampleExercise(User author, int sampleExerciseIndex) {
		switch (sampleExerciseIndex) {
		case 1:
			return createSampleExerciseJACK(author);
		case 2:
			return createSampleExerciseEinfacheMengenlehre(author);
		case 3:
			return createSampleExerciseRechentabellen(author);
		case 4:
			return createSampleExerciseStaedteUndLaender(author);
		case 5:
			return importSampleExercise(MITTELWERT_BERECHNEN_XML, author);
		case 6:
			return importSampleExercise(ADD_NUMBERS_PYTHON_XML, author);
		case 7:
			return importSampleExercise(DEMOPREOJEKT_JAVA_XML, author);
		case 8:
			return importSampleExercise(UML_TEST_XML, author);
		case 9:
			return importSampleExercise(ESSIGSAEURE_XML, author);
		default:
			throw new IllegalArgumentException(
					"Sample exercise index should be between 1 and 9, was " + sampleExerciseIndex);
		}
	}

	/**
	 * Creates all sample exercises.
	 * 
	 * @param author
	 *            Author of the exercises
	 */
	public void createSampleExercises(User author) {
		createSampleExerciseJACK(author);
		createSampleExerciseEinfacheMengenlehre(author);
		createSampleExerciseRechentabellen(author);
		createSampleExerciseStaedteUndLaender(author);
		importSampleExercise(MITTELWERT_BERECHNEN_XML, author);
		importSampleExercise(ADD_NUMBERS_PYTHON_XML, author);
		importSampleExercise(DEMOPREOJEKT_JAVA_XML, author);
		importSampleExercise(UML_TEST_XML, author);
		importSampleExercise(ESSIGSAEURE_XML, author);
	}

	/**
	 * Creates the Exercise "JACK" (originally from Alpha-Server):
	 *
	 * <ul>
	 * <li>Two random integers</li>
	 * <li>MCStage: Was ist JACK? - Ein E-Assessment-System, eine Raumstation, ein
	 * Bier", randomized and single-choice.</li>
	 * <li>FillInStage: Addition of the two variables with a hin</li>
	 * <li>Feedbacks for correct and wrong answers</li>
	 * </ul>
	 * 
	 * @return Persisted exercise
	 */
	public Exercise createSampleExerciseJACK(User author) {

		String varX = PlaceholderPatternProducer.forExerciseVariable("x");
		String varY = PlaceholderPatternProducer.forExerciseVariable("y");

		String input1 = getInputFillInExpression(1);

		Exercise exercise = new ExerciseBuilder("JACK") //
				.withPublicDescription("Original JACK-Beispielaufgabe.") //
				.withVariableDeclaration("x", RANDOM_INT_1_10) //
				.withVariableDeclaration("y", RANDOM_INT_10_20) //
				.withDifficulty(10) //
				.withTag("JACK") //
				.withTag("Original") //
				.withTag("rechnen") //
				.withTag("Wissen")

				.withMCStage() //
				.withDescription("Was ist JACK?") //
				.withAnswerOption("Ein E-Assessment-System", true) //
				.withAnswerOption("Eine Raumstation", false) //
				.withAnswerOption("Ein Bier", false) //

				.withCorrectFeedback(FEEDBACK_CORRECT) //
				.withDefaultFeedback(FEEDBACK_NOT_CORRECT, 0) //
				.withRandomizedAnswerOrder().selectOne().and() //

				.withFillInStage() //

				.withDescription() //
				.append("Berechnen Sie " + varX + " + " + varY) //
				.appendFillInField(EFillInEditorType.NONE) //
				.and() //

				.withFeedbackRule(createFeedbackTitle(1), input1 + "=" + varX + "+" + varY, FEEDBACK_CORRECT, 100,
						false) //
				.withFeedbackRule(createFeedbackTitle(2), input1 + "<" + varX + "+" + varY, "Das ist zu wenig.", 0,
						false) //
				.withFeedbackRule(createFeedbackTitle(3), input1 + ">" + varX + "+" + varY, "Das ist zu viel.", 0,
						false) //
				.withHint("Das Ergebnis ist zweistellig.") //

				.and().create();

		return insertExerciseIntoPersonalFolder(exercise, Objects.requireNonNull(author));
	}

	private static String getInputFillInExpression(int i) {
		return PlaceholderPatternProducer.forInputVariable(FILLIN_FIELD_PREFIX + i);
	}

	/**
	 * Creates the Exercise "Einfache Mengenlehre"
	 *
	 * <ul>
	 * <li>Random integer</li>
	 * <li>MCStage: Leere Menge</li>
	 * <li>FillInStage: Mächtigkeit Potenzmenge</li>
	 * <li>FillInStage: Potenzmenge Formeleingabe</li>
	 * <li>Feedbacks for correct and wrong answers</li>
	 * </ul>
	 * 
	 * @return Persisted exercise
	 */
	public Exercise createSampleExerciseEinfacheMengenlehre(User author) {
		String varA = PlaceholderPatternProducer.forExerciseVariable("a");
		String input1 = getInputFillInExpression(1);

		Exercise exercise = new ExerciseBuilder("Einfache Mengenlehre") //
				.withPublicDescription("Einfache Aufgaben zur Mengenlehre.") //
				.withVariableDeclaration("a", "randomIntegerBetween(1,4)") //
				.withDifficulty(40) //
				.withTag("Mengen") //
				.withTag("Leere Menge") //
				.withTag("Potenzmenge") //
				.withTag("LaTeX")

				.withMCStage() //
				.withDescription("$A:=\\emptyset$. A bezeichnet man als") //
				.withAnswerOption("Leere Menge", true) //
				.withAnswerOption("Volle Menge", false) //
				.withAnswerOption("Menge", true) //

				.withCorrectFeedback(FEEDBACK_CORRECT) //
				.withDefaultFeedback(FEEDBACK_NOT_CORRECT + " Richtig wäre: 'Leere Menge' und 'Menge'.", 0) //
				.withRandomizedAnswerOrder().and() //

				.withFillInStage() //
				.withFormularEditorPalette(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_NO_PALETTE) //
				.withDescription() //
				.append("Wir betrachten eine " + varA + "-elementige Menge. Die Mächtigkeit der Potenzmenge ist ") //
				.appendFillInField(EFillInEditorType.MATHDOX_FORMULAR_EDITOR) //
				.appendLine(".") //
				.append("Vereinfachen Sie die Zahl soweit wie möglich.") //
				.and() //

				.withFeedbackRule(createFeedbackTitle(1), input1 + "!=(2 ^ " + varA + ")", FEEDBACK_NOT_CORRECT, 0,
						false) //
				.withFeedbackRule(createFeedbackTitle(2),
						input1 + "==(2 ^ " + varA + ")&&!(isIntegerNumber(" + input1 + "))",
						"Das ist zwar korrekt, aber die Zahl könnte noch weiter vereinfacht werden.", 80, false) //
				.withFeedbackRule(createFeedbackTitle(3),
						input1 + "==(2 ^ " + varA + ")&&isIntegerNumber(" + input1 + ")", FEEDBACK_CORRECT, 100, false) //
				.withHint("Bei einer Menge mit n Elementen hat die Potenzmenge $2^n$ Elemente.") //
				.and() //

				.withFillInStage() //
				.withFormularEditorPalette(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_NO_PALETTE) //
				.withDescription() //
				.appendLine("$B:=\\left \\{ 1, 2 \\right \\}$") //
				.append("$\\mathcal{P}\\left ( B \\right )=$") //
				.appendFillInField(EFillInEditorType.MATHDOX_FORMULAR_EDITOR) //
				.appendLine() //
				// no formular-editor symbol
				.append("<small><i>Tipp: Anstelle des Symbols ∅ können auch leere Mengenklammern genutzt werden.</small></i>") //
				.and() //

				.withFeedbackRule(createFeedbackTitle(1),
						"equalSet(" + input1 + ", list(list(),list(1),list(2),list(1,2)))", FEEDBACK_CORRECT, 100,
						false) //
				.withFeedbackRule(createFeedbackTitle(2),
						"!equalSet(" + input1 + ", list(list(),list(1),list(2),list(1,2)))", FEEDBACK_NOT_CORRECT, 0,
						false) //

				.and().create();

		return insertExerciseIntoPersonalFolder(exercise, Objects.requireNonNull(author));
	}

	/**
	 * Creates the Exercise "Rechentabellen"
	 *
	 * <ul>
	 * <li>Random integers</li>
	 * <li>FillInStage: Multiplication</li>
	 * <li>FillInStage: Addition</li>
	 * <li>FillInStage: Modulo Addition</li>
	 * </ul>
	 * 
	 * @return Persisted exercise
	 */
	public Exercise createSampleExerciseRechentabellen(User author) {
		String varA1 = PlaceholderPatternProducer.forExerciseVariable("a1");
		String varA2 = PlaceholderPatternProducer.forExerciseVariable("a2");
		String varB1 = PlaceholderPatternProducer.forExerciseVariable("b1");
		String varB2 = PlaceholderPatternProducer.forExerciseVariable("b2");

		String input1 = getInputFillInExpression(1);
		String input2 = getInputFillInExpression(2);
		String input3 = getInputFillInExpression(3);
		String input4 = getInputFillInExpression(4);
		String input5 = getInputFillInExpression(5);
		String input6 = getInputFillInExpression(6);
		String input7 = getInputFillInExpression(7);
		String input8 = getInputFillInExpression(8);
		String input9 = getInputFillInExpression(9);

		Exercise exercise = new ExerciseBuilder("Rechentabellen") //
				.withPublicDescription("Diese Aufgabe beinhaltet Multiplikations- und Additions-Tabellen.") //
				.withVariableDeclaration("a1", RANDOM_INT_1_10) //
				.withVariableDeclaration("a2", RANDOM_INT_1_10) //
				.withVariableDeclaration("b1", RANDOM_INT_1_10) //
				.withVariableDeclaration("b2", RANDOM_INT_1_10) //
				.withDifficulty(70) //
				.withTag("Multiplikation") //
				.withTag("Addition") //
				.withTag("rechnen") //
				.withTag("Tabelle")

				/*
				 * ######################### FillInStage 1: Multiplication
				 * #########################
				 */

				.withFillInStage() //
				.withTitle("Multiplikation") //
				.withFormularEditorPalette(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_NO_PALETTE) //
				.withDescription() //
				.appendLine("Multipliziere jeweils die eine Zahl mit der anderen Zahl.") //
				.append(HTML_TABLE) //
				.append(HTML_THEAD) //
				.append(HTML_TR) //
				.append("            <th scope=\"row\" style=\"text-align: center;\">$\\cdot$</th>") //
				.append("            <th scope=\"col\" style=\"text-align: center;\">" + varB1 + "</th>") //
				.append("            <th scope=\"col\" style=\"text-align: center;\">" + varB2 + "</th>") //
				.append(HTML_TR_E) //
				.append(HTML_THEAD_E) //
				.append(HTML_TBODY) //

				.append(HTML_TR) //
				.append("            <th scope=\"row\" style=\"text-align: center;\">" + varA1 + "</th>") //
				.append(HTML_TD) //
				.appendFillInField(EFillInEditorType.NUMBER, 2) //
				.append(HTML_TD_E) //
				.append(HTML_TD) //
				.appendFillInField(EFillInEditorType.NUMBER, 2) //
				.append(HTML_TD_E) //
				.append(HTML_TR_E) //

				.append(HTML_TR) //
				.append("            <th scope=\"row\" style=\"text-align: center;\">" + varA2 + "</th>") //
				.append(HTML_TD) //
				.appendFillInField(EFillInEditorType.NUMBER, 2) //
				.append(HTML_TD_E) //
				.append(HTML_TD) //
				.appendFillInField(EFillInEditorType.NUMBER, 2) //
				.append(HTML_TD_E) //
				.append(HTML_TR_E) //

				.append(HTML_TBODY_E) //
				.appendLine(HTML_TABLE_E) //
				.append("Schreibe die Zahl in die Felder.") //
				.and() //

				.withFeedbackRule(createFeedbackName(1, true), "equalsExpr(" + input1 + "," + varB1 + "*" + varA1 + ")",
						createFeedbackText(1, true), 25, false) //
				.withFeedbackRule(createFeedbackName(1, false),
						"!equalsExpr(" + input1 + "," + varB1 + "*" + varA1 + ")", createFeedbackText(1, false), 0,
						false) //
				.withFeedbackRule(createFeedbackName(2, true), "equalsExpr(" + input2 + "," + varB2 + "*" + varA1 + ")",
						createFeedbackText(2, true), 25, false) //
				.withFeedbackRule(createFeedbackName(2, false),
						"!equalsExpr(" + input2 + "," + varB2 + "*" + varA1 + ")", createFeedbackText(2, false), 0,
						false) //
				.withFeedbackRule(createFeedbackName(3, true), "equalsExpr(" + input3 + "," + varB1 + "*" + varA2 + ")",
						createFeedbackText(3, true), 25, false) //
				.withFeedbackRule(createFeedbackName(3, false),
						"!equalsExpr(" + input3 + "," + varB1 + "*" + varA2 + ")", createFeedbackText(3, false), 0,
						false) //
				.withFeedbackRule(createFeedbackName(4, true), "equalsExpr(" + input4 + "," + varB2 + "*" + varA2 + ")",
						createFeedbackText(4, true), 25, false) //
				.withFeedbackRule(createFeedbackName(4, false),
						"!equalsExpr(" + input4 + "," + varB2 + "*" + varA2 + ")", createFeedbackText(4, false), 0,
						false) //
				.and() //

				/*
				 * ######################### FillInStage 2: Addition #########################
				 */

				.withFillInStage() //
				.withTitle("Addition") //
				.withFormularEditorPalette(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_NO_PALETTE) //
				.withDescription() //
				.appendLine("Addiere jeweils die eine Zahl mit der anderen Zahl.") //
				.append(HTML_TABLE) //
				.append(HTML_THEAD) //
				.append(HTML_TR) //
				.append("            <th scope=\"row\" style=\"text-align: center;\">$+$</th>") //
				.append("            <th scope=\"col\" style=\"text-align: center;\">" + varB1 + "</th>") //
				.append("            <th scope=\"col\" style=\"text-align: center;\">" + varB2 + "</th>") //
				.append(HTML_TR_E) //
				.append(HTML_THEAD_E) //
				.append(HTML_TBODY) //

				.append(HTML_TR) //
				.append("            <th scope=\"row\" style=\"text-align: center;\">" + varA1 + "</th>") //
				.append(HTML_TD) //
				.appendFillInField(EFillInEditorType.NUMBER, 2) //
				.append(HTML_TD_E) //
				.append(HTML_TD) //
				.appendFillInField(EFillInEditorType.NUMBER, 2) //
				.append(HTML_TD_E) //
				.append(HTML_TR_E) //

				.append(HTML_TR) //
				.append("            <th scope=\"row\" style=\"text-align: center;\">" + varA2 + "</th>") //
				.append(HTML_TD) //
				.appendFillInField(EFillInEditorType.NUMBER, 2) //
				.append(HTML_TD_E) //
				.append(HTML_TD) //
				.appendFillInField(EFillInEditorType.NUMBER, 2) //
				.append(HTML_TD_E) //
				.append(HTML_TR_E) //

				.append(HTML_TBODY_E) //
				.appendLine(HTML_TABLE_E) //
				.append("Schreibe die Zahl in die Felder.") //
				.and() //

				.withFeedbackRule(createFeedbackName(1, true), "equalsExpr(" + input1 + "," + varB1 + "+" + varA1 + ")",
						createFeedbackText(1, true), 25, false) //
				.withFeedbackRule(createFeedbackName(1, false),
						"!equalsExpr(" + input1 + "," + varB1 + "+" + varA1 + ")", createFeedbackText(1, false), 0,
						false) //
				.withFeedbackRule(createFeedbackName(2, true), "equalsExpr(" + input2 + "," + varB2 + "+" + varA1 + ")",
						createFeedbackText(2, true), 25, false) //
				.withFeedbackRule(createFeedbackName(2, false),
						"!equalsExpr(" + input2 + "," + varB2 + "+" + varA1 + ")", createFeedbackText(2, false), 0,
						false) //
				.withFeedbackRule(createFeedbackName(3, true), "equalsExpr(" + input3 + "," + varB1 + "+" + varA2 + ")",
						createFeedbackText(3, true), 25, false) //
				.withFeedbackRule(createFeedbackName(3, false),
						"!equalsExpr(" + input3 + "," + varB1 + "+" + varA2 + ")", createFeedbackText(3, false), 0,
						false) //
				.withFeedbackRule(createFeedbackName(4, true), "equalsExpr(" + input4 + "," + varB2 + "+" + varA2 + ")",
						createFeedbackText(4, true), 25, false) //
				.withFeedbackRule(createFeedbackName(4, false),
						"!equalsExpr(" + input4 + "," + varB2 + "+" + varA2 + ")", createFeedbackText(4, false), 0,
						false) //

				.and() //

				/*
				 * ######################### FillInStage 3: Addition modulo table
				 * #########################
				 */

				.withFillInStage() //
				.withTitle("Addition in Restklassen") //
				.withFormularEditorPalette(EFormularEditorPalette.MATHDOX_FORMULAR_EDITOR_NO_PALETTE) //
				.withDescription() //
				.appendLine("Addiere jeweils die eine Zahl mit der anderen Zahl in $\\mathbb{Z}_{3}$.") //
				.append(HTML_TABLE) //
				.append(HTML_THEAD) //
				.append(HTML_TR) //
				.append("            <th scope=\"row\" style=\"text-align: center;\">$\\oplus$</th>") //
				.append("            <th scope=\"col\" style=\"text-align: center;\">0</th>") //
				.append("            <th scope=\"col\" style=\"text-align: center;\">1</th>") //
				.append("            <th scope=\"col\" style=\"text-align: center;\">2</th>") //
				.append(HTML_TR_E) //
				.append(HTML_THEAD_E) //
				.append(HTML_TBODY) //

				.append(HTML_TR) //
				.append("            <th scope=\"row\" style=\"text-align: center;\">0</th>") //
				.append(HTML_TD) //
				.appendFillInField(EFillInEditorType.NUMBER, 1) //
				.append(HTML_TD_E) //
				.append(HTML_TD) //
				.appendFillInField(EFillInEditorType.NUMBER, 1) //
				.append(HTML_TD_E) //
				.append(HTML_TD) //
				.appendFillInField(EFillInEditorType.NUMBER, 1) //
				.append(HTML_TD_E) //
				.append(HTML_TR_E) //

				.append(HTML_TR) //
				.append("            <th scope=\"row\" style=\"text-align: center;\">1</th>") //
				.append(HTML_TD) //
				.appendFillInField(EFillInEditorType.NUMBER, 1) //
				.append(HTML_TD_E) //
				.append(HTML_TD) //
				.appendFillInField(EFillInEditorType.NUMBER, 1) //
				.append(HTML_TD_E) //
				.append(HTML_TD) //
				.appendFillInField(EFillInEditorType.NUMBER, 1) //
				.append(HTML_TD_E) //
				.append(HTML_TR_E) //

				.append(HTML_TR) //
				.append("            <th scope=\"row\" style=\"text-align: center;\">2</th>") //
				.append(HTML_TD) //
				.appendFillInField(EFillInEditorType.NUMBER, 1) //
				.append(HTML_TD_E) //
				.append(HTML_TD) //
				.appendFillInField(EFillInEditorType.NUMBER, 1) //
				.append(HTML_TD_E) //
				.append(HTML_TD) //
				.appendFillInField(EFillInEditorType.NUMBER, 1) //
				.append(HTML_TD_E) //
				.append(HTML_TR_E) //

				.append(HTML_TBODY_E) //
				.appendLine(HTML_TABLE_E) //
				.append("Schreibe die Zahl in die Felder. Pro richtiges Feld gibt es einen Punkt.") //
				.and() //

				.withFeedbackRule(createFeedbackName(1, true), input1 + "==0", createFeedbackText(1, true), 11, false) //
				.withFeedbackRule(createFeedbackName(1, false), "!(" + input1 + "==0)", createFeedbackText(1, false), 0,
						false) //
				.withFeedbackRule(createFeedbackName(2, true), "" + input2 + "==1", createFeedbackText(2, true), 11,
						false) //
				.withFeedbackRule(createFeedbackName(2, false), "!(" + input2 + "==1)", createFeedbackText(2, false), 0,
						false) //
				.withFeedbackRule(createFeedbackName(3, true), "" + input3 + "==2", createFeedbackText(3, true), 11,
						false) //
				.withFeedbackRule(createFeedbackName(3, false), "!(" + input3 + "==2)", createFeedbackText(3, false), 0,
						false) //
				.withFeedbackRule(createFeedbackName(4, true), "" + input4 + "==1", createFeedbackText(4, true), 11,
						false) //
				.withFeedbackRule(createFeedbackName(4, false), "!(" + input4 + "==1)", createFeedbackText(4, false), 0,
						false) //
				.withFeedbackRule(createFeedbackName(5, true), "" + input5 + "==2", createFeedbackText(5, true), 11,
						false) //
				.withFeedbackRule(createFeedbackName(5, false), "!(" + input5 + "==2)", createFeedbackText(5, false), 0,
						false) //
				.withFeedbackRule(createFeedbackName(6, true), "" + input6 + "==0", createFeedbackText(6, true), 11,
						false) //
				.withFeedbackRule(createFeedbackName(6, false), "!(" + input6 + "==0)", createFeedbackText(6, false), 0,
						false) //
				.withFeedbackRule(createFeedbackName(7, true), "" + input7 + "==2", createFeedbackText(7, true), 11,
						false) //
				.withFeedbackRule(createFeedbackName(7, false), "!(" + input7 + "==2)", createFeedbackText(7, false), 0,
						false) //
				.withFeedbackRule(createFeedbackName(8, true), "" + input8 + "==0", createFeedbackText(8, true), 11,
						false) //
				.withFeedbackRule(createFeedbackName(8, false), "!(" + input8 + "==0)", createFeedbackText(8, false), 0,
						false) //
				.withFeedbackRule(createFeedbackName(9, true), "" + input9 + "==1", createFeedbackText(9, true), 12,
						false) //
				.withFeedbackRule(createFeedbackName(9, false), "!(" + input9 + "==1)", createFeedbackText(9, false), 0,
						false) //

				.and().create();

		return insertExerciseIntoPersonalFolder(exercise, Objects.requireNonNull(author));
	}

	/**
	 * Creates the Exercise "St&auml;dte &amp; L&auml;nder"
	 *
	 * <ul>
	 * <li>MCStage: User selects one German state and answers some questions</li>
	 * </ul>
	 * 
	 * @return Persisted exercise
	 */
	public Exercise createSampleExerciseStaedteUndLaender(User author) {
		String mc0 = PlaceholderPatternProducer.forMcInputVariable(0);
		String mc1 = PlaceholderPatternProducer.forMcInputVariable(1);
		String mc2 = PlaceholderPatternProducer.forMcInputVariable(2);

		Exercise exercise = new ExerciseBuilder("Städte & Länder") //
				.withPublicDescription(
						"Diese Aufgabe testet interaktiv das geographische Wissen über deutsche Städte und Bundesländer.") //
				.withDifficulty(30) //
				.withTag("Städte") //
				.withTag("Länder") //
				.withTag("Wissen")
				// Selection of the state

				.withMCStage().selectOne().withTitle("Auswahl").withRandomizedAnswerOrder().withWeight(0) //
				.withDescription("Bitte wählen Sie das gewünschte Bundesland aus.") //
				.withAnswerOption("Nordrhein-Westfalen", true) //
				.withAnswerOption("Bayern", true) //
				.withAnswerOption("Bremen", true) //
				.and() //

				// NRW

				.withMCStage().withTitle("Nordrhein-Westfalen").withRandomizedAnswerOrder() //
				.withDescription("Wählen Sie alle Städte aus, die in Nordrhein-Westfalen liegen.") //
				.withAnswerOption("Essen", true) //
				.withAnswerOption("Dortmund", true) //
				.withAnswerOption("Wolfsburg", false) //
				.withAnswerOption("Stralsund", false) //
				.and()

				// Head of NRW
				.withMCStage().withTitle("Landeshauptstadt").withRandomizedAnswerOrder().selectOne() //
				.withDescription("Was ist die Landeshauptstadt von Nordrhein-Westfalen?") //
				.withAnswerOption("Köln", false) //
				.withAnswerOption("Düsseldorf", true) //
				.withAnswerOption("Gelsenkirchen", false) //
				.withAnswerOption("Aachen", false) //
				.and()

				// Bayern

				.withMCStage().withTitle("Bayern").withRandomizedAnswerOrder() //
				.withDescription("Wählen Sie alle Städte aus, die in Bayern liegen.") //
				.withAnswerOption("Nürnberg", true) //
				.withAnswerOption("Würzburg", true) //
				.withAnswerOption("Kiel", false) //
				.withAnswerOption("Hamburg", false) //
				.and()

				// Head of Bayern
				.withMCStage().withTitle("Landeshauptstadt").withRandomizedAnswerOrder().selectOne() //
				.withDescription("Was ist die Landeshauptstadt von Bayern?") //
				.withAnswerOption("Lindau", false) //
				.withAnswerOption("München", true) //
				.withAnswerOption("Bamberg", false) //
				.withAnswerOption("Regensburg", false) //
				.and()

				// Bremen

				.withMCStage().withTitle("Bremen").withRandomizedAnswerOrder() //
				.withDescription("Wählen Sie alle Städte aus, die in Bremen liegen.") //
				.withAnswerOption("Bremen", true) //
				.withAnswerOption("Bremerhaven", true) //
				.withAnswerOption("Bremervörde", false) //
				.withAnswerOption("Paderborn", false) //
				.and()

				// Head of Bremen
				.withMCStage().withTitle("Landeshauptstadt").withRandomizedAnswerOrder().selectOne() //
				.withDescription("Was ist die Landeshauptstadt von Bremen?") //
				.withAnswerOption("Bremerhaven", false) //
				.withAnswerOption("Bremen", true) //
				.withAnswerOption("Oldenburg", false) //
				.withAnswerOption("Minden", false) //
				.and()

				.create();

		// After creating the exercise, the transitions must be changed

		/*-
		 * Order of the stages:
		 * #0 Selection
		 * #1 NRW
		 * #2 Head of NRW
		 * #3 Bavaria
		 * #4 Head of Bavaria
		 * #5 Bremen
		 * #6 Head of Bremen
		 */
		List<Stage> stageList = exercise.getStagesAsList();

		stageList.get(0).setInternalName("#1 Auswahl");
		stageList.get(1).setInternalName("#2 NRW");
		stageList.get(2).setInternalName("#3 NRW Hauptstadt");
		stageList.get(3).setInternalName("#4 Bayern");
		stageList.get(4).setInternalName("#5 Bayern Hauptstadt");
		stageList.get(5).setInternalName("#6 Bremen");
		stageList.get(6).setInternalName("#7 Bremen Hauptstadt");

		// Default transition
		stageList.get(0).setDefaultTransition(new StageTransition(new RepeatStage())); // no selection --> repeat

		// Feedback for no selection
		((MCStage) (stageList.get(0)))
				.addFeedbackOption(new EvaluatorExpression("!" + mc0 + "&&!" + mc1 + "&&!" + mc2));
		((MCStage) (stageList.get(0))).getExtraFeedbacks().get(0).setFeedbackText("Es wurde keine Antwort ausgewählt.");

		// Transition for NRW
		StageTransition transition = new StageTransition(stageList.get(1));
		transition.setStageExpression(new EvaluatorExpression(mc0 + "&&true()&&true()"));
		stageList.get(0).addStageTransition(transition);

		// Transition for Bavaria
		transition = new StageTransition(stageList.get(3));
		transition.setStageExpression(new EvaluatorExpression("true()&&" + mc1 + "&&true()"));
		stageList.get(0).addStageTransition(transition);

		// Transition for Bremen
		transition = new StageTransition(stageList.get(5));
		transition.setStageExpression(new EvaluatorExpression("true()&&true()&&" + mc2));
		stageList.get(0).addStageTransition(transition);

		// Transition for ending NRW
		stageList.get(2).setDefaultTransition(new StageTransition());

		// Transition for ending Bavaria
		stageList.get(4).setDefaultTransition(new StageTransition());

		exercise.generateSuffixWeights();

		return insertExerciseIntoPersonalFolder(exercise, Objects.requireNonNull(author));
	}

	/**
	 * <ol>
	 * <li>Creates user "lecturer", "student", "testuser1-9" and
	 * "testlecturer1-9"</li>
	 * <li>Creates all sample exercises in the directory "lecturer's personal folder
	 * / Beispielaufgaben"</li>
	 * <li>Creates a course with the sample exercises</li>
	 * <li>Creates a course offer with the sample course with a presentation folder
	 * for "lecturer"</li>
	 * </ol>
	 * 
	 * @throws ActionNotAllowedException
	 */
	public void setupReadyToPlayEnvironment() throws ActionNotAllowedException {

		// 1. Create users
		User lecturer = getOrCreateUser("lecturer", false, true);
		getOrCreateUser("student", false, false);

		for (int i = 1; i <= 9; i++) {
			getOrCreateUser("testuser" + i, false, false);
		}

		UserGroup testLecturers = getOrCreateUserGroup("Testlecturers", "Test-Accounts");
		for (int i = 1; i <= 9; i++) {
			User user = getOrCreateUser("testlecturer" + i, false, true);
			userBusiness.addUserToUserGroup(user, testLecturers);
		}

		// 2. Create sample exercises
		if (lecturer.getPersonalFolder() == null) {
			getLogger().warn("Environment could not be set up: User 'lecturer' does not have a personal folder!");
			return;
		}

		ContentFolder sampleFolder = folderBusiness.createContentFolder(lecturer, "Beispielaufgaben",
				lecturer.getPersonalFolder());

		Exercise sampleExerciseMittelwertBerechnenR = importSampleExercise(MITTELWERT_BERECHNEN_XML, lecturer);
		Exercise sampleExercisePython = importSampleExercise(ADD_NUMBERS_PYTHON_XML, lecturer);
		Exercise sampleExerciseJava = importSampleExercise(DEMOPREOJEKT_JAVA_XML, lecturer);
		Exercise sampleExerciseUML = importSampleExercise(UML_TEST_XML, lecturer);
		Exercise sampleExericseMolecule = importSampleExercise(ESSIGSAEURE_XML, lecturer);

		List<Exercise> exercises = Arrays.asList(createSampleExerciseJACK(lecturer),
				createSampleExerciseEinfacheMengenlehre(lecturer), createSampleExerciseRechentabellen(lecturer),
				createSampleExerciseStaedteUndLaender(lecturer), sampleExerciseMittelwertBerechnenR, sampleExercisePython, sampleExerciseJava, sampleExerciseUML, sampleExericseMolecule);

		exercises.stream().forEach(exercise -> {
			try {
				exerciseBusiness.moveExercise(exercise, sampleFolder, lecturer);
			} catch (ActionNotAllowedException e) {
				throw new IllegalStateException("Moving a fresh-created sample exercise was not allowed,"
						+ " although the exercise was created in the lecturer's personal folder.", e);
			}
		});

		// 3. Create a course with the sample exercises (folder-based)
		Course course = courseBusiness.createCourse("Beispielkurs", lecturer, sampleFolder);
		FolderExerciseProvider fep = new FolderExerciseProvider();
		fep.addFolder(sampleFolder);
		course.setContentProvider(fep);
		course.setExerciseOrder(ECourseExercisesOrder.ALPHABETIC_ASCENDING);
		course.setExternalDescription("Dieser Kurs ist ein Beispielkurs und enthält einige Beispiel-Aufgaben.");
		course.setScoringMode(ECourseScoring.LAST);
		course.addResultFeedbackMapping(new ResultFeedbackMapping("[meta=currentResult]==100", "Perfekt!",
				"Das Ergebnis könnte nicht besser sein."));
		course.addResultFeedbackMapping(
				new ResultFeedbackMapping("[meta=currentResult]==0", "0 Punkte", "Es wurden keine Punkte erreicht."));
		course = (Course) courseBusiness.updateCourse(course);

		// 4. Create presentation folder

		PresentationFolder offerFolder = folderBusiness.createPresentationFolder("Kursangebote",
				folderBusiness.getPresentationRoot());
		folderBusiness.updateFolderRightsForUser(offerFolder, lecturer, AccessRight.getFull());
		folderBusiness.updateFolderRightsForUserGroup(offerFolder, testLecturers, AccessRight.getFull());

		// 5. Create a course offer with the course (blocklist only mode)

		CourseOffer offer = courseBusiness.createCourseOffer("Beispielaufgaben", course, offerFolder, lecturer);
		offer.setCourse(course);
		offer.setExplicitEnrollment(false);
		offer.setExplicitSubmission(false);
		offer.setCanBeVisible(true);
		courseBusiness.updateCourseOffer(offer);

	}

	@Nonnull
	protected Exercise importSampleExercise(String resourcePathToExerciseAsXML, User user) {
		// We use an exported sample exercise
		final ClassLoader classLoader = this.getClass().getClassLoader();
		final InputStream input = classLoader.getResourceAsStream(resourcePathToExerciseAsXML);
		final InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
		final String xml = new BufferedReader(reader).lines().collect(Collectors.joining());
		Exercise importedExercise = serDeBusiness.toExerciseFromXML(xml);
		return insertExerciseIntoPersonalFolder(importedExercise, user);
	}

	/**
	 * Returns a user with the specified name. If no user exists, a new user is
	 * created. If the user already exists, the user's password and rights are not
	 * changed.
	 * 
	 * @param loginName
	 *            Login name of the user
	 * @param hasAdminRights
	 *            If the newly-created user has admin rights
	 * @param hasEditRights
	 *            If the newly-created user has edit rights.
	 * @return User from database
	 */
	public User getOrCreateUser(String loginName, boolean hasAdminRights, boolean hasEditRights) {
		return userBusiness.getUserByName(loginName)
				.orElseGet(() -> userBusiness.createUser(loginName, "secret", null, hasAdminRights, hasEditRights));
	}

	/**
	 * Returns a user with the specified name. If no user exists, a new user is
	 * created. If the user already exists, the user's password and rights are not
	 * changed.
	 * 
	 * @param loginName
	 *            Login name of the user
	 * @param plainPassword
	 *            Password of the newly-created user.
	 * @param hasAdminRights
	 *            If the newly-created user has admin rights
	 * @param hasEditRights
	 *            If the newly-created user has edit rights.
	 * @return User from database
	 */
	public User getOrCreateUser(String loginName, String plainPassword, boolean hasAdminRights, boolean hasEditRights) {
		return userBusiness.getUserByName(loginName).orElseGet(
				() -> userBusiness.createUser(loginName, plainPassword, null, hasAdminRights, hasEditRights));
	}

	public UserGroup getOrCreateUserGroup(String name, String description) {
		return userBusiness.getUserGroup(name).orElseGet(() -> userBusiness.createUserGroup(name, description));
	}

	public int getSampleExerciseCount() {
		// This number should be increased when new sample exercises are created
		return 4;
	}

	private String createFeedbackTitle(int feedbackNumber) {
		return "Feedback " + feedbackNumber;
	}

	private String createFeedbackName(int fieldNumber, boolean correct) {
		return String.format("Feld %s %s", fieldNumber, rightWrongText(correct));
	}

	private String createFeedbackText(int fieldNumber, boolean correct) {
		return String.format("Das %s Feld ist %s.", ordinalNumber(fieldNumber), rightWrongText(correct));
	}

	private String rightWrongText(boolean correct) {
		return correct ? "richtig" : "falsch";
	}

	private String ordinalNumber(int number) {
		switch (number) {
		case 1:
			return "erste";
		case 2:
			return "zweite";
		case 3:
			return "dritte";
		case 4:
			return "vierte";
		case 5:
			return "fünfte";
		case 6:
			return "sechste";
		case 7:
			return "siebte";
		case 8:
			return "achte";
		case 9:
			return "neunte";
		case 10:
			return "zehnte";
		case 11:
			return "elfte";
		case 12:
			return "zwölfte";
		default:
			return number + ".";
		}
	}

	/*
	 * For debugging/testing only
	 */
	public void testMethod() {
		developmentService.testMethod();
	}

}
