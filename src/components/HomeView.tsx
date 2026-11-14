import React from 'react';
import { BookOpen, Calculator, CheckCircle2, ClipboardList, Play, ShieldAlert } from 'lucide-react';
import { SubjectInfo } from '../data/repository';
import { getExamReviewStatus } from '../engine/examSession';
import { ProgressMap } from '../engine/storage';
import { PackageId, PackageProgress, QuestionPackage } from '../engine/types';

interface HomeViewProps {
  subjects: SubjectInfo[];
  packages: Record<PackageId, QuestionPackage>;
  progressMap: ProgressMap;
  onSelectPackage: (packageId: PackageId) => void;
  onViewResultPackage: (packageId: PackageId) => void;
  onReviewPackage: (packageId: PackageId) => void;
  onViewActivityLog: (packageId: PackageId) => void;
}

function getProgressLabel(pkg: QuestionPackage, progress?: PackageProgress): string {
  if (pkg.questions.length === 0) {
    return 'Belum tersedia';
  }
  if (!progress || progress.status === 'NOT_STARTED') {
    return 'Belum dimulai';
  }
  if (progress.status === 'IN_PROGRESS') {
    return `Ujian Berlangsung · ${progress.lastAnsweredCount}/${pkg.questions.length} dijawab`;
  }
  if (progress.lastKeyedCount > 0 && progress.lastScorePercent !== null) {
    return `Selesai · Skor ${progress.lastScorePercent}`;
  }
  return `Selesai · ${progress.lastAnsweredCount}/${pkg.questions.length} dijawab`;
}

