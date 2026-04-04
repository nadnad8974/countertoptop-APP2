import type { Job } from '../types';

function download(name: string, content: string, type: string) {
  const blob = new Blob([content], { type });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  a.click();
  URL.revokeObjectURL(url);
}

export function exportAsJson(job: Job) {
  download(`${job.jobName.replace(/\s+/g, '_')}.json`, JSON.stringify(job, null, 2), 'application/json');
}

// Placeholder DXF exporter for MVP.
export function exportAsDxfPlaceholder(job: Job) {
  const lines = [
    '0', 'SECTION', '2', 'ENTITIES'
  ];

  job.pieces.forEach((piece) => {
    piece.points.forEach((point, i) => {
      const next = piece.points[i + 1] ?? (piece.closed ? piece.points[0] : null);
      if (!next) return;
      lines.push('0', 'LINE', '8', piece.name, '10', `${point.x}`, '20', `${point.y}`, '11', `${next.x}`, '21', `${next.y}`);
    });
  });

  lines.push('0', 'ENDSEC', '0', 'EOF');
  download(`${job.jobName.replace(/\s+/g, '_')}.dxf`, lines.join('\n'), 'application/dxf');
}

export function exportAsSvg(job: Job) {
  const path = job.pieces
    .map((piece) => {
      if (!piece.points.length) return '';
      const first = piece.points[0];
      const cmds = [`M ${first.x} ${first.y}`, ...piece.points.slice(1).map((p) => `L ${p.x} ${p.y}`)];
      if (piece.closed) cmds.push('Z');
      return `<path d="${cmds.join(' ')}" fill="none" stroke="black" stroke-width="0.25"/>`;
    })
    .join('\n');

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200">${path}</svg>`;
  download(`${job.jobName.replace(/\s+/g, '_')}.svg`, svg, 'image/svg+xml');
}
