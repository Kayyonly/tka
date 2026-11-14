import allPackagesDb from './TKA_SMP_2027_All_Packages.json';
import { normalizeRawQuestion } from '../engine/questionNormalizer';
import { PackageId, QuestionPackage, SubjectId } from '../engine/types';

export interface SubjectInfo {
  id: SubjectId;
  name: string;
  subtitle: string;
  packageIds: PackageId[];
}

interface RawDatabasePackage {
  package_id: PackageId;
  subject_id: SubjectId;
  subject_name: string;
  package_number: number;
  package_name: string;
  question_count: number;
  id_mapel?: number;
  soal: Array<Record<string, unknown>>;
}

interface RawDatabase {
  database_version?: string;
  subjects?: Array<{
    subject_id: SubjectId;
    subject_name: string;
    package_ids: PackageId[];
  }>;
  packages: RawDatabasePackage[];
}

const SUBJECT_SUBTITLES: Record<SubjectId, string> = {
  matematika: 'Aljabar, Geometri, Bilangan, Statistika & Peluang',
  bahasa_indonesia: 'Pemahaman Bacaan, Literasi Informasi, Teks Sastra & Kebahasaan',
};

const rawDb = allPackagesDb as unknown as RawDatabase;

export const SUBJECTS: SubjectInfo[] = (rawDb.subjects ?? [
  {
    subject_id: 'matematika',
    subject_name: 'Matematika',
    package_ids: ['matematika_paket_1', 'matematika_paket_2'],
  },
  {
    subject_id: 'bahasa_indonesia',
    subject_name: 'Bahasa Indonesia',
    package_ids: ['bahasa_indonesia_paket_1', 'bahasa_indonesia_paket_2'],
  },
]).map((s) => ({
  id: s.subject_id,
  name: s.subject_name,
  subtitle: SUBJECT_SUBTITLES[s.subject_id] ?? '',
  packageIds: s.package_ids,
}));

function hydrateOfficialPackage(rawPkg: RawDatabasePackage): QuestionPackage {
  const normalizedQuestions = (rawPkg.soal ?? [])
    .map((item, idx) => normalizeRawQuestion(item, idx, rawPkg.package_id))
    .sort((a, b) => a.number - b.number);

  return {
    packageId: rawPkg.package_id,
    subjectId: rawPkg.subject_id,
    subjectName: rawPkg.subject_name,
    packageNumber: rawPkg.package_number,
    title: `Paket ${rawPkg.package_number}`,
    description: `${rawPkg.package_name} · ${normalizedQuestions.length} soal resmi TKA SMP 2027.`,
    expectedQuestionCount: rawPkg.question_count ?? normalizedQuestions.length,
    idMapel: rawPkg.id_mapel,
    schemaVersion: rawDb.database_version ?? '1.0',
    questions: normalizedQuestions,
  };
}

function buildPackagesMap(): Record<PackageId, QuestionPackage> {
  const byId = new Map<PackageId, QuestionPackage>();
  for (const rawPkg of rawDb.packages ?? []) {
    byId.set(rawPkg.package_id, hydrateOfficialPackage(rawPkg));
  }

  const requiredIds: PackageId[] = [
    'matematika_paket_1',
    'matematika_paket_2',
    'bahasa_indonesia_paket_1',
    'bahasa_indonesia_paket_2',
  ];

  const result = {} as Record<PackageId, QuestionPackage>;
  for (const id of requiredIds) {
    const found = byId.get(id);
    if (!found) {
      throw new Error(`Paket resmi tidak ditemukan di database: ${id}`);
    }
    result[id] = found;
  }
  return result;
}

const BUNDLED_PACKAGES: Record<PackageId, QuestionPackage> = buildPackagesMap();

export function getBundledPackages(): Record<PackageId, QuestionPackage> {
  return BUNDLED_PACKAGES;
}

export function getPackageById(packageId: PackageId): QuestionPackage {
  return BUNDLED_PACKAGES[packageId];
}
