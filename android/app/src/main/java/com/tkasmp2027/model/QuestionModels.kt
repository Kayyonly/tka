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

data class QuestionMetadata(
    val originality: String? = null,
    val subject: String? = null,
    val element: String? = null,
    val topic: String? = null,
    val competency: String? = null,
    val subcompetency: String? = null,
    val cognitiveLevel: String? = null,
    val difficulty: String? = null,
    val format: String? = null,
    val stimulusType: String? = null,
    val stimulusAssetId: String? = null,
    val explanationLanguage: String? = null
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
    val explanationHtml: String? = null,
    val competency: String? = null,
    val subCompetency: String? = null,
    val metadata: QuestionMetadata? = null
)

data class QuestionPackage(
    val packageId: String,
    val subjectId: SubjectId,
    val subjectName: String,
    val title: String,
    val description: String,
    val expectedQuestionCount: Int,
    val schemaVersion: String,
    val questions: List<QuestionItem>,
    val bankType: String = "reference"
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

enum class ActivityEventType(val label: String) {
    APP_LEFT_FOREGROUND("App left foreground"),
    EXAM_RESUMED_AFTER_LEAVE("Ujian dilanjutkan setelah aplikasi kembali"),
    ATTEMPT_EXIT_NAVIGATION("Percobaan keluar dari halaman ujian melalui navigasi aplikasi"),
    EXAM_SUBMITTED("Ujian selesai atau dikumpulkan")
}

data class ExamActivityEvent(
    val type: ActivityEventType,
    val timestampMillis: Long,
    val detail: String
)

enum class LockModeStatus(val badgeText: String) {
    FULL_LOCKDOWN_AVAILABLE("FULL LOCKDOWN AVAILABLE"),
    MONITORING_MODE_ONLY("MONITORING MODE ONLY")
}

enum class ExamReviewStatus(val label: String) {
    NORMAL("Normal"),
    NEEDS_SUPERVISOR_REVIEW("Perlu Ditinjau Pengawas")
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
    val updatedAtMillis: Long,
    val examDurationSeconds: Int = 0,
    val examStartedAtMillis: Long? = null,
    val examDeadlineMillis: Long? = null,
    val examCompletedAtMillis: Long? = null,
    val lockModeStatus: LockModeStatus = LockModeStatus.MONITORING_MODE_ONLY,
    val violationCount: Int = 0,
    val activityLog: List<ExamActivityEvent> = emptyList()
)
