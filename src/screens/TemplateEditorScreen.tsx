import React, { useMemo, useState } from 'react';
import { CanvasEditor, type ToolMode } from '../components/CanvasEditor';
import { demoLayouts, demoToPoints } from '../lib/demoLayouts';
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
  const [tool, setTool] = useState<ToolMode>('add');
  const [manualX, setManualX] = useState<number>(0);
  const [manualY, setManualY] = useState<number>(0);
  const [history, setHistory] = useState<Job[]>([]);
  const [future, setFuture] = useState<Job[]>([]);

  const activePiece = useMemo(() => job.pieces.find((p) => p.id === activePieceId) ?? job.pieces[0], [job.pieces, activePieceId]);

  const pushHistory = (before: Job) => {
    setHistory((h) => [...h.slice(-49), before]);
    setFuture([]);
  };

  const updateJob = (next: Job) => {
    pushHistory(job);
    onChangeJob({ ...next, modifiedAt: new Date().toISOString() });
  };

  const updatePiece = (nextPiece: TemplatePiece) => {
    updateJob({ ...job, pieces: job.pieces.map((p) => (p.id === nextPiece.id ? nextPiece : p)) });
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
    updateJob({ ...job, pieces: [...job.pieces, piece] });
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

  const addManualPoint = () => {
    if (!activePiece || activePiece.locked) return;
    updatePiece({ ...activePiece, points: [...activePiece.points, { id: crypto.randomUUID(), x: manualX, y: manualY }] });
  };

  const loadDemo = (layoutId: string) => {
    if (!activePiece) return;
    const layout = demoLayouts.find((d) => d.id === layoutId);
    if (!layout) return;
    updatePiece({ ...activePiece, points: demoToPoints(layout.points), closed: true });
  };

  const undo = () => {
    const prev = history[history.length - 1];
    if (!prev) return;
    setFuture((f) => [job, ...f]);
    setHistory((h) => h.slice(0, -1));
    onChangeJob(prev);
  };

  const redo = () => {
    const next = future[0];
    if (!next) return;
    setHistory((h) => [...h, job]);
    setFuture((f) => f.slice(1));
    onChangeJob(next);
  };

  if (!activePiece) return <div>No pieces yet.</div>;

  return (
    <section>
      <div className="title-row">
        <h2>Template Editor - Simulator Mode</h2>
        <div className="actions">
          <button onClick={undo} disabled={!history.length}>Undo</button>
          <button onClick={redo} disabled={!future.length}>Redo</button>
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

          <h4>Tool Mode</h4>
          <div className="actions">
            <button className={tool === 'add' ? 'active' : ''} onClick={() => setTool('add')}>Add</button>
            <button className={tool === 'select' ? 'active' : ''} onClick={() => setTool('select')}>Select/Move</button>
            <button className={tool === 'insert' ? 'active' : ''} onClick={() => setTool('insert')}>Insert</button>
            <button className={tool === 'pan' ? 'active' : ''} onClick={() => setTool('pan')}>Pan</button>
          </div>

          <h4>Manual Point Entry</h4>
          <label>X<input type="number" value={manualX} onChange={(e) => setManualX(Number(e.target.value))} /></label>
          <label>Y<input type="number" value={manualY} onChange={(e) => setManualY(Number(e.target.value))} /></label>
          <button onClick={addManualPoint}>Add Point by XY</button>

          <h4>Load Example Layout</h4>
          <div className="actions">
            {demoLayouts.map((layout) => (
              <button key={layout.id} onClick={() => loadDemo(layout.id)}>{layout.name}</button>
            ))}
          </div>

          <h4>Countertop Tools</h4>
          <button onClick={() => addFeature('sink')}>Add Sink Cutout</button>
          <button onClick={() => addFeature('cooktop')}>Add Cooktop Cutout</button>
          <button onClick={() => addFeature('seam')}>Add Seam Line</button>
        </aside>

        <CanvasEditor piece={activePiece} settings={settings} tool={tool} onChange={updatePiece} />
      </div>
    </section>
  );
}
