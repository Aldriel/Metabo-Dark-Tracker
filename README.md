# Metabo-Dark Tracker

Outil de métabolomique non ciblée en Java 21 qui permet de :

- lire un fichier **mzML** ou **mzXML** (éventuellement gzippé) et en extraire les spectres MS/MS ;
- comparer chaque spectre à **20 000 spectres de référence expérimentaux de MassBank Europe**
  (environ 8 800 composés et modes d'ionisation) par un cosinus sur les intensités en racine
  carrée, dans le style de GNPS ;
- classer les spectres restants par une **perte neutre** caractéristique (choline, hexose,
  glucuronide, phosphate, et quelques autres) ;
- proposer jusqu'à trois **formules brutes** C, H, N, O, P, S par masse exacte, la première étape
  d'une recherche de type SIRIUS, **sans être SIRIUS** (pas d'arbre de fragmentation) ;
- interroger, seulement si on le demande, **PubChem**, **ClassyFire** et un contrat JSON côté
  **GNPS** ;
- produire un **rapport PDF** et un tableau, en français ou en anglais.

Le même modèle est servi par deux applications indépendantes, toutes deux en architecture MVC :
une application **desktop JavaFX** et une application **web React**, prévue pour
[metabodark.maximeethier.com](https://metabodark.maximeethier.com).

## Données d'exemple

L'exemple « plasma simulé » est **inventé par Metabo-Dark Tracker**. Il contient un spectre MS1,
la phénylalanine, le citrate, un glycoside reconnu par la perte d'un hexose, et un précurseur qui
reste dans le métabolome sombre. Ce ne sont pas des données de patients. Les accès HMDB sont ceux
de la bibliothèque embarquée : il n'y a pas d'interrogation en direct de HMDB.

## Bibliothèque de référence

`core/src/main/resources/com/bioinfo/metabodark/library/massbank.tsv.gz` (1,6 Mo) est construit
par `tools/massbank/build-library.mjs` à partir de la version 2026.03 de
[MassBank Europe](https://github.com/MassBank/MassBank-data) :

- seuls les enregistrements sous licence **CC0, CC BY 4.0, CC BY-SA 4.0 ou DL-DE BY 2.0** sont
  gardés ; les licences non commerciales sont exclues ;
- spectres MS2 LC-ESI en [M+H]+ ou [M-H]-, avec une InChIKey, dont le précurseur est à moins de
  10 ppm de la formule (environ 4 % des enregistrements de MassBank sont écartés par ce contrôle) ;
- 25 pics les plus intenses, au plus trois énergies de collision par composé et par mode.

Chaque spectre garde son accession MassBank, affichée et liée dans l'application. Les
contributeurs et leurs licences sont listés dans `massbank-sources.txt`, à côté du fichier. Ces
données restent sous leurs licences d'origine ; la licence Apache 2.0 du dépôt couvre le code.

Une correspondance exige **au moins trois pics communs** (ou tous les pics d'une référence qui en a
moins), comme les recherches de bibliothèque de GNPS : un ou deux pics communs sont trop souvent
une coïncidence.

```bash
node tools/massbank/build-library.mjs MassBank_NISTformat.msp MassBank.json 2026.03
```

## Fichiers réels pour essayer

| Fichier | Taille | Résultat |
|---|---|---|
| [PestMix1_DDA.mzML](https://zenodo.org/records/20729093) (mélange de pesticides Agilent, Sciex TripleTOF) | 38 Mo | 2 975 spectres MS/MS, une cinquantaine de composés nommés (azaconazole, fluopicolide, flufénacet, méthamidophos, paclobutrazole…) en 2 s |
| [Compound_1_POS.mzML](https://zenodo.org/records/7506099) (Janssen, Q-Exactive) | 17 Mo | 2 590 spectres MS/MS en 2 s |

## Réglages

| Réglage | Valeur de départ | Rôle |
|---|---|---|
| Fenêtre m/z | 0,02 Da | Précurseur, pics et pertes neutres |
| Cosinus minimum | 0,70 | Seuil d'une correspondance de bibliothèque |
| Bases publiques | éteintes | PubChem, ClassyFire et GNPS, au mieux |

Une correspondance exige une polarité compatible et un précurseur dans la fenêtre. Les formules
sont cherchées à 10 ppm, entre 20 et 850 Da, avec au plus trois réponses.

## Organisation du dépôt

```
core/       Modèle partagé (Maven, metabodark-core) : lecture mzML/mzXML, bibliothèque,
            cosinus, familles, formules, exemple, rapport PDF et façade MetaboService
desktop/    Application desktop (Maven, metabodark-desktop) : vue JavaFX, contrôleur, aide, i18n
server/     API REST (Maven, metabodark-server) : espaces de travail en mémoire, port 7074
web/        Application web (npm) : React + TypeScript + Vite, port 5177
deploy/     Installation sur un serveur Ubuntu (systemd + Caddy)
config/     Règles Checkstyle communes aux modules Java
```

### Architecture MVC et patrons de conception

| Couche | Desktop | Web |
|---|---|---|
| Modèle | `core` (`MetaboService`) + `DesktopModel` | `core` via l'API REST ; `web/src/model` |
| Vue | `MainView` (JavaFX, sans calcul) | `web/src/views` |
| Contrôleur | `MainController` | `ApiHandler` et les actions de `App` |

- **Façade** : `MetaboService` est le seul point d'entrée vers le modèle.
- **Stratégie** : une correspondance de bibliothèque, sinon une perte neutre, sinon le sombre.
- **Objets immuables** : les spectres, les réglages et le résultat sont des `record`.

## Application desktop

- Ouverture d'un mzML ou mzXML, ou du plasma simulé.
- Interface **bilingue** et **taille du texte** réglable (Ctrl + / Ctrl − / Ctrl 0), mémorisées.
- Menu **Aide** : guide d'utilisateur.

```powershell
.\mvnw -pl desktop javafx:run
```

L'archive Windows autonome se construit avec `desktop/package-windows.ps1` (JDK 21, `jlink` et
`jpackage`). Elle produit `desktop/target/dist/Metabo-Dark-Tracker-0.1.0-windows-x64.zip`.
L'exécutable s'appelle `MetaboDark.exe`.

## Application web

Les mêmes calculs et les mêmes textes. Les fichiers restent **en mémoire** 30 minutes, jamais sur
disque (40 Mo au plus).

```powershell
.\mvnw -pl server exec:java              # API sur http://127.0.0.1:7074/api
cd web; npm ci; npm run dev              # http://localhost:5177
```

## Qualité du code

- Aucune ligne ne dépasse 100 colonnes. Java formaté en indentation de 4 espaces, TypeScript avec
  Prettier.
- Checkstyle, ESLint et l'intégration continue GitHub (`.github/workflows/ci.yml`).
- JaCoCo et Vitest exigent **95 % des lignes** et **90 % des branches**.
- **268 tests automatisés** : 169 tests JUnit (masses exactes vérifiées contre une table IUPAC
  indépendante, qualité des 20 000 spectres MassBank, cosinus, pertes neutres, formules, lecture
  mzML/mzXML, fichiers piégés XXE et bombes gzip, contrat de l'API REST, bureau JavaFX), 81 tests
  Vitest et 18 parcours Playwright (9 scénarios en français et en anglais, dont un pesticide
  mesuré reconnu dans MassBank).
- Une vérification quotidienne (`.github/workflows/monitor.yml`) rejoue le parcours d'un visiteur
  sur le site en ligne, en français et en anglais.

```powershell
.\mvnw verify
cd web; npm run check
npm run test:e2e:local
```

## Déploiement

Le site est prévu sur le serveur Ubuntu déjà utilisé par les autres applications, avec un tas Java
de **384 Mo**. Après `npm run build` :

```bash
bash setup-server.sh metabodark.maximeethier.com
```

Il faut un enregistrement DNS `metabodark.maximeethier.com` vers le même droplet. Un droplet plus
gros n'est nécessaire que si `free -h` montre moins d'environ 500 Mo disponibles une fois les
autres applications démarrées. Tant que le DNS et ce script n'ont pas été appliqués, le bouton
« Essayer » et la vérification quotidienne ne peuvent pas aboutir.

## Licence

Copyright 2026 Maxime Ethier - Consultant en Bioinformatique/Biocomputing Consultant.

Ce projet est distribué sous la licence Apache 2.0 : voir le fichier [`LICENSE`](LICENSE).
