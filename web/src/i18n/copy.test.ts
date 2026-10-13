import { describe, expect, it } from 'vitest';
import { describe as summary, errorText, label, texts } from './copy';
import type { Workspace } from '../model/types';

describe('copy', () => {
  it('describes a workspace in both languages', () => {
    const french = texts('fr');
    const english = texts('en');
    expect(french.example).not.toBe(english.example);
    expect(errorText(english, 'NO_FILE')).toContain('Open');
    expect(errorText(english, 'OTHER')).toBe(english.errors.unknown);
    expect(label(english.families, 'AMINO_ACIDS')).toBe('Amino acids');
    expect(label(english.families, 'OTHER')).toBe('OTHER');
    const open: Workspace = {
      id: 'w',
      fileName: 'demo.mzML',
      simulated: true,
      settings: { mzToleranceDa: 0.02, cosineMinimum: 0.7, queryPublic: false },
      analysis: {
        fileName: 'demo.mzML',
        simulated: true,
        ms2: 4,
        libraryHits: 2,
        familyHits: 1,
        dark: 1,
        annotations: [],
      },
    };
    expect(summary(english, open)).toContain('not a patient');
    expect(summary(english, { ...open, simulated: false })).toBe('4 MS/MS · 2 · 1 · 1');
    expect(summary(english, { ...open, analysis: null })).toBe(english.empty);
  });
});
