import { v4 as uuidv4 } from 'uuid';
import type { Job } from '../types';

const now = new Date().toISOString();

export const sampleJobs: Job[] = [
  {
    id: uuidv4(),
    customerName: 'A. Ramirez',
    jobName: 'Oakview Straight Top',
    address: '121 Oakview Dr',
    roomName: 'Kitchen',
    notes: '36 in sink center, eased edge front.',
    createdAt: now,
    modifiedAt: now,
    pieces: [
      {
        id: uuidv4(),
        name: 'Main Top',
        pieceType: 'main_top',
        closed: true,
        locked: false,
        points: [
          { id: uuidv4(), x: 0, y: 0 },
          { id: uuidv4(), x: 120, y: 0 },
          { id: uuidv4(), x: 120, y: 25.5 },
          { id: uuidv4(), x: 0, y: 25.5 }
        ],
        cutouts: [
          { id: uuidv4(), type: 'sink', label: 'Sink 33"', x: 43.5, y: 5, width: 33, depth: 18 }
        ],
        seams: []
      }
    ]
  },
  {
    id: uuidv4(),
    customerName: 'B. Chen',
    jobName: 'L-Shaped Kitchen',
    address: '89 Riverview Ct',
    roomName: 'Kitchen',
    notes: 'Out-of-square back wall near corner.',
    createdAt: now,
    modifiedAt: now,
    pieces: [
      {
        id: uuidv4(),
        name: 'L Main',
        pieceType: 'main_top',
        closed: true,
        locked: false,
        points: [
          { id: uuidv4(), x: 0, y: 0 },
          { id: uuidv4(), x: 110, y: 0 },
          { id: uuidv4(), x: 110, y: 25 },
          { id: uuidv4(), x: 65, y: 25 },
          { id: uuidv4(), x: 65, y: 82 },
          { id: uuidv4(), x: 0, y: 82 }
        ],
        cutouts: [
          { id: uuidv4(), type: 'cooktop', label: 'Cooktop', x: 20, y: 30, width: 30, depth: 21 }
        ],
        seams: [{ id: uuidv4(), label: 'Seam A', start: { x: 65, y: 25 }, end: { x: 65, y: 82 } }]
      }
    ]
  },
  {
    id: uuidv4(),
    customerName: 'C. Brooks',
    jobName: 'Island Radius Corners',
    address: '700 Meadow Ln',
    roomName: 'Kitchen',
    notes: '1.5" radius all corners, waterfall side planned.',
    createdAt: now,
    modifiedAt: now,
    pieces: [
      {
        id: uuidv4(),
        name: 'Island',
        pieceType: 'island',
        closed: true,
        locked: false,
        points: [
          { id: uuidv4(), x: 0, y: 0 },
          { id: uuidv4(), x: 78, y: 0 },
          { id: uuidv4(), x: 78, y: 44 },
          { id: uuidv4(), x: 0, y: 44 }
        ],
        cutouts: [],
        seams: []
      }
    ]
  }
];
