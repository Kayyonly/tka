import {
  QuestionEvaluation,
  QuestionGradeStatus,
  QuestionItem,
  QuestionPackage,
  QuestionType,
  QuizEvaluation,
  TypeBreakdown,
  UserAnswer,
  UserAnswersMap,
} from './types';

export function isQuestionAnswered(question: QuestionItem, answer?: UserAnswer): boolean {
  if (!answer || answer.type !== question.type) {
    return false;
  }

  switch (question.type) {
    case 'PG':
      return (
        answer.type === 'PG' &&
        typeof answer.selectedOptionId === 'string' &&
        answer.selectedOptionId.length > 0
      );
    case 'PG_KOMPLEKS':
      return (
        answer.type === 'PG_KOMPLEKS' &&
        Array.isArray(answer.selectedOptionIds) &&
        answer.selectedOptionIds.length > 0
      );
    case 'BENAR_SALAH': {
      if (answer.type !== 'BENAR_SALAH') return false;
      const statements = question.statements ?? [];
      if (statements.length === 0) return false;
      return statements.every((stmt) => {
        const val = answer.statementSelections[stmt.id];
        return typeof val === 'number' && Number.isFinite(val);
      });
    }
  }
}

export function hasValidAnswerKey(question: QuestionItem): boolean {
  const key = question.answerKey;
  if (!key || key.type !== question.type) {
    return false;
  }

  switch (key.type) {
    case 'PG':
      return typeof key.correctOptionId === 'string' && key.correctOptionId.length > 0;
    case 'PG_KOMPLEKS':
      return Array.isArray(key.correctOptionIds) && key.correctOptionIds.length > 0;
    case 'BENAR_SALAH': {
      const statements = question.statements ?? [];
      if (!key.statementAnswers || statements.length === 0) return false;
      return statements.every((stmt) => {
        const val = key.statementAnswers?.[stmt.id];
        return typeof val === 'number' && Number.isFinite(val);
      });
    }
  }
}

export function gradeQuestion(question: QuestionItem, answer?: UserAnswer): QuestionEvaluation {
  const answered = isQuestionAnswered(question, answer);
  const hasKey = hasValidAnswerKey(question);

  if (!hasKey || !question.answerKey) {
    return {
      questionId: question.id,
      questionNumber: question.number,
      type: question.type,
      answered,
      hasKey: false,
      status: 'UNKEYED',
    };
  }

  if (!answered || !answer || answer.type !== question.type) {
    return {
      questionId: question.id,
      questionNumber: question.number,
      type: question.type,
      answered: false,
      hasKey: true,
      status: 'UNANSWERED',
    };
  }

  let status: QuestionGradeStatus = 'INCORRECT';
  let statementResults: Record<string, boolean> | undefined;

  if (question.answerKey.type === 'PG' && answer.type === 'PG') {
    status = answer.selectedOptionId === question.answerKey.correctOptionId ? 'CORRECT' : 'INCORRECT';
  } else if (question.answerKey.type === 'PG_KOMPLEKS' && answer.type === 'PG_KOMPLEKS') {
    const expected = [...new Set(question.answerKey.correctOptionIds ?? [])].sort();
    const actual = [...new Set(answer.selectedOptionIds)].sort();
    const exactMatch =
      expected.length === actual.length && expected.every((val, idx) => val === actual[idx]);
    status = exactMatch ? 'CORRECT' : 'INCORRECT';
  } else if (question.answerKey.type === 'BENAR_SALAH' && answer.type === 'BENAR_SALAH') {
    const keyMap = question.answerKey.statementAnswers ?? {};
    const statements = question.statements ?? [];
    statementResults = {};
    let allCorrect = statements.length > 0;
    for (const stmt of statements) {
      const isStmtCorrect = answer.statementSelections[stmt.id] === keyMap[stmt.id];
      statementResults[stmt.id] = isStmtCorrect;
      if (!isStmtCorrect) {
        allCorrect = false;
      }
    }
    status = allCorrect ? 'CORRECT' : 'INCORRECT';
  }

  return {
    questionId: question.id,
    questionNumber: question.number,
    type: question.type,
    answered: true,
    hasKey: true,
    status,
    statementResults,
  };
}

export function evaluateQuiz(pkg: QuestionPackage, answers: UserAnswersMap): QuizEvaluation {
  const byType: Record<QuestionType, TypeBreakdown> = {
    PG: { type: 'PG', total: 0, answered: 0, keyed: 0, correct: 0 },
    PG_KOMPLEKS: { type: 'PG_KOMPLEKS', total: 0, answered: 0, keyed: 0, correct: 0 },
    BENAR_SALAH: { type: 'BENAR_SALAH', total: 0, answered: 0, keyed: 0, correct: 0 },
  };

  let answeredCount = 0;
  let keyedCount = 0;
  let correctCount = 0;

  const questionResults: QuestionEvaluation[] = pkg.questions.map((question) => {
    const result = gradeQuestion(question, answers[question.id]);
    const bucket = byType[question.type];
    bucket.total += 1;

    if (result.answered) {
      answeredCount += 1;
      bucket.answered += 1;
    }

    if (result.hasKey) {
      keyedCount += 1;
      bucket.keyed += 1;
      if (result.status === 'CORRECT') {
        correctCount += 1;
        bucket.correct += 1;
      }
    }

    return result;
  });

  const totalQuestions = pkg.questions.length;
  const unkeyedCount = totalQuestions - keyedCount;
  const incorrectCount = keyedCount - correctCount;
  const scorePercent = keyedCount > 0 ? Math.round((correctCount / keyedCount) * 100) : null;
  const completionPercent = totalQuestions > 0 ? Math.round((answeredCount / totalQuestions) * 100) : 0;

  return {
    packageId: pkg.packageId,
    totalQuestions,
    answeredCount,
    keyedCount,
    unkeyedCount,
    correctCount,
    incorrectCount,
    scorePercent,
    completionPercent,
    byType,
    questionResults,
  };
}

export function togglePgKompleksOption(current: string[], optionId: string): string[] {
  if (current.includes(optionId)) {
    return current.filter((id) => id !== optionId);
  }
  return [...current, optionId];
}

export function setStatementChoiceSelection(
  current: Record<string, number>,
  statementId: string,
  choiceOrder: number,
): Record<string, number> {
  return {
    ...current,
    [statementId]: choiceOrder,
  };
}
