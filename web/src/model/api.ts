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

import type { AnalysisSettings, Workspace } from './types';

/** Failure of a request, with the stable code the interface translates. */
export class ApiError extends Error {
  readonly code: string;

  constructor(code: string, message: string) {
    super(message);
    this.code = code;
  }
}

/** Opens the simulated plasma. */
export async function openExample(): Promise<Workspace> {
  const response = await fetch('/api/workspaces', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ example: 'demo-plasma' }),
  });
  return readJson(response);
}

/**
 * Uploads an mzML or mzXML file. The server opens a new workspace for it.
 *
 * @param file the file chosen by the visitor
 */
export async function uploadFile(file: File): Promise<Workspace> {
  const response = await fetch(`/api/workspaces?name=${encodeURIComponent(file.name)}`, {
    method: 'POST',
    body: file,
  });
  return readJson(response);
}

/**
 * Replaces the settings and returns the new annotation.
 *
 * @param id workspace id
 * @param settings the choices
 */
export async function saveSettings(id: string, settings: AnalysisSettings): Promise<Workspace> {
  const response = await fetch(`/api/workspaces/${id}/settings`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(settings),
  });
  return readJson(response);
}

/**
 * Downloads the tab-separated table.
 *
 * @param id workspace id
 */
export async function downloadTable(id: string): Promise<Blob> {
  const response = await fetch(`/api/workspaces/${id}/export`);
  if (!response.ok) {
    throw await toError(response);
  }
  return response.blob();
}

/**
 * Downloads the PDF report.
 *
 * @param id workspace id
 * @param language fr or en
 * @param title title typed by the visitor
 */
export async function downloadReport(id: string, language: string, title: string): Promise<Blob> {
  const params = new URLSearchParams({ language, title });
  const response = await fetch(`/api/workspaces/${id}/report?${params}`);
  if (!response.ok) {
    throw await toError(response);
  }
  const blob = await response.blob();
  return blob.size > 0 ? blob : new Blob(['%PDF']);
}

/**
 * Starts a download in the browser.
 *
 * @param blob file contents
 * @param name proposed file name
 */
export function saveBlob(blob: Blob, name: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  link.click();
  URL.revokeObjectURL(url);
}

/**
 * First file of a picker, or null when the visitor cancelled.
 *
 * @param list the files from the input
 */
export function firstFile(list: FileList | null): File | null {
  if (!list || list.length === 0) {
    return null;
  }
  return list[0];
}

/**
 * Public page of a library reference, or null when there is none.
 *
 * @param reference "HMDB0000159" or "MassBank MSBNK-…"
 */
export function referenceUrl(reference: string | undefined): string | null {
  const massBank = /^MassBank (MSBNK-\S+)$/.exec(reference ?? '');
  if (massBank) {
    return `https://massbank.eu/MassBank/RecordDisplay?id=${encodeURIComponent(massBank[1])}`;
  }
  if (/^HMDB\d{7}$/.test(reference ?? '')) {
    return `https://hmdb.ca/metabolites/${reference}`;
  }
  return null;
}

/**
 * Stable code for a tolerance and a cosine, or null when both are acceptable.
 *
 * @param tolerance absolute m/z window
 * @param cosine minimum cosine
 */
export function settingsError(tolerance: number, cosine: number): string | null {
  if (!Number.isFinite(tolerance) || !Number.isFinite(cosine)) {
    return 'NUMBER';
  }
  if (tolerance <= 0 || tolerance > 1) {
    return 'TOLERANCE';
  }
  if (cosine < 0 || cosine > 1) {
    return 'COSINE';
  }
  return null;
}

async function readJson(response: Response): Promise<Workspace> {
  if (!response.ok) {
    throw await toError(response);
  }
  return (await response.json()) as Workspace;
}

async function toError(response: Response): Promise<ApiError> {
  try {
    const body = (await response.json()) as { code?: string; message?: string };
    const code = body.code ?? 'UNEXPECTED';
    const message = body.message ?? response.statusText;
    return new ApiError(code, message || 'Error');
  } catch {
    return new ApiError('UNEXPECTED', response.statusText || 'Error');
  }
}
