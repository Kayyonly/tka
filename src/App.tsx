import React, { useEffect, useMemo, useRef, useState } from 'react';
import { getBundledPackages, SUBJECTS } from './data/repository';
import {
  finalizeExamProgress,
  findActiveExamPackageId,
  getRemainingSeconds,
  recordExamActivityEvent,
  startExamProgress,
  updateExamAnswersAndIndex,
} from './engine/examSession';
import { evaluateQuiz } from './engine/scoring';
import {
  clearPackageProgress,
  loadAllProgress,
  ProgressMap,
  savePackageProgress,
} from './engine/storage';
import { ActivityEventType, PackageId, UserAnswer } from './engine/types';
import { ActivityLogView } from './components/ActivityLogView';
import { ExamPrepView } from './components/ExamPrepView';
import { HomeView } from './components/HomeView';
import { QuizView } from './components/QuizView';
import { ResultView } from './components/ResultView';
import { ReviewView } from './components/ReviewView';

type ScreenState =
  | { name: 'HOME' }
  | { name: 'PREP'; packageId: PackageId }
  | { name: 'QUIZ'; packageId: PackageId; questionIndex: number }
  | { name: 'RESULT'; packageId: PackageId }
  | { name: 'REVIEW'; packageId: PackageId }
  | { name: 'ACTIVITY_LOG'; packageId: PackageId; returnToResult: boolean };

function tryEnterBrowserFullscreen() {
  try {
    if (document.documentElement.requestFullscreen && !document.fullscreenElement) {
      void document.documentElement.requestFullscreen().catch(() => {});
    }
  } catch {
    // Fullscreen may be blocked by browser policy
  }
}

function tryExitBrowserFullscreen() {
  try {
    if (document.fullscreenElement && document.exitFullscreen) {
      void document.exitFullscreen().catch(() => {});
    }
  } catch {
    // Ignore fullscreen exit errors
  }
}

