import { describe, expect, it } from 'vitest';
import {
  FONT_SIZES,
  initialFont,
  initialLanguage,
  saveFont,
  saveLanguage,
  stepFont,
} from './preferences';

describe('preferences', () => {
  it('remembers a language and a text size', () => {
    expect(initialLanguage('en-CA')).toBe('en');
    expect(initialLanguage('FR')).toBe('fr');
    saveLanguage('fr');
    expect(initialLanguage('en')).toBe('fr');
    saveLanguage('en');
    expect(initialLanguage('fr')).toBe('en');
    localStorage.setItem('metabodark-language', 'other');
    expect(initialLanguage('fr-CA')).toBe('fr');
    expect(initialFont()).toBe(13);
    saveFont(16);
    expect(initialFont()).toBe(16);
    localStorage.setItem('metabodark-font', 'nope');
    expect(initialFont()).toBe(13);
    expect(stepFont(13, 1)).toBe(16);
    expect(stepFont(11, -1)).toBe(11);
    expect(stepFont(20, 1)).toBe(20);
    expect(stepFont(99, -1)).toBe(FONT_SIZES[0]);
  });
});
