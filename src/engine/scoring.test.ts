import test from 'node:test';
import assert from 'node:assert/strict';
import { getBundledPackages, getPackageById } from '../data/repository';
import { hasMeaningfulHtmlContent, resolveImageUrl, transformEducationalHtml } from './htmlUtils';
import { mapJenisSoalToQuestionType, normalizeRawQuestion } from './questionNormalizer';
import {
  evaluateQuiz,
  gradeQuestion,
  hasValidAnswerKey,
  isQuestionAnswered,
  setStatementChoiceSelection,
  togglePgKompleksOption,
} from './scoring';
import { QuestionItem, QuestionPackage } from './types';

test('bundled official database loads all 4 separate packages totaling 115 questions sorted by no_soal', () => {
  const packages = getBundledPackages();
  assert.equal(Object.keys(packages).length, 4);

  const mtk1 = getPackageById('matematika_paket_1');
  const mtk2 = getPackageById('matematika_paket_2');
  const bindo1 = getPackageById('bahasa_indonesia_paket_1');
  const bindo2 = getPackageById('bahasa_indonesia_paket_2');

  assert.equal(mtk1.subjectId, 'matematika');
  assert.equal(mtk2.subjectId, 'matematika');
  assert.equal(bindo1.subjectId, 'bahasa_indonesia');
  assert.equal(bindo2.subjectId, 'bahasa_indonesia');

  assert.equal(mtk1.questions.length, 30);
  assert.equal(mtk2.questions.length, 30);
  assert.equal(bindo1.questions.length, 30);
  assert.equal(bindo2.questions.length, 25);

  const total = Object.values(packages).reduce((sum, pkg) => sum + pkg.questions.length, 0);
  assert.equal(total, 115);

  for (const pkg of Object.values(packages)) {
    const numbers = pkg.questions.map((q) => q.number);
    const expectedNumbers = Array.from({ length: pkg.questions.length }, (_, i) => i + 1);
    assert.deepEqual(numbers, expectedNumbers, `${pkg.packageId} must be sorted by no_soal 1..N`);

    for (const q of pkg.questions) {
      assert.equal(hasValidAnswerKey(q), true, `${pkg.packageId} soal ${q.number} missing key`);
      assert.equal(
        hasMeaningfulHtmlContent(q.questionHtml),
        true,
        `${pkg.packageId} soal ${q.number} missing questionHtml`,
      );
      if (q.type === 'PG' || q.type === 'PG_KOMPLEKS') {
        assert.ok((q.options?.length ?? 0) >= 4, `${pkg.packageId} soal ${q.number} needs options`);
      }
      if (q.type === 'BENAR_SALAH') {
        assert.ok(
          (q.statements?.length ?? 0) >= 2,
          `${pkg.packageId} soal ${q.number} needs statements`,
        );
        assert.ok(
          (q.statementChoices?.length ?? 0) >= 2,
          `${pkg.packageId} soal ${q.number} needs statementChoices`,
        );
      }
    }
  }

  // Bahasa Indonesia Paket 2 uses 5 options (A-E) on PG questions
  const bindo2Pg = bindo2.questions.find((q) => q.type === 'PG');
  assert.equal(bindo2Pg?.options?.length, 5);
  assert.deepEqual(
    bindo2Pg?.options?.map((o) => o.id),
    ['A', 'B', 'C', 'D', 'E'],
  );

  // Dynamic cbt_soal_answer_judul labels are preserved (e.g. Fungsi / Bukan Fungsi, Setuju / Tidak Setuju)
  const fungsiSoal = mtk2.questions.find((q) => q.number === 7);
  assert.deepEqual(
    fungsiSoal?.statementChoices?.map((c) => c.label),
    ['Fungsi', 'Bukan Fungsi'],
  );
  const setujuSoal = bindo1.questions.find((q) => q.number === 6);
  assert.deepEqual(
    setujuSoal?.statementChoices?.map((c) => c.label),
    ['Setuju', 'Tidak Setuju'],
  );
});

test('mapJenisSoalToQuestionType maps jenis_soal 1, 2, 3 accurately', () => {
  assert.deepEqual(mapJenisSoalToQuestionType('1'), { jenis_soal: '1', type: 'PG' });
  assert.deepEqual(mapJenisSoalToQuestionType('2'), { jenis_soal: '2', type: 'PG_KOMPLEKS' });
  assert.deepEqual(mapJenisSoalToQuestionType('3'), { jenis_soal: '3', type: 'BENAR_SALAH' });
});

