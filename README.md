# Stone Countertop Template Field App (Simulator MVP)

Original prototype for digital countertop templating workflows in the stone fabrication industry. This project intentionally avoids proprietary Proliner code, visuals, and branding.

## What this MVP does

- **Job management**: create/open/duplicate/delete countertop jobs.
- **Template editor in simulator mode**:
  - Tap in canvas to add measured points.
  - Manually edit X/Y points in list.
  - View live connected outline and segment lengths.
  - Close/open shapes.
  - Add sink/cooktop cutouts and seam lines.
- **Multiple pieces per job** (main top, island, vanity, backsplash, separate).
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
