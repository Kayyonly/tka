import { hasMeaningfulHtmlContent } from './htmlUtils';
import {
  AnswerKey,
  JenisSoalCode,
  QuestionItem,
  QuestionOption,
  QuestionStatement,
  QuestionStatementChoice,
  QuestionType,
} from './types';

const OPTION_FIELDS = [
  { letter: 'A', field: 'opsi_a' },
  { letter: 'B', field: 'opsi_b' },
  { letter: 'C', field: 'opsi_c' },
  { letter: 'D', field: 'opsi_d' },
  { letter: 'E', field: 'opsi_e' },
  { letter: 'F', field: 'opsi_f' },
] as const;

export function mapJenisSoalToQuestionType(jenisSoal: string | number | undefined): {
  jenis_soal: JenisSoalCode;
  type: QuestionType;
} {
  const normalized = String(jenisSoal ?? '').trim();
  if (normalized === '1' || normalized === 'PG') {
    return { jenis_soal: '1', type: 'PG' };
  }
  if (normalized === '2' || normalized === 'PG_KOMPLEKS' || normalized === 'MCMA') {
    return { jenis_soal: '2', type: 'PG_KOMPLEKS' };
  }
  if (normalized === '3' || normalized === 'BENAR_SALAH') {
    return { jenis_soal: '3', type: 'BENAR_SALAH' };
  }
  throw new Error(`Tipe soal (jenis_soal) tidak dikenal: "${normalized}"`);
}

function extractOptions(raw: Record<string, unknown>): QuestionOption[] | undefined {
  const rawOptions = (raw.options ?? raw.pilihan ?? raw.opsi) as
    | Array<Record<string, unknown>>
    | undefined;
  if (Array.isArray(rawOptions)) {
    return rawOptions.map((opt, idx) => ({
      id: String(opt.id ?? opt.kode ?? String.fromCharCode(65 + idx)),
      label: String(opt.label ?? opt.kode ?? opt.id ?? String.fromCharCode(65 + idx)),
      html: String(opt.html ?? opt.teks ?? opt.text ?? ''),
    }));
  }

  const declaredCount =
    typeof raw.jumlah_opsi === 'number' && raw.jumlah_opsi > 0
      ? Math.min(raw.jumlah_opsi, OPTION_FIELDS.length)
      : OPTION_FIELDS.length;

  const extracted: QuestionOption[] = [];
  for (let i = 0; i < declaredCount; i++) {
    const { letter, field } = OPTION_FIELDS[i];
    const value = raw[field];
    if (typeof value === 'string' && hasMeaningfulHtmlContent(value)) {
      extracted.push({
        id: letter,
        label: letter,
        html: value,
      });
    }
  }

  return extracted.length > 0 ? extracted : undefined;
}

function extractStatementChoices(raw: Record<string, unknown>): QuestionStatementChoice[] | undefined {
  const rawChoices = raw.cbt_soal_answer_judul as Array<Record<string, unknown>> | undefined;
  if (!Array.isArray(rawChoices) || rawChoices.length === 0) {
    return undefined;
  }

  return [...rawChoices]
    .map((choice, idx) => ({
      id: Number(choice.id ?? idx + 1),
      order: Number(choice.order_soal ?? idx + 1),
      label: String(choice.description ?? choice.label ?? choice.judul_jawaban ?? `Pilihan ${idx + 1}`),
    }))
    .sort((a, b) => a.order - b.order);
}

function extractStatements(raw: Record<string, unknown>): QuestionStatement[] | undefined {
  const rawAnswerTexts = raw.cbt_soal_answer_text as Array<Record<string, unknown>> | undefined;
  if (Array.isArray(rawAnswerTexts) && rawAnswerTexts.length > 0) {
    return rawAnswerTexts.map((stmt, idx) => {
      const key = String.fromCharCode(65 + idx);
      const judul = typeof stmt.judul === 'string' && stmt.judul.trim() ? stmt.judul.trim() : 'Pernyataan';
      return {
        id: String(stmt.id ?? key),
        key,
        label: `${judul} ${key}`,
        html: String(stmt.text ?? stmt.html ?? ''),
      };
    });
  }

  const rawStatements = (raw.statements ?? raw.pernyataan) as
    | Array<Record<string, unknown>>
    | undefined;
  if (Array.isArray(rawStatements)) {
    return rawStatements.map((stmt, idx) => {
      const key = String.fromCharCode(65 + idx);
      return {
        id: String(stmt.id ?? stmt.kode ?? key),
        key: String(stmt.key ?? stmt.kode ?? key),
        label: typeof stmt.label === 'string' ? stmt.label : undefined,
        html: String(stmt.html ?? stmt.teks ?? stmt.text ?? ''),
      };
    });
  }

  return undefined;
}

