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

package com.bioinfo.metabodark.match;

import com.bioinfo.metabodark.model.Polarity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Small exact-mass formula search over C, H, N, O, P and S. This is only the first step of a tool
 * such as SIRIUS: it proposes formulas, it does not build a fragmentation tree.
 */
public final class FormulaFinder {

    /** Mass of a proton, used to recover the neutral mass from an ion. */
    public static final double PROTON = 1.007276;

    private static final double CARBON = 12.0;
    private static final double HYDROGEN = 1.007825;
    private static final double NITROGEN = 14.003074;
    private static final double OXYGEN = 15.994915;
    private static final double PHOSPHORUS = 30.973762;
    private static final double SULFUR = 31.972071;

    private FormulaFinder() {}

    /**
     * A formula close to the measured mass.
     *
     * @param formula Hill notation
     * @param ppm absolute error in parts per million of the neutral mass
     */
    public record Guess(String formula, double ppm) {}

    /**
     * Lists up to three formulas within {@code ppm} of the neutral mass.
     *
     * @param mz measured precursor m/z
     * @param charge precursor charge; a missing charge is treated as 1
     * @param polarity ion mode; unknown is treated as positive
     * @param ppm maximum error
     * @return the closest formulas, possibly empty
     */
    public static List<Guess> find(double mz, int charge, Polarity polarity, double ppm) {
        if (!Double.isFinite(mz) || ppm <= 0) {
            return List.of();
        }
        int z = charge < 1 ? 1 : charge;
        double neutral = polarity == Polarity.NEGATIVE ? mz * z + PROTON * z : mz * z - PROTON * z;
        if (neutral < 20 || neutral > 850) {
            return List.of();
        }
        double window = neutral * ppm * 1e-6;
        int maxCarbon = Math.min(30, (int) (neutral / 12) + 1);
        List<Guess> found = new ArrayList<>();
        for (int carbon = 0; carbon <= maxCarbon; carbon++) {
            for (int nitrogen = 0; nitrogen <= 6; nitrogen++) {
                for (int oxygen = 0; oxygen <= 10; oxygen++) {
                    for (int phosphorus = 0; phosphorus <= 2; phosphorus++) {
                        for (int sulfur = 0; sulfur <= 2; sulfur++) {
                            consider(
                                    found,
                                    neutral,
                                    window,
                                    carbon,
                                    nitrogen,
                                    oxygen,
                                    phosphorus,
                                    sulfur);
                        }
                    }
                }
            }
        }
        found.sort(Comparator.comparingDouble(Guess::ppm));
        if (found.size() > 3) {
            return List.copyOf(found.subList(0, 3));
        }
        return List.copyOf(found);
    }

    private static void consider(
            List<Guess> found,
            double neutral,
            double window,
            int carbon,
            int nitrogen,
            int oxygen,
            int phosphorus,
            int sulfur) {
        if (carbon == 0) {
            return;
        }
        double hetero =
                carbon * CARBON
                        + nitrogen * NITROGEN
                        + oxygen * OXYGEN
                        + phosphorus * PHOSPHORUS
                        + sulfur * SULFUR;
        int hydrogen = (int) Math.round((neutral - hetero) / HYDROGEN);
        if (hydrogen < 0 || hydrogen > 2 * carbon + nitrogen + 2 + phosphorus) {
            return;
        }
        if (phosphorus == 0 && (hydrogen + nitrogen) % 2 != 0) {
            return;
        }
        double mass = hetero + hydrogen * HYDROGEN;
        double error = Math.abs(mass - neutral);
        if (error > window) {
            return;
        }
        found.add(
                new Guess(
                        hill(carbon, hydrogen, nitrogen, oxygen, phosphorus, sulfur),
                        error / neutral * 1e6));
    }

    private static String hill(
            int carbon, int hydrogen, int nitrogen, int oxygen, int phosphorus, int sulfur) {
        StringBuilder text = new StringBuilder();
        append(text, "C", carbon);
        append(text, "H", hydrogen);
        append(text, "N", nitrogen);
        append(text, "O", oxygen);
        append(text, "P", phosphorus);
        append(text, "S", sulfur);
        return text.toString();
    }

    private static void append(StringBuilder text, String symbol, int count) {
        if (count <= 0) {
            return;
        }
        text.append(symbol);
        if (count > 1) {
            text.append(count);
        }
    }
}
