import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  downloadReport,
  downloadTable,
  openExample,
  referenceUrl,
  saveSettings,
  settingsError,
  uploadFile,
} from './api';

/** The exact requests the REST server expects. */
describe('requests sent to the server', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => Response.json({ id: 'w1' })),
    );
  });

  function call(index = 0): [string, RequestInit | undefined] {
    const [url, init] = vi.mocked(fetch).mock.calls[index];
    return [String(url), init];
  }

  it('the example is opened by its id as JSON', async () => {
    await openExample();
    const [url, init] = call();
    expect(url).toBe('/api/workspaces');
    expect(init?.method).toBe('POST');
    expect(new Headers(init?.headers).get('Content-Type')).toBe('application/json');
    expect(JSON.parse(String(init?.body))).toEqual({ example: 'demo-plasma' });
  });

  it.each([
    ['plasma run.mzML', 'plasma%20run.mzML'],
    ['a&b=c.mzXML', 'a%26b%3Dc.mzXML'],
    ['échantillon#2.mzML.gz', '%C3%A9chantillon%232.mzML.gz'],
  ])('the file name %s is sent encoded', async (name, encoded) => {
    const file = new File(['<mzML/>'], name);
    await uploadFile(file);
    const [url, init] = call();
    expect(url).toBe(`/api/workspaces?name=${encoded}`);
    expect(init?.body).toBe(file);
  });

  it('settings are replaced with a PUT', async () => {
    const settings = { mzToleranceDa: 0.01, cosineMinimum: 0.8, queryPublic: true };
    await saveSettings('w1', settings);
    const [url, init] = call();
    expect(url).toBe('/api/workspaces/w1/settings');
    expect(init?.method).toBe('PUT');
    expect(JSON.parse(String(init?.body))).toEqual(settings);
  });

  it('the report title and language travel in the query', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response('%PDF-1.7', { status: 200 }));
    await downloadReport('w1', 'fr', 'Plasma & sérum');
    const query = new URL(call()[0], 'http://x').searchParams;
    expect(query.get('language')).toBe('fr');
    expect(query.get('title')).toBe('Plasma & sérum');
  });

  it('the table is fetched from the export route', async () => {
    vi.mocked(fetch).mockResolvedValue(new Response('scan', { status: 200 }));
    await downloadTable('w9');
    expect(call()[0]).toBe('/api/workspaces/w9/export');
  });
});

describe('links to the library references', () => {
  it.each([
    [
      'MassBank MSBNK-Eawag-EQ000001',
      'https://massbank.eu/MassBank/RecordDisplay?id=MSBNK-Eawag-EQ000001',
    ],
    ['HMDB0000159', 'https://hmdb.ca/metabolites/HMDB0000159'],
    ['', null],
    [undefined, null],
    ['HMDB159', null],
    ['MassBank <script>', null],
    ['javascript:alert(1)', null],
  ])('%s links to %s', (reference, url) => {
    expect(referenceUrl(reference)).toBe(url);
  });
});

describe('settings checked before a request', () => {
  it.each([
    [0.02, 0.7, null],
    [1, 0, null],
    [0.0001, 1, null],
    [0, 0.7, 'TOLERANCE'],
    [-0.01, 0.7, 'TOLERANCE'],
    [1.0001, 0.7, 'TOLERANCE'],
    [0.02, -0.0001, 'COSINE'],
    [0.02, 1.0001, 'COSINE'],
    [Number.NaN, 0.7, 'NUMBER'],
    [0.02, Number.NEGATIVE_INFINITY, 'NUMBER'],
  ])('tolerance %s and cosine %s give %s', (tolerance, cosine, expected) => {
    expect(settingsError(tolerance, cosine)).toBe(expected);
  });
});
