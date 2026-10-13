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

package com.bioinfo.metabodark.example;

import com.bioinfo.metabodark.parse.PeakDecoder;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * A tiny mzML written by the application. It is simulated plasma, not a patient file: two library
 * matches, one hexose loss and one spectrum that stays in the dark metabolome.
 */
public final class SimulatedStudy {

    /** Identifier accepted by the web API. */
    public static final String ID = "demo-plasma";

    /** File name shown in the interface. */
    public static final String FILE_NAME = "demo.mzML";

    private SimulatedStudy() {}

    /** The demonstration file. */
    public static byte[] bytes() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<mzML xmlns=\"http://psi.hupo.org/ms/mzml\">\n");
        xml.append("<run><spectrumList>\n");
        fullScan(xml);
        fragment(
                xml,
                "positive",
                166.0863,
                1,
                new double[] {103.0542, 120.0808, 166.0863},
                new double[] {40, 100, 15});
        fragment(
                xml,
                "negative",
                191.0197,
                1,
                new double[] {87.0088, 111.0088, 191.0197},
                new double[] {40, 100, 30});
        double glycoside = 343.1234;
        fragment(
                xml,
                "positive",
                glycoside,
                1,
                new double[] {100.0, glycoside - 162.053},
                new double[] {10, 100});
        fragment(xml, "positive", 410.2, 1, new double[] {100.0, 250.0}, new double[] {80, 40});
        xml.append("</spectrumList></run></mzML>\n");
        return xml.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void fullScan(StringBuilder xml) {
        xml.append("<spectrum id=\"scan=1\">");
        xml.append("<cvParam accession=\"MS:1000511\" value=\"1\"/>");
        xml.append(
                "<cvParam accession=\"MS:1000016\" value=\"0.5\" unitAccession=\"UO:0000010\"/>");
        binary(xml, new double[] {150.0, 300.0}, new double[] {1000, 800});
        xml.append("</spectrum>\n");
    }

    private static void fragment(
            StringBuilder xml,
            String mode,
            double precursor,
            int charge,
            double[] mz,
            double[] intensity) {
        String accession = "positive".equals(mode) ? "MS:1000130" : "MS:1000129";
        xml.append("<spectrum id=\"fragment\">");
        xml.append("<cvParam accession=\"MS:1000511\" value=\"2\"/>");
        xml.append(
                "<cvParam accession=\"MS:1000016\" value=\"12.5\" unitAccession=\"UO:0000031\"/>");
        xml.append("<cvParam accession=\"").append(accession).append("\"/>");
        xml.append("<precursor><selectedIon>");
        xml.append("<cvParam accession=\"MS:1000744\" value=\"").append(precursor).append("\"/>");
        xml.append("<cvParam accession=\"MS:1000041\" value=\"").append(charge).append("\"/>");
        xml.append("</selectedIon></precursor>");
        binary(xml, mz, intensity);
        xml.append("</spectrum>\n");
    }

    private static void binary(StringBuilder xml, double[] mz, double[] intensity) {
        xml.append("<binaryDataArrayList>");
        array(xml, "MS:1000514", mz);
        array(xml, "MS:1000515", intensity);
        xml.append("</binaryDataArrayList>");
    }

    private static void array(StringBuilder xml, String accession, double[] values) {
        String encoded = PeakDecoder.encode(values, true, ByteOrder.LITTLE_ENDIAN, false);
        xml.append("<binaryDataArray>");
        xml.append("<cvParam accession=\"MS:1000523\"/>");
        xml.append("<cvParam accession=\"").append(accession).append("\"/>");
        xml.append("<binary>").append(encoded).append("</binary>");
        xml.append("</binaryDataArray>");
    }
}
