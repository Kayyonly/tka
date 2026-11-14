package com.tkasmp2027.engine

import com.tkasmp2027.model.AnswerKey
import com.tkasmp2027.model.QuestionEvaluation
import com.tkasmp2027.model.QuestionGradeStatus
import com.tkasmp2027.model.QuestionItem
import com.tkasmp2027.model.QuestionPackage
import com.tkasmp2027.model.QuestionType
import com.tkasmp2027.model.QuizEvaluation
import com.tkasmp2027.model.TypeBreakdown
import com.tkasmp2027.model.UserAnswer
import kotlin.math.roundToInt

object QuizScoringEngine {

    fun isQuestionAnswered(question: QuestionItem, answer: UserAnswer?): Boolean {
        if (answer == null) return false
        return when (question.type) {
            QuestionType.PG -> answer is UserAnswer.Pg && !answer.selectedOptionId.isNullOrBlank()
            QuestionType.PG_KOMPLEKS -> answer is UserAnswer.PgKompleks && answer.selectedOptionIds.isNotEmpty()
            QuestionType.BENAR_SALAH -> {
                answer is UserAnswer.BenarSalah &&
                    question.statements.isNotEmpty() &&
                    question.statements.all { stmt -> answer.statementSelections.containsKey(stmt.id) }
            }
        }
    }

    fun hasValidAnswerKey(question: QuestionItem): Boolean {
        val key = question.answerKey ?: return false
        return when (question.type) {
            QuestionType.PG -> key is AnswerKey.Pg && !key.correctOptionId.isNullOrBlank()
            QuestionType.PG_KOMPLEKS -> key is AnswerKey.PgKompleks && !key.correctOptionIds.isNullOrEmpty()
            QuestionType.BENAR_SALAH -> {
                key is AnswerKey.BenarSalah &&
                    !key.statementAnswers.isNullOrEmpty() &&
                    question.statements.isNotEmpty() &&
                    question.statements.all { stmt -> key.statementAnswers.containsKey(stmt.id) }
            }
        }
    }

    fun gradeQuestion(question: QuestionItem, answer: UserAnswer?): QuestionEvaluation {
        val answered = isQuestionAnswered(question, answer)
        val hasKey = hasValidAnswerKey(question)

        if (!hasKey || question.answerKey == null) {
            return QuestionEvaluation(
                questionId = question.id,
                questionNumber = question.number,
                type = question.type,
                answered = answered,
                hasKey = false,
                status = QuestionGradeStatus.UNKEYED
            )
        }

        if (!answered || answer == null) {
            return QuestionEvaluation(
                questionId = question.id,
                questionNumber = question.number,
                type = question.type,
                answered = false,
                hasKey = true,
                status = QuestionGradeStatus.UNANSWERED
            )
        }

        val isCorrect = when (val key: AnswerKey? = question.answerKey) {
            is AnswerKey.Pg -> {
                answer is UserAnswer.Pg && answer.selectedOptionId == key.correctOptionId
            }
            is AnswerKey.PgKompleks -> {
                val expected = key.correctOptionIds?.toSet().orEmpty()
                answer is UserAnswer.PgKompleks && answer.selectedOptionIds == expected
            }
            is AnswerKey.BenarSalah -> {
                val expected = key.statementAnswers.orEmpty()
                answer is UserAnswer.BenarSalah &&
                    question.statements.all { stmt ->
                        answer.statementSelections[stmt.id] == expected[stmt.id]
                    }
            }
            null -> false
        }

        return QuestionEvaluation(
            questionId = question.id,
            questionNumber = question.number,
            type = question.type,
            answered = true,
            hasKey = true,
            status = if (isCorrect) QuestionGradeStatus.CORRECT else QuestionGradeStatus.INCORRECT
        )
    }

    fun evaluateQuiz(pkg: QuestionPackage, answers: Map<String, UserAnswer>): QuizEvaluation {
        val counts = QuestionType.entries.associateWith { mutableListOf(0, 0, 0, 0) }
        var answeredCount = 0
        var keyedCount = 0
        var correctCount = 0

        val results = pkg.questions.map { q ->
            val eval = gradeQuestion(q, answers[q.id])
            val bucket = counts.getValue(q.type)
            bucket[0] += 1 // total
            if (eval.answered) {
                answeredCount += 1
                bucket[1] += 1
            }
            if (eval.hasKey) {
                keyedCount += 1
                bucket[2] += 1
                if (eval.status == QuestionGradeStatus.CORRECT) {
                    correctCount += 1
                    bucket[3] += 1
                }
            }
            eval
        }

        val total = pkg.questions.size
        val scorePercent = if (keyedCount > 0) {
            ((correctCount.toDouble() / keyedCount.toDouble()) * 100.0).roundToInt()
        } else null
        val completionPercent = if (total > 0) {
            ((answeredCount.toDouble() / total.toDouble()) * 100.0).roundToInt()
        } else 0

        val byType = counts.mapValues { (type, list) ->
            TypeBreakdown(
                type = type,
                total = list[0],
                answered = list[1],
                keyed = list[2],
                correct = list[3]
            )
        }

        return QuizEvaluation(
            packageId = pkg.packageId,
            totalQuestions = total,
            answeredCount = answeredCount,
            keyedCount = keyedCount,
            unkeyedCount = total - keyedCount,
            correctCount = correctCount,
            incorrectCount = keyedCount - correctCount,
            scorePercent = scorePercent,
            completionPercent = completionPercent,
            byType = byType,
            questionResults = results
        )
    }
}
