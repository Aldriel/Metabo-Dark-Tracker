/** One annotation row returned by the API. */
export interface Annotation {
  scan: number;
  retentionSeconds: number;
  precursorMz: number;
  name: string;
  family: string;
  kind: string;
  cosine: number;
  evidence: string;
}

/** Settings the visitor can change. */
export interface AnalysisSettings {
  mzToleranceDa: number;
  cosineMinimum: number;
  queryPublic: boolean;
}

/** Annotation of one raw file. */
export interface Analysis {
  fileName: string;
  simulated: boolean;
  ms2: number;
  libraryHits: number;
  familyHits: number;
  dark: number;
  annotations: Annotation[];
}

/** A visitor's workspace. */
export interface Workspace {
  id: string;
  fileName: string;
  simulated: boolean;
  settings: AnalysisSettings;
  analysis: Analysis | null;
}
