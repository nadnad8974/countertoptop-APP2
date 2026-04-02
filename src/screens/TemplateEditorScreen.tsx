import React, { useMemo, useState } from 'react';
import { CanvasEditor } from '../components/CanvasEditor';
import type { Job, TemplatePiece, TemplatePieceType, Settings } from '../types';

const pieceTypes: TemplatePieceType[] = ['main_top', 'island', 'vanity', 'backsplash', 'separate'];

type Props = {
  job: Job;
  settings: Settings;
  onChangeJob: (job: Job) => void;
  onBack: () => void;
  onGoExport: () => void;
};

export function TemplateEditorScreen({ job, settings, onChangeJob, onBack, onGoExport }: Props) {
  const [activePieceId, setActivePieceId] = useState(job.pieces[0]?.id ?? '');
  const activePiece = useMemo(() => job.pieces.find((p) => p.id === activePieceId) ?? job.pieces[0], [job.pieces, activePieceId]);

  const updatePiece = (nextPiece: TemplatePiece) => {
    onChangeJob({ ...job, modifiedAt: new Date().toISOString(), pieces: job.pieces.map((p) => (p.id === nextPiece.id ? nextPiece : p)) });
  };

  const addPiece = () => {
    const piece: TemplatePiece = {
      id: crypto.randomUUID(),
      name: `Piece ${job.pieces.length + 1}`,
      pieceType: 'separate',
      points: [],
      closed: false,
      locked: false,
      cutouts: [],
      seams: []
    };
    onChangeJob({ ...job, pieces: [...job.pieces, piece] });
    setActivePieceId(piece.id);
  };

  const addFeature = (kind: 'sink' | 'cooktop' | 'seam') => {
    if (!activePiece) return;
    if (kind === 'seam') {
      updatePiece({
        ...activePiece,
        seams: [...activePiece.seams, { id: crypto.randomUUID(), label: `Seam ${activePiece.seams.length + 1}`, start: { x: 10, y: 10 }, end: { x: 40, y: 10 } }]
      });
      return;
    }

    updatePiece({
      ...activePiece,
      cutouts: [...activePiece.cutouts, { id: crypto.randomUUID(), type: kind, label: `${kind} ${activePiece.cutouts.length + 1}`, x: 10, y: 5, width: 30, depth: 18 }]
    });
  };

  if (!activePiece) return <div>No pieces yet.</div>;

  return (
    <section>
      <div className="title-row">
        <h2>Template Editor - Simulator Mode</h2>
        <div className="actions">
          <button onClick={onBack}>Back</button>
          <button onClick={onGoExport}>Export</button>
        </div>
      </div>

      <div className="editor-layout">
        <aside className="sidebar">
          <h4>Sections</h4>
          {job.pieces.map((p) => (
            <button key={p.id} className={p.id === activePiece.id ? 'active' : ''} onClick={() => setActivePieceId(p.id)}>
              {p.name}
            </button>
          ))}
          <button onClick={addPiece}>+ Add Section</button>

          <label>Name<input value={activePiece.name} onChange={(e) => updatePiece({ ...activePiece, name: e.target.value })} /></label>
          <label>Type
            <select value={activePiece.pieceType} onChange={(e) => updatePiece({ ...activePiece, pieceType: e.target.value as TemplatePieceType })}>
              {pieceTypes.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
          </label>

          <h4>Countertop Tools</h4>
          <button onClick={() => addFeature('sink')}>Add Sink Cutout</button>
          <button onClick={() => addFeature('cooktop')}>Add Cooktop Cutout</button>
          <button onClick={() => addFeature('seam')}>Add Seam Line</button>
        </aside>

        <CanvasEditor piece={activePiece} settings={settings} onChange={updatePiece} />
      </div>
    </section>
  );
}
