import React, { useMemo, useRef, useState } from 'react';
import type { Point, Settings, TemplatePiece } from '../types';
import { bounds, distance, formatDim, snapPoint } from '../lib/geometry';

type ToolMode = 'add' | 'select' | 'insert' | 'pan';

type Props = {
  piece: TemplatePiece;
  settings: Settings;
  tool: ToolMode;
  onChange: (next: TemplatePiece) => void;
};

type Camera = { scale: number; tx: number; ty: number };
const DEFAULT_CAMERA: Camera = { scale: 3, tx: 80, ty: 80 };
const HIT_RADIUS = 16;

const toScreen = (x: number, y: number, camera: Camera) => ({ sx: x * camera.scale + camera.tx, sy: y * camera.scale + camera.ty });
const toWorld = (sx: number, sy: number, camera: Camera) => ({ x: (sx - camera.tx) / camera.scale, y: (sy - camera.ty) / camera.scale });

const pointToSegmentDistance = (p: { x: number; y: number }, a: Point, b: Point) => {
  const dx = b.x - a.x;
  const dy = b.y - a.y;
  if (dx === 0 && dy === 0) return Math.hypot(p.x - a.x, p.y - a.y);
  const t = Math.max(0, Math.min(1, ((p.x - a.x) * dx + (p.y - a.y) * dy) / (dx * dx + dy * dy)));
  const px = a.x + t * dx;
  const py = a.y + t * dy;
  return Math.hypot(p.x - px, p.y - py);
};

