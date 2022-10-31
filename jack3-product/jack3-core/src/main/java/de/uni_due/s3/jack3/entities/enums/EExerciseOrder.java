package de.uni_due.s3.jack3.entities.enums;

// REVIEW lg - Diese Klasse ist mE obsolet, "ECourseExercisesOrder" enthält die gleichen Einträge
// Genutzt wird diese nur in "UserExerciseFilter", siehe Kommentar dort
// Die ECourseExercisesOrder-Klasse ist ein Bestandteil von https://s3gitlab.paluno.uni-due.de/JACK/jack3-core/-/merge_requests/116 gewesen
/*
 * TODO: Discussion: Is a descending sorting for names needed?
 */
public enum EExerciseOrder {

	NONE,
	NAME,
	DIFFICULTY_ASC,
	DIFFICULTY_DESC;
}
