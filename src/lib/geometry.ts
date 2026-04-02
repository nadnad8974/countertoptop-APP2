import type { Point, Settings } from '../types';

export const distance = (a: Point, b: Point) => Math.hypot(b.x - a.x, b.y - a.y);

export const bounds = (points: Point[]) => {
  const xs = points.map((p) => p.x);
  const ys = points.map((p) => p.y);
  return {
    minX: Math.min(...xs),
    maxX: Math.max(...xs),
    minY: Math.min(...ys),
    maxY: Math.max(...ys)
  };
};

export const formatDim = (value: number, settings: Settings) => `${value.toFixed(settings.precision)} ${settings.units}`;

export const snapPoint = (x: number, y: number, settings: Settings) => {
  if (!settings.snapToGrid) return { x, y };
  const g = settings.gridSize;
  return {
    x: Math.round(x / g) * g,
    y: Math.round(y / g) * g
  };
};
