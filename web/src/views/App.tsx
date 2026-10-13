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

import { useEffect, useState } from 'react';
import { describe, errorText, label, texts } from '../i18n/copy';
import {
  ApiError,
  downloadReport,
  downloadTable,
  firstFile,
  openExample,
  saveBlob,
  saveSettings,
  settingsError,
  uploadFile,
} from '../model/api';
import {
  initialFont,
  initialLanguage,
  saveFont,
  saveLanguage,
  stepFont,
  type Lang,
} from '../model/preferences';
import type { Workspace } from '../model/types';
import { Footer } from './Footer';

/** The whole page: the file, the settings, the table and the footer. */
export function App() {
  const [language, setLanguage] = useState<Lang>(() => initialLanguage(navigator.language));
  const [font, setFont] = useState(initialFont);
  const [workspace, setWorkspace] = useState<Workspace | null>(null);
  const [tolerance, setTolerance] = useState('0.02');
  const [cosine, setCosine] = useState('0.7');
  const [queryPublic, setQueryPublic] = useState(false);
  const [title, setTitle] = useState('');
  const [status, setStatus] = useState('');
  const [guide, setGuide] = useState(false);
  const text = texts(language);

  useEffect(() => {
    document.documentElement.lang = language;
    document.documentElement.style.fontSize = `${font}px`;
  }, [language, font]);

  function choose(next: Lang) {
    setLanguage(next);
    saveLanguage(next);
    if (workspace) {
      setStatus(describe(texts(next), workspace));
    }
  }

  function resize(delta: number) {
    const next = stepFont(font, delta);
    setFont(next);
    saveFont(next);
  }

  async function accept(next: Workspace) {
    setWorkspace(next);
    setTolerance(String(next.settings.mzToleranceDa));
    setCosine(String(next.settings.cosineMinimum));
    setQueryPublic(next.settings.queryPublic);
    setStatus(describe(texts(language), next));
  }

  async function fail(error: unknown) {
    const code = error instanceof ApiError ? error.code : 'unknown';
    setStatus(errorText(texts(language), code));
  }

  async function apply() {
    const problem = settingsError(Number(tolerance), Number(cosine));
    if (problem) {
      setStatus(errorText(text, problem));
      return;
    }
    if (!workspace?.analysis) {
      setStatus(errorText(text, 'NO_FILE'));
      return;
    }
    try {
      await accept(
        await saveSettings(workspace.id, {
          mzToleranceDa: Number(tolerance),
          cosineMinimum: Number(cosine),
          queryPublic,
        }),
      );
    } catch (error) {
      await fail(error);
    }
  }

  async function save(kind: 'table' | 'pdf') {
    if (!workspace?.analysis) {
      setStatus(errorText(text, 'NO_FILE'));
      return;
    }
    try {
      const blob =
        kind === 'table'
          ? await downloadTable(workspace.id)
          : await downloadReport(workspace.id, language, title);
      saveBlob(blob, kind === 'table' ? 'Metabo-Dark.tsv' : 'Metabo-Dark.pdf');
      setStatus(text.saved);
    } catch (error) {
      await fail(error);
    }
  }

  const rows = workspace?.analysis?.annotations ?? [];

  return (
    <div className="app">
      <header className="toolbar">
        <h1>
          {text.title} <span className="visually-hidden">{__APP_VERSION__}</span>
        </h1>
        <button type="button" onClick={() => choose('fr')}>
          {text.french}
        </button>
        <button type="button" onClick={() => choose('en')}>
          {text.english}
        </button>
        <button type="button" onClick={() => resize(-1)}>
          {text.smaller}
        </button>
        <button type="button" onClick={() => resize(1)}>
          {text.larger}
        </button>
        <button type="button" onClick={() => setGuide((value) => !value)}>
          {text.guideButton}
        </button>
      </header>
      <p>{text.intro}</p>
      {guide ? <p className="guide">{text.guide}</p> : null}
      <div className="toolbar">
        <button
          type="button"
          onClick={() => {
            openExample().then(accept).catch(fail);
          }}
        >
          {text.example}
        </button>
        <label>
          {text.open}
          <input
            aria-label={text.open}
            type="file"
            accept=".mzML,.mzXML,.gz"
            onChange={(event) => {
              const file = firstFile(event.target.files);
              event.target.value = '';
              if (!file) {
                return;
              }
              uploadFile(file).then(accept).catch(fail);
            }}
          />
        </label>
      </div>
      <div className="fields">
        <label>
          {text.tolerance}
          <input
            aria-label={text.tolerance}
            value={tolerance}
            onChange={(event) => setTolerance(event.target.value)}
          />
        </label>
        <label>
          {text.cosine}
          <input
            aria-label={text.cosine}
            value={cosine}
            onChange={(event) => setCosine(event.target.value)}
          />
        </label>
        <label>
          <input
            type="checkbox"
            checked={queryPublic}
            onChange={(event) => setQueryPublic(event.target.checked)}
          />
          {text.queryPublic}
        </label>
        <button type="button" onClick={() => apply()}>
          {text.apply}
        </button>
        <button type="button" onClick={() => save('table')}>
          {text.export}
        </button>
        <label>
          {text.titleField}
          <input
            aria-label={text.titleField}
            value={title}
            onChange={(event) => setTitle(event.target.value)}
          />
        </label>
        <button type="button" onClick={() => save('pdf')}>
          {text.report}
        </button>
      </div>
      <p role="status">{status || text.empty}</p>
      <table>
        <thead>
          <tr>
            <th>{text.scan}</th>
            <th>{text.rt}</th>
            <th>{text.mz}</th>
            <th>{text.name}</th>
            <th>{text.family}</th>
            <th>{text.kind}</th>
            <th>{text.cosineColumn}</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={`${row.scan}-${row.precursorMz}`}>
              <td>{row.scan}</td>
              <td>{row.retentionSeconds.toFixed(1)}</td>
              <td>{row.precursorMz.toFixed(4)}</td>
              <td>{row.name || '—'}</td>
              <td>{label(text.families, row.family)}</td>
              <td>{label(text.kinds, row.kind)}</td>
              <td>{row.cosine.toFixed(2)}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <Footer language={language} />
    </div>
  );
}
