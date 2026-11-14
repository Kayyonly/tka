export type SubjectId = 'matematika' | 'bahasa_indonesia';

export type PackageId =
  | 'matematika_paket_1'
  | 'matematika_paket_2'
  | 'bahasa_indonesia_paket_1'
  | 'bahasa_indonesia_paket_2';

/**
 * Official question types:
 * - "1" -> PG (single choice)
 * - "2" -> PG_KOMPLEKS (multiple choice / MCMA)
 * - "3" -> BENAR_SALAH (per-statement classification)
 */
export type JenisSoalCode = '1' | '2' | '3';

export type QuestionType = 'PG' | 'PG_KOMPLEKS' | 'BENAR_SALAH';

export interface QuestionImage {
  url: string;
  alt: string;
  caption?: string;
}

export interface QuestionOption {
  id: string;
  label: string;
  html: string;
}

export interface QuestionStatementChoice {
  id: number;
  order: number;
  label: string;
}

export interface QuestionStatement {
  id: string;
  key: string;
  label?: string;
  html: string;
}

export type AnswerKey =
  | {
      type: 'PG';
      correctOptionId: string | null;
    }
  | {
      type: 'PG_KOMPLEKS';
      correctOptionIds: string[] | null;
    }
  | {
      type: 'BENAR_SALAH';
      statementAnswers: Record<string, number> | null;
    };

export interface QuestionItem {
  id: string;
  number: number;
  idMapel?: number;
  jenis_soal: JenisSoalCode;
  type: QuestionType;
  kompetensi?: string | null;
  subKompetensi?: string | null;
  stimulusHtml?: string | null;
  questionHtml: string;
  mathFormula?: string | null;
  image?: QuestionImage | null;
  options?: QuestionOption[];
  statementChoices?: QuestionStatementChoice[];
  statements?: QuestionStatement[];
  answerKey?: AnswerKey | null;
  kunciCaption?: string | null;
  explanationHtml?: string | null;
  raw?: Record<string, unknown>;
}

export interface QuestionPackage {
  packageId: PackageId;
  subjectId: SubjectId;
  subjectName: string;
  packageNumber: number;
  title: string;
  description: string;
  expectedQuestionCount: number;
  idMapel?: number;
  schemaVersion: string;
  questions: QuestionItem[];
}

export type UserAnswer =
  | {
      type: 'PG';
      selectedOptionId: string | null;
    }
  | {
      type: 'PG_KOMPLEKS';
      selectedOptionIds: string[];
    }
  | {
      type: 'BENAR_SALAH';
      statementSelections: Record<string, number>;
    };

export type UserAnswersMap = Record<string, UserAnswer>;

export type QuestionGradeStatus = 'CORRECT' | 'INCORRECT' | 'UNANSWERED' | 'UNKEYED';

export interface QuestionEvaluation {
  questionId: string;
  questionNumber: number;
  type: QuestionType;
  answered: boolean;
  hasKey: boolean;
  status: QuestionGradeStatus;
  statementResults?: Record<string, boolean>;
}

export interface TypeBreakdown {
  type: QuestionType;
  total: number;
  answered: number;
  keyed: number;
  correct: number;
}

export interface QuizEvaluation {
  packageId: PackageId;
  totalQuestions: number;
  answeredCount: number;
  keyedCount: number;
  unkeyedCount: number;
  correctCount: number;
  incorrectCount: number;
  scorePercent: number | null;
  completionPercent: number;
  byType: Record<QuestionType, TypeBreakdown>;
  questionResults: QuestionEvaluation[];
}

export interface PackageProgress {
  packageId: PackageId;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';
  currentQuestionIndex: number;
  answers: UserAnswersMap;
  lastScorePercent: number | null;
  lastCorrectCount: number;
  lastKeyedCount: number;
  lastAnsweredCount: number;
  totalQuestions: number;
  attempts: number;
  updatedAt: string;
}
