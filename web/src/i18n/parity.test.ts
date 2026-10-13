import { describe, expect, it } from 'vitest';
import { errorText, label, texts, type Copy } from './copy';

/** Codes the Java core can send; each must have a sentence in both languages. */
const FAMILIES = [
  'AMINO_ACIDS',
  'ALKALOIDS',
  'CARBOHYDRATES',
  'CARBOXYLIC_ACIDS',
  'GLYCEROPHOSPHOCHOLINES',
  'GLYCEROPHOSPHOETHANOLAMINES',
  'GLYCOSIDES',
  'GLUCURONIDES',
  'ORGANOPHOSPHATES',
  'UNCLASSIFIED',
];
const KINDS = ['LIBRARY', 'FAMILY', 'DARK'];
const UPLOAD_ERRORS = ['EMPTY', 'TOO_LARGE', 'NOT_XML', 'NO_SPECTRUM', 'BAD_BINARY', 'NO_FILE'];

const french = texts('fr');
const english = texts('en');

describe('French and English copy', () => {
  it.each(Object.keys(english) as (keyof Copy)[])('both languages define %s', (key) => {
    expect(french[key]).toBeTruthy();
    expect(english[key]).toBeTruthy();
  });

  it.each(FAMILIES)('the family %s is translated', (code) => {
    expect(label(english.families, code)).not.toBe(code);
    expect(label(french.families, code)).not.toBe(code);
  });

  it.each(KINDS)('the kind %s is translated differently in each language', (code) => {
    expect(label(english.kinds, code)).not.toBe(code);
    expect(label(french.kinds, code)).not.toBe(label(english.kinds, code));
  });

  it.each(UPLOAD_ERRORS)('the upload error %s has its own sentence', (code) => {
    expect(errorText(english, code)).not.toBe(english.errors.unknown);
    expect(errorText(french, code)).not.toBe(french.errors.unknown);
  });

  it('both languages translate the same error codes', () => {
    expect(Object.keys(french.errors).sort()).toEqual(Object.keys(english.errors).sort());
  });

  it('the buttons a visitor clicks differ between languages', () => {
    for (const key of ['example', 'apply', 'export', 'report', 'guideButton'] as const) {
      expect(french[key]).not.toBe(english[key]);
    }
  });

  it('the simulated warning is explicit in both languages', () => {
    expect(english.simulated).toMatch(/simulated/i);
    expect(french.simulated).toMatch(/simulée/i);
  });
});
