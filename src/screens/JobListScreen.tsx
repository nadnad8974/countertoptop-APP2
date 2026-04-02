import React from 'react';
import type { Job } from '../types';

type Props = {
  jobs: Job[];
  onOpen: (jobId: string) => void;
  onCreate: () => void;
  onDelete: (jobId: string) => void;
  onDuplicate: (jobId: string) => void;
};

export function JobListScreen({ jobs, onOpen, onCreate, onDelete, onDuplicate }: Props) {
  return (
    <section>
      <div className="title-row">
        <h2>Countertop Jobs</h2>
        <button onClick={onCreate}>New Job</button>
      </div>
      <div className="card-grid">
        {jobs.map((job) => (
          <article className="card" key={job.id}>
            <h3>{job.jobName}</h3>
            <p>{job.customerName}</p>
            <p>{job.address}</p>
            <p>{job.roomName}</p>
            <small>Modified: {new Date(job.modifiedAt).toLocaleString()}</small>
            <div className="actions">
              <button onClick={() => onOpen(job.id)}>Open</button>
              <button onClick={() => onDuplicate(job.id)}>Duplicate</button>
              <button onClick={() => onDelete(job.id)}>Delete</button>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
