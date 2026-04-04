import type { Point } from '../types';

export type DemoLayout = {
  id: string;
  name: string;
  points: Array<{ x: number; y: number }>;
};

export const demoLayouts: DemoLayout[] = [
  {
    id: 'straight',
    name: 'Straight Top',
    points: [
      { x: 0, y: 0 },
      { x: 120, y: 0 },
      { x: 120, y: 25.5 },
      { x: 0, y: 25.5 }
    ]
  },
  {
    id: 'lshape',
    name: 'L-Shaped Kitchen',
    points: [
      { x: 0, y: 0 },
      { x: 110, y: 0 },
      { x: 110, y: 25 },
      { x: 65, y: 25 },
      { x: 65, y: 82 },
      { x: 0, y: 82 }
    ]
  },
  {
    id: 'island',
    name: 'Island',
    points: [
      { x: 0, y: 0 },
      { x: 78, y: 0 },
      { x: 78, y: 44 },
      { x: 0, y: 44 }
    ]
  }
];

export function demoToPoints(points: Array<{ x: number; y: number }>): Point[] {
  return points.map((p) => ({ id: crypto.randomUUID(), x: p.x, y: p.y }));
}
