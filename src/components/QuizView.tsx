import React, { useEffect, useRef, useState } from 'react';
import {
  AlertTriangle,
  ArrowLeft,
  ArrowRight,
  Check,
  Clock,
  Grid2x2,
  Send,
  X,
} from 'lucide-react';
import { formatTimer } from '../engine/examSession';
import { resolveImageUrl } from '../engine/htmlUtils';
import {
  isQuestionAnswered,
  setStatementChoiceSelection,
  togglePgKompleksOption,
} from '../engine/scoring';
import {
  ActivityEventType,
  QuestionItem,
  QuestionPackage,
  UserAnswer,
  UserAnswersMap,
} from '../engine/types';
import { RichHtmlRenderer } from './RichHtmlRenderer';

interface QuizViewProps {
  pkg: QuestionPackage;
  currentIndex: number;
  answers: UserAnswersMap;
  remainingSeconds: number;
  violationCount: number;
  warningMessage: string | null;
  onDismissWarning: () => void;
  onIndexChange: (nextIndex: number) => void;
  onUpdateAnswer: (questionId: string, answer: UserAnswer) => void;
  onRecordActivityEvent: (type: ActivityEventType, detail: string, warningText?: string) => void;
  onSubmitQuiz: () => void;
}

function getTypeLabel(question: QuestionItem): string {
  switch (question.type) {
    case 'PG':
      return 'Pilihan Ganda';
    case 'PG_KOMPLEKS':
      return 'PG Kompleks (MCMA)';
    case 'BENAR_SALAH': {
      const choices = question.statementChoices ?? [];
      if (choices.length >= 2) {
        return choices.map((c) => c.label).join(' / ');
      }
      return 'Benar / Salah';
    }
  }
}

