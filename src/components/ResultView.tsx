import React from 'react';
import {
  ArrowLeft,
  CheckCircle2,
  ClipboardList,
  FileClock,
  ShieldAlert,
  ShieldCheck,
} from 'lucide-react';
import { getExamReviewStatus } from '../engine/examSession';
import { PackageProgress, QuestionPackage, QuestionType, QuizEvaluation } from '../engine/types';

interface ResultViewProps {
  pkg: QuestionPackage;
  evaluation: QuizEvaluation;
  progress?: PackageProgress;
  onReviewAnswers: () => void;
  onViewActivityLog: () => void;
  onBackHome: () => void;
}

const TYPE_LABELS: Record<QuestionType, string> = {
  PG: 'Pilihan Ganda',
  PG_KOMPLEKS: 'PG Kompleks (MCMA)',
  BENAR_SALAH: 'Benar / Salah',
};

function formatDateTime(iso?: string | null): string {
  if (!iso) return '—';
  const dt = new Date(iso);
  if (Number.isNaN(dt.getTime())) return iso;
  return dt.toLocaleString('id-ID', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

export const ResultView: React.FC<ResultViewProps> = ({
  pkg,
  evaluation,
  progress,
  onReviewAnswers,
  onViewActivityLog,
  onBackHome,
}) => {
  const hasKeyedQuestions = evaluation.keyedCount > 0;
  const unansweredCount = evaluation.totalQuestions - evaluation.answeredCount;
  const answeredIncorrectCount = Math.max(0, evaluation.answeredCount - evaluation.correctCount);
  const violationCount = progress?.violationCount ?? 0;
  const reviewStatus = getExamReviewStatus(violationCount);

  return (
    <div className="min-h-screen bg-[#F6F8FB] text-[#111827]">
      <header className="sticky top-0 z-30 bg-white border-b border-slate-200">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center justify-between gap-4">
          <button
            type="button"
            onClick={onBackHome}
            className="inline-flex items-center gap-1.5 px-3 py-2 min-h-[40px] rounded-xl text-sm font-medium text-slate-700 hover:bg-slate-100"
          >
            <ArrowLeft className="w-4 h-4" aria-hidden="true" />
            <span>Beranda</span>
          </button>
          <h1 className="text-sm sm:text-base font-bold text-slate-900 truncate">
            Hasil Ujian — {pkg.subjectName} {pkg.title}
          </h1>
          <div className="w-16" aria-hidden="true" />
        </div>
      </header>

      <main className="max-w-3xl mx-auto px-4 py-6 sm:py-8 space-y-6">
        <section
          aria-label="Ringkasan hasil"
          className="bg-white border border-slate-200 rounded-2xl p-6 space-y-5"
        >
          <div className="flex items-start justify-between gap-4">
            <div className="space-y-1">
              <p className="text-xs font-medium text-slate-500">
                {pkg.subjectName} · {pkg.title}
              </p>
              <h2 className="text-xl font-bold text-slate-900">
                {hasKeyedQuestions ? 'Skor Ujian Anda' : 'Ujian Selesai'}
              </h2>
            </div>
            <CheckCircle2 className="w-7 h-7 text-emerald-600 shrink-0" aria-hidden="true" />
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2">
            <div className="rounded-xl bg-[#EEF4FF] border border-blue-200 p-4">
              <div className="text-xs text-slate-600">Skor Akhir</div>
              <div
                data-testid="result-score-value"
                className="text-3xl font-bold text-[#1D4ED8] tabular-nums mt-1"
              >
                {evaluation.scorePercent !== null ? `${evaluation.scorePercent}` : '—'}
              </div>
              <div className="text-xs text-slate-600 mt-1 tabular-nums">
                Dari {evaluation.totalQuestions} soal
              </div>
            </div>

            <div className="rounded-xl bg-emerald-50/70 border border-emerald-200 p-4">
              <div className="text-xs text-emerald-800">Jawaban Benar</div>
              <div
                data-testid="result-correct-value"
                className="text-3xl font-bold text-emerald-800 tabular-nums mt-1"
              >
                {evaluation.correctCount}
              </div>
              <div className="text-xs text-emerald-700 mt-1 tabular-nums">
                Tepat sesuai kunci
              </div>
            </div>

            <div className="rounded-xl bg-rose-50/70 border border-rose-200 p-4">
              <div className="text-xs text-rose-800">Jawaban Salah</div>
              <div
                data-testid="result-incorrect-value"
                className="text-3xl font-bold text-rose-800 tabular-nums mt-1"
              >
                {answeredIncorrectCount}
              </div>
              <div className="text-xs text-rose-700 mt-1 tabular-nums">
                Total salah/kosong: {evaluation.incorrectCount}
              </div>
            </div>

            <div className="rounded-xl bg-[#F8FAFC] border border-slate-200 p-4">
              <div className="text-xs text-slate-600">Belum Dijawab</div>
              <div
                data-testid="result-unanswered-value"
                className="text-3xl font-bold text-slate-900 tabular-nums mt-1"
              >
                {unansweredCount}
              </div>
              <div className="text-xs text-slate-600 mt-1 tabular-nums">
                Dijawab: {evaluation.answeredCount}/{evaluation.totalQuestions}
              </div>
            </div>
          </div>

          <div
            data-testid="result-supervisor-banner"
            className={`rounded-xl border p-4 flex flex-wrap items-center justify-between gap-3 ${
              reviewStatus === 'NEEDS_SUPERVISOR_REVIEW'
                ? 'bg-rose-50 border-rose-300 text-rose-900'
                : 'bg-slate-50 border-slate-200 text-slate-800'
            }`}
          >
            <div className="flex items-center gap-2.5 text-sm font-semibold">
              {reviewStatus === 'NEEDS_SUPERVISOR_REVIEW' ? (
                <ShieldAlert className="w-5 h-5 text-rose-700 shrink-0" aria-hidden="true" />
              ) : (
                <ShieldCheck className="w-5 h-5 text-emerald-700 shrink-0" aria-hidden="true" />
              )}
              <span>
                Status Pengawasan:{' '}
                {reviewStatus === 'NEEDS_SUPERVISOR_REVIEW'
                  ? 'Perlu Ditinjau Pengawas'
                  : 'Normal'}{' '}
                ({violationCount} pelanggaran)
              </span>
            </div>
            <div className="text-xs text-slate-600 tabular-nums">
              Mulai: {formatDateTime(progress?.examStartedAt)} · Selesai:{' '}
              {formatDateTime(progress?.examCompletedAt)}
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-3 pt-2 border-t border-slate-100">
            <button
              type="button"
              onClick={onReviewAnswers}
              className="inline-flex items-center justify-center gap-2 px-5 py-2.5 min-h-[44px] rounded-xl bg-[#1D4ED8] text-white text-sm font-semibold hover:bg-[#1E40AF] transition-colors"
            >
              <ClipboardList className="w-4 h-4" aria-hidden="true" />
              <span>Tinjau Jawaban</span>
            </button>

            <button
              type="button"
              onClick={onViewActivityLog}
              className="inline-flex items-center justify-center gap-2 px-5 py-2.5 min-h-[44px] rounded-xl border border-slate-300 bg-white text-sm font-medium text-slate-800 hover:bg-slate-50 transition-colors"
            >
              <FileClock className="w-4 h-4" aria-hidden="true" />
              <span>Riwayat Aktivitas Ujian</span>
            </button>

            <button
              type="button"
              onClick={onBackHome}
              className="inline-flex items-center justify-center px-4 py-2.5 min-h-[44px] rounded-xl text-sm font-medium text-slate-600 hover:bg-slate-100 sm:ml-auto"
            >
              Kembali ke Beranda
            </button>
          </div>
        </section>

        <section
          aria-label="Rincian per tipe soal"
          className="bg-white border border-slate-200 rounded-2xl p-6 space-y-4"
        >
          <h3 className="text-base font-bold text-slate-900">Rincian per Tipe Soal</h3>
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left border-collapse tabular-nums">
              <thead>
                <tr className="border-b border-slate-200 text-xs text-slate-500">
                  <th className="py-2.5 pr-4 font-semibold">Tipe Soal</th>
                  <th className="py-2.5 px-3 font-semibold text-right">Total</th>
                  <th className="py-2.5 px-3 font-semibold text-right">Dijawab</th>
                  <th className="py-2.5 pl-3 font-semibold text-right">Benar</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {(['PG', 'PG_KOMPLEKS', 'BENAR_SALAH'] as QuestionType[])
                  .filter((type) => evaluation.byType[type].total > 0)
                  .map((type) => {
                    const row = evaluation.byType[type];
                    return (
                      <tr key={type}>
                        <td className="py-3 pr-4 font-medium text-slate-900">{TYPE_LABELS[type]}</td>
                        <td className="py-3 px-3 text-right text-slate-700">{row.total}</td>
                        <td className="py-3 px-3 text-right text-slate-700">{row.answered}</td>
                        <td className="py-3 pl-3 text-right font-semibold text-slate-900">
                          {row.keyed > 0 ? `${row.correct}/${row.keyed}` : '—'}
                        </td>
                      </tr>
                    );
                  })}
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </div>
  );
};
