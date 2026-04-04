import type { Job, Settings } from '../types';
import { sampleJobs } from '../data/sampleJobs';

const JOBS_KEY = 'stone_template_jobs_v1';
const SETTINGS_KEY = 'stone_template_settings_v1';

export const defaultSettings: Settings = {
  units: 'in',
  precision: 2,
  snapToGrid: false,
  gridSize: 0.25
};

export function loadJobs(): Job[] {
  const raw = localStorage.getItem(JOBS_KEY);
  if (!raw) {
    localStorage.setItem(JOBS_KEY, JSON.stringify(sampleJobs));
    return sampleJobs;
  }
  return JSON.parse(raw);
}

export function saveJobs(jobs: Job[]) {
  localStorage.setItem(JOBS_KEY, JSON.stringify(jobs));
}

export function loadSettings(): Settings {
  const raw = localStorage.getItem(SETTINGS_KEY);
  return raw ? JSON.parse(raw) : defaultSettings;
}

export function saveSettings(settings: Settings) {
  localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
}