export function CanvasEditor({ piece, settings, tool, onChange }: Props) {
  const svgRef = useRef<SVGSVGElement | null>(null);
  const [camera, setCamera] = useState<Camera>(DEFAULT_CAMERA);
  const [selectedPointId, setSelectedPointId] = useState<string | null>(null);
  const [draggingPointId, setDraggingPointId] = useState<string | null>(null);
  const [panStart, setPanStart] = useState<{ x: number; y: number } | null>(null);

  const dims = useMemo(() => (piece.points.length ? bounds(piece.points) : null), [piece.points]);

  const setPoints = (points: Point[]) => onChange({ ...piece, points });

  const addWorldPoint = (worldX: number, worldY: number) => {
    if (piece.locked) return;
    const snapped = snapPoint(worldX, worldY, settings);
    const next = { id: crypto.randomUUID(), x: Math.max(0, snapped.x), y: Math.max(0, snapped.y) };
    setPoints([...piece.points, next]);
  };

  const movePoint = (pointId: string, worldX: number, worldY: number) => {
    if (piece.locked) return;
    const snapped = snapPoint(worldX, worldY, settings);
    setPoints(piece.points.map((p) => (p.id === pointId ? { ...p, x: snapped.x, y: snapped.y } : p)));
  };

  const insertOnClosestSegment = (world: { x: number; y: number }) => {
    if (piece.locked || piece.points.length < 2) return;
    const segments = piece.points
      .map((p, i) => {
        const next = piece.points[i + 1] ?? (piece.closed ? piece.points[0] : null);
        if (!next) return null;
        return { i, dist: pointToSegmentDistance(world, p, next) };
      })
      .filter((x): x is { i: number; dist: number } => Boolean(x))
      .sort((a, b) => a.dist - b.dist);

    const best = segments[0];
    if (!best) return;
    const snapped = snapPoint(world.x, world.y, settings);
    const nextPoint: Point = { id: crypto.randomUUID(), x: snapped.x, y: snapped.y };
    const out = [...piece.points];
    out.splice(best.i + 1, 0, nextPoint);
    setPoints(out);
    setSelectedPointId(nextPoint.id);
  };

  const onCanvasPointerDown = (e: React.PointerEvent<SVGSVGElement>) => {
    const rect = svgRef.current?.getBoundingClientRect();
    if (!rect) return;
    const world = toWorld(e.clientX - rect.left, e.clientY - rect.top, camera);
    if (tool === 'pan') {
      setPanStart({ x: e.clientX, y: e.clientY });
      return;
    }
    if (tool === 'add') addWorldPoint(world.x, world.y);
    if (tool === 'insert') insertOnClosestSegment(world);
  };

  const onPointPointerDown = (e: React.PointerEvent<SVGCircleElement>, pointId: string) => {
    e.stopPropagation();
    setSelectedPointId(pointId);
    if (tool === 'select' && !piece.locked) setDraggingPointId(pointId);
  };

  const onPointerMove = (e: React.PointerEvent<SVGSVGElement>) => {
    const rect = svgRef.current?.getBoundingClientRect();
    if (!rect) return;

    if (draggingPointId) {
      const world = toWorld(e.clientX - rect.left, e.clientY - rect.top, camera);
      movePoint(draggingPointId, world.x, world.y);
      return;
    }

    if (panStart) {
      const dx = e.clientX - panStart.x;
      const dy = e.clientY - panStart.y;
      setCamera((prev) => ({ ...prev, tx: prev.tx + dx, ty: prev.ty + dy }));
      setPanStart({ x: e.clientX, y: e.clientY });
    }
  };

  const onPointerUp = () => {
    setDraggingPointId(null);
    setPanStart(null);
  };

  return (
    <div className="editor-pane">
      <div className="editor-toolbar">
        <button onClick={() => onChange({ ...piece, closed: !piece.closed })}>{piece.closed ? 'Open Shape' : 'Close Shape'}</button>
        <button onClick={() => onChange({ ...piece, locked: !piece.locked })}>{piece.locked ? 'Unlock Shape' : 'Lock Shape'}</button>
        <button onClick={() => setCamera((c) => ({ ...c, scale: Math.max(0.5, c.scale * 0.9) }))}>- Zoom</button>
        <button onClick={() => setCamera((c) => ({ ...c, scale: Math.min(15, c.scale * 1.1) }))}>+ Zoom</button>
        <button onClick={() => setCamera(DEFAULT_CAMERA)}>Reset View</button>
      </div>

      {dims && (
        <div className="metrics">
          Overall: {formatDim(dims.maxX - dims.minX, settings)} W × {formatDim(dims.maxY - dims.minY, settings)} D
        </div>
      )}

      <svg
        ref={svgRef}
        className="canvas"
        viewBox="0 0 1000 560"
        onPointerDown={onCanvasPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        onPointerLeave={onPointerUp}
        onWheel={(e) => {
          e.preventDefault();
          const step = e.deltaY > 0 ? 0.95 : 1.05;
          setCamera((c) => ({ ...c, scale: Math.max(0.5, Math.min(15, c.scale * step)) }));
        }}
      >
        {piece.points.map((p, i) => {
          const next = piece.points[i + 1] ?? (piece.closed ? piece.points[0] : null);
          if (!next) return null;
          const a = toScreen(p.x, p.y, camera);
          const b = toScreen(next.x, next.y, camera);
          const d = distance(p, next);
          return (
            <g key={`seg-${p.id}`}>
              <line x1={a.sx} y1={a.sy} x2={b.sx} y2={b.sy} stroke="#20b2aa" strokeWidth={2} />
              <text x={(a.sx + b.sx) / 2 + 8} y={(a.sy + b.sy) / 2 - 8} fontSize={12} fill="#111827">{formatDim(d, settings)}</text>
            </g>
          );
        })}

        {piece.points.map((p, i) => {
          const s = toScreen(p.x, p.y, camera);
          return (
            <g key={p.id}>
              <circle
                cx={s.sx}
                cy={s.sy}
                r={selectedPointId === p.id ? 8 : 6}
                fill={selectedPointId === p.id ? '#f59e0b' : '#2563eb'}
                onPointerDown={(e) => onPointPointerDown(e, p.id)}
              />
              <text x={s.sx + 8} y={s.sy - 8} fontSize={12} fill="#111827">{`P${i + 1} (${p.x.toFixed(settings.precision)}, ${p.y.toFixed(settings.precision)})`}</text>
            </g>
          );
        })}

        {piece.cutouts.map((c) => {
          const p = toScreen(c.x, c.y, camera);
          return (
            <g key={c.id}>
              <rect x={p.sx} y={p.sy} width={c.width * camera.scale} height={c.depth * camera.scale} fill="none" stroke="#ef4444" strokeDasharray="5 3" />
              <text x={p.sx + 4} y={p.sy - 6} fontSize={12} fill="#7f1d1d">{c.label}</text>
            </g>
          );
        })}

        {piece.seams.map((s) => {
          const a = toScreen(s.start.x, s.start.y, camera);
          const b = toScreen(s.end.x, s.end.y, camera);
          return (
            <g key={s.id}>
              <line x1={a.sx} y1={a.sy} x2={b.sx} y2={b.sy} stroke="#7c3aed" strokeWidth={2} strokeDasharray="7 3" />
              <text x={(a.sx + b.sx) / 2 + 4} y={(a.sy + b.sy) / 2 - 8} fontSize={12} fill="#4c1d95">{s.label}</text>
            </g>
          );
        })}
      </svg>

      <div className="point-list">
        <h4>Points</h4>
        {piece.points.map((p, i) => (
          <div key={p.id} className="point-row">
            <span>P{i + 1}</span>
            <input
              type="number"
              value={p.x}
              onChange={(e) => movePoint(p.id, Number(e.target.value), p.y)}
            />
            <input
              type="number"
              value={p.y}
              onChange={(e) => movePoint(p.id, p.x, Number(e.target.value))}
            />
            <button onClick={() => setPoints(piece.points.filter((x) => x.id !== p.id))}>Delete</button>
          </div>
        ))}
      </div>

      <small>Tool hints: add=tap canvas, select=drag points, insert=tap near segment, pan=drag canvas.</small>
    </div>
  );
}

export type { ToolMode };
