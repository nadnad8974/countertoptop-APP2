# Stone Countertop Template Field App (Simulator MVP+)

Original prototype for digital countertop templating workflows in the stone fabrication industry. This project intentionally avoids proprietary Proliner code, visuals, and branding.

## Two runnable paths

1. **React + TypeScript (Vite) app** in `src/` (primary architecture).
2. **Zero-dependency standalone fallback** in `standalone/` (runs without npm install).

## What this version does

- **Job management**: create/open/duplicate/delete countertop jobs.
- **Template editor in simulator mode**:
  - Tap canvas to add measured points.
  - Manual X/Y point entry panel.
  - Load sample layouts directly into current section.
  - Live connected outline + segment dimensions.
  - Drag points in Select/Move mode.
  - Insert point between segments in Insert mode.
  - Pan and zoom controls with reset view.
  - Lock/unlock finished shapes.
- **Editing flow**:
  - Undo / Redo (job-level history stack)
  - Point list editing and delete
  - Multi-piece jobs (main top, island, vanity, backsplash, separate)
- **Countertop tools**:
  - Sink cutouts
  - Cooktop cutouts
  - Seam lines
- **Offline-first local persistence** via browser localStorage.
- **Settings** for units, precision, and snap-to-grid.
- **Exports**:
  - DXF placeholder exporter (line entities)
  - SVG export for print/render conversion
  - JSON backup export

## Folder structure

- `src/` - React app shell + modular screens/components/libs
- `standalone/index.html` + `standalone/app.js` - dependency-free fallback build
- `src/lib/demoLayouts.ts` - built-in example layouts for simulator mode
- `src/data/sampleJobs.ts` - 3 demo jobs preloaded

## Run option A: React app (requires npm registry access)

```bash
npm install
npm run dev:react
```

Open `http://localhost:5173`.

## Quick start via npm (works even without node_modules)

```bash
npm run dev
```

- If dependencies are installed, this starts the React/Vite app.
- If dependencies are missing, it automatically falls back to the standalone app on `http://localhost:8080`.

## Run option B: Standalone fallback (no npm required)

```bash
python3 -m http.server 8080 -d standalone
```

Open `http://localhost:8080`.


## NPM / registry setup (for restricted environments)

If `npm install` fails with `403 Forbidden`, use an internal npm registry mirror.

1. Copy and edit the template:

```bash
cp .npmrc.example .npmrc
```

2. Set your internal registry URL and auth token in `.npmrc`.
3. Retry:

```bash
npm install
npm run dev
```

## Android path

This web prototype is structured to be wrapped into Android later using a WebView container or migrated to React Native/Jetpack Compose while reusing domain models and editor logic.

## Future hardware-ready integration

`MeasurementInputAdapter` in `src/types.ts` defines a clean interface for future live coordinate streaming from a measuring device. Current version uses simulator interactions only.
