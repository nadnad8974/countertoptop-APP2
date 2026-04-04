export type UnitSystem = 'in' | 'mm';

export type TemplatePieceType = 'main_top' | 'island' | 'vanity' | 'backsplash' | 'separate';

export type Point = {
  id: string;
  x: number;
  y: number;
  note?: string;
};

export type CutoutType = 'sink' | 'cooktop';

export type Cutout = {
  id: string;
  type: CutoutType;
  label: string;
  x: number;
  y: number;
  width: number;
  depth: number;
};

export type SeamLine = {
  id: string;
  label: string;
  start: { x: number; y: number };
  end: { x: number; y: number };
};

export type TemplatePiece = {
  id: string;
  name: string;
  pieceType: TemplatePieceType;
  points: Point[];
  closed: boolean;
  locked: boolean;
  note?: string;
  cutouts: Cutout[];
  seams: SeamLine[];
};

export type Job = {
  id: string;
  customerName: string;
  jobName: string;
  address: string;
  roomName: string;
  notes: string;
  createdAt: string;
  modifiedAt: string;
  pieces: TemplatePiece[];
};

export type Settings = {
  units: UnitSystem;
  precision: number;
  snapToGrid: boolean;
  gridSize: number;
};

export type Screen = 'jobs' | 'details' | 'editor' | 'export' | 'settings';

export interface MeasurementInputAdapter {
  name: string;
  mode: 'simulator' | 'hardware';
  start(): void;
  stop(): void;
  onPoint(callback: (point: { x: number; y: number }) => void): void;
}
