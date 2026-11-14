import { evaluateQuiz, isQuestionAnswered } from './scoring';
import {
  ActivityEventType,
  ExamActivityEvent,
  ExamReviewStatus,
  PackageId,
  PackageProgress,
  QuestionPackage,
  UserAnswersMap,
} from './types';

export const SUPERVISOR_REVIEW_THRESHOLD = 3;
export const PROCTOR_RESET_PIN = '202700';

export const ACTIVITY_EVENT_LABELS: Record<ActivityEventType, string> = {
  APP_LEFT_FOREGROUND: 'App left foreground',
  EXAM_RESUMED_AFTER_LEAVE: 'Ujian dilanjutkan setelah aplikasi kembali',
  ATTEMPT_EXIT_NAVIGATION: 'Percobaan keluar dari halaman ujian melalui navigasi aplikasi',
  EXAM_SUBMITTED: 'Ujian selesai atau dikumpulkan',
};

export function defaultExamDurationSeconds(questionCount: number): number {
  return Math.max(1, questionCount) * 120;
}

export function getRemainingSeconds(
  examDeadlineAt: string | null | undefined,
  nowMs: number = Date.now(),
): number {
  if (!examDeadlineAt) return 0;
  const deadlineMs = Date.parse(examDeadlineAt);
  if (!Number.isFinite(deadlineMs)) return 0;
  return Math.max(0, Math.floor((deadlineMs - nowMs) / 1000));
}

export function formatTimer(totalSeconds: number): string {
  const safe = Math.max(0, Math.floor(totalSeconds));
  const mins = Math.floor(safe / 60);
  const secs = safe % 60;
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

export function isViolationEvent(type: ActivityEventType): boolean {
  return type === 'APP_LEFT_FOREGROUND' || type === 'ATTEMPT_EXIT_NAVIGATION';
}

export function getExamReviewStatus(violationCount: number): ExamReviewStatus {
  return violationCount >= SUPERVISOR_REVIEW_THRESHOLD ? 'NEEDS_SUPERVISOR_REVIEW' : 'NORMAL';
}

export function startExamProgress(
  pkg: QuestionPackage,
  existing: PackageProgress | undefined,
  nowMs: number = Date.now(),
): PackageProgress {
  const durationSeconds = defaultExamDurationSeconds(pkg.questions.length);
  const startedIso = new Date(nowMs).toISOString();
  const deadlineIso = new Date(nowMs + durationSeconds * 1000).toISOString();

  return {
    packageId: pkg.packageId,
    status: 'IN_PROGRESS',
    currentQuestionIndex: 0,
    answers: {},
    lastScorePercent: existing?.lastScorePercent ?? null,
    lastCorrectCount: existing?.lastCorrectCount ?? 0,
    lastKeyedCount: existing?.lastKeyedCount ?? 0,
    lastAnsweredCount: 0,
    totalQuestions: pkg.questions.length,
    attempts: existing?.attempts ?? 0,
    updatedAt: startedIso,
    examDurationSeconds: durationSeconds,
    examStartedAt: startedIso,
    examDeadlineAt: deadlineIso,
    examCompletedAt: null,
    violationCount: 0,
    activityLog: [],
  };
}

export function updateExamAnswersAndIndex(
  pkg: QuestionPackage,
  current: PackageProgress,
  answers: UserAnswersMap,
  questionIndex: number,
  nowMs: number = Date.now(),
): PackageProgress {
  const answeredCount = pkg.questions.filter((q) =>
    isQuestionAnswered(q, answers[q.id]),
  ).length;
  const boundedIndex = Math.max(0, Math.min(questionIndex, Math.max(0, pkg.questions.length - 1)));

  return {
    ...current,
    status: 'IN_PROGRESS',
    currentQuestionIndex: boundedIndex,
    answers,
    lastAnsweredCount: answeredCount,
    totalQuestions: pkg.questions.length,
    updatedAt: new Date(nowMs).toISOString(),
  };
}

export function recordExamActivityEvent(
  current: PackageProgress,
  type: ActivityEventType,
  detail: string,
  nowMs: number = Date.now(),
): PackageProgress {
  const iso = new Date(nowMs).toISOString();
  const event: ExamActivityEvent = {
    type,
    label: ACTIVITY_EVENT_LABELS[type],
    timestamp: iso,
    detail,
  };
  const nextViolations = isViolationEvent(type)
    ? current.violationCount + 1
    : current.violationCount;

  return {
    ...current,
    violationCount: nextViolations,
    activityLog: [...current.activityLog, event],
    updatedAt: iso,
  };
}

export function finalizeExamProgress(
  pkg: QuestionPackage,
  current: PackageProgress,
  submitDetail: string,
  nowMs: number = Date.now(),
): PackageProgress {
  const evaluation = evaluateQuiz(pkg, current.answers);
  const completedIso = new Date(nowMs).toISOString();
  const submitEvent: ExamActivityEvent = {
    type: 'EXAM_SUBMITTED',
    label: ACTIVITY_EVENT_LABELS.EXAM_SUBMITTED,
    timestamp: completedIso,
    detail: submitDetail,
  };

  return {
    ...current,
    status: 'COMPLETED',
    currentQuestionIndex: 0,
    lastScorePercent: evaluation.scorePercent,
    lastCorrectCount: evaluation.correctCount,
    lastKeyedCount: evaluation.keyedCount,
    lastAnsweredCount: evaluation.answeredCount,
    totalQuestions: evaluation.totalQuestions,
    attempts: current.attempts + 1,
    examCompletedAt: completedIso,
    activityLog: [...current.activityLog, submitEvent],
    updatedAt: completedIso,
  };
}

export function findActiveExamPackageId(
  progressMap: Partial<Record<PackageId, PackageProgress>>,
): PackageId | null {
  for (const progress of Object.values(progressMap)) {
    if (progress && progress.status === 'IN_PROGRESS' && progress.examStartedAt) {
      return progress.packageId;
    }
  }
  return null;
}