test('normalizeRawQuestion parses official opsi_a..opsi_f, cbt_soal_answer_judul, and kunci_essai', () => {
  const rawType3 = {
    id_soal: 287,
    no_soal: 6,
    jenis_soal: '3',
    soal: '<p>Teks informasi.</p>',
    pertanyaan: '<p>Tentukan Setuju atau Tidak Setuju.</p>',
    cbt_soal_answer_judul: [
      { id: 2723, description: 'Tidak Setuju', order_soal: 2 },
      { id: 2722, description: 'Setuju', order_soal: 1 },
    ],
    cbt_soal_answer_text: [
      { id: 1400, judul: 'Pernyataan', text: '<?xml encoding="utf-8" ?><!--?xml encoding="utf-8" ?--><p>Pernyataan satu</p>' },
      { id: 1401, judul: 'Pernyataan', text: '<?xml encoding="utf-8" ?><p>Pernyataan dua</p>' },
    ],
    kunci_essai: 'A|1;B|2',
  };

  const normalized = normalizeRawQuestion(rawType3, 5, 'bahasa_indonesia_paket_1');
  assert.equal(normalized.id, '287');
  assert.equal(normalized.number, 6);
  assert.equal(normalized.type, 'BENAR_SALAH');
  assert.deepEqual(
    normalized.statementChoices?.map((c) => `${c.order}:${c.label}`),
    ['1:Setuju', '2:Tidak Setuju'],
  );
  assert.deepEqual(normalized.answerKey, {
    type: 'BENAR_SALAH',
    statementAnswers: { '1400': 1, '1401': 2 },
  });
});

test('resolveImageUrl and transformEducationalHtml resolve relative URLs, strip XML PIs, and preserve aspect ratio', () => {
  assert.equal(
    resolveImageUrl('/tka/cbt_images/24751_0cb56d66a423d68b293a4ff189160301.png'),
    'https://pusmendik.kemendikdasmen.go.id/tka/cbt_images/24751_0cb56d66a423d68b293a4ff189160301.png',
  );
  assert.equal(
    resolveImageUrl('https://pusmendik.kemendikdasmen.go.id/tka/cbt_images/56579.jpg'),
    'https://pusmendik.kemendikdasmen.go.id/tka/cbt_images/56579.jpg',
  );

  const transformed = transformEducationalHtml(
    '<?xml encoding="utf-8" ?><!--comment--><p>Diagram:</p><img src="/tka/cbt_images/bagan.png" width="558" height="504" style="width: 900px; height: 269px;">',
  );
  assert.ok(!transformed.includes('<?xml'));
  assert.ok(!transformed.includes('<!--comment-->'));
  assert.match(transformed, /src="https:\/\/pusmendik\.kemendikdasmen\.go\.id\/tka\/cbt_images\/bagan\.png"/);
  assert.ok(!transformed.includes('height="504"'));
  assert.ok(!transformed.includes('height: 269px'));
});

