import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Workspace } from '../model/types';
import { App } from './App';

const workspace: Workspace = {
  id: 'w1',
  fileName: 'demo.mzML',
  simulated: true,
  settings: { mzToleranceDa: 0.02, cosineMinimum: 0.7, queryPublic: false },
  analysis: {
    fileName: 'demo.mzML',
    simulated: true,
    ms2: 2,
    libraryHits: 1,
    familyHits: 0,
    dark: 1,
    annotations: [
      {
        scan: 1,
        retentionSeconds: 30,
        precursorMz: 166.0863,
        name: 'Phenylalanine',
        family: 'AMINO_ACIDS',
        kind: 'LIBRARY',
        cosine: 0.91,
        evidence: 'MATCH',
        reference: 'MassBank MSBNK-Eawag-EQ000001',
      },
      {
        scan: 2,
        retentionSeconds: 40,
        precursorMz: 410.2,
        name: '',
        family: 'OTHER',
        kind: 'MYSTERY',
        cosine: 0,
        evidence: 'DARK',
      },
    ],
  },
};

let mode = 'ok';

beforeEach(() => {
  mode = 'ok';
  localStorage.setItem('metabodark-language', 'en');
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: string, init?: RequestInit) => {
      if (mode === 'throw') {
        throw new Error('down');
      }
      if (mode === 'empty') {
        return Response.json({ ...workspace, analysis: null, simulated: false });
      }
      if (String(url).includes('/report')) {
        if (mode === 'pdf-error') {
          return Response.json({ code: 'NO_FILE', message: 'no' }, { status: 400 });
        }
        return new Response(new Blob(['%PDF']), { status: 200 });
      }
      if (String(url).includes('/export')) {
        return new Response(new Blob(['scan']), { status: 200 });
      }
      if (init?.method === 'PUT') {
        return Response.json({ ...workspace, simulated: false });
      }
      return Response.json(workspace);
    }),
  );
});

describe('App', () => {
  it('annotates the example in both languages', async () => {
    const user = userEvent.setup();
    render(<App />);
    expect(screen.getByText('No spectrum file yet.')).toBeTruthy();
    await user.click(screen.getByRole('button', { name: 'Export the table' }));
    expect(screen.getByText('Open a file first.')).toBeTruthy();
    await user.click(screen.getByRole('button', { name: 'Save the PDF' }));
    await user.click(screen.getByRole('button', { name: 'Open the simulated plasma' }));
    expect(screen.getByRole('cell', { name: 'Phenylalanine' })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'Phenylalanine' }).getAttribute('href')).toBe(
      'https://massbank.eu/MassBank/RecordDisplay?id=MSBNK-Eawag-EQ000001',
    );
    expect(screen.getByRole('cell', { name: '—' })).toBeTruthy();
    expect(screen.getByRole('cell', { name: 'OTHER' })).toBeTruthy();
    expect(screen.getByRole('status').textContent).toContain('not a patient');
    await user.click(screen.getByRole('button', { name: 'User guide' }));
    expect(screen.getByText(/SIRIUS/)).toBeTruthy();
    await user.click(screen.getByRole('button', { name: 'User guide' }));
    await user.click(screen.getByRole('button', { name: 'Larger text' }));
    await user.click(screen.getByRole('button', { name: 'Smaller text' }));
    await user.click(screen.getByRole('button', { name: 'French' }));
    expect(screen.getByRole('button', { name: 'Ouvrir le plasma simulé' })).toBeTruthy();
    expect(screen.getByRole('status').textContent).toContain('pas un');
    await user.click(screen.getByRole('button', { name: 'Anglais' }));
    await user.clear(screen.getByLabelText('m/z tolerance (Da)'));
    await user.type(screen.getByLabelText('m/z tolerance (Da)'), 'nope');
    await user.click(screen.getByRole('button', { name: 'Apply' }));
    expect(screen.getByText('Enter a number.')).toBeTruthy();
    await user.clear(screen.getByLabelText('m/z tolerance (Da)'));
    await user.type(screen.getByLabelText('m/z tolerance (Da)'), '5');
    await user.click(screen.getByRole('button', { name: 'Apply' }));
    expect(screen.getByText(/at most 1/)).toBeTruthy();
    await user.clear(screen.getByLabelText('Minimum cosine'));
    await user.type(screen.getByLabelText('Minimum cosine'), '-1');
    await user.clear(screen.getByLabelText('m/z tolerance (Da)'));
    await user.type(screen.getByLabelText('m/z tolerance (Da)'), '0.02');
    await user.click(screen.getByRole('button', { name: 'Apply' }));
    expect(screen.getByText(/between 0 and 1/)).toBeTruthy();
    await user.clear(screen.getByLabelText('Minimum cosine'));
    await user.type(screen.getByLabelText('Minimum cosine'), '0.70');
    await user.click(screen.getByRole('checkbox'));
    await user.click(screen.getByRole('button', { name: 'Apply' }));
    expect(screen.getByRole('status').textContent).not.toContain('not a patient');
    await user.click(screen.getByRole('button', { name: 'Export the table' }));
    expect(screen.getByText('Saved.')).toBeTruthy();
    await user.type(screen.getByLabelText('Report title'), 'Window');
    await user.click(screen.getByRole('button', { name: 'Save the PDF' }));
    mode = 'pdf-error';
    await user.click(screen.getByRole('button', { name: 'Save the PDF' }));
    expect(screen.getByText('Open a file first.')).toBeTruthy();
    const input = screen.getByLabelText('Open a raw file');
    fireEvent.change(input, { target: { files: null } });
    fireEvent.change(input, { target: { files: [] } });
    const file = new File(['<mzML/>'], 'sample.mzML', { type: 'application/xml' });
    await user.upload(input, file);
    expect(screen.getByRole('cell', { name: 'Phenylalanine' })).toBeTruthy();
    mode = 'throw';
    await user.click(screen.getByRole('button', { name: 'Open the simulated plasma' }));
    expect(screen.getByText('The request failed.')).toBeTruthy();
    mode = 'empty';
    await user.click(screen.getByRole('button', { name: 'Open the simulated plasma' }));
    await user.click(screen.getByRole('button', { name: 'Apply' }));
    expect(screen.getByText('Open a file first.')).toBeTruthy();
    expect(screen.getByRole('link', { name: /Ko-fi/ })).toBeTruthy();
    expect(screen.getByRole('link', { name: 'contact@maximeethier.com' })).toBeTruthy();
  });
});
