/*
 * ******************************************************************************
 *  * Copyright 2021 EMBL-EBI, Hinxton outstation
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *****************************************************************************
 */

package uk.ac.ebi.ena.app.menu.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum AccessionTypeEnum {

    EXPERIMENT("experiment_accession", "^[ESDR]RX[0-9]{6,}$", "SRA Experiment (e.g. ERX6534960)"),
    SAMPLE("sample_accession", "^SAM(E[A]?[0-9]{6,}|[ND][0-9]{8})$", "Biosample (e.g. SAMEA10254937)"),
    STUDY("study_accession", "^PRJ(EB|DB|NA)[0-9]+$", "Project a.k.a. Bioproject/Study (e.g. PRJEB47823)"),
    RUN("run_accession", "^[ESD]RR[0-9]{6,}$", "SRA Run (e.g. ERR6912696)"),
    ANALYSIS("analysis_accession", "^[ESD]RZ[0-9]{6,}$", "SRA Analysis (e.g. ERZ3914048)");

    private final static Map<String, AccessionTypeEnum> map = new HashMap<>();

    static {
        Arrays.stream(AccessionTypeEnum.values()).forEach(k -> map.put(k.accessionField, k));
    }

    private final String accessionField;
    private final String pattern;
    private final String display;

    public static AccessionTypeEnum getAccessionType(String accessionField) {
        return map.get(accessionField);
    }

    public static AccessionTypeEnum getAccessionTypeByPattern(String baseAccession) {
        for (AccessionTypeEnum t : values()) {
            if (Pattern.matches(t.getPattern(), baseAccession)) {
                return t;
            }
        }
        return null;
    }

    public static String getDisplayTypes() {
        return Arrays.stream(values()).map(AccessionTypeEnum::getDisplay).collect(Collectors.joining(", "));
    }
}