function parseAnswerKey(
  raw: Record<string, unknown>,
  type: QuestionType,
  statements?: QuestionStatement[],
): AnswerKey | null {
  if (raw.answerKey && typeof raw.answerKey === 'object') {
    return raw.answerKey as AnswerKey;
  }

  const rawKeyEssai = typeof raw.kunci_essai === 'string' ? raw.kunci_essai.trim() : '';

  if (type === 'PG') {
    const candidate = rawKeyEssai || (raw.kunci !== undefined && raw.kunci !== null ? String(raw.kunci).trim() : '');
    return candidate ? { type: 'PG', correctOptionId: candidate.toUpperCase() } : null;
  }

  if (type === 'PG_KOMPLEKS') {
    if (rawKeyEssai) {
      const ids = rawKeyEssai
        .split(';')
        .map((token) => token.trim().toUpperCase())
        .filter(Boolean);
      return ids.length > 0 ? { type: 'PG_KOMPLEKS', correctOptionIds: ids } : null;
    }
    if (Array.isArray(raw.kunci)) {
      return {
        type: 'PG_KOMPLEKS',
        correctOptionIds: raw.kunci.map((item) => String(item).trim().toUpperCase()).filter(Boolean),
      };
    }
    return null;
  }

  if (type === 'BENAR_SALAH') {
    if (rawKeyEssai && statements && statements.length > 0) {
      const map: Record<string, number> = {};
      const entries = rawKeyEssai
        .split(';')
        .map((token) => token.trim())
        .filter(Boolean);

      for (const entry of entries) {
        const [letterRaw, orderRaw] = entry.split('|');
        const letter = (letterRaw ?? '').trim().toUpperCase();
        const order = Number((orderRaw ?? '').trim());
        const stmt =
          statements.find((s) => s.key.toUpperCase() === letter) ??
          statements[letter.charCodeAt(0) - 65];
        if (stmt && Number.isFinite(order)) {
          map[stmt.id] = order;
        }
      }
      return Object.keys(map).length > 0 ? { type: 'BENAR_SALAH', statementAnswers: map } : null;
    }

    if (raw.kunci && typeof raw.kunci === 'object' && !Array.isArray(raw.kunci)) {
      const map: Record<string, number> = {};
      for (const [k, v] of Object.entries(raw.kunci as Record<string, unknown>)) {
        if (typeof v === 'number' && Number.isFinite(v)) {
          map[k] = v;
        } else if (v === true || v === 'B' || v === 'Benar' || v === '1') {
          map[k] = 1;
        } else if (v === false || v === 'S' || v === 'Salah' || v === '2') {
          map[k] = 2;
        }
      }
      return Object.keys(map).length > 0 ? { type: 'BENAR_SALAH', statementAnswers: map } : null;
    }
  }

  return null;
}

export function normalizeRawQuestion(
  raw: Record<string, unknown>,
  fallbackIndex: number,
  packagePrefix: string,
): QuestionItem {
  const { jenis_soal, type } = mapJenisSoalToQuestionType(
    (raw.jenis_soal as string | number | undefined) ?? (raw.type as string | undefined),
  );

  const id = String(raw.id_soal ?? raw.id ?? `${packagePrefix}-${fallbackIndex + 1}`);
  const number = Number(raw.no_soal ?? raw.number ?? raw.nomor ?? fallbackIndex + 1);
  const idMapel = typeof raw.id_mapel === 'number' ? raw.id_mapel : undefined;

  const rawStimulus =
    typeof raw.stimulusHtml === 'string'
      ? raw.stimulusHtml
      : typeof raw.soal === 'string'
        ? raw.soal
        : typeof raw.stimulus === 'string'
          ? raw.stimulus
          : typeof raw.wacana === 'string'
            ? raw.wacana
            : null;

  const rawPertanyaan =
    typeof raw.questionHtml === 'string'
      ? raw.questionHtml
      : typeof raw.pertanyaan === 'string'
        ? raw.pertanyaan
        : typeof raw.teks_soal === 'string'
          ? raw.teks_soal
          : '';

  const hasStimulus = hasMeaningfulHtmlContent(rawStimulus);
  const hasPertanyaan = hasMeaningfulHtmlContent(rawPertanyaan);

  let stimulusHtml: string | null = null;
  let questionHtml = '';

  if (hasPertanyaan) {
    stimulusHtml = hasStimulus ? rawStimulus : null;
    questionHtml = rawPertanyaan;
  } else if (hasStimulus && rawStimulus) {
    stimulusHtml = null;
    questionHtml = rawStimulus;
  }

  const mathFormula = typeof raw.mathFormula === 'string' ? raw.mathFormula : null;
  const explanationHtml =
    typeof raw.explanationHtml === 'string'
      ? raw.explanationHtml
      : typeof raw.pembahasan === 'string'
        ? raw.pembahasan
        : null;

  const rawImage = raw.image as Record<string, unknown> | undefined;
  const image =
    rawImage && typeof rawImage.url === 'string' && rawImage.url.trim()
      ? {
          url: rawImage.url,
          alt: String(rawImage.alt ?? `Gambar soal ${number}`),
          caption: typeof rawImage.caption === 'string' ? rawImage.caption : undefined,
        }
      : typeof raw.gambar === 'string' && raw.gambar.trim()
        ? { url: raw.gambar, alt: `Gambar soal ${number}` }
        : null;

  const options = type === 'PG' || type === 'PG_KOMPLEKS' ? extractOptions(raw) : undefined;
  const statementChoices =
    type === 'BENAR_SALAH'
      ? extractStatementChoices(raw) ?? [
          { id: 1, order: 1, label: 'Benar' },
          { id: 2, order: 2, label: 'Salah' },
        ]
      : undefined;
  const statements = type === 'BENAR_SALAH' ? extractStatements(raw) : undefined;
  const answerKey = parseAnswerKey(raw, type, statements);

  return {
    id,
    number,
    idMapel,
    jenis_soal,
    type,
    kompetensi: typeof raw.kompetensi === 'string' ? raw.kompetensi : null,
    subKompetensi: typeof raw.sub_kompetensi === 'string' ? raw.sub_kompetensi : null,
    stimulusHtml,
    questionHtml,
    mathFormula,
    image,
    options,
    statementChoices,
    statements,
    answerKey,
    kunciCaption: typeof raw.kunci_caption === 'string' ? raw.kunci_caption : null,
    explanationHtml,
    raw,
  };
}
