import { existsSync } from 'node:fs';
import { spawn } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, '..');
const viteBin = path.join(repoRoot, 'node_modules', 'vite', 'bin', 'vite.js');

const run = (cmd, args, cwd = repoRoot) => {
  const child = spawn(cmd, args, { cwd, stdio: 'inherit' });
  child.on('exit', (code) => process.exit(code ?? 0));
};

if (existsSync(viteBin)) {
  console.log('[dev] using React/Vite app');
  run(process.execPath, [viteBin]);
} else {
  console.log('[dev] node_modules not found. Falling back to standalone server on port 8080.');
  run(process.execPath, [path.join(repoRoot, 'scripts', 'serve-standalone.mjs')]);
}
