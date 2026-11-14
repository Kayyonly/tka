import { PackageId, PackageProgress } from './types';

const PROGRESS_STORAGE_KEY = 'tka_smp_2027_progress_v2';

export type ProgressMap = Partial<Record<PackageId, PackageProgress>>;

function safeGetStorage(): Storage | null {
  try {
    if (typeof window !== 'undefined' && window.localStorage) {
      return window.localStorage;
    }
  } catch {
    // Ignore storage access errors in restricted contexts
  }
  return null;
}

export function loadAllProgress(): ProgressMap {
  const storage = safeGetStorage();
  if (!storage) return {};
  try {
    const raw = storage.getItem(PROGRESS_STORAGE_KEY);
    if (!raw) return {};
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === 'object' ? (parsed as ProgressMap) : {};
  } catch {
    return {};
  }
}

export function savePackageProgress(progress: PackageProgress): ProgressMap {
  const all = loadAllProgress();
  const next: ProgressMap = {
    ...all,
    [progress.packageId]: progress,
  };
  const storage = safeGetStorage();
  if (storage) {
    try {
      storage.setItem(PROGRESS_STORAGE_KEY, JSON.stringify(next));
    } catch {
      // Ignore quota errors
    }
  }
  return next;
}

export function clearPackageProgress(packageId: PackageId): ProgressMap {
  const all = loadAllProgress();
  const next: ProgressMap = { ...all };
  delete next[packageId];
  const storage = safeGetStorage();
  if (storage) {
    try {
      storage.setItem(PROGRESS_STORAGE_KEY, JSON.stringify(next));
    } catch {
      // Ignore storage errors
    }
  }
  return next;
}
