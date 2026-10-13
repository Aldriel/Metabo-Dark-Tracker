import { describe, expect, it, vi } from 'vitest';
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
} from './api';
import type { Workspace } from './types';

const workspace = { id: 'w1' } as Workspace;

describe('api', () => {
  it('reads a workspace and translates failures', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => Response.json(workspace)),
    );
    await expect(openExample()).resolves.toMatchObject({ id: 'w1' });
    const file = new File(['<mzML/>'], 'demo.mzML');
    await uploadFile(file);
    await saveSettings('w1', { mzToleranceDa: 0.02, cosineMinimum: 0.7, queryPublic: false });
    expect(String(vi.mocked(fetch).mock.calls[1][0])).toContain('demo.mzML');

    vi.mocked(fetch).mockResolvedValue(new Response('nope', { status: 500, statusText: '' }));
    await expect(openExample()).rejects.toMatchObject({ code: 'UNEXPECTED', message: 'Error' });
    vi.mocked(fetch).mockResolvedValue(new Response('nope', { status: 500, statusText: 'Down' }));
    await expect(openExample()).rejects.toMatchObject({ message: 'Down' });
    vi.mocked(fetch).mockResolvedValue(Response.json({}, { status: 400, statusText: 'Bad' }));
    await expect(downloadTable('w1')).rejects.toMatchObject({
      code: 'UNEXPECTED',
      message: 'Bad',
    });

    vi.mocked(fetch).mockResolvedValue(
      Response.json({ message: 'missing' }, { status: 400, statusText: 'Bad' }),
    );
    await expect(downloadTable('w1')).rejects.toBeInstanceOf(ApiError);

    vi.mocked(fetch).mockResolvedValue(
      Response.json({ code: 'NOT_XML', message: '' }, { status: 400, statusText: '' }),
    );
    await expect(downloadTable('w1')).rejects.toMatchObject({ code: 'NOT_XML', message: 'Error' });

    vi.mocked(fetch).mockResolvedValue(new Response('scan', { status: 200 }));
    const table = await downloadTable('w1');
    expect(await table.text()).toBe('scan');

    vi.mocked(fetch).mockResolvedValue(new Response('', { status: 200 }));
    const empty = await downloadReport('w1', 'en', 'Run');
    expect(empty.size).toBe(4);

    vi.mocked(fetch).mockResolvedValue(new Response('%PDF-real', { status: 200 }));
    const pdf = await downloadReport('w1', 'fr', '');
    expect(pdf.size).toBe(9);

    saveBlob(pdf, 'Metabo-Dark.pdf');
    expect(URL.createObjectURL).toHaveBeenCalled();
  });

  it('checks files and settings before a request', () => {
    expect(firstFile(null)).toBeNull();
    expect(firstFile({ length: 0 } as FileList)).toBeNull();
    const file = new File(['a'], 'a.mzML');
    expect(firstFile({ length: 1, 0: file } as unknown as FileList)).toBe(file);
    expect(settingsError(Number.NaN, 0.7)).toBe('NUMBER');
    expect(settingsError(0.02, Number.POSITIVE_INFINITY)).toBe('NUMBER');
    expect(settingsError(0, 0.7)).toBe('TOLERANCE');
    expect(settingsError(2, 0.7)).toBe('TOLERANCE');
    expect(settingsError(0.02, -0.1)).toBe('COSINE');
    expect(settingsError(0.02, 1.1)).toBe('COSINE');
    expect(settingsError(0.02, 0.7)).toBeNull();
  });
});
