import React from 'react';
import { ArrowLeft, Clock, Info, ShieldAlert } from 'lucide-react';
import { defaultExamDurationSeconds } from '../engine/examSession';
import { QuestionPackage } from '../engine/types';

interface ExamPrepViewProps {
  pkg: QuestionPackage;
  onConfirmStartExam: () => void;
  onCancelBack: () => void;
}

export const ExamPrepView: React.FC<ExamPrepViewProps> = ({
  pkg,
  onConfirmStartExam,
  onCancelBack,
}) => {
  const durationMinutes = Math.round(defaultExamDurationSeconds(pkg.questions.length) / 60);

  return (
    <div className="min-h-screen bg-[#F6F8FB] text-[#111827]">
      <header className="sticky top-0 z-30 bg-white border-b border-slate-200">
        <div className="max-w-4xl mx-auto px-4 h-14 flex items-center justify-between gap-4">
          <button
            type="button"
            onClick={onCancelBack}
            className="inline-flex items-center gap-1.5 px-3 py-2 min-h-[40px] rounded-xl text-sm font-medium text-slate-700 hover:bg-slate-100"
          >
            <ArrowLeft className="w-4 h-4" aria-hidden="true" />
            <span>Kembali</span>
          </button>
          <h1 className="text-sm sm:text-base font-bold text-slate-900 truncate">
            Persiapan Ujian
          </h1>
          <div className="w-16" aria-hidden="true" />
        </div>
      </header>

      <main className="max-w-3xl mx-auto px-4 py-6 sm:py-8 space-y-5">
        <section className="bg-white border border-slate-200 rounded-2xl p-6 space-y-4">
          <div>
            <p className="text-xs font-medium text-slate-500">{pkg.subjectName}</p>
            <h2 className="text-xl font-bold text-slate-900 mt-0.5">
              {pkg.subjectName} — {pkg.title}
            </h2>
            <p className="text-sm text-slate-600 mt-1">{pkg.description}</p>
          </div>

          <div className="grid grid-cols-2 gap-3 pt-3 border-t border-slate-100">
            <div className="rounded-xl bg-[#F8FAFC] border border-slate-200 p-4">
              <div className="text-xs text-slate-500">Jumlah Soal</div>
              <div className="text-xl font-bold text-slate-900 tabular-nums mt-1">
                {pkg.questions.length} Soal
              </div>
            </div>
            <div className="rounded-xl bg-[#F8FAFC] border border-slate-200 p-4">
              <div className="text-xs text-slate-500 flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5" aria-hidden="true" />
                <span>Durasi Ujian</span>
              </div>
              <div className="text-xl font-bold text-slate-900 tabular-nums mt-1">
                {durationMinutes} Menit
              </div>
            </div>
          </div>
        </section>

        <section className="bg-white border border-slate-200 rounded-2xl p-6 space-y-3">
          <div className="flex items-center justify-between gap-2 flex-wrap">
            <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
              <ShieldAlert className="w-4 h-4 text-amber-600" aria-hidden="true" />
              <span>Mode Pengawasan Browser</span>
            </h3>
            <span className="inline-flex items-center px-2.5 py-1 rounded-md bg-amber-50 border border-amber-200 text-xs font-bold text-amber-800">
              MONITORING MODE ONLY
            </span>
          </div>
          <p className="text-sm text-slate-600 leading-relaxed">
            Pada versi web browser, aplikasi menggunakan deteksi{' '}
            <code className="font-mono text-xs bg-slate-100 px-1 py-0.5 rounded">visibilitychange</code>{' '}
            dan <code className="font-mono text-xs bg-slate-100 px-1 py-0.5 rounded">blur</code>{' '}
            sebagai indikator aktivitas meninggalkan halaman ujian. Browser web tidak dapat mengunci
            perangkat secara penuh atau mencegah pembukaan tab lain (penguncian penuh Lock Task Mode
            tersedia pada aplikasi Android dengan konfigurasi DPC).
          </p>
        </section>

        <section className="bg-white border border-slate-200 rounded-2xl p-6 space-y-3">
          <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
            <Info className="w-4 h-4 text-[#1D4ED8]" aria-hidden="true" />
            <span>Petunjuk Pengerjaan</span>
          </h3>
          <ol className="list-decimal pl-5 space-y-1.5 text-sm text-slate-700 leading-relaxed">
            <li>Setiap jawaban yang Anda pilih langsung tersimpan otomatis di perangkat.</li>
            <li>Timer berjalan berdasarkan waktu nyata dan sisa waktu tetap terhitung jika halaman dibuka ulang.</li>
            <li>Navigasi keluar ke Beranda dinonaktifkan selama Exam Mode berlangsung.</li>
            <li>
              Aktivitas berpindah tab/jendela atau percobaan keluar akan tercatat di riwayat pengawas.
              Ujian yang sudah dimulai atau dikumpulkan tidak dapat diulang peserta untuk menghapus
              catatan aktivitas.
            </li>
          </ol>
        </section>

        <div className="flex flex-wrap items-center gap-3 pt-1">
          <button
            type="button"
            onClick={onCancelBack}
            className="px-5 py-2.5 min-h-[44px] rounded-xl border border-slate-300 bg-white text-sm font-medium text-slate-800 hover:bg-slate-50"
          >
            Batal
          </button>
          <button
            type="button"
            data-testid="confirm-start-exam-btn"
            onClick={onConfirmStartExam}
            className="px-6 py-2.5 min-h-[44px] rounded-xl bg-[#1D4ED8] text-white text-sm font-semibold hover:bg-[#1E40AF]"
          >
            Konfirmasi & Mulai Ujian
          </button>
        </div>
      </main>
    </div>
  );
};
