import React, { useState } from 'react';
import { ArrowLeft, ShieldAlert, ShieldCheck } from 'lucide-react';
import {
  getExamReviewStatus,
  isViolationEvent,
  PROCTOR_RESET_PIN,
  SUPERVISOR_REVIEW_THRESHOLD,
} from '../engine/examSession';
import { PackageProgress, QuestionPackage } from '../engine/types';

interface ActivityLogViewProps {
  pkg: QuestionPackage;
  progress?: PackageProgress;
  onBack: () => void;
  onProctorResetSession: () => void;
}

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

export const ActivityLogView: React.FC<ActivityLogViewProps> = ({
  pkg,
  progress,
  onBack,
  onProctorResetSession,
}) => {
  const [showProctorModal, setShowProctorModal] = useState(false);
  const [pinInput, setPinInput] = useState('');
  const [pinError, setPinError] = useState<string | null>(null);

  const violationCount = progress?.violationCount ?? 0;
  const reviewStatus = getExamReviewStatus(violationCount);
  const events = progress?.activityLog ?? [];

  return (
    <div className="min-h-screen bg-[#F6F8FB] text-[#111827]">
      <header className="sticky top-0 z-30 bg-white border-b border-slate-200">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center justify-between gap-4">
          <button
            type="button"
            onClick={onBack}
            className="inline-flex items-center gap-1.5 px-3 py-2 min-h-[40px] rounded-xl text-sm font-medium text-slate-700 hover:bg-slate-100"
          >
            <ArrowLeft className="w-4 h-4" aria-hidden="true" />
            <span>Kembali</span>
          </button>
          <h1 className="text-sm sm:text-base font-bold text-slate-900 truncate">
            Riwayat Aktivitas Ujian
          </h1>
          <div className="w-16" aria-hidden="true" />
        </div>
      </header>

      <main className="max-w-3xl mx-auto px-4 py-6 space-y-5">
        <section className="bg-white border border-slate-200 rounded-2xl p-6 space-y-4">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 className="text-lg font-bold text-slate-900">
                {pkg.subjectName} — {pkg.title}
              </h2>
              <p className="text-xs text-slate-500 mt-1">
                Mode Pemantauan: MONITORING MODE ONLY (Browser)
              </p>
            </div>
            <div
              data-testid="activity-review-status"
              className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border text-xs font-bold ${
                reviewStatus === 'NEEDS_SUPERVISOR_REVIEW'
                  ? 'bg-rose-50 border-rose-300 text-rose-800'
                  : 'bg-emerald-50 border-emerald-200 text-emerald-800'
              }`}
            >
              {reviewStatus === 'NEEDS_SUPERVISOR_REVIEW' ? (
                <ShieldAlert className="w-4 h-4" aria-hidden="true" />
              ) : (
                <ShieldCheck className="w-4 h-4" aria-hidden="true" />
              )}
              <span>
                {reviewStatus === 'NEEDS_SUPERVISOR_REVIEW'
                  ? 'Perlu Ditinjau Pengawas'
                  : 'Status Normal'}
              </span>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-sm pt-2 border-t border-slate-100">
            <div className="rounded-xl bg-slate-50 border border-slate-200 p-3">
              <div className="text-xs text-slate-500">Pelanggaran Tercatat</div>
              <div
                data-testid="activity-violation-count"
                className="text-xl font-bold text-slate-900 tabular-nums mt-0.5"
              >
                {violationCount}
              </div>
              <div className="text-[11px] text-slate-500 mt-0.5">
                Batas tinjauan: {SUPERVISOR_REVIEW_THRESHOLD}x
              </div>
            </div>
            <div className="rounded-xl bg-slate-50 border border-slate-200 p-3">
              <div className="text-xs text-slate-500">Waktu Mulai</div>
              <div className="text-xs font-semibold text-slate-900 mt-1 tabular-nums">
                {formatDateTime(progress?.examStartedAt)}
              </div>
            </div>
            <div className="rounded-xl bg-slate-50 border border-slate-200 p-3">
              <div className="text-xs text-slate-500">Waktu Selesai</div>
              <div className="text-xs font-semibold text-slate-900 mt-1 tabular-nums">
                {formatDateTime(progress?.examCompletedAt)}
              </div>
            </div>
          </div>

          {progress && (
            <div className="pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => {
                  setPinInput('');
                  setPinError(null);
                  setShowProctorModal(true);
                }}
                className="px-4 py-2 min-h-[40px] rounded-xl border border-slate-300 bg-white text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                Prosedur Pengawas: Buka Ulang Sesi
              </button>
            </div>
          )}
        </section>

        <section className="space-y-3">
          <h3 className="text-sm font-bold text-slate-900">
            Log Kejadian ({events.length})
          </h3>
          {events.length === 0 ? (
            <div className="bg-white border border-slate-200 rounded-2xl p-6 text-sm text-slate-600">
              Belum ada aktivitas yang tercatat pada sesi ini.
            </div>
          ) : (
            <div className="space-y-2.5">
              {[...events].reverse().map((ev, idx) => {
                const violation = isViolationEvent(ev.type);
                return (
                  <article
                    key={`${ev.timestamp}-${idx}`}
                    className={`rounded-2xl border p-4 space-y-1 ${
                      violation
                        ? 'bg-rose-50/50 border-rose-200'
                        : 'bg-white border-slate-200'
                    }`}
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <span
                        className={`text-xs font-bold ${
                          violation ? 'text-rose-800' : 'text-[#1D4ED8]'
                        }`}
                      >
                        {ev.label}
                      </span>
                      <span className="text-xs text-slate-500 font-mono tabular-nums">
                        {formatDateTime(ev.timestamp)}
                      </span>
                    </div>
                    <p className="text-sm text-slate-700">{ev.detail}</p>
                  </article>
                );
              })}
            </div>
          )}
        </section>
      </main>

      {showProctorModal && (
        <div
          className="fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="proctor-reset-title"
        >
          <div className="bg-white border border-slate-200 rounded-2xl w-full max-w-md p-6 space-y-4">
            <h2 id="proctor-reset-title" className="text-lg font-bold text-slate-900">
              Prosedur Resmi Pengawas
            </h2>
            <p className="text-sm text-slate-600 leading-relaxed">
              Hanya guru atau pengawas yang berwenang membuka ulang ujian atau mereset catatan
              aktivitas sesi ini.
            </p>
            <div className="space-y-1.5">
              <label htmlFor="proctor-pin-input" className="block text-xs font-semibold text-slate-700">
                Kode Pengawas
              </label>
              <input
                id="proctor-pin-input"
                type="password"
                value={pinInput}
                onChange={(e) => {
                  setPinInput(e.target.value);
                  setPinError(null);
                }}
                placeholder="Masukkan kode pengawas"
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-sm focus:outline-hidden focus:border-[#1D4ED8]"
              />
              {pinError && <p className="text-xs font-medium text-rose-700">{pinError}</p>}
            </div>
            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setShowProctorModal(false)}
                className="px-4 py-2 min-h-[40px] rounded-xl border border-slate-300 bg-white text-sm font-medium text-slate-700 hover:bg-slate-50"
              >
                Batal
              </button>
              <button
                type="button"
                onClick={() => {
                  if (pinInput.trim() === PROCTOR_RESET_PIN) {
                    setShowProctorModal(false);
                    onProctorResetSession();
                  } else {
                    setPinError('Kode pengawas tidak valid.');
                  }
                }}
                className="px-5 py-2 min-h-[40px] rounded-xl bg-[#1D4ED8] text-white text-sm font-semibold hover:bg-[#1E40AF]"
              >
                Reset Sesi
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