export default function App() {
  const packages = useMemo(() => getBundledPackages(), []);

  const initialRestored = useMemo(() => {
    const loaded = loadAllProgress();
    const activeId = findActiveExamPackageId(loaded);
    if (!activeId) {
      return { progressMap: loaded, screen: { name: 'HOME' } as ScreenState };
    }
    const activeProgress = loaded[activeId];
    const pkg = packages[activeId];
    if (!activeProgress || !pkg) {
      return { progressMap: loaded, screen: { name: 'HOME' } as ScreenState };
    }
    const remaining = getRemainingSeconds(activeProgress.examDeadlineAt, Date.now());
    if (remaining <= 0) {
      const finalized = finalizeExamProgress(
        pkg,
        activeProgress,
        'Waktu ujian habis saat aplikasi tertutup; jawaban tersimpan otomatis dikumpulkan.',
      );
      const updated = savePackageProgress(finalized);
      return { progressMap: updated, screen: { name: 'RESULT', packageId: activeId } as ScreenState };
    }
    return {
      progressMap: loaded,
      screen: {
        name: 'QUIZ',
        packageId: activeId,
        questionIndex: activeProgress.currentQuestionIndex,
      } as ScreenState,
    };
  }, [packages]);

  const [progressMap, setProgressMap] = useState<ProgressMap>(initialRestored.progressMap);
  const [screen, setScreen] = useState<ScreenState>(initialRestored.screen);
  const [nowMs, setNowMs] = useState(() => Date.now());
  const [warningMessage, setWarningMessage] = useState<string | null>(null);

  const progressMapRef = useRef(progressMap);
  progressMapRef.current = progressMap;

  const activeQuizPackageId = screen.name === 'QUIZ' ? screen.packageId : null;

  // Live 1-second timer tick & automatic submit on timeout
  useEffect(() => {
    if (!activeQuizPackageId) return;
    const intervalId = window.setInterval(() => {
      const currentNow = Date.now();
      setNowMs(currentNow);
      const currentProgress = progressMapRef.current[activeQuizPackageId];
      if (!currentProgress || currentProgress.status !== 'IN_PROGRESS') return;
      const remaining = getRemainingSeconds(currentProgress.examDeadlineAt, currentNow);
      if (remaining <= 0) {
        const finalized = finalizeExamProgress(
          packages[activeQuizPackageId],
          currentProgress,
          'Waktu ujian habis; jawaban dikumpulkan otomatis.',
          currentNow,
        );
        const nextMap = savePackageProgress(finalized);
        setProgressMap(nextMap);
        setWarningMessage(null);
        tryExitBrowserFullscreen();
        setScreen({ name: 'RESULT', packageId: activeQuizPackageId });
      }
    }, 1000);

    return () => window.clearInterval(intervalId);
  }, [activeQuizPackageId, packages]);

  const handleSelectPackage = (packageId: PackageId) => {
    const pkg = packages[packageId];
    if (pkg.questions.length === 0) return;

    const existing = progressMap[packageId];
    if (existing?.status === 'IN_PROGRESS') {
      const remaining = getRemainingSeconds(existing.examDeadlineAt, Date.now());
      if (remaining <= 0) {
        const finalized = finalizeExamProgress(
          pkg,
          existing,
          'Waktu ujian habis; jawaban dikumpulkan otomatis.',
        );
        const nextMap = savePackageProgress(finalized);
        setProgressMap(nextMap);
        setScreen({ name: 'RESULT', packageId });
        return;
      }
      setWarningMessage(null);
      setNowMs(Date.now());
      tryEnterBrowserFullscreen();
      setScreen({ name: 'QUIZ', packageId, questionIndex: existing.currentQuestionIndex });
      return;
    }

    if (existing?.status === 'COMPLETED') {
      setScreen({ name: 'RESULT', packageId });
      return;
    }

    setScreen({ name: 'PREP', packageId });
  };

  const handleConfirmStartExam = (packageId: PackageId) => {
    const pkg = packages[packageId];
    const existing = progressMap[packageId];
    if (existing?.status === 'COMPLETED') {
      setScreen({ name: 'RESULT', packageId });
      return;
    }
    const now = Date.now();
    const started = startExamProgress(pkg, existing, now);
    const nextMap = savePackageProgress(started);
    setProgressMap(nextMap);
    setWarningMessage(null);
    setNowMs(now);
    tryEnterBrowserFullscreen();
    setScreen({ name: 'QUIZ', packageId, questionIndex: 0 });
  };

  const handleUpdateAnswer = (packageId: PackageId, questionId: string, answer: UserAnswer) => {
    const current = progressMapRef.current[packageId];
    if (!current || current.status !== 'IN_PROGRESS') return;
    const nextAnswers = { ...current.answers, [questionId]: answer };
    const idx = screen.name === 'QUIZ' ? screen.questionIndex : current.currentQuestionIndex;
    const updated = updateExamAnswersAndIndex(packages[packageId], current, nextAnswers, idx);
    const nextMap = savePackageProgress(updated);
    setProgressMap(nextMap);
  };

  const handleIndexChange = (packageId: PackageId, nextIndex: number) => {
    const current = progressMapRef.current[packageId];
    setScreen({ name: 'QUIZ', packageId, questionIndex: nextIndex });
    if (current && current.status === 'IN_PROGRESS') {
      const updated = updateExamAnswersAndIndex(
        packages[packageId],
        current,
        current.answers,
        nextIndex,
      );
      const nextMap = savePackageProgress(updated);
      setProgressMap(nextMap);
    }
  };

  const handleRecordActivityEvent = (
    packageId: PackageId,
    type: ActivityEventType,
    detail: string,
    warningText?: string,
  ) => {
    const current = progressMapRef.current[packageId];
    if (!current || current.status !== 'IN_PROGRESS') return;
    const updated = recordExamActivityEvent(current, type, detail);
    const nextMap = savePackageProgress(updated);
    setProgressMap(nextMap);
    if (warningText) {
      setWarningMessage(`${warningText} (Pelanggaran tercatat: ${updated.violationCount}x)`);
    }
  };

  const handleSubmitQuiz = (packageId: PackageId) => {
    const current = progressMapRef.current[packageId];
    if (!current) return;
    const finalized = finalizeExamProgress(
      packages[packageId],
      current,
      'Peserta mengumpulkan ujian secara resmi.',
    );
    const nextMap = savePackageProgress(finalized);
    setProgressMap(nextMap);
    setWarningMessage(null);
    tryExitBrowserFullscreen();
    setScreen({ name: 'RESULT', packageId });
  };

  const handleProctorReset = (packageId: PackageId) => {
    const nextMap = clearPackageProgress(packageId);
    setProgressMap(nextMap);
    setWarningMessage(null);
    setScreen({ name: 'HOME' });
  };

  return (
    <>
      {screen.name === 'HOME' && (
        <HomeView
          subjects={SUBJECTS}
          packages={packages}
          progressMap={progressMap}
          onSelectPackage={handleSelectPackage}
          onViewResultPackage={(packageId) => setScreen({ name: 'RESULT', packageId })}
          onReviewPackage={(packageId) => setScreen({ name: 'REVIEW', packageId })}
          onViewActivityLog={(packageId) =>
            setScreen({ name: 'ACTIVITY_LOG', packageId, returnToResult: false })
          }
        />
      )}

      {screen.name === 'PREP' && (
        <ExamPrepView
          pkg={packages[screen.packageId]}
          onConfirmStartExam={() => handleConfirmStartExam(screen.packageId)}
          onCancelBack={() => setScreen({ name: 'HOME' })}
        />
      )}

      {screen.name === 'QUIZ' && (
        <QuizView
          pkg={packages[screen.packageId]}
          currentIndex={screen.questionIndex}
          answers={progressMap[screen.packageId]?.answers ?? {}}
          remainingSeconds={getRemainingSeconds(
            progressMap[screen.packageId]?.examDeadlineAt,
            nowMs,
          )}
          violationCount={progressMap[screen.packageId]?.violationCount ?? 0}
          warningMessage={warningMessage}
          onDismissWarning={() => setWarningMessage(null)}
          onIndexChange={(nextIndex) => handleIndexChange(screen.packageId, nextIndex)}
          onUpdateAnswer={(qId, ans) => handleUpdateAnswer(screen.packageId, qId, ans)}
          onRecordActivityEvent={(type, detail, warningText) =>
            handleRecordActivityEvent(screen.packageId, type, detail, warningText)
          }
          onSubmitQuiz={() => handleSubmitQuiz(screen.packageId)}
        />
      )}

      {screen.name === 'RESULT' && (
        <ResultView
          pkg={packages[screen.packageId]}
          evaluation={evaluateQuiz(
            packages[screen.packageId],
            progressMap[screen.packageId]?.answers ?? {},
          )}
          progress={progressMap[screen.packageId]}
          onReviewAnswers={() => setScreen({ name: 'REVIEW', packageId: screen.packageId })}
          onViewActivityLog={() =>
            setScreen({
              name: 'ACTIVITY_LOG',
              packageId: screen.packageId,
              returnToResult: true,
            })
          }
          onBackHome={() => setScreen({ name: 'HOME' })}
        />
      )}

      {screen.name === 'REVIEW' && (
        <ReviewView
          pkg={packages[screen.packageId]}
          answers={progressMap[screen.packageId]?.answers ?? {}}
          evaluation={evaluateQuiz(
            packages[screen.packageId],
            progressMap[screen.packageId]?.answers ?? {},
          )}
          onBackToResult={() => setScreen({ name: 'RESULT', packageId: screen.packageId })}
          onViewActivityLog={() =>
            setScreen({
              name: 'ACTIVITY_LOG',
              packageId: screen.packageId,
              returnToResult: true,
            })
          }
          onBackHome={() => setScreen({ name: 'HOME' })}
        />
      )}

      {screen.name === 'ACTIVITY_LOG' && (
        <ActivityLogView
          pkg={packages[screen.packageId]}
          progress={progressMap[screen.packageId]}
          onBack={() =>
            setScreen(
              screen.returnToResult
                ? { name: 'RESULT', packageId: screen.packageId }
                : { name: 'HOME' },
            )
          }
          onProctorResetSession={() => handleProctorReset(screen.packageId)}
        />
      )}
    </>
  );
}