export const QuizView: React.FC<QuizViewProps> = ({
  pkg,
  currentIndex,
  answers,
  remainingSeconds,
  violationCount,
  warningMessage,
  onDismissWarning,
  onIndexChange,
  onUpdateAnswer,
  onRecordActivityEvent,
  onSubmitQuiz,
}) => {
  const [showNavigator, setShowNavigator] = useState(false);
  const [showSubmitConfirm, setShowSubmitConfirm] = useState(false);

  const leftActiveRef = useRef(false);
  const lastLeaveAtRef = useRef(0);
  const onRecordRef = useRef(onRecordActivityEvent);
  onRecordRef.current = onRecordActivityEvent;

  // Browser-level activity monitoring using visibilitychange + blur (indicator only)
  useEffect(() => {
    const handleLeave = (source: string) => {
      const now = Date.now();
      if (leftActiveRef.current || now - lastLeaveAtRef.current < 800) {
        return;
      }
      leftActiveRef.current = true;
      lastLeaveAtRef.current = now;
      onRecordRef.current(
        'APP_LEFT_FOREGROUND',
        `Halaman ujian kehilangan fokus/visibilitas (${source}).`,
      );
    };

    const handleReturn = () => {
      if (!leftActiveRef.current) return;
      leftActiveRef.current = false;
      onRecordRef.current(
        'EXAM_RESUMED_AFTER_LEAVE',
        'Ujian dilanjutkan setelah halaman ujian kembali aktif.',
        'Aktivitas meninggalkan tab/jendela ujian telah dicatat. Jawaban Anda tetap tersimpan.',
      );
    };

    const onVisibilityChange = () => {
      if (document.hidden) {
        handleLeave('visibilitychange');
      } else {
        handleReturn();
      }
    };

    const onBlur = () => {
      handleLeave('window blur');
    };

    const onFocus = () => {
      if (!document.hidden) {
        handleReturn();
      }
    };

    // Guard browser Back button navigation during active exam
    window.history.pushState({ examLock: true }, '', window.location.href);
    const onPopState = () => {
      window.history.pushState({ examLock: true }, '', window.location.href);
      onRecordRef.current(
        'ATTEMPT_EXIT_NAVIGATION',
        'Percobaan keluar dari halaman ujian melalui tombol Back browser.',
        'Navigasi keluar dinonaktifkan selama Exam Mode. Gunakan tombol Kumpulkan untuk menyelesaikan ujian.',
      );
    };

    document.addEventListener('visibilitychange', onVisibilityChange);
    window.addEventListener('blur', onBlur);
    window.addEventListener('focus', onFocus);
    window.addEventListener('popstate', onPopState);

    return () => {
      document.removeEventListener('visibilitychange', onVisibilityChange);
      window.removeEventListener('blur', onBlur);
      window.removeEventListener('focus', onFocus);
      window.removeEventListener('popstate', onPopState);
    };
  }, []);

  const questions = pkg.questions;
  const safeIndex = Math.max(0, Math.min(currentIndex, questions.length - 1));
  const question = questions[safeIndex];
  const currentAnswer = answers[question.id];

  const answeredCount = questions.filter((q) => isQuestionAnswered(q, answers[q.id])).length;
  const progressPercent = Math.round((answeredCount / questions.length) * 100);
  const isLast = safeIndex === questions.length - 1;
  const isLowTime = remainingSeconds > 0 && remainingSeconds <= 300;

  const resolvedImageUrl = question.image?.url ? resolveImageUrl(question.image.url) : null;
  const statementChoices = question.statementChoices ?? [
    { id: 1, order: 1, label: 'Benar' },
    { id: 2, order: 2, label: 'Salah' },
  ];

  const handleSelectPg = (optionId: string) => {
    onUpdateAnswer(question.id, {
      type: 'PG',
      selectedOptionId: optionId,
    });
  };

  const handleTogglePgKompleks = (optionId: string) => {
    const existing =
      currentAnswer?.type === 'PG_KOMPLEKS' ? currentAnswer.selectedOptionIds : [];
    onUpdateAnswer(question.id, {
      type: 'PG_KOMPLEKS',
      selectedOptionIds: togglePgKompleksOption(existing, optionId),
    });
  };

  const handleSelectStatementChoice = (statementId: string, choiceOrder: number) => {
    const existing =
      currentAnswer?.type === 'BENAR_SALAH' ? currentAnswer.statementSelections : {};
    onUpdateAnswer(question.id, {
      type: 'BENAR_SALAH',
      statementSelections: setStatementChoiceSelection(existing, statementId, choiceOrder),
    });
  };

  return (
    <div className="min-h-screen bg-[#F6F8FB] text-[#111827] flex flex-col">
      <header className="sticky top-0 z-30 bg-white border-b border-slate-200">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center justify-between gap-3">
          <div className="min-w-0">
            <div className="text-xs text-slate-500 truncate">
              {pkg.subjectName} — {pkg.title}
            </div>
            <div
              data-testid="quiz-question-counter"
              className="text-sm font-bold text-slate-900 tabular-nums"
            >
              Soal {safeIndex + 1} / {questions.length}
            </div>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            <div
              data-testid="exam-timer"
              className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl font-mono text-sm font-bold tabular-nums border ${
                isLowTime
                  ? 'bg-rose-50 border-rose-300 text-rose-800'
                  : 'bg-[#EEF4FF] border-blue-200 text-[#1D4ED8]'
              }`}
            >
              <Clock className="w-4 h-4" aria-hidden="true" />
              <span>{formatTimer(remainingSeconds)}</span>
            </div>

            <button
              type="button"
              onClick={() => setShowNavigator(true)}
              className="inline-flex items-center gap-1.5 px-3 py-2 min-h-[40px] rounded-xl border border-slate-300 bg-white text-sm font-medium text-slate-800 hover:bg-slate-50 transition-colors whitespace-nowrap"
            >
              <Grid2x2 className="w-4 h-4 text-[#1D4ED8]" aria-hidden="true" />
              <span className="hidden sm:inline">Daftar Soal</span>
            </button>
          </div>
        </div>

        <div className="h-1 w-full bg-slate-100">
          <div
            className="h-full bg-[#1D4ED8] transition-all duration-200"
            style={{ width: `${progressPercent}%` }}
          />
        </div>

        <div className="bg-slate-50 border-t border-slate-200/80">
          <div className="max-w-4xl mx-auto px-4 py-1.5 flex flex-wrap items-center justify-between gap-2 text-[11px] font-semibold">
            <div className="flex flex-wrap items-center gap-2 text-slate-700">
              <span data-testid="exam-mode-badge" className="text-[#1D4ED8]">
                EXAM MODE ACTIVE
              </span>
              <span aria-hidden="true">·</span>
              <span className="text-amber-800">MONITORING MODE ONLY</span>
            </div>
            <span
              data-testid="exam-violation-counter"
              className={`tabular-nums ${violationCount > 0 ? 'text-rose-700' : 'text-slate-600'}`}
            >
              Pelanggaran: {violationCount}
            </span>
          </div>
        </div>
      </header>

      <main className="flex-1 max-w-3xl w-full mx-auto px-4 py-6 pb-28 space-y-5">
        {warningMessage && (
          <div
            role="alert"
            data-testid="exam-warning-banner"
            className="bg-rose-50 border border-rose-300 rounded-2xl px-4 py-3 flex items-start justify-between gap-3 text-rose-900"
          >
            <div className="flex items-start gap-2.5 text-sm">
              <AlertTriangle className="w-4 h-4 text-rose-700 shrink-0 mt-0.5" aria-hidden="true" />
              <div>
                <div className="text-xs font-bold uppercase tracking-wide text-rose-800">WARNING</div>
                <p className="text-xs sm:text-sm mt-0.5">{warningMessage}</p>
              </div>
            </div>
            <button
              type="button"
              onClick={onDismissWarning}
              className="text-xs font-semibold text-rose-800 hover:underline shrink-0"
            >
              Tutup
            </button>
          </div>
        )}

        <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-slate-600">
          <div className="flex flex-wrap items-center gap-2 tabular-nums">
            <span className="font-semibold text-slate-900">Nomor {question.number}</span>
            <span aria-hidden="true">·</span>
            <span className="font-mono">ID: {question.id}</span>
            <span aria-hidden="true">·</span>
            <span className="font-medium text-[#1D4ED8]">{getTypeLabel(question)}</span>
          </div>
          <span className="tabular-nums">
            {answeredCount}/{questions.length} terjawab
          </span>
        </div>

        {question.stimulusHtml && (
          <section
            aria-label="Stimulus soal"
            className="bg-white border border-slate-200 rounded-2xl p-5"
          >
            <RichHtmlRenderer html={question.stimulusHtml} />
          </section>
        )}

        {resolvedImageUrl && question.image && (
          <figure className="bg-white border border-slate-200 rounded-2xl p-4 space-y-2">
            <img
              src={resolvedImageUrl}
              alt={question.image.alt}
              referrerPolicy="no-referrer"
              className="max-w-full h-auto rounded-xl mx-auto"
            />
            {question.image.caption && (
              <figcaption className="text-xs text-center text-slate-500">
                {question.image.caption}
              </figcaption>
            )}
          </figure>
        )}

        <section className="bg-white border border-slate-200 rounded-2xl p-5 space-y-4">
          <RichHtmlRenderer html={question.questionHtml} className="text-base" />

          {question.mathFormula && (
            <div className="bg-slate-50 border border-slate-200 rounded-xl px-4 py-3 font-mono text-sm text-slate-900 overflow-x-auto">
              {question.mathFormula}
            </div>
          )}

          <div className="pt-2 space-y-3">
            {question.type === 'PG' && (
              <fieldset className="space-y-2.5">
                <legend className="text-xs font-semibold text-slate-500 mb-2">
                  Pilih satu jawaban yang paling tepat:
                </legend>
                {(question.options ?? []).map((opt) => {
                  const selected =
                    currentAnswer?.type === 'PG' && currentAnswer.selectedOptionId === opt.id;
                  return (
                    <label
                      key={opt.id}
                      data-testid={`pg-option-${opt.id}`}
                      className={`flex items-start gap-3.5 p-3.5 min-h-[52px] rounded-xl border cursor-pointer transition-colors ${
                        selected
                          ? 'bg-[#EEF4FF] border-[#1D4ED8]'
                          : 'bg-white border-slate-200 hover:bg-slate-50'
                      }`}
                    >
                      <input
                        type="radio"
                        name={`pg-${question.id}`}
                        value={opt.id}
                        checked={selected}
                        onChange={() => handleSelectPg(opt.id)}
                        className="mt-1 h-4 w-4 accent-[#1D4ED8] shrink-0"
                      />
                      <span className="font-semibold text-sm text-slate-900 w-5 shrink-0 pt-0.5">
                        {opt.label}.
                      </span>
                      <div className="flex-1 min-w-0 text-sm text-slate-800">
                        <RichHtmlRenderer html={opt.html} />
                      </div>
                    </label>
                  );
                })}
              </fieldset>
            )}

            {question.type === 'PG_KOMPLEKS' && (
              <fieldset className="space-y-2.5">
                <legend className="text-xs font-semibold text-slate-500 mb-2">
                  Pilih lebih dari satu jawaban yang benar (MCMA):
                </legend>
                {(question.options ?? []).map((opt) => {
                  const selectedIds =
                    currentAnswer?.type === 'PG_KOMPLEKS' ? currentAnswer.selectedOptionIds : [];
                  const checked = selectedIds.includes(opt.id);
                  return (
                    <label
                      key={opt.id}
                      data-testid={`mcma-option-${opt.id}`}
                      className={`flex items-start gap-3.5 p-3.5 min-h-[52px] rounded-xl border cursor-pointer transition-colors ${
                        checked
                          ? 'bg-[#EEF4FF] border-[#1D4ED8]'
                          : 'bg-white border-slate-200 hover:bg-slate-50'
                      }`}
                    >
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => handleTogglePgKompleks(opt.id)}
                        className="mt-1 h-4 w-4 rounded accent-[#1D4ED8] shrink-0"
                      />
                      <span className="font-semibold text-sm text-slate-900 w-5 shrink-0 pt-0.5">
                        {opt.label}.
                      </span>
                      <div className="flex-1 min-w-0 text-sm text-slate-800">
                        <RichHtmlRenderer html={opt.html} />
                      </div>
                    </label>
                  );
                })}
              </fieldset>
            )}

            {question.type === 'BENAR_SALAH' && (
              <div className="space-y-3">
                <div className="text-xs font-semibold text-slate-500">
                  Tentukan {statementChoices.map((c) => c.label).join(' atau ')} pada masing-masing
                  pernyataan:
                </div>
                <div className="divide-y divide-slate-200 border border-slate-200 rounded-xl overflow-hidden">
                  {(question.statements ?? []).map((stmt, idx) => {
                    const selections =
                      currentAnswer?.type === 'BENAR_SALAH'
                        ? currentAnswer.statementSelections
                        : {};
                    const selectedOrder = selections[stmt.id];
                    const stmtLabel = stmt.label ?? `Pernyataan ${idx + 1}`;
                    return (
                      <div
                        key={stmt.id}
                        data-testid={`statement-row-${stmt.key}`}
                        className="p-4 bg-white flex flex-col gap-3"
                      >
                        <div className="space-y-1">
                          <div className="text-xs font-semibold text-slate-500">{stmtLabel}</div>
                          <RichHtmlRenderer html={stmt.html} className="text-sm" />
                        </div>

                        <div
                          className="flex flex-wrap items-center gap-2"
                          role="group"
                          aria-label={stmtLabel}
                        >
                          {statementChoices.map((choice) => {
                            const isSelected = selectedOrder === choice.order;
                            return (
                              <button
                                key={choice.id}
                                type="button"
                                aria-pressed={isSelected}
                                aria-label={`${stmtLabel}: ${choice.label}`}
                                onClick={() => handleSelectStatementChoice(stmt.id, choice.order)}
                                className={`px-4 py-2 min-h-[44px] rounded-xl text-sm font-semibold border transition-colors ${
                                  isSelected
                                    ? 'bg-[#1D4ED8] border-[#1D4ED8] text-white'
                                    : 'bg-white border-slate-300 text-slate-700 hover:bg-slate-50'
                                }`}
                              >
                                {choice.label}
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        </section>
      </main>

      <footer className="fixed bottom-0 left-0 right-0 z-30 bg-white border-t border-slate-200">
        <div className="max-w-4xl mx-auto px-4 h-16 flex items-center justify-between gap-3">
          <button
            type="button"
            disabled={safeIndex === 0}
            onClick={() => onIndexChange(safeIndex - 1)}
            className="inline-flex items-center justify-center gap-1.5 px-4 py-2.5 min-h-[44px] rounded-xl border border-slate-300 bg-white text-sm font-medium text-slate-800 hover:bg-slate-50 disabled:opacity-40 disabled:pointer-events-none whitespace-nowrap"
          >
            <ArrowLeft className="w-4 h-4" aria-hidden="true" />
            <span>Sebelumnya</span>
          </button>

          <button
            type="button"
            onClick={() => setShowSubmitConfirm(true)}
            className="hidden sm:inline-flex items-center justify-center gap-1.5 px-4 py-2.5 min-h-[44px] rounded-xl text-sm font-medium text-[#1D4ED8] hover:bg-[#EEF4FF] whitespace-nowrap"
          >
            <Check className="w-4 h-4" aria-hidden="true" />
            <span>Selesaikan & Kumpulkan</span>
          </button>

          {isLast ? (
            <button
              type="button"
              onClick={() => setShowSubmitConfirm(true)}
              className="inline-flex items-center justify-center gap-2 px-5 py-2.5 min-h-[44px] rounded-xl bg-[#1D4ED8] text-white text-sm font-semibold hover:bg-[#1E40AF] whitespace-nowrap"
            >
              <Send className="w-4 h-4" aria-hidden="true" />
              <span>Kumpulkan</span>
            </button>
          ) : (
            <button
              type="button"
              onClick={() => onIndexChange(safeIndex + 1)}
              className="inline-flex items-center justify-center gap-1.5 px-5 py-2.5 min-h-[44px] rounded-xl bg-[#1D4ED8] text-white text-sm font-semibold hover:bg-[#1E40AF] whitespace-nowrap"
            >
              <span>Selanjutnya</span>
              <ArrowRight className="w-4 h-4" aria-hidden="true" />
            </button>
          )}
        </div>
      </footer>

      {showNavigator && (
        <div
          className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-end sm:items-center justify-center p-0 sm:p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="navigator-title"
        >
          <div className="bg-white border border-slate-200 rounded-t-3xl sm:rounded-2xl w-full max-w-lg max-h-[85vh] flex flex-col overflow-hidden">
            <div className="px-5 py-4 border-b border-slate-200 flex items-center justify-between">
              <div>
                <h2 id="navigator-title" className="text-base font-bold text-slate-900">
                  Daftar Nomor Soal
                </h2>
                <p className="text-xs text-slate-500 tabular-nums">
                  {answeredCount} dari {questions.length} soal sudah dijawab
                </p>
              </div>
              <button
                type="button"
                onClick={() => setShowNavigator(false)}
                aria-label="Tutup daftar soal"
                className="w-10 h-10 rounded-xl flex items-center justify-center text-slate-500 hover:bg-slate-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-5 overflow-y-auto grid grid-cols-5 sm:grid-cols-6 gap-2.5">
              {questions.map((q, idx) => {
                const answered = isQuestionAnswered(q, answers[q.id]);
                const isCurrent = idx === safeIndex;
                return (
                  <button
                    key={q.id}
                    type="button"
                    aria-label={`Buka soal nomor ${q.number}`}
                    onClick={() => {
                      onIndexChange(idx);
                      setShowNavigator(false);
                    }}
                    className={`h-11 rounded-xl text-sm font-semibold tabular-nums border transition-colors ${
                      isCurrent
                        ? 'bg-[#1D4ED8] border-[#1D4ED8] text-white'
                        : answered
                          ? 'bg-emerald-50 border-emerald-300 text-emerald-900 hover:bg-emerald-100'
                          : 'bg-white border-slate-300 text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    {q.number}
                  </button>
                );
              })}
            </div>

            <div className="px-5 py-4 border-t border-slate-200 flex items-center justify-between gap-3">
              <div className="text-xs text-slate-600">Biru: Aktif · Hijau: Dijawab</div>
              <button
                type="button"
                onClick={() => {
                  setShowNavigator(false);
                  setShowSubmitConfirm(true);
                }}
                className="px-4 py-2.5 min-h-[44px] rounded-xl bg-[#1D4ED8] text-white text-sm font-semibold hover:bg-[#1E40AF]"
              >
                Kumpulkan Jawaban
              </button>
            </div>
          </div>
        </div>
      )}

      {showSubmitConfirm && (
        <div
          className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="submit-confirm-title"
        >
          <div className="bg-white border border-slate-200 rounded-2xl w-full max-w-md p-6 space-y-4">
            <h2 id="submit-confirm-title" className="text-lg font-bold text-slate-900">
              Kumpulkan Jawaban Ujian?
            </h2>
            <p className="text-sm text-slate-600 leading-relaxed tabular-nums">
              Anda telah menjawab <strong>{answeredCount}</strong> dari{' '}
              <strong>{questions.length}</strong> soal pada {pkg.subjectName} — {pkg.title}.
              {answeredCount < questions.length
                ? ` Masih ada ${questions.length - answeredCount} soal yang belum dijawab.`
                : ''}{' '}
              Setelah dikumpulkan, Exam Mode berakhir dan ujian tidak dapat diulang kembali.
            </p>
            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setShowSubmitConfirm(false)}
                className="px-4 py-2.5 min-h-[44px] rounded-xl border border-slate-300 bg-white text-sm font-medium text-slate-700 hover:bg-slate-50"
              >
                Periksa Kembali
              </button>
              <button
                type="button"
                onClick={() => {
                  setShowSubmitConfirm(false);
                  onSubmitQuiz();
                }}
                className="px-5 py-2.5 min-h-[44px] rounded-xl bg-[#1D4ED8] text-white text-sm font-semibold hover:bg-[#1E40AF]"
              >
                Ya, Kumpulkan
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
