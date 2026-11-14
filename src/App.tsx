import React, { useMemo, useState } from 'react';
import { getBundledPackages, SUBJECTS } from './data/repository';
import { evaluateQuiz, isQuestionAnswered } from './engine/scoring';
import {
  clearPackageProgress,
  loadAllProgress,
  ProgressMap,
  savePackageProgress,
} from './engine/storage';
import { PackageId, UserAnswer, UserAnswersMap } from './engine/types';
import { HomeView } from './components/HomeView';
import { QuizView } from './components/QuizView';
import { ResultView } from './components/ResultView';
import { ReviewView } from './components/ReviewView';

type ScreenState =
  | { name: 'HOME' }
  | { name: 'QUIZ'; packageId: PackageId; questionIndex: number }
  | { name: 'RESULT'; packageId: PackageId }
  | { name: 'REVIEW'; packageId: PackageId };

export default function App() {
  const packages = useMemo(() => getBundledPackages(), []);
  const [progressMap, setProgressMap] = useState<ProgressMap>(() => loadAllProgress());
  const [screen, setScreen] = useState<ScreenState>({ name: 'HOME' });

  const handleStartPackage = (packageId: PackageId) => {
    const pkg = packages[packageId];
    if (pkg.questions.length === 0) return;

    const existing = progressMap[packageId];
    if (existing?.status === 'COMPLETED') {
      const freshNext = savePackageProgress({
        packageId,
        status: 'IN_PROGRESS',
        currentQuestionIndex: 0,
        answers: {},
        lastScorePercent: existing.lastScorePercent,
        lastCorrectCount: existing.lastCorrectCount,
        lastKeyedCount: existing.lastKeyedCount,
        lastAnsweredCount: 0,
        totalQuestions: pkg.questions.length,
        attempts: existing.attempts,
        updatedAt: new Date().toISOString(),
      });
      setProgressMap(freshNext);
      setScreen({ name: 'QUIZ', packageId, questionIndex: 0 });
      return;
    }

    const startIndex = existing?.currentQuestionIndex ?? 0;
    const boundedIndex = Math.max(0, Math.min(startIndex, pkg.questions.length - 1));
    setScreen({ name: 'QUIZ', packageId, questionIndex: boundedIndex });
  };

  const handleReviewPackage = (packageId: PackageId) => {
    setScreen({ name: 'REVIEW', packageId });
  };

  const handleResetProgress = (packageId: PackageId) => {
    const next = clearPackageProgress(packageId);
    setProgressMap(next);
  };

  const persistInProgress = (
    packageId: PackageId,
    nextAnswers: UserAnswersMap,
    nextIndex: number,
  ) => {
    const pkg = packages[packageId];
    const existing = progressMap[packageId];
    const answeredCount = pkg.questions.filter((q) =>
      isQuestionAnswered(q, nextAnswers[q.id]),
    ).length;

    const nextMap = savePackageProgress({
      packageId,
      status: 'IN_PROGRESS',
      currentQuestionIndex: nextIndex,
      answers: nextAnswers,
      lastScorePercent: existing?.lastScorePercent ?? null,
      lastCorrectCount: existing?.lastCorrectCount ?? 0,
      lastKeyedCount: existing?.lastKeyedCount ?? 0,
      lastAnsweredCount: answeredCount,
      totalQuestions: pkg.questions.length,
      attempts: existing?.attempts ?? 0,
      updatedAt: new Date().toISOString(),
    });
    setProgressMap(nextMap);
  };

  const handleUpdateAnswer = (packageId: PackageId, questionId: string, answer: UserAnswer) => {
    const existing = progressMap[packageId];
    const nextAnswers: UserAnswersMap = {
      ...(existing?.answers ?? {}),
      [questionId]: answer,
    };
    const currentIndex = screen.name === 'QUIZ' ? screen.questionIndex : 0;
    persistInProgress(packageId, nextAnswers, currentIndex);
  };

  const handleIndexChange = (packageId: PackageId, nextIndex: number) => {
    const existing = progressMap[packageId];
    const currentAnswers = existing?.answers ?? {};
    setScreen({ name: 'QUIZ', packageId, questionIndex: nextIndex });
    if (existing) {
      persistInProgress(packageId, currentAnswers, nextIndex);
    }
  };

  const handleSubmitQuiz = (packageId: PackageId) => {
    const pkg = packages[packageId];
    const existing = progressMap[packageId];
    const answers = existing?.answers ?? {};
    const evaluation = evaluateQuiz(pkg, answers);

    const nextMap = savePackageProgress({
      packageId,
      status: 'COMPLETED',
      currentQuestionIndex: 0,
      answers,
      lastScorePercent: evaluation.scorePercent,
      lastCorrectCount: evaluation.correctCount,
      lastKeyedCount: evaluation.keyedCount,
      lastAnsweredCount: evaluation.answeredCount,
      totalQuestions: evaluation.totalQuestions,
      attempts: (existing?.attempts ?? 0) + 1,
      updatedAt: new Date().toISOString(),
    });
    setProgressMap(nextMap);
    setScreen({ name: 'RESULT', packageId });
  };

  const handleRetryQuiz = (packageId: PackageId) => {
    const pkg = packages[packageId];
    const existing = progressMap[packageId];
    const nextMap = savePackageProgress({
      packageId,
      status: 'IN_PROGRESS',
      currentQuestionIndex: 0,
      answers: {},
      lastScorePercent: existing?.lastScorePercent ?? null,
      lastCorrectCount: existing?.lastCorrectCount ?? 0,
      lastKeyedCount: existing?.lastKeyedCount ?? 0,
      lastAnsweredCount: 0,
      totalQuestions: pkg.questions.length,
      attempts: existing?.attempts ?? 0,
      updatedAt: new Date().toISOString(),
    });
    setProgressMap(nextMap);
    setScreen({ name: 'QUIZ', packageId, questionIndex: 0 });
  };

  return (
    <>
      {screen.name === 'HOME' && (
        <HomeView
          subjects={SUBJECTS}
          packages={packages}
          progressMap={progressMap}
          onStartPackage={handleStartPackage}
          onReviewPackage={handleReviewPackage}
          onResetProgress={handleResetProgress}
        />
      )}

      {screen.name === 'QUIZ' && (
        <QuizView
          pkg={packages[screen.packageId]}
          currentIndex={screen.questionIndex}
          answers={progressMap[screen.packageId]?.answers ?? {}}
          onIndexChange={(nextIndex) => handleIndexChange(screen.packageId, nextIndex)}
          onUpdateAnswer={(qId, ans) => handleUpdateAnswer(screen.packageId, qId, ans)}
          onExitHome={() => setScreen({ name: 'HOME' })}
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
          onReviewAnswers={() => setScreen({ name: 'REVIEW', packageId: screen.packageId })}
          onRetryQuiz={() => handleRetryQuiz(screen.packageId)}
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
          onRetryQuiz={() => handleRetryQuiz(screen.packageId)}
          onBackHome={() => setScreen({ name: 'HOME' })}
        />
      )}
    </>
  );
}
