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

package com.bioinfo.metabodark.library;

import com.bioinfo.metabodark.model.ChemicalFamily;
import com.bioinfo.metabodark.model.LibraryEntry;
import com.bioinfo.metabodark.model.Polarity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Reference library shipped with the application: six teaching spectra planted in the simulated
 * plasma, and experimental MS/MS spectra of MassBank Europe under open licenses (see
 * massbank-sources.txt next to the data file).
 */
public final class ReferenceLibrary {

    private static final String MASSBANK = "massbank.tsv.gz";

    private ReferenceLibrary() {}

    /** Every reference: the teaching spectra first, then MassBank. */
    public static List<LibraryEntry> entries() {
        List<LibraryEntry> all = new ArrayList<>(curated());
        all.addAll(massBank());
        return List.copyOf(all);
    }

    /** MassBank spectra, read once from the resources. */
    public static List<LibraryEntry> massBank() {
        return MassBankHolder.ENTRIES;
    }

    /** Lazy holder, so tests of the teaching spectra do not read the MassBank file. */
    private static final class MassBankHolder {
        private static final List<LibraryEntry> ENTRIES = load();

        private static List<LibraryEntry> load() {
            try (InputStream raw = ReferenceLibrary.class.getResourceAsStream(MASSBANK)) {
                if (raw == null) {
                    return List.of();
                }
                return parse(new GZIPInputStream(raw));
            } catch (IOException e) {
                throw new UncheckedIOException("The MassBank library could not be read.", e);
            }
        }
    }

    /**
     * Reads the tab-separated MassBank lines written by tools/massbank/build-library.mjs:
     * accession, name, formula, InChIKey, polarity sign, precursor m/z and "mz:intensity" peaks.
     * Malformed lines are skipped.
     *
     * @param in uncompressed text
     * @return the entries, in file order
     * @throws IOException when the stream fails
     */
    static List<LibraryEntry> parse(InputStream in) throws IOException {
        List<LibraryEntry> entries = new ArrayList<>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            LibraryEntry entry = line(line);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static LibraryEntry line(String line) {
        String[] fields = line.split("\t", -1);
        if (fields.length != 7) {
            return null;
        }
        String[] peaks = fields[6].isBlank() ? new String[0] : fields[6].split(" ");
        double[] mz = new double[peaks.length];
        double[] intensity = new double[peaks.length];
        try {
            for (int i = 0; i < peaks.length; i++) {
                int colon = peaks[i].indexOf(':');
                mz[i] = Double.parseDouble(peaks[i].substring(0, colon));
                intensity[i] = Double.parseDouble(peaks[i].substring(colon + 1));
            }
            double precursor = Double.parseDouble(fields[5]);
            if (peaks.length == 0 || !(precursor > 0)) {
                return null;
            }
            Polarity polarity = "-".equals(fields[4]) ? Polarity.NEGATIVE : Polarity.POSITIVE;
            return new LibraryEntry(
                    fields[1],
                    fields[2],
                    fields[3],
                    "MassBank " + fields[0],
                    precursor,
                    polarity,
                    ChemicalFamily.UNCLASSIFIED,
                    mz,
                    intensity);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return null;
        }
    }

    /** The six teaching spectra, in a stable order. */
    public static List<LibraryEntry> curated() {
        return List.of(
                entry(
                        "Phenylalanine",
                        "C9H11NO2",
                        "COLNVLDHVKWLRT-QMMMGPOBSA-N",
                        "HMDB0000159",
                        166.0863,
                        Polarity.POSITIVE,
                        ChemicalFamily.AMINO_ACIDS,
                        new double[] {103.0542, 120.0808, 166.0863},
                        new double[] {40, 100, 15}),
                entry(
                        "Tryptophan",
                        "C11H12N2O2",
                        "QIVBCDIJIAJPQS-VIFPVBQESA-N",
                        "HMDB0000929",
                        205.0972,
                        Polarity.POSITIVE,
                        ChemicalFamily.AMINO_ACIDS,
                        new double[] {146.0600, 188.0706, 205.0972},
                        new double[] {35, 100, 20}),
                entry(
                        "Caffeine",
                        "C8H10N4O2",
                        "RYYVLZVUVIJVGH-UHFFFAOYSA-N",
                        "HMDB0001847",
                        195.0877,
                        Polarity.POSITIVE,
                        ChemicalFamily.ALKALOIDS,
                        new double[] {110.0713, 138.0662, 195.0877},
                        new double[] {40, 100, 25}),
                entry(
                        "Glucose",
                        "C6H12O6",
                        "WQZGKKKJIJFFOK-GASJEMHNSA-N",
                        "HMDB0000122",
                        179.0561,
                        Polarity.NEGATIVE,
                        ChemicalFamily.CARBOHYDRATES,
                        new double[] {59.0139, 89.0244, 179.0561},
                        new double[] {30, 55, 100}),
                entry(
                        "Citrate",
                        "C6H8O7",
                        "KRKNYBCHXYNGOX-UHFFFAOYSA-N",
                        "HMDB0000094",
                        191.0197,
                        Polarity.NEGATIVE,
                        ChemicalFamily.CARBOXYLIC_ACIDS,
                        new double[] {87.0088, 111.0088, 191.0197},
                        new double[] {40, 100, 30}),
                entry(
                        "LPC 18:1",
                        "C26H52NO7P",
                        "",
                        "",
                        522.3554,
                        Polarity.POSITIVE,
                        ChemicalFamily.GLYCEROPHOSPHOCHOLINES,
                        new double[] {104.1070, 184.0733, 522.3554},
                        new double[] {30, 100, 20}));
    }

    private static LibraryEntry entry(
            String name,
            String formula,
            String inchikey,
            String reference,
            double precursor,
            Polarity polarity,
            ChemicalFamily family,
            double[] mz,
            double[] intensity) {
        return new LibraryEntry(
                name, formula, inchikey, reference, precursor, polarity, family, mz, intensity);
    }
}
