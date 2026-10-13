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

import com.bioinfo.metabodark.model.ChemicalFamily;

/**
 * Chemical family guessed from a neutral loss when no library spectrum matches. The most specific
 * loss wins. These losses are the classical ones of untargeted metabolomics; they do not identify a
 * single molecule.
 */
public final class FamilyRules {

    private FamilyRules() {}

    /**
     * One diagnostic loss.
     *
     * @param daltons exact mass lost from the precursor
     * @param family family suggested by that loss
     * @param evidence stable code translated by the interfaces
     */
    public record Loss(double daltons, ChemicalFamily family, String evidence) {}

    private static final Loss[] LOSSES = {
        new Loss(183.066, ChemicalFamily.GLYCEROPHOSPHOCHOLINES, "LOSS_CHOLINE"),
        new Loss(141.019, ChemicalFamily.GLYCEROPHOSPHOETHANOLAMINES, "LOSS_PE"),
        new Loss(176.032, ChemicalFamily.GLUCURONIDES, "LOSS_GLUCURONIDE"),
        new Loss(162.053, ChemicalFamily.GLYCOSIDES, "LOSS_HEXOSE"),
        new Loss(146.058, ChemicalFamily.GLYCOSIDES, "LOSS_DEOXYHEXOSE"),
        new Loss(79.966, ChemicalFamily.ORGANOPHOSPHATES, "LOSS_PHOSPHATE"),
        new Loss(43.990, ChemicalFamily.CARBOXYLIC_ACIDS, "LOSS_CO2")
    };

    /**
     * Finds the first diagnostic loss present as a fragment.
     *
     * @param precursor precursor m/z
     * @param mz fragment m/z
     * @param intensity fragment intensity
     * @param toleranceDa absolute window
     * @return the loss, or null when none is present
     */
    public static Loss match(
            double precursor, double[] mz, double[] intensity, double toleranceDa) {
        if (!Double.isFinite(precursor) || mz.length == 0) {
            return null;
        }
        double max = 0;
        int peaks = Math.min(mz.length, intensity.length);
        for (int i = 0; i < peaks; i++) {
            if (intensity[i] > max) {
                max = intensity[i];
            }
        }
        if (max <= 0) {
            return null;
        }
        for (Loss loss : LOSSES) {
            double expected = precursor - loss.daltons();
            for (int i = 0; i < peaks; i++) {
                if (intensity[i] >= max * 0.05 && Math.abs(mz[i] - expected) <= toleranceDa) {
                    return loss;
                }
            }
        }
        return null;
    }
}
