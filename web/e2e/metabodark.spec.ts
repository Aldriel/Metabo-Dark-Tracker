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

import { readFile } from 'node:fs/promises';
import { expect, test, type Page } from '@playwright/test';

/** The visible words of one language, so each journey runs in French and in English. */
function words(locale: string | undefined) {
  const french = (locale ?? '').toLowerCase().startsWith('fr');
  return french
    ? {
        example: 'Ouvrir le plasma simulé',
        guide: 'Guide d’utilisation',
        larger: 'Texte plus grand',
        pdf: 'Enregistrer le PDF',
        table: 'Exporter le tableau',
        apply: 'Appliquer',
        open: 'Ouvrir un fichier brut',
        tolerance: 'Tolérance m/z (Da)',
        cosine: 'Cosinus minimum',
        other: 'Anglais',
        otherExample: 'Open the simulated plasma',
        simulated: /pas un échantillon/,
        loss: 'Perte neutre',
        dark: 'Métabolome sombre',
        badCosine: 'Le cosinus doit être compris entre 0 et 1.',
        notXml: 'Ce n’est pas du mzML ni du mzXML.',
        saved: 'Enregistré.',
      }
    : {
        example: 'Open the simulated plasma',
        guide: 'User guide',
        larger: 'Larger text',
        pdf: 'Save the PDF',
        table: 'Export the table',
        apply: 'Apply',
        open: 'Open a raw file',
        tolerance: 'm/z tolerance (Da)',
        cosine: 'Minimum cosine',
        other: 'French',
        otherExample: 'Ouvrir le plasma simulé',
        simulated: /not a patient/,
        loss: 'Neutral loss',
        dark: 'Dark metabolome',
        badCosine: 'The cosine must be between 0 and 1.',
        notXml: 'This is not mzML or mzXML.',
        saved: 'Saved.',
      };
}

async function openExample(page: Page, example: string) {
  await page.goto('/');
  await page.getByRole('button', { name: example }).click();
  await expect(page.getByRole('cell', { name: 'Phenylalanine' })).toBeVisible();
}

/** Base64 of little-endian doubles, the way mzML stores a peak array. */
function peaks(values: number[]): string {
  return Buffer.from(new Float64Array(values).buffer).toString('base64');
}

/** One positive-mode MS/MS spectrum in mzML. */
function mzml(precursor: number, mz: number[], intensity: number[]): Buffer {
  const array = (accession: string, values: number[]) =>
    `<binaryDataArray><cvParam accession="MS:1000523"/><cvParam accession="${accession}"/>` +
    `<binary>${peaks(values)}</binary></binaryDataArray>`;
  const xml =
    '<?xml version="1.0"?><mzML><spectrum>' +
    '<cvParam accession="MS:1000511" value="2"/><cvParam accession="MS:1000130"/>' +
    `<precursor><selectedIon><cvParam accession="MS:1000744" value="${precursor}"/>` +
    '<cvParam accession="MS:1000041" value="1"/></selectedIon></precursor>' +
    '<binaryDataArrayList>' +
    array('MS:1000514', mz) +
    array('MS:1000515', intensity) +
    '</binaryDataArrayList></spectrum></mzML>';
  return Buffer.from(xml, 'utf8');
}

function phenylalanineFile(): Buffer {
  return mzml(166.0863, [103.0542, 120.0808, 166.0863], [40, 100, 15]);
}

/**
 * Azaconazole as an instrument would see it: MassBank record MSBNK-LCSB-LU056603 with masses
 * shifted by 3 mDa, intensities changed by up to 20 % and an extra noise peak.
 */
function azaconazoleFile(): Buffer {
  return mzml(
    300.0329,
    [70.0428, 101.5, 158.9791, 172.9583, 216.9847, 230.9998],
    [33, 12, 999, 11, 21, 120],
  );
}

test('the simulated plasma can be annotated', async ({ page, locale }) => {
  const w = words(locale);
  await openExample(page, w.example);
  await expect(page.getByRole('status')).toHaveText(w.simulated);
  await page.getByRole('button', { name: w.guide }).click();
  await expect(page.getByText(/SIRIUS/)).toBeVisible();
  await page.getByRole('button', { name: w.larger }).click();
  const download = page.waitForEvent('download');
  await page.getByRole('button', { name: w.pdf }).click();
  expect((await download).suggestedFilename()).toContain('.pdf');
  await expect(page.getByRole('link', { name: /Ko-fi/ })).toBeVisible();
  await expect(page.getByRole('link', { name: 'contact@maximeethier.com' })).toBeVisible();
});

