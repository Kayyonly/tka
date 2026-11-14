package com.tkasmp2027.model

enum class SubjectId(val key: String, val displayName: String, val subtitle: String) {
    MATEMATIKA(
        key = "matematika",
        displayName = "Matematika",
        subtitle = "Aljabar, Geometri, Bilangan, Statistika & Peluang"
    ),
    BAHASA_INDONESIA(
        key = "bahasa_indonesia",
        displayName = "Bahasa Indonesia",
        subtitle = "Pemahaman Bacaan, Literasi Informasi, Teks Sastra & Kebahasaan"
    )
}

enum class QuestionType(val raw: String, val label: String) {
    PG("1", "Pilihan Ganda"),
    PG_KOMPLEKS("2", "PG Kompleks (MCMA)"),
    BENAR_SALAH("3", "Benar / Salah");

    companion object {
        fun fromRaw(raw: String): QuestionType = when (raw.trim()) {
            "1", "PG" -> PG
            "2", "PG_KOMPLEKS", "MCMA" -> PG_KOMPLEKS
            "3", "BENAR_SALAH" -> BENAR_SALAH
            else -> throw IllegalArgumentException("Tipe soal tidak dikenal: $raw")
        }
    }
}

data class QuestionImage(
    val url: String,
    val alt: String,
    val caption: String? = null
)

data class QuestionOption(
    val id: String,
    val label: String,
    val html: String
)

data class QuestionStatementChoice(
    val id: Int,
    val order: Int,
    val label: String
)

data class QuestionStatement(
    val id: String,
    val key: String,
    val label: String? = null,
    val html: String
)

sealed interface AnswerKey {
    data class Pg(val correctOptionId: String?) : AnswerKey
    data class PgKompleks(val correctOptionIds: List<String>?) : AnswerKey
    data class BenarSalah(val statementAnswers: Map<String, Int>?) : AnswerKey
}

data class QuestionItem(
    val id: String,
    val number: Int,
    val type: QuestionType,
    val stimulusHtml: String? = null,
    val questionHtml: String,
    val mathFormula: String? = null,
    val image: QuestionImage? = null,
    val options: List<QuestionOption> = emptyList(),
    val statementChoices: List<QuestionStatementChoice> = emptyList(),
    val statements: List<QuestionStatement> = emptyList(),
    val answerKey: AnswerKey? = null,
    val explanationHtml: String? = null
)

data class QuestionPackage(
    val packageId: String,
    val subjectId: SubjectId,
    val subjectName: String,
    val title: String,
    val description: String,
    val expectedQuestionCount: Int,
    val schemaVersion: String,
    val questions: List<QuestionItem>
)

sealed interface UserAnswer {
    data class Pg(val selectedOptionId: String?) : UserAnswer
    data class PgKompleks(val selectedOptionIds: Set<String>) : UserAnswer
    data class BenarSalah(val statementSelections: Map<String, Int>) : UserAnswer
}

enum class QuestionGradeStatus {
    CORRECT,
    INCORRECT,
    UNANSWERED,
    UNKEYED
}

data class QuestionEvaluation(
    val questionId: String,
    val questionNumber: Int,
    val type: QuestionType,
    val answered: Boolean,
    val hasKey: Boolean,
    val status: QuestionGradeStatus
)

data class TypeBreakdown(
    val type: QuestionType,
    val total: Int,
    val answered: Int,
    val keyed: Int,
    val correct: Int
)

data class QuizEvaluation(
    val packageId: String,
    val totalQuestions: Int,
    val answeredCount: Int,
    val keyedCount: Int,
    val unkeyedCount: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val scorePercent: Int?,
    val completionPercent: Int,
    val byType: Map<QuestionType, TypeBreakdown>,
    val questionResults: List<QuestionEvaluation>
)

enum class ProgressStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

data class PackageProgress(
    val packageId: String,
    val status: ProgressStatus,
    val currentQuestionIndex: Int,
    val answers: Map<String, UserAnswer>,
    val lastScorePercent: Int?,
    val lastCorrectCount: Int,
    val lastKeyedCount: Int,
    val lastAnsweredCount: Int,
    val totalQuestions: Int,
    val attempts: Int,
    val updatedAtMillis: Long
)