export const HomeView: React.FC<HomeViewProps> = ({
  subjects,
  packages,
  progressMap,
  onSelectPackage,
  onViewResultPackage,
  onReviewPackage,
  onViewActivityLog,
}) => {
  const allPackageList = Object.values(packages);
  const completedCount = allPackageList.filter(
    (pkg) => progressMap[pkg.packageId]?.status === 'COMPLETED',
  ).length;
  const totalQuestions = allPackageList.reduce((acc, pkg) => acc + pkg.questions.length, 0);

  return (
    <div className="min-h-screen bg-[#F6F8FB] text-[#111827]">
      <header className="sticky top-0 z-30 bg-white/95 backdrop-blur-sm border-b border-slate-200">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3 min-w-0">
            <h1 className="text-lg sm:text-xl font-bold tracking-tight text-slate-900 whitespace-nowrap">
              TKA SMP 2027
            </h1>
            <span className="hidden sm:inline text-slate-300" aria-hidden="true">
              |
            </span>
            <span className="hidden sm:inline text-sm text-slate-600 truncate">
              Ujian Digital (Exam Mode)
            </span>
          </div>

          <div className="text-xs font-medium text-slate-600 tabular-nums whitespace-nowrap">
            {completedCount}/{allPackageList.length} paket selesai · {totalQuestions} soal tersedia
          </div>
        </div>
      </header>

      <main className="max-w-5xl mx-auto px-4 sm:px-6 py-6 sm:py-8 space-y-8">
        <section
          aria-label="Ringkasan ujian"
          className="bg-white border border-slate-200 rounded-2xl p-5 sm:p-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"
        >
          <div className="space-y-1.5">
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="text-base sm:text-lg font-semibold text-slate-900">
                Ujian Digital TKA SMP 2027
              </h2>
              <span className="inline-flex items-center px-2 py-0.5 rounded-md bg-amber-50 border border-amber-200 text-[11px] font-bold text-amber-800">
                MONITORING MODE ONLY
              </span>
            </div>
            <p className="text-sm text-slate-600 leading-relaxed max-w-2xl">
              Mendukung soal Pilihan Ganda, PG Kompleks (MCMA), dan Benar/Salah dengan timer waktu
              nyata, penyimpanan jawaban otomatis, dan catatan aktivitas ujian.
            </p>
          </div>
          <div className="flex items-center gap-6 shrink-0 border-t sm:border-t-0 pt-3 sm:pt-0 border-slate-100">
            <div>
              <div className="text-xs text-slate-500">Paket Selesai</div>
              <div className="text-xl font-bold text-slate-900 tabular-nums">
                {completedCount} / {allPackageList.length}
              </div>
            </div>
            <div>
              <div className="text-xs text-slate-500">Total Soal</div>
              <div className="text-xl font-bold text-slate-900 tabular-nums">
                {totalQuestions}
              </div>
            </div>
          </div>
        </section>

        {subjects.map((subject) => {
          const SubjectIcon = subject.id === 'matematika' ? Calculator : BookOpen;
          return (
            <section key={subject.id} aria-labelledby={`subject-${subject.id}`} className="space-y-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-[#E8F0FE] text-[#1D4ED8] flex items-center justify-center shrink-0">
                  <SubjectIcon className="w-5 h-5" aria-hidden="true" />
                </div>
                <div>
                  <h2 id={`subject-${subject.id}`} className="text-lg font-bold text-slate-900">
                    {subject.name}
                  </h2>
                  <p className="text-xs sm:text-sm text-slate-600">{subject.subtitle}</p>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {subject.packageIds.map((packageId) => {
                  const pkg = packages[packageId];
                  const progress = progressMap[packageId];
                  const total = pkg.questions.length;
                  const answered = progress?.lastAnsweredCount ?? 0;
                  const percent = total > 0 ? Math.round((answered / total) * 100) : 0;
                  const isCompleted = progress?.status === 'COMPLETED' && total > 0;
                  const isInProgress = progress?.status === 'IN_PROGRESS' && total > 0;
                  const violations = progress?.violationCount ?? 0;
                  const reviewStatus = getExamReviewStatus(violations);

                  return (
                    <article
                      key={packageId}
                      data-testid={`package-card-${packageId}`}
                      className="bg-white border border-slate-200 rounded-2xl p-5 flex flex-col justify-between gap-5"
                    >
                      <div className="space-y-3">
                        <div className="flex items-start justify-between gap-3">
                          <div>
                            <h3 className="text-base font-semibold text-slate-900">
                              {pkg.subjectName} — {pkg.title}
                            </h3>
                            <p className="text-xs text-slate-500 mt-1 tabular-nums">
                              {total} Soal
                              <span className="mx-1.5" aria-hidden="true">
                                ·
                              </span>
                              {getProgressLabel(pkg, progress)}
                            </p>
                          </div>
                          {isCompleted && (
                            <CheckCircle2
                              className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5"
                              aria-label="Ujian selesai"
                            />
                          )}
                        </div>

                        {progress && violations > 0 && (
                          <div className="inline-flex items-center gap-1.5 text-xs font-semibold text-rose-700">
                            <ShieldAlert className="w-3.5 h-3.5" aria-hidden="true" />
                            <span>
                              {reviewStatus === 'NEEDS_SUPERVISOR_REVIEW'
                                ? `Perlu Ditinjau Pengawas (${violations} pelanggaran)`
                                : `Aktivitas tercatat: ${violations} pelanggaran`}
                            </span>
                          </div>
                        )}

                        <p className="text-sm text-slate-600 leading-relaxed">{pkg.description}</p>

                        <div className="space-y-1.5">
                          <div className="flex items-center justify-between text-xs text-slate-500 tabular-nums">
                            <span>Progres Pengerjaan</span>
                            <span>
                              {answered}/{total} ({percent}%)
                            </span>
                          </div>
                          <div className="h-2 w-full bg-slate-100 rounded-full overflow-hidden">
                            <div
                              className={`h-full rounded-full transition-all ${
                                isCompleted ? 'bg-emerald-600' : 'bg-[#1D4ED8]'
                              }`}
                              style={{ width: `${percent}%` }}
                            />
                          </div>
                        </div>
                      </div>

                      <div className="flex flex-wrap items-center gap-2 pt-2 border-t border-slate-100">
                        {!isCompleted ? (
                          <button
                            type="button"
                            disabled={total === 0}
                            onClick={() => onSelectPackage(packageId)}
                            className="inline-flex items-center justify-center gap-2 px-4 py-2.5 min-h-[44px] rounded-xl bg-[#1D4ED8] text-white text-sm font-semibold hover:bg-[#1E40AF] active:bg-[#1E3A8A] disabled:opacity-40 disabled:pointer-events-none transition-colors whitespace-nowrap"
                          >
                            <Play className="w-4 h-4" aria-hidden="true" />
                            <span>{isInProgress ? 'Lanjutkan Ujian' : 'Mulai Ujian'}</span>
                          </button>
                        ) : (
                          <>
                            <button
                              type="button"
                              onClick={() => onViewResultPackage(packageId)}
                              className="inline-flex items-center justify-center px-4 py-2.5 min-h-[44px] rounded-xl bg-[#1D4ED8] text-white text-sm font-semibold hover:bg-[#1E40AF] transition-colors whitespace-nowrap"
                            >
                              Lihat Hasil
                            </button>
                            <button
                              type="button"
                              onClick={() => onReviewPackage(packageId)}
                              className="inline-flex items-center justify-center px-4 py-2.5 min-h-[44px] rounded-xl border border-slate-300 bg-white text-sm font-medium text-slate-800 hover:bg-slate-50 transition-colors whitespace-nowrap"
                            >
                              Tinjau Jawaban
                            </button>
                          </>
                        )}

                        {progress && (
                          <button
                            type="button"
                            onClick={() => onViewActivityLog(packageId)}
                            className="inline-flex items-center justify-center gap-1.5 px-3 py-2.5 min-h-[44px] rounded-xl text-xs font-semibold text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition-colors ml-auto whitespace-nowrap"
                          >
                            <ClipboardList className="w-3.5 h-3.5" aria-hidden="true" />
                            <span>Riwayat</span>
                          </button>
                        )}
                      </div>
                    </article>
                  );
                })}
              </div>
            </section>
          );
        })}
      </main>
    </div>
  );
};