test('each strategy appears in the simulated plasma', async ({ page, locale }) => {
  const w = words(locale);
  await openExample(page, w.example);
  await expect(page.getByRole('cell', { name: 'Citrate' })).toBeVisible();
  await expect(page.getByRole('cell', { name: w.loss })).toBeVisible();
  await expect(page.getByRole('cell', { name: w.dark })).toBeVisible();
  await expect(page.locator('tbody tr')).toHaveCount(4);
});

test('the table downloads as tab-separated text', async ({ page, locale }) => {
  const w = words(locale);
  await openExample(page, w.example);
  const download = page.waitForEvent('download');
  await page.getByRole('button', { name: w.table }).click();
  const file = await download;
  expect(file.suggestedFilename()).toBe('Metabo-Dark.tsv');
  const text = await readFile(await file.path(), 'utf8');
  expect(text.startsWith('scan\tretention_s\tprecursor_mz')).toBe(true);
  expect(text.trim().split('\n')).toHaveLength(5);
  await expect(page.getByRole('status')).toHaveText(w.saved);
});

test('a cosine out of range is refused before any request', async ({ page, locale }) => {
  const w = words(locale);
  await openExample(page, w.example);
  await page.getByLabel(w.cosine).fill('1.5');
  await page.getByRole('button', { name: w.apply }).click();
  await expect(page.getByRole('status')).toHaveText(w.badCosine);
});

test('exact library spectra survive the strictest settings', async ({ page, locale }) => {
  const w = words(locale);
  await openExample(page, w.example);
  await page.getByLabel(w.tolerance).fill('0.001');
  await page.getByLabel(w.cosine).fill('0.99');
  const saved = page.waitForResponse((r) => r.url().endsWith('/settings') && r.ok());
  await page.getByRole('button', { name: w.apply }).click();
  await saved;
  await expect(page.getByRole('cell', { name: 'Phenylalanine' })).toBeVisible();
  await expect(page.getByRole('cell', { name: 'Citrate' })).toBeVisible();
  await expect(page.getByRole('status')).toHaveText(w.simulated);
});

test('an uploaded mzML spectrum is named from the library', async ({ page, locale }) => {
  const w = words(locale);
  await page.goto('/');
  await page.getByLabel(w.open).setInputFiles({
    name: 'phenylalanine.mzML',
    mimeType: 'application/xml',
    buffer: phenylalanineFile(),
  });
  await expect(page.getByRole('cell', { name: 'Phenylalanine' })).toBeVisible();
  await expect(page.locator('tbody tr')).toHaveCount(1);
  await expect(page.getByRole('status')).not.toHaveText(w.simulated);
});

test('a measured pesticide is named from MassBank and linked to its record', async ({
  page,
  locale,
}) => {
  const w = words(locale);
  await page.goto('/');
  await page.getByLabel(w.open).setInputFiles({
    name: 'azaconazole.mzML',
    mimeType: 'application/xml',
    buffer: azaconazoleFile(),
  });
  const link = page.getByRole('link', { name: 'Azaconazole' });
  await expect(link).toBeVisible();
  await expect(link).toHaveAttribute('href', /massbank\.eu\/MassBank\/RecordDisplay\?id=MSBNK-/);
});

test('a file that is not mzML is explained', async ({ page, locale }) => {
  const w = words(locale);
  await page.goto('/');
  await page.getByLabel(w.open).setInputFiles({
    name: 'notes.mzML',
    mimeType: 'text/plain',
    buffer: Buffer.from('these are not spectra'),
  });
  await expect(page.getByRole('status')).toHaveText(w.notXml);
});

test('the visitor can switch language without losing the analysis', async ({ page, locale }) => {
  const w = words(locale);
  await openExample(page, w.example);
  await page.getByRole('button', { name: w.other }).click();
  await expect(page.getByRole('button', { name: w.otherExample })).toBeVisible();
  await expect(page.getByRole('cell', { name: 'Phenylalanine' })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('button', { name: w.otherExample })).toBeVisible();
});
