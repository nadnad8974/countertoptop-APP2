import { createServer } from 'node:http';
import { readFile, stat } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const root = path.resolve(__dirname, '..', 'standalone');
const port = Number(process.env.PORT || 8080);

const mime = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml'
};

createServer(async (req, res) => {
  try {
    const urlPath = decodeURIComponent((req.url || '/').split('?')[0]);
    const rel = urlPath === '/' ? '/index.html' : urlPath;
    const full = path.normalize(path.join(root, rel));

    if (!full.startsWith(root)) {
      res.writeHead(403);
      res.end('Forbidden');
      return;
    }

    const file = await stat(full).then(() => full).catch(() => path.join(root, 'index.html'));
    const ext = path.extname(file);
    const body = await readFile(file);
    res.writeHead(200, { 'Content-Type': mime[ext] || 'application/octet-stream' });
    res.end(body);
  } catch {
    res.writeHead(500);
    res.end('Server error');
  }
}).listen(port, '0.0.0.0', () => {
  console.log(`[standalone] serving ${root} on http://0.0.0.0:${port}`);
});
