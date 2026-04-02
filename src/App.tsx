import React, { useMemo, useState } from 'react';
import { v4 as uuidv4 } from 'uuid';
import { JobListScreen } from './screens/JobListScreen';
import { JobDetailsScreen } from './screens/JobDetailsScreen';
import { TemplateEditorScreen } from './screens/TemplateEditorScreen';
import { ExportScreen } from './screens/ExportScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import { defaultSettings, loadJobs, loadSettings, saveJobs, saveSettings } from './lib/storage';
import type { Job, Screen } from './types';

export default function App() {
  const [jobs, setJobs] = useState<Job[]>(() => loadJobs());
  const [settings, setSettings] = useState(() => loadSettings() ?? defaultSettings);
  const [activeJobId, setActiveJobId] = useState<string | null>(jobs[0]?.id ?? null);
  const [screen, setScreen] = useState<Screen>('jobs');

  const activeJob = useMemo(() => jobs.find((j) => j.id === activeJobId) ?? null, [jobs, activeJobId]);

  const patchJobs = (next: Job[]) => {
    setJobs(next);
    saveJobs(next);
  };

  const patchOneJob = (job: Job) => patchJobs(jobs.map((j) => (j.id === job.id ? job : j)));

  const createJob = () => {
    const now = new Date().toISOString();
    const job: Job = {
      id: uuidv4(),
      customerName: '',
      jobName: `New Job ${jobs.length + 1}`,
      address: '',
      roomName: 'Kitchen',
      notes: '',
      createdAt: now,
      modifiedAt: now,
      pieces: [
        {
          id: uuidv4(),
          name: 'Main Top',
          pieceType: 'main_top',
          points: [],
          closed: false,
          locked: false,
          cutouts: [],
          seams: []
        }
      ]
    };
    patchJobs([job, ...jobs]);
    setActiveJobId(job.id);
    setScreen('details');
  };

  return (
    <main>
      <header>
        <h1>Stone Countertop Template Field App (Simulator MVP)</h1>
        <nav>
          <button onClick={() => setScreen('jobs')}>Jobs</button>
          <button onClick={() => setScreen('settings')}>Settings</button>
        </nav>
      </header>

      {screen === 'jobs' && (
        <JobListScreen
          jobs={jobs}
          onCreate={createJob}
          onOpen={(id) => {
            setActiveJobId(id);
            setScreen('details');
          }}
          onDelete={(id) => {
            const next = jobs.filter((j) => j.id !== id);
            patchJobs(next);
            if (id === activeJobId) setActiveJobId(next[0]?.id ?? null);
          }}
          onDuplicate={(id) => {
            const source = jobs.find((j) => j.id === id);
            if (!source) return;
            const copy: Job = {
              ...source,
              id: uuidv4(),
              jobName: `${source.jobName} (Copy)`,
              createdAt: new Date().toISOString(),
              modifiedAt: new Date().toISOString()
            };
            patchJobs([copy, ...jobs]);
          }}
        />
      )}

      {screen === 'details' && activeJob && (
        <JobDetailsScreen
          job={activeJob}
          onBack={() => setScreen('jobs')}
          onChange={patchOneJob}
          onOpenEditor={() => setScreen('editor')}
        />
      )}

      {screen === 'editor' && activeJob && (
        <TemplateEditorScreen
          job={activeJob}
          settings={settings}
          onChangeJob={patchOneJob}
          onBack={() => setScreen('details')}
          onGoExport={() => setScreen('export')}
        />
      )}

      {screen === 'export' && activeJob && <ExportScreen job={activeJob} onBack={() => setScreen('editor')} />}

      {screen === 'settings' && (
        <SettingsScreen
          settings={settings}
          onChange={(next) => {
            setSettings(next);
            saveSettings(next);
          }}
        />
      )}
    </main>
  );
}
