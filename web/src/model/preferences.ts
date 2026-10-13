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

/** Language of the interface. */
export type Lang = 'fr' | 'en';

const LANGUAGE_KEY = 'metabodark-language';
const FONT_KEY = 'metabodark-font';

/** Text sizes offered by the size buttons, in pixels. */
export const FONT_SIZES = [11, 13, 16, 20];

/**
 * Language remembered by the browser, or the language of the browser, or English.
 *
 * @param browserLanguage value of navigator.language
 */
export function initialLanguage(browserLanguage: string): Lang {
  const stored = localStorage.getItem(LANGUAGE_KEY);
  if (stored === 'fr' || stored === 'en') {
    return stored;
  }
  return browserLanguage.toLowerCase().startsWith('fr') ? 'fr' : 'en';
}

/** Remembers the language. */
export function saveLanguage(language: Lang): void {
  localStorage.setItem(LANGUAGE_KEY, language);
}

/** Remembered text size, or 13. */
export function initialFont(): number {
  const stored = Number(localStorage.getItem(FONT_KEY));
  return FONT_SIZES.includes(stored) ? stored : 13;
}

/** Remembers the text size. */
export function saveFont(size: number): void {
  localStorage.setItem(FONT_KEY, String(size));
}

/**
 * Next size in {@link FONT_SIZES}. The ends of the list do not wrap.
 *
 * @param size current size
 * @param delta +1 or -1
 */
export function stepFont(size: number, delta: number): number {
  const index = FONT_SIZES.indexOf(size);
  const current = index < 0 ? 1 : index;
  const next = Math.min(FONT_SIZES.length - 1, Math.max(0, current + delta));
  return FONT_SIZES[next];
}
