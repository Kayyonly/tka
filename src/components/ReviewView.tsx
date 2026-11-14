import React, { useState } from 'react';
import {
  ArrowLeft,
  CheckCircle2,
  Clock,
  FileClock,
  MinusCircle,
  XCircle,
} from 'lucide-react';
import {
  QuestionGradeStatus,
  QuestionItem,
  QuestionPackage,
  QuizEvaluation,
  UserAnswer,
  UserAnswersMap,
} from '../engine/types';
import { RichHtmlRenderer } from './RichHtmlRenderer';

interface ReviewViewProps {
  pkg: QuestionPackage;
  answers: UserAnswersMap;
  evaluation: QuizEvaluation;
  onBackToResult: () => void;
  onViewActivityLog: () => void;
  onBackHome: () => void;
}

type ReviewFilter = 'ALL' | 'ANSWERED' | 'UNANSWERED' | 'CORRECT' | 'INCORRECT';

function getStatusDisplay(status: QuestionGradeStatus) {
  switch (status) {
    case 'CORRECT':
      return {
        label: 'Benar',
        className: 'text-emerald-800 bg-emerald-50 border-emerald-200',
        Icon: CheckCircle2,
      };
    case 'INCORRECT':
      return {
        label: 'Salah',
        className: 'text-rose-800 bg-rose-50 border-rose-200',
        Icon: XCircle,
      };
    case 'UNANSWERED':
      return {
        label: 'Belum Dijawab',
        className: 'text-slate-700 bg-slate-100 border-slate-200',
        Icon: MinusCircle,
      };
    case 'UNKEYED':
      return {
        label: 'Menunggu Kunci Resmi',
        className: 'text-amber-800 bg-amber-50 border-amber-200',
        Icon: Clock,
      };
  }
}

function getChoiceLabel(question: QuestionItem, order?: number): string {
  if (typeof order !== 'number') return '—';
  const found = (question.statementChoices ?? []).find((c) => c.order === order);
  return found ? found.label : String(order);
}

function formatAnswerSummary(question: QuestionItem, answer?: UserAnswer): string {
  if (!answer || answer.type !== question.type) {
    return 'Tidak dijawab';
  }
  if (answer.type === 'PG') {
    return answer.selectedOptionId ? `Pilihan ${answer.selectedOptionId}` : 'Tidak dijawab';
  }
  if (answer.type === 'PG_KOMPLEKS') {
    return answer.selectedOptionIds.length > 0
      ? `Pilihan ${[...answer.selectedOptionIds].sort().join(', ')}`
      : 'Tidak dijawab';
  }
  const statements = question.statements ?? [];
  const entries = statements.map((stmt, idx) => {
    const val = answer.statementSelections[stmt.id];
    const label = stmt.key || `P${idx + 1}`;
    return `${label}: ${getChoiceLabel(question, val)}`;
  });
  return entries.join(' · ');
}

function formatKeySummary(question: QuestionItem): string {
  const key = question.answerKey;
  if (!key) return 'Belum tersedia';
  if (key.type === 'PG') {
    return key.correctOptionId ? `Pilihan ${key.correctOptionId}` : 'Belum tersedia';
  }
  if (key.type === 'PG_KOMPLEKS') {
    return key.correctOptionIds && key.correctOptionIds.length > 0
      ? `Pilihan ${[...key.correctOptionIds].sort().join(', ')}`
      : 'Belum tersedia';
  }
  const stmtAnswers = key.statementAnswers;
  if (!stmtAnswers) return 'Belum tersedia';
  return (question.statements ?? [])
    .map((stmt, idx) => {
      const val = stmtAnswers[stmt.id];
      const label = stmt.key || `P${idx + 1}`;
      return `${label}: ${getChoiceLabel(question, val)}`;
    })
    .join(' · ');
}

