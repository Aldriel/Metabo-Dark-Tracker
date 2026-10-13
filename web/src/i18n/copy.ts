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

import type { Lang } from '../model/preferences';
import type { Workspace } from '../model/types';

/** Texts of one language. */
export interface Copy {
  title: string;
  intro: string;
  example: string;
  open: string;
  tolerance: string;
  cosine: string;
  queryPublic: string;
  apply: string;
  export: string;
  report: string;
  titleField: string;
  guideButton: string;
  french: string;
  english: string;
  smaller: string;
  larger: string;
  empty: string;
  simulated: string;
  saved: string;
  guide: string;
  scan: string;
  rt: string;
  mz: string;
  name: string;
  family: string;
  kind: string;
  cosineColumn: string;
  families: Record<string, string>;
  kinds: Record<string, string>;
  errors: Record<string, string>;
}

const FAMILIES_EN: Record<string, string> = {
  AMINO_ACIDS: 'Amino acids',
  ALKALOIDS: 'Alkaloids',
  CARBOHYDRATES: 'Carbohydrates',
  CARBOXYLIC_ACIDS: 'Carboxylic acids',
  GLYCEROPHOSPHOCHOLINES: 'Glycerophosphocholines',
  GLYCEROPHOSPHOETHANOLAMINES: 'Glycerophosphoethanolamines',
  GLYCOSIDES: 'Glycosides',
  GLUCURONIDES: 'Glucuronides',
  ORGANOPHOSPHATES: 'Organophosphates',
  UNCLASSIFIED: 'Unclassified',
};

const FAMILIES_FR: Record<string, string> = {
  AMINO_ACIDS: 'Acides aminés',
  ALKALOIDS: 'Alcaloïdes',
  CARBOHYDRATES: 'Glucides',
  CARBOXYLIC_ACIDS: 'Acides carboxyliques',
  GLYCEROPHOSPHOCHOLINES: 'Glycérophosphocholines',
  GLYCEROPHOSPHOETHANOLAMINES: 'Glycérophosphoéthanolamines',
  GLYCOSIDES: 'Glycosides',
  GLUCURONIDES: 'Glucuronides',
  ORGANOPHOSPHATES: 'Organophosphates',
  UNCLASSIFIED: 'Non classé',
};

const ENGLISH: Copy = {
  title: 'Metabo-Dark Tracker',
  intro: 'Open an mzML or mzXML file, or the simulated plasma.',
  example: 'Open the simulated plasma',
  open: 'Open a raw file',
  tolerance: 'm/z tolerance (Da)',
  cosine: 'Minimum cosine',
  queryPublic: 'Query PubChem, ClassyFire and GNPS',
  apply: 'Apply',
  export: 'Export the table',
  report: 'Save the PDF',
  titleField: 'Report title',
  guideButton: 'User guide',
  french: 'French',
  english: 'English',
  smaller: 'Smaller text',
  larger: 'Larger text',
  empty: 'No spectrum file yet.',
  simulated: 'Simulated demonstration, not a patient sample.',
  saved: 'Saved.',
  guide:
    'A library hit needs a close precursor and a square-root cosine. A neutral loss can still name a family. Exact-mass formulas are a bounded search, the first step SIRIUS takes, not SIRIUS itself. Public databases stay off unless you ask.',
  scan: 'Scan',
  rt: 'RT (s)',
  mz: 'Precursor m/z',
  name: 'Name',
  family: 'Family',
  kind: 'Kind',
  cosineColumn: 'Cosine',
  families: FAMILIES_EN,
  kinds: { LIBRARY: 'Library', FAMILY: 'Neutral loss', DARK: 'Dark metabolome' },
  errors: {
    NO_FILE: 'Open a file first.',
    NUMBER: 'Enter a number.',
    TOLERANCE: 'The m/z tolerance must be greater than 0 and at most 1.',
    COSINE: 'The cosine must be between 0 and 1.',
    NOT_XML: 'This is not mzML or mzXML.',
    EMPTY: 'The file is empty.',
    TOO_LARGE: 'The file is too large.',
    NO_SPECTRUM: 'No spectrum was found.',
    BAD_BINARY: 'A spectrum could not be decoded.',
    unknown: 'The request failed.',
  },
};

const FRENCH: Copy = {
  ...ENGLISH,
  intro: 'Ouvrez un fichier mzML ou mzXML, ou le plasma simulé.',
  example: 'Ouvrir le plasma simulé',
  open: 'Ouvrir un fichier brut',
  tolerance: 'Tolérance m/z (Da)',
  cosine: 'Cosinus minimum',
  queryPublic: 'Interroger PubChem, ClassyFire et GNPS',
  apply: 'Appliquer',
  export: 'Exporter le tableau',
  report: 'Enregistrer le PDF',
  titleField: 'Titre du rapport',
  guideButton: 'Guide d’utilisation',
  french: 'Français',
  english: 'Anglais',
  smaller: 'Texte plus petit',
  larger: 'Texte plus grand',
  empty: 'Aucun fichier de spectres pour l’instant.',
  simulated: 'Démonstration simulée, pas un échantillon de patient.',
  saved: 'Enregistré.',
  guide:
    'Une correspondance de bibliothèque exige un précurseur proche et un cosinus sur intensités racine carrée. Une perte neutre peut encore nommer une famille. Les formules sont une recherche bornée, la première étape de SIRIUS, pas SIRIUS lui-même. Les bases publiques restent éteintes sauf demande.',
  rt: 'TR (s)',
  mz: 'Précurseur m/z',
  name: 'Nom',
  family: 'Famille',
  kind: 'Type',
  cosineColumn: 'Cosinus',
  families: FAMILIES_FR,
  kinds: { LIBRARY: 'Bibliothèque', FAMILY: 'Perte neutre', DARK: 'Métabolome sombre' },
  errors: {
    NO_FILE: 'Ouvrez d’abord un fichier.',
    NUMBER: 'Entrez un nombre.',
    TOLERANCE: 'La tolérance m/z doit être supérieure à 0 et d’au plus 1.',
    COSINE: 'Le cosinus doit être compris entre 0 et 1.',
    NOT_XML: 'Ce n’est pas du mzML ni du mzXML.',
    EMPTY: 'Le fichier est vide.',
    TOO_LARGE: 'Le fichier est trop gros.',
    NO_SPECTRUM: 'Aucun spectre n’a été trouvé.',
    BAD_BINARY: 'Un spectre n’a pas pu être décodé.',
    unknown: 'La requête a échoué.',
  },
};

/** Texts of a language. */
export function texts(language: Lang): Copy {
  return language === 'fr' ? FRENCH : ENGLISH;
}

/**
 * Status line for an open workspace.
 *
 * @param text current language
 * @param workspace the open workspace
 */
export function describe(text: Copy, workspace: Workspace): string {
  const analysis = workspace.analysis;
  if (!analysis) {
    return text.empty;
  }
  const summary = `${analysis.ms2} MS/MS · ${analysis.libraryHits} · ${analysis.familyHits} · ${analysis.dark}`;
  return workspace.simulated ? `${summary} ${text.simulated}` : summary;
}

/**
 * Family or kind label. An unknown code is shown as itself.
 *
 * @param labels translations
 * @param code stable code
 */
export function label(labels: Record<string, string>, code: string): string {
  return labels[code] ?? code;
}

/**
 * Sentence for an error code.
 *
 * @param text current language
 * @param code stable code
 */
export function errorText(text: Copy, code: string): string {
  return text.errors[code] ?? text.errors.unknown;
}
