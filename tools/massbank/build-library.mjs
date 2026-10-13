/*
 * Copyright 2026 Maxime Ethier - Consultant en Bioinformatique/Biocomputing Consultant
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Builds the MassBank part of the reference library from a MassBank-data release
 * (https://github.com/MassBank/MassBank-data/releases): MassBank_NISTformat.msp for the spectra and
 * MassBank.json for the license of each record. Records under a non-commercial license are left
 * out. Usage:
 *
 *   node build-library.mjs MassBank_NISTformat.msp MassBank.json <release>
 *
 * Writes massbank.tsv.gz and massbank-sources.txt into the core resources.
 */

import { createReadStream, writeFileSync } from 'node:fs';
import { createInterface } from 'node:readline';
import { gzipSync } from 'node:zlib';

const [msp, json, release] = process.argv.slice(2);
if (!msp || !json || !release) {
  console.error('usage: node build-library.mjs <msp> <json> <release>');
  process.exit(1);
}

const ALLOWED = new Map([
  ['https://creativecommons.org/publicdomain/zero/1.0/', 'CC0 1.0'],
  ['https://creativecommons.org/licenses/by/4.0/', 'CC BY 4.0'],
  ['https://creativecommons.org/licenses/by-sa/4.0', 'CC BY-SA 4.0'],
  ['https://www.govdata.de/dl-de/by-2-0', 'DL-DE BY 2.0'],
]);
const MAX_PER_COMPOUND = 3;
const MAX_PEAKS = 25;
const MIN_RELATIVE = 0.01;
const OUT = new URL(
  '../../core/src/main/resources/com/bioinfo/metabodark/library/',
  import.meta.url,
);

async function licenses() {
  const byId = new Map();
  let id = null;
  for await (const line of createInterface({ input: createReadStream(json) })) {
    const ident = /^ {4}"identifier": "(.*)",?$/.exec(line);
    if (ident) id = ident[1];
    const lic = /^ {4}"license": "(.*)",?$/.exec(line);
    if (lic && id) byId.set(id, lic[1]);
  }
  return byId;
}

async function* records() {
  let record = { peaks: [] };
  let inPeaks = false;
  for await (const raw of createInterface({ input: createReadStream(msp) })) {
    const line = raw.trim();
    if (line === '') {
      if (record.Name) yield record;
      record = { peaks: [] };
      inPeaks = false;
      continue;
    }
    if (inPeaks) {
      const [mz, intensity] = line.split(/\s+/).map(Number);
      if (Number.isFinite(mz) && Number.isFinite(intensity)) record.peaks.push([mz, intensity]);
      continue;
    }
    const at = line.indexOf(': ');
    if (at < 0) continue;
    const key = line.slice(0, at);
    const value = line.slice(at + 2).trim();
    if (key === 'Num Peaks') inPeaks = true;
    else if (!(key in record)) record[key] = value;
  }
  if (record.Name) yield record;
}

const ELEMENTS = {
  C: 12,
  H: 1.00782503207,
  N: 14.0030740048,
  O: 15.99491461956,
  P: 30.97376163,
  S: 31.972071,
  F: 18.99840322,
  Cl: 34.96885268,
  Br: 78.9183371,
  I: 126.904473,
  Si: 27.9769265325,
  Se: 79.9165213,
  B: 11.0093054,
};
const PROTON = 1.007276467;

/** Monoisotopic mass of a formula, or NaN when an element is missing from the table. */
function mass(formula) {
  let total = 0;
  for (const [, element, count] of formula.matchAll(/([A-Z][a-z]?)(\d*)/g)) {
    if (!(element in ELEMENTS)) return Number.NaN;
    total += ELEMENTS[element] * (count === '' ? 1 : Number(count));
  }
  return total;
}

/** The precursor must be the protonated or deprotonated formula within 10 ppm. */
function consistent(r) {
  const neutral = mass(r['Formula']);
  const expected = r['Precursor_type'] === '[M+H]+' ? neutral + PROTON : neutral - PROTON;
  return Math.abs(Number(r['PrecursorMZ']) - expected) / expected < 10e-6;
}

function usable(r) {
  return (
    r['Spectrum_type'] === 'MS2' &&
    /^LC-ESI-/.test(r['Instrument_type'] ?? '') &&
    (r['Precursor_type'] === '[M+H]+' || r['Precursor_type'] === '[M-H]-') &&
    /^[A-Z]{14}-[A-Z]{10}-[A-Z]$/.test(r['InChIKey'] ?? '') &&
    /^[A-Z][A-Za-z0-9]*$/.test(r['Formula'] ?? '') &&
    Number(r['PrecursorMZ']) > 50 &&
    !/^[A-Za-z]+_\d+(\.\d+)?_/.test(r['Name']) &&
    consistent(r)
  );
}

function peaks(r) {
  const base = Math.max(...r.peaks.map((p) => p[1]));
  if (!(base > 0)) return [];
  return r.peaks
    .filter((p) => p[1] / base >= MIN_RELATIVE)
    .sort((a, b) => b[1] - a[1])
    .slice(0, MAX_PEAKS)
    .sort((a, b) => a[0] - b[0])
    .map(([mz, i]) => `${mz.toFixed(4)}:${Math.max(1, Math.round((999 * i) / base))}`);
}

const byId = await licenses();
const kept = new Map();
const contributors = new Map();
let seen = 0;
for await (const r of records()) {
  seen++;
  const id = r['DB#'];
  const license = ALLOWED.get(byId.get(id));
  if (!license || !usable(r)) continue;
  const list = peaks(r);
  if (list.length < 2) continue;
  const polarity = r['Precursor_type'].endsWith('+') ? '+' : '-';
  const key = `${r['InChIKey']}${polarity}`;
  const group = kept.get(key) ?? [];
  const energy = r['Collision_energy'] ?? '';
  if (group.length >= MAX_PER_COMPOUND || group.some((g) => g.energy === energy)) continue;
  const name = r['Name'].replace(/[\t\r\n]/g, ' ');
  const precursor = Number(r['PrecursorMZ']).toFixed(4);
  group.push({
    energy,
    line: [id, name, r['Formula'], r['InChIKey'], polarity, precursor, list.join(' ')].join('\t'),
  });
  kept.set(key, group);
  const contributor = id.split('-')[1];
  const entry = contributors.get(contributor) ?? { license: new Set(), count: 0 };
  entry.license.add(license);
  entry.count++;
  contributors.set(contributor, entry);
}

const lines = [...kept.values()].flat().map((g) => g.line);
writeFileSync(new URL('massbank.tsv.gz', OUT), gzipSync(lines.join('\n') + '\n', { level: 9 }));
const sources = [
  `MassBank Europe, release ${release} (https://massbank.eu, https://github.com/MassBank/MassBank-data).`,
  'Records under a non-commercial license, and records whose precursor is more than 10 ppm from',
  'the protonated or deprotonated formula, are not included. Spectra are trimmed to their',
  `${MAX_PEAKS} most intense peaks and at most ${MAX_PER_COMPOUND} collision energies per compound and ion mode.`,
  'Each line of massbank.tsv.gz starts with the MassBank accession of the record it comes from.',
  '',
  'Contributor (accession prefix)\tSpectra\tLicense',
  ...[...contributors.entries()]
    .sort((a, b) => a[0].localeCompare(b[0]))
    .map(([c, e]) => `${c}\t${e.count}\t${[...e.license].join(', ')}`),
];
writeFileSync(new URL('massbank-sources.txt', OUT), sources.join('\n') + '\n');
console.log(`${seen} records read, ${lines.length} spectra of ${kept.size} compound-modes kept.`);