export const ReviewView: React.FC<ReviewViewProps> = ({
  pkg,
  answers,
  evaluation,
  onBackToResult,
  onViewActivityLog,
  onBackHome,
}) => {
  const [filter, setFilter] = useState<ReviewFilter>('ALL');

  const resultById = new Map(evaluation.questionResults.map((r) => [r.questionId, r]));

  const filteredQuestions = pkg.questions.filter((q) => {
    const res = resultById.get(q.id);
    if (!res) return true;
    switch (filter) {
      case 'ANSWERED':
        return res.answered;
      case 'UNANSWERED':
        return !res.answered;
      case 'CORRECT':
        return res.status === 'CORRECT';
      case 'INCORRECT':
        return res.status === 'INCORRECT';
      default:
        return true;
    }
  });

  return (
    <div className="min-h-screen bg-[#F6F8FB] text-[#111827]">
      <header className="sticky top-0 z-30 bg-white border-b border-slate-200">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center justify-between gap-3">
          <button
            type="button"
            onClick={onBackToResult}
            className="inline-flex items-center gap-1.5 px-3 py-2 min-h-[40px] rounded-xl text-sm font-medium text-slate-700 hover:bg-slate-100 whitespace-nowrap"
          >
            <ArrowLeft className="w-4 h-4" aria-hidden="true" />
            <span>Hasil</span>
          </button>

          <h1 className="text-sm sm:text-base font-bold text-slate-900 truncate">
            Tinjauan Jawaban — {pkg.subjectName} {pkg.title}
          </h1>

          <button
            type="button"
            onClick={onViewActivityLog}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 min-h-[40px] rounded-xl border border-slate-300 bg-white text-slate-800 text-sm font-semibold hover:bg-slate-50 whitespace-nowrap"
          >
            <FileClock className="w-4 h-4" aria-hidden="true" />
            <span>Riwayat</span>
          </button>
        </div>
      </header>

      <main className="max-w-3xl mx-auto px-4 py-6 space-y-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-1 p-1 bg-slate-200/70 rounded-xl">
            {(
              [
                { id: 'ALL', label: `Semua (${pkg.questions.length})` },
                { id: 'ANSWERED', label: `Dijawab (${evaluation.answeredCount})` },
                {
                  id: 'UNANSWERED',
                  label: `Kosong (${evaluation.totalQuestions - evaluation.answeredCount})`,
                },
                ...(evaluation.keyedCount > 0
                  ? [
                      { id: 'CORRECT' as const, label: `Benar (${evaluation.correctCount})` },
                      { id: 'INCORRECT' as const, label: `Salah (${evaluation.incorrectCount})` },
                    ]
                  : []),
              ] as Array<{ id: ReviewFilter; label: string }>
            ).map((tab) => (
              <button
                key={tab.id}
                type="button"
                onClick={() => setFilter(tab.id)}
                className={`px-3 py-1.5 min-h-[36px] text-xs font-semibold rounded-lg transition-colors whitespace-nowrap ${
                  filter === tab.id
                    ? 'bg-white text-slate-900 shadow-xs'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          <button
            type="button"
            onClick={onBackHome}
            className="text-xs font-medium text-[#1D4ED8] hover:underline"
          >
            Kembali ke Beranda
          </button>
        </div>

        {filteredQuestions.length === 0 ? (
          <div className="bg-white border border-slate-200 rounded-2xl p-8 text-center text-sm text-slate-600">
            Tidak ada soal pada filter ini.
          </div>
        ) : (
          <div className="space-y-4">
            {filteredQuestions.map((question) => {
              const res = resultById.get(question.id);
              const statusDisplay = getStatusDisplay(res?.status ?? 'UNKEYED');
              const StatusIcon = statusDisplay.Icon;
              const userAnswer = answers[question.id];

              return (
                <article
                  key={question.id}
                  className="bg-white border border-slate-200 rounded-2xl p-5 space-y-4"
                >
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div className="text-xs text-slate-600 tabular-nums">
                      <span className="font-bold text-slate-900">Nomor {question.number}</span>
                      <span className="mx-1.5" aria-hidden="true">
                        ·
                      </span>
                      <span className="font-mono">ID: {question.id}</span>
                      <span className="mx-1.5" aria-hidden="true">
                        ·
                      </span>
                      <span>{question.type}</span>
                    </div>

                    <div
                      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg border text-xs font-semibold ${statusDisplay.className}`}
                    >
                      <StatusIcon className="w-3.5 h-3.5" aria-hidden="true" />
                      <span>{statusDisplay.label}</span>
                    </div>
                  </div>

                  {question.stimulusHtml && (
                    <div className="bg-slate-50 border border-slate-200 rounded-xl p-4 text-sm">
                      <RichHtmlRenderer html={question.stimulusHtml} />
                    </div>
                  )}

                  <RichHtmlRenderer html={question.questionHtml} className="text-sm sm:text-base" />

                  {question.options && question.options.length > 0 && (
                    <div className="space-y-1.5">
                      {question.options.map((opt) => {
                        const isCorrectOpt =
                          (question.answerKey?.type === 'PG' &&
                            question.answerKey.correctOptionId === opt.id) ||
                          (question.answerKey?.type === 'PG_KOMPLEKS' &&
                            (question.answerKey.correctOptionIds ?? []).includes(opt.id));
                        const isSelectedOpt =
                          (userAnswer?.type === 'PG' && userAnswer.selectedOptionId === opt.id) ||
                          (userAnswer?.type === 'PG_KOMPLEKS' &&
                            userAnswer.selectedOptionIds.includes(opt.id));

                        return (
                          <div
                            key={opt.id}
                            className={`flex items-start gap-2.5 px-3 py-2 rounded-xl border text-xs sm:text-sm ${
                              isCorrectOpt
                                ? 'bg-emerald-50/70 border-emerald-300'
                                : isSelectedOpt
                                  ? 'bg-rose-50/70 border-rose-300'
                                  : 'bg-white border-slate-200'
                            }`}
                          >
                            <span className="font-semibold text-slate-900 w-5 shrink-0 pt-0.5">
                              {opt.label}.
                            </span>
                            <div className="flex-1 min-w-0">
                              <RichHtmlRenderer html={opt.html} />
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}

                  {question.statements && question.statements.length > 0 && (
                    <div className="divide-y divide-slate-200 border border-slate-200 rounded-xl overflow-hidden">
                      {question.statements.map((stmt, idx) => {
                        const userChoiceOrder =
                          userAnswer?.type === 'BENAR_SALAH'
                            ? userAnswer.statementSelections[stmt.id]
                            : undefined;
                        const keyChoiceOrder =
                          question.answerKey?.type === 'BENAR_SALAH'
                            ? question.answerKey.statementAnswers?.[stmt.id]
                            : undefined;
                        const isStmtCorrect =
                          typeof userChoiceOrder === 'number' &&
                          typeof keyChoiceOrder === 'number' &&
                          userChoiceOrder === keyChoiceOrder;

                        return (
                          <div key={stmt.id} className="p-3 bg-white space-y-1.5 text-xs sm:text-sm">
                            <div className="flex items-center justify-between gap-2">
                              <span className="font-semibold text-slate-600">
                                {stmt.label ?? `Pernyataan ${idx + 1}`}
                              </span>
                              <span
                                className={`font-semibold ${
                                  typeof userChoiceOrder !== 'number'
                                    ? 'text-slate-500'
                                    : isStmtCorrect
                                      ? 'text-emerald-700'
                                      : 'text-rose-700'
                                }`}
                              >
                                {typeof userChoiceOrder !== 'number'
                                  ? 'Kosong'
                                  : isStmtCorrect
                                    ? 'Tepat'
                                    : 'Kurang Tepat'}
                              </span>
                            </div>
                            <RichHtmlRenderer html={stmt.html} />
                            <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-600 pt-1">
                              <span>
                                Jawaban Anda:{' '}
                                <strong className="text-slate-900">
                                  {getChoiceLabel(question, userChoiceOrder)}
                                </strong>
                              </span>
                              <span>
                                Kunci:{' '}
                                <strong className="text-emerald-800">
                                  {getChoiceLabel(question, keyChoiceOrder)}
                                </strong>
                              </span>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2 border-t border-slate-100 text-sm">
                    <div className="rounded-xl bg-slate-50 border border-slate-200 p-3 space-y-1">
                      <div className="text-xs font-semibold text-slate-500">Jawaban Anda</div>
                      <div className="font-medium text-slate-900">
                        {formatAnswerSummary(question, userAnswer)}
                      </div>
                    </div>

                    <div className="rounded-xl bg-slate-50 border border-slate-200 p-3 space-y-1">
                      <div className="text-xs font-semibold text-slate-500">Kunci Jawaban</div>
                      <div className="font-medium text-slate-900">
                        {formatKeySummary(question)}
                      </div>
                    </div>
                  </div>

                  {question.explanationHtml && (
                    <div className="rounded-xl bg-[#F8FAFC] border border-slate-200 p-3.5 text-sm space-y-1">
                      <div className="text-xs font-semibold text-slate-600">Pembahasan</div>
                      <RichHtmlRenderer html={question.explanationHtml} className="text-sm" />
                    </div>
                  )}
                </article>
              );
            })}
          </div>
        )}
      </main>
    </div>
  );
};
