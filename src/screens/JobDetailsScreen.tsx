import React from 'react';
import type { Job } from '../types';

type Props = {
  job: Job;
  onBack: () => void;
  onChange: (job: Job) => void;
  onOpenEditor: () => void;
};

export function JobDetailsScreen({ job, onBack, onChange, onOpenEditor }: Props) {
  const update = (field: keyof Job, value: string) => onChange({ ...job, [field]: value, modifiedAt: new Date().toISOString() });

  return (
    <section>
      <div className="title-row">
        <h2>Job Details</h2>
        <button onClick={onBack}>Back</button>
      </div>
      <div className="form-grid">
        <label>Customer Name<input value={job.customerName} onChange={(e) => update('customerName', e.target.value)} /></label>
        <label>Job Name<input value={job.jobName} onChange={(e) => update('jobName', e.target.value)} /></label>
        <label>Address<input value={job.address} onChange={(e) => update('address', e.target.value)} /></label>
        <label>Room<input value={job.roomName} onChange={(e) => update('roomName', e.target.value)} /></label>
        <label className="full">Notes<textarea value={job.notes} onChange={(e) => update('notes', e.target.value)} /></label>
      </div>
      <button onClick={onOpenEditor}>Open Template Editor</button>
    </section>
  );
}
