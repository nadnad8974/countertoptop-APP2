# Stone Countertop Template Field App (Simulator MVP+)

Original prototype for digital countertop templating workflows in the stone fabrication industry. This project intentionally avoids proprietary Proliner code, visuals, and branding.

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

- `src/App.tsx` - app shell + screen routing state
- `src/screens/` - job list/details/editor/export/settings screens
- `src/components/CanvasEditor.tsx` - drawing canvas + simulator interactions
- `src/types.ts` - data model + future measurement adapter interface
- `src/lib/geometry.ts` - dimensions and snapping helpers
- `src/lib/storage.ts` - local persistence
- `src/lib/exporters.ts` - export functions (with DXF placeholder)
- `src/lib/demoLayouts.ts` - built-in example layouts for simulator mode
- `src/data/sampleJobs.ts` - 3 demo jobs preloaded

## Demo data included

1. Straight countertop with sink cutout
2. L-shaped kitchen with seam and cooktop
3. Island with rounded-corner notes

## Run locally

```bash
npm install
npm run dev
```

Open `http://localhost:5173`.

## Android path

This web prototype is structured to be wrapped into Android later using a WebView container or migrated to React Native/Jetpack Compose while reusing domain models and editor logic.

## Future hardware-ready integration

`MeasurementInputAdapter` in `src/types.ts` defines a clean interface for future live coordinate streaming from a measuring device. Current version uses simulator interactions only.
