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

const EMAIL = 'contact@maximeethier.com';
const KOFI_URL = 'https://ko-fi.com/K1S228CL7A';
const LICENSE_URL = `${import.meta.env.BASE_URL}LICENSE.txt`;

const TEXTS = {
  fr: {
    author: '© 2026 Maxime Ethier – Consultant en bio-informatique',
    website: 'https://www.maximeethier.com',
    license: 'Licence Apache 2.0',
    availability: 'Disponible pour des contrats ou un emploi en bio-informatique.',
    kofi: 'Soutenez-moi sur Ko-fi',
    kofiTitle: 'Soutenir le développement de cette application par un don sur Ko-fi',
  },
  en: {
    author: '© 2026 Maxime Ethier – Biocomputing Consultant',
    website: 'https://www.maximeethier.com/en',
    license: 'Apache License 2.0',
    licenseTitle: 'Read the license under which this application is distributed',
    availability: 'Available for bioinformatics contracts or employment.',
    kofi: 'Support me on Ko-fi',
    kofiTitle: 'Support the development of this application with a donation on Ko-fi',
  },
} as const;

/** Contact line, the offer of services and the Ko-fi button. */
export function Footer({ language }: { language: Lang }) {
  const text = TEXTS[language];
  const licenseTitle = language === 'fr' ? 'Lire la licence' : TEXTS.en.licenseTitle;
  return (
    <footer className="app-footer">
      <a className="footer-license" href={LICENSE_URL} target="_blank" rel="noreferrer">
        {text.license}
      </a>
      <div className="footer-main">
        <div>
          <a href={text.website} target="_blank" rel="noreferrer">
            {text.author}
          </a>
          <span aria-hidden="true"> · </span>
          <a href={`mailto:${EMAIL}`}>{EMAIL}</a>
        </div>
        <div>{text.availability}</div>
        <a
          className="kofi-button"
          href={KOFI_URL}
          target="_blank"
          rel="noreferrer"
          title={language === 'fr' ? TEXTS.fr.kofiTitle : text.kofiTitle}
        >
          {text.kofi}
        </a>
      </div>
      <span className="visually-hidden">{licenseTitle}</span>
    </footer>
  );
}
