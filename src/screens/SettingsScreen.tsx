import React from 'react';
import type { Settings } from '../types';

type Props = {
  settings: Settings;
  onChange: (settings: Settings) => void;
};

export function SettingsScreen({ settings, onChange }: Props) {
  return (
    <section>
      <h2>Settings</h2>
      <div className="form-grid">
        <label>Units
          <select value={settings.units} onChange={(e) => onChange({ ...settings, units: e.target.value as 'in' | 'mm' })}>
            <option value="in">Inches</option>
            <option value="mm">Millimeters</option>
          </select>
        </label>
        <label>Precision
          <input type="number" min={0} max={4} value={settings.precision} onChange={(e) => onChange({ ...settings, precision: Number(e.target.value) })} />
        </label>
        <label>Snap To Grid
          <input type="checkbox" checked={settings.snapToGrid} onChange={(e) => onChange({ ...settings, snapToGrid: e.target.checked })} />
        </label>
        <label>Grid Size
          <input type="number" step="0.125" value={settings.gridSize} onChange={(e) => onChange({ ...settings, gridSize: Number(e.target.value) })} />
        </label>
      </div>
    </section>
  );
}
