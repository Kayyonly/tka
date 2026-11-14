package com.tkasmp2027

import com.tkasmp2027.engine.ExamSessionEngine
import com.tkasmp2027.model.ActivityEventType
import com.tkasmp2027.model.AnswerKey
import com.tkasmp2027.model.ExamReviewStatus
import com.tkasmp2027.model.LockModeStatus
import com.tkasmp2027.model.ProgressStatus
import com.tkasmp2027.model.QuestionItem
import com.tkasmp2027.model.QuestionOption
import com.tkasmp2027.model.QuestionPackage
import com.tkasmp2027.model.QuestionType
import com.tkasmp2027.model.SubjectId
import com.tkasmp2027.model.UserAnswer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExamSessionEngineTest {

    private fun samplePackage(): QuestionPackage {
        return QuestionPackage(
            packageId = "matematika_paket_1",
            subjectId = SubjectId.MATEMATIKA,
            subjectName = "Matematika",
            title = "Paket 1",
            description = "Uji",
            expectedQuestionCount = 1,
            schemaVersion = "1.0",
            questions = listOf(
                QuestionItem(
                    id = "Q1",
                    number = 1,
                    type = QuestionType.PG,
                    questionHtml = "<p>1 + 1 = ?</p>",
                    options = listOf(
                        QuestionOption("A", "A", "1"),
                        QuestionOption("B", "B", "2")
                    ),
                    answerKey = AnswerKey.Pg("B")
                )
            )
        )
    }

    @Test
    fun examSessionTracksTimerViolationsAndPreservesAnswers() {
        val pkg = samplePackage()
        val startMs = 1_700_000_000_000L
        var progress = ExamSessionEngine.startExamProgress(
            pkg = pkg,
            existing = null,
            nowMillis = startMs,
            lockModeStatus = LockModeStatus.MONITORING_MODE_ONLY
        )

        assertEquals(ProgressStatus.IN_PROGRESS, progress.status)
        assertEquals(120, ExamSessionEngine.remainingSeconds(progress.examDeadlineMillis, startMs))
        assertEquals("02:00", ExamSessionEngine.formatTimer(120))

        // Answer saved automatically
        progress = ExamSessionEngine.updateAnswersAndIndex(
            pkg = pkg,
            current = progress,
            answers = mapOf("Q1" to UserAnswer.Pg("B")),
            questionIndex = 0,
            nowMillis = startMs + 10_000L
        )
        assertEquals(1, progress.lastAnsweredCount)

        // 3 violation events trigger supervisor review status without deleting answers
        progress = ExamSessionEngine.recordActivityEvent(
            progress,
            ActivityEventType.APP_LEFT_FOREGROUND,
            startMs + 20_000L,
            "App left foreground"
        )
        progress = ExamSessionEngine.recordActivityEvent(
            progress,
            ActivityEventType.EXAM_RESUMED_AFTER_LEAVE,
            startMs + 25_000L,
            "Kembali"
        )
        progress = ExamSessionEngine.recordActivityEvent(
            progress,
            ActivityEventType.ATTEMPT_EXIT_NAVIGATION,
            startMs + 30_000L,
            "Back ditekan"
        )
        progress = ExamSessionEngine.recordActivityEvent(
            progress,
            ActivityEventType.APP_LEFT_FOREGROUND,
            startMs + 40_000L,
            "App left foreground lagi"
        )

        assertEquals(3, progress.violationCount)
        assertEquals(ExamReviewStatus.NEEDS_SUPERVISOR_REVIEW, ExamSessionEngine.reviewStatus(progress.violationCount))
        assertEquals(UserAnswer.Pg("B"), progress.answers["Q1"])

        val finalized = ExamSessionEngine.finalizeExamProgress(
            pkg = pkg,
            current = progress,
            completedAtMillis = startMs + 50_000L,
            submitDetail = "Dikumpulkan"
        )
        assertEquals(ProgressStatus.COMPLETED, finalized.status)
        assertEquals(100, finalized.lastScorePercent)
        assertNotNull(finalized.examCompletedAtMillis)
        assertEquals(5, finalized.activityLog.size)
        assertEquals(ActivityEventType.EXAM_SUBMITTED, finalized.activityLog.last().type)
    }
}
