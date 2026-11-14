package com.tkasmp2027

import com.tkasmp2027.engine.QuizScoringEngine
import com.tkasmp2027.model.AnswerKey
import com.tkasmp2027.model.QuestionGradeStatus
import com.tkasmp2027.model.QuestionItem
import com.tkasmp2027.model.QuestionOption
import com.tkasmp2027.model.QuestionPackage
import com.tkasmp2027.model.QuestionStatement
import com.tkasmp2027.model.QuestionStatementChoice
import com.tkasmp2027.model.QuestionType
import com.tkasmp2027.model.SubjectId
import com.tkasmp2027.model.UserAnswer
import org.junit.Assert.assertEquals
import org.junit.Test

class QuizScoringEngineTest {

    @Test
    fun evaluatesMixedQuestionTypesAccurately() {
        val pkg = QuestionPackage(
            packageId = "matematika_paket_1",
            subjectId = SubjectId.MATEMATIKA,
            subjectName = "Matematika",
            title = "Paket 1",
            description = "Uji",
            expectedQuestionCount = 3,
            schemaVersion = "1.0",
            questions = listOf(
                QuestionItem(
                    id = "Q1",
                    number = 1,
                    type = QuestionType.PG,
                    questionHtml = "<p>PG</p>",
                    options = listOf(
                        QuestionOption("A", "A", "1"),
                        QuestionOption("B", "B", "2")
                    ),
                    answerKey = AnswerKey.Pg("B")
                ),
                QuestionItem(
                    id = "Q2",
                    number = 2,
                    type = QuestionType.PG_KOMPLEKS,
                    questionHtml = "<p>MCMA</p>",
                    options = listOf(
                        QuestionOption("A", "A", "X"),
                        QuestionOption("B", "B", "Y")
                    ),
                    answerKey = AnswerKey.PgKompleks(listOf("A", "B"))
                ),
                QuestionItem(
                    id = "Q3",
                    number = 3,
                    type = QuestionType.BENAR_SALAH,
                    questionHtml = "<p>BS</p>",
                    statementChoices = listOf(
                        QuestionStatementChoice(1, 1, "Benar"),
                        QuestionStatementChoice(2, 2, "Salah")
                    ),
                    statements = listOf(
                        QuestionStatement("S1", "A", "P1", "Pernyataan 1"),
                        QuestionStatement("S2", "B", "P2", "Pernyataan 2")
                    ),
                    answerKey = AnswerKey.BenarSalah(mapOf("S1" to 1, "S2" to 2))
                )
            )
        )

        val evaluation = QuizScoringEngine.evaluateQuiz(
            pkg,
            mapOf(
                "Q1" to UserAnswer.Pg("B"),
                "Q2" to UserAnswer.PgKompleks(setOf("A", "B")),
                "Q3" to UserAnswer.BenarSalah(mapOf("S1" to 1, "S2" to 1))
            )
        )

        assertEquals(3, evaluation.totalQuestions)
        assertEquals(3, evaluation.answeredCount)
        assertEquals(2, evaluation.correctCount)
        assertEquals(67, evaluation.scorePercent)
        assertEquals(QuestionGradeStatus.CORRECT, evaluation.questionResults[0].status)
        assertEquals(QuestionGradeStatus.CORRECT, evaluation.questionResults[1].status)
        assertEquals(QuestionGradeStatus.INCORRECT, evaluation.questionResults[2].status)
    }
}
