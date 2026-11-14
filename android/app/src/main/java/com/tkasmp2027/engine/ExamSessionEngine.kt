package com.tkasmp2027.engine

import com.tkasmp2027.model.ActivityEventType
import com.tkasmp2027.model.ExamActivityEvent
import com.tkasmp2027.model.ExamReviewStatus
import com.tkasmp2027.model.LockModeStatus
import com.tkasmp2027.model.PackageProgress
import com.tkasmp2027.model.ProgressStatus
import com.tkasmp2027.model.QuestionPackage
import com.tkasmp2027.model.UserAnswer
import java.util.Locale

object ExamSessionEngine {

    const val SUPERVISOR_REVIEW_THRESHOLD = 3
    const val PROCTOR_RESET_PIN = "202700"

    fun defaultDurationSeconds(questionCount: Int): Int {
        return (questionCount.coerceAtLeast(1)) * 120
    }

    fun remainingSeconds(deadlineMillis: Long?, nowMillis: Long): Int {
        if (deadlineMillis == null) return 0
        return ((deadlineMillis - nowMillis) / 1000L).toInt().coerceAtLeast(0)
    }

    fun formatTimer(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val minutes = safe / 60
        val seconds = safe % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    fun isViolationEvent(type: ActivityEventType): Boolean {
        return type == ActivityEventType.APP_LEFT_FOREGROUND ||
            type == ActivityEventType.ATTEMPT_EXIT_NAVIGATION
    }

    fun reviewStatus(violationCount: Int): ExamReviewStatus {
        return if (violationCount >= SUPERVISOR_REVIEW_THRESHOLD) {
            ExamReviewStatus.NEEDS_SUPERVISOR_REVIEW
        } else {
            ExamReviewStatus.NORMAL
        }
    }

    fun startExamProgress(
        pkg: QuestionPackage,
        existing: PackageProgress?,
        nowMillis: Long,
        lockModeStatus: LockModeStatus
    ): PackageProgress {
        val durationSeconds = defaultDurationSeconds(pkg.questions.size)
        return PackageProgress(
            packageId = pkg.packageId,
            status = ProgressStatus.IN_PROGRESS,
            currentQuestionIndex = 0,
            answers = emptyMap(),
            lastScorePercent = existing?.lastScorePercent,
            lastCorrectCount = existing?.lastCorrectCount ?: 0,
            lastKeyedCount = existing?.lastKeyedCount ?: 0,
            lastAnsweredCount = 0,
            totalQuestions = pkg.questions.size,
            attempts = existing?.attempts ?: 0,
            updatedAtMillis = nowMillis,
            examDurationSeconds = durationSeconds,
            examStartedAtMillis = nowMillis,
            examDeadlineMillis = nowMillis + (durationSeconds * 1000L),
            examCompletedAtMillis = null,
            lockModeStatus = lockModeStatus,
            violationCount = 0,
            activityLog = emptyList()
        )
    }

    fun updateAnswersAndIndex(
        pkg: QuestionPackage,
        current: PackageProgress,
        answers: Map<String, UserAnswer>,
        questionIndex: Int,
        nowMillis: Long
    ): PackageProgress {
        val answeredCount = pkg.questions.count {
            QuizScoringEngine.isQuestionAnswered(it, answers[it.id])
        }
        return current.copy(
            status = ProgressStatus.IN_PROGRESS,
            currentQuestionIndex = questionIndex.coerceIn(0, (pkg.questions.size - 1).coerceAtLeast(0)),
            answers = answers,
            lastAnsweredCount = answeredCount,
            totalQuestions = pkg.questions.size,
            updatedAtMillis = nowMillis
        )
    }

    fun recordActivityEvent(
        current: PackageProgress,
        type: ActivityEventType,
        timestampMillis: Long,
        detail: String
    ): PackageProgress {
        val event = ExamActivityEvent(
            type = type,
            timestampMillis = timestampMillis,
            detail = detail
        )
        val nextViolations = if (isViolationEvent(type)) {
            current.violationCount + 1
        } else {
            current.violationCount
        }
        return current.copy(
            violationCount = nextViolations,
            activityLog = current.activityLog + event,
            updatedAtMillis = timestampMillis
        )
    }

    fun finalizeExamProgress(
        pkg: QuestionPackage,
        current: PackageProgress,
        completedAtMillis: Long,
        submitDetail: String
    ): PackageProgress {
        val evaluation = QuizScoringEngine.evaluateQuiz(pkg, current.answers)
        val submitEvent = ExamActivityEvent(
            type = ActivityEventType.EXAM_SUBMITTED,
            timestampMillis = completedAtMillis,
            detail = submitDetail
        )
        return current.copy(
            status = ProgressStatus.COMPLETED,
            currentQuestionIndex = 0,
            lastScorePercent = evaluation.scorePercent,
            lastCorrectCount = evaluation.correctCount,
            lastKeyedCount = evaluation.keyedCount,
            lastAnsweredCount = evaluation.answeredCount,
            totalQuestions = evaluation.totalQuestions,
            attempts = current.attempts + 1,
            examCompletedAtMillis = completedAtMillis,
            activityLog = current.activityLog + submitEvent,
            updatedAtMillis = completedAtMillis
        )
    }

    fun findActiveExamPackageId(progressMap: Map<String, PackageProgress>): String? {
        return progressMap.values
            .firstOrNull { it.status == ProgressStatus.IN_PROGRESS && it.examStartedAtMillis != null }
            ?.packageId
    }
}