test('PG, PG Kompleks, and dynamic Benar/Salah grading works accurately', () => {
  const pg: QuestionItem = {
    id: 'Q1',
    number: 1,
    jenis_soal: '1',
    type: 'PG',
    questionHtml: '<p>2 + 3 = ...</p>',
    options: [
      { id: 'A', label: 'A', html: '4' },
      { id: 'B', label: 'B', html: '5' },
    ],
    answerKey: { type: 'PG', correctOptionId: 'B' },
  };
  assert.equal(isQuestionAnswered(pg, undefined), false);
  assert.equal(gradeQuestion(pg, { type: 'PG', selectedOptionId: 'B' }).status, 'CORRECT');

  const mcma: QuestionItem = {
    id: 'Q2',
    number: 2,
    jenis_soal: '2',
    type: 'PG_KOMPLEKS',
    questionHtml: '<p>Pilih bilangan prima.</p>',
    options: [
      { id: 'A', label: 'A', html: '2' },
      { id: 'B', label: 'B', html: '3' },
      { id: 'C', label: 'C', html: '4' },
    ],
    answerKey: { type: 'PG_KOMPLEKS', correctOptionIds: ['A', 'B'] },
  };
  let selections = togglePgKompleksOption([], 'B');
  selections = togglePgKompleksOption(selections, 'A');
  assert.equal(
    gradeQuestion(mcma, { type: 'PG_KOMPLEKS', selectedOptionIds: selections }).status,
    'CORRECT',
  );

  const bs: QuestionItem = {
    id: 'Q3',
    number: 3,
    jenis_soal: '3',
    type: 'BENAR_SALAH',
    questionHtml: '<p>Tentukan benar atau salah.</p>',
    statementChoices: [
      { id: 1, order: 1, label: 'Benar' },
      { id: 2, order: 2, label: 'Salah' },
    ],
    statements: [
      { id: '2037', key: 'A', html: '5 > 3' },
      { id: '2038', key: 'B', html: '2 > 7' },
    ],
    answerKey: { type: 'BENAR_SALAH', statementAnswers: { '2037': 1, '2038': 2 } },
  };
  let bsMap = setStatementChoiceSelection({}, '2037', 1);
  bsMap = setStatementChoiceSelection(bsMap, '2038', 2);
  assert.equal(
    gradeQuestion(bs, { type: 'BENAR_SALAH', statementSelections: bsMap }).status,
    'CORRECT',
  );

  const pkg: QuestionPackage = {
    packageId: 'matematika_paket_1',
    subjectId: 'matematika',
    subjectName: 'Matematika',
    packageNumber: 1,
    title: 'Paket 1',
    description: 'Uji',
    expectedQuestionCount: 3,
    schemaVersion: '1.0',
    questions: [pg, mcma, bs],
  };
  const result = evaluateQuiz(pkg, {
    Q1: { type: 'PG', selectedOptionId: 'B' },
    Q2: { type: 'PG_KOMPLEKS', selectedOptionIds: ['A'] },
    Q3: { type: 'BENAR_SALAH', statementSelections: bsMap },
  });
  assert.equal(result.scorePercent, 67);
  assert.equal(result.correctCount, 2);
  assert.equal(result.incorrectCount, 1);
});

import {
  finalizeExamProgress,
  findActiveExamPackageId,
  formatTimer,
  getExamReviewStatus,
  getRemainingSeconds,
  recordExamActivityEvent,
  startExamProgress,
  updateExamAnswersAndIndex,
} from './examSession';

test('examSession tracks real-time timer, violations, supervisor review status, and preserves answers', () => {
  const pkg = getPackageById('matematika_paket_1');
  const startMs = 1_700_000_000_000;
  let progress = startExamProgress(pkg, undefined, startMs);

  assert.equal(progress.status, 'IN_PROGRESS');
  assert.equal(progress.examDurationSeconds, 3600);
  assert.equal(getRemainingSeconds(progress.examDeadlineAt, startMs + 65_000), 3535);
  assert.equal(formatTimer(3535), '58:55');
  assert.equal(findActiveExamPackageId({ matematika_paket_1: progress }), 'matematika_paket_1');

  const q1 = pkg.questions[0];
  progress = updateExamAnswersAndIndex(
    pkg,
    progress,
    { [q1.id]: { type: 'PG', selectedOptionId: 'A' } },
    1,
    startMs + 70_000,
  );
  assert.equal(progress.lastAnsweredCount, 1);
  assert.equal(progress.currentQuestionIndex, 1);

  progress = recordExamActivityEvent(progress, 'APP_LEFT_FOREGROUND', 'Left 1', startMs + 80_000);
  progress = recordExamActivityEvent(progress, 'EXAM_RESUMED_AFTER_LEAVE', 'Back 1', startMs + 82_000);
  progress = recordExamActivityEvent(progress, 'ATTEMPT_EXIT_NAVIGATION', 'Back pressed', startMs + 85_000);
  progress = recordExamActivityEvent(progress, 'APP_LEFT_FOREGROUND', 'Left 2', startMs + 90_000);

  assert.equal(progress.violationCount, 3);
  assert.equal(getExamReviewStatus(progress.violationCount), 'NEEDS_SUPERVISOR_REVIEW');
  assert.deepEqual(progress.answers[q1.id], { type: 'PG', selectedOptionId: 'A' });

  const finalized = finalizeExamProgress(pkg, progress, 'Submitted', startMs + 100_000);
  assert.equal(finalized.status, 'COMPLETED');
  assert.equal(finalized.activityLog.length, 5);
  assert.equal(finalized.activityLog[4].type, 'EXAM_SUBMITTED');
  assert.equal(findActiveExamPackageId({ matematika_paket_1: finalized }), null);
});
