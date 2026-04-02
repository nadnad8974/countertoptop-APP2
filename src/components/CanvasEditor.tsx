import React, { useMemo, useRef, useState } from 'react';
import type { Point, TemplatePiece, Settings } from '../types';
import { distance, formatDim, snapPoint } from '../lib/geometry';

type Props = {
  piece: TemplatePiece;
  settings: Settings;
  onChange: (next: TemplatePiece) => void;
};

const SCALE = 4;

export function CanvasEditor({ piece, settings, onChange }: Props) {
  const svgRef = useRef<SVGSVGElement | null>(null);
  const [selectedPointId, setSelectedPointId] = useState<string | null>(null);

  const viewPoints = useMemo(() => piece.points.map((p) => ({ ...p, sx: p.x * SCALE + 50, sy: p.y * SCALE + 50 })), [piece.points]);

  const addPointAt = (clientX: number, clientY: number) => {
    if (piece.locked) return;
    const rect = svgRef.current?.getBoundingClientRect();
    if (!rect) return;
    const rawX = (clientX - rect.left - 50) / SCALE;
    const rawY = (clientY - rect.top - 50) / SCALE;
    const snapped = snapPoint(rawX, rawY, settings);
    const next: Point = {
      id: crypto.randomUUID(),
      x: Math.max(0, snapped.x),
      y: Math.max(0, snapped.y)
    };
    onChange({ ...piece, points: [...piece.points, next] });
  };

  const moveSelected = (index: number, x: number, y: number) => {
    if (piece.locked) return;
    const snapped = snapPoint(x, y, settings);
    const points = [...piece.points];
    points[index] = { ...points[index], x: snapped.x, y: snapped.y };
    onChange({ ...piece, points });
  };

  return (
    <div className="editor-pane">
      <div className="editor-toolbar">
        <button onClick={() => onChange({ ...piece, closed: !piece.closed })}>{piece.closed ? 'Open Shape' : 'Close Shape'}</button>
        <button onClick={() => onChange({ ...piece, locked: !piece.locked })}>{piece.locked ? 'Unlock' : 'Lock'}</button>
        <button onClick={() => onChange({ ...piece, points: piece.points.slice(0, -1) })}>Undo Point</button>
        <button onClick={() => onChange({ ...piece, points: [] })}>Clear</button>
      </div>

      <svg
        ref={svgRef}
        className="canvas"
        viewBox="0 0 900 500"
        onClick={(e) => {
          if ((e.target as Element).tagName === 'svg') addPointAt(e.clientX, e.clientY);
        }}
      >
        {viewPoints.map((p, i) => {
          const next = viewPoints[i + 1] ?? (piece.closed ? viewPoints[0] : null);
          if (!next) return null;
          const d = distance(piece.points[i], piece.points[(i + 1) % piece.points.length]);
          const mx = (p.sx + next.sx) / 2;
          const my = (p.sy + next.sy) / 2;
          return (
            <g key={`seg-${p.id}`}>
              <line x1={p.sx} y1={p.sy} x2={next.sx} y2={next.sy} stroke="#20b2aa" strokeWidth={2} />
              <text x={mx + 6} y={my - 6} fontSize={12}>{formatDim(d, settings)}</text>
            </g>
          );
        })}

        {viewPoints.map((p, i) => (
          <g key={p.id}>
            <circle
              cx={p.sx}
              cy={p.sy}
              r={7}
              fill={selectedPointId === p.id ? '#f59e0b' : '#2563eb'}
              onClick={(e) => {
                e.stopPropagation();
                setSelectedPointId(p.id);
              }}
            />
            <text x={p.sx + 8} y={p.sy - 8} fontSize={12}>{`P${i + 1} (${p.x.toFixed(settings.precision)}, ${p.y.toFixed(settings.precision)})`}</text>
          </g>
        ))}

        {piece.cutouts.map((c) => (
          <rect key={c.id} x={c.x * SCALE + 50} y={c.y * SCALE + 50} width={c.width * SCALE} height={c.depth * SCALE} fill="none" stroke="#ef4444" strokeDasharray="4 3" />
        ))}

        {piece.seams.map((s) => (
          <line key={s.id} x1={s.start.x * SCALE + 50} y1={s.start.y * SCALE + 50} x2={s.end.x * SCALE + 50} y2={s.end.y * SCALE + 50} stroke="#7c3aed" strokeWidth={2} strokeDasharray="7 3" />
        ))}
      </svg>

      <div className="point-list">
        <h4>Points</h4>
        {piece.points.map((p, i) => (
          <div key={p.id} className="point-row">
            <span>P{i + 1}</span>
            <input type="number" value={p.x} onChange={(e) => moveSelected(i, Number(e.target.value), p.y)} />
            <input type="number" value={p.y} onChange={(e) => moveSelected(i, p.x, Number(e.target.value))} />
            <button onClick={() => onChange({ ...piece, points: piece.points.filter((x) => x.id !== p.id) })}>Delete</button>
          </div>
        ))}
      </div>
    </div>
  );
}
