import React from 'react';
import { exportAsDxfPlaceholder, exportAsJson, exportAsSvg } from '../lib/exporters';
import type { Job } from '../types';

type Props = {
  job: Job;
  onBack: () => void;
};

export function ExportScreen({ job, onBack }: Props) {
  return (
    <section>
      <div className="title-row">
        <h2>Export Layout</h2>
        <button onClick={onBack}>Back</button>
      </div>
      <p>Export includes outlines, dimensions, labels, cutouts, seams, and notes in MVP placeholders.</p>
      <div className="actions">
        <button onClick={() => exportAsDxfPlaceholder(job)}>Export DXF (placeholder)</button>
        <button onClick={() => exportAsSvg(job)}>Export SVG/PNG source</button>
        <button onClick={() => exportAsJson(job)}>Export JSON backup</button>
      </div>
    </section>
  );
}
