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

@Getter
@AllArgsConstructor
public enum AccessionTypeEnum {

    EXPERIMENT("experiment_accession", "^[ESDR]RX[0-9]{6,}$"),
    SAMPLE("sample_accession", "((SAME[A]?[0-9]{6,}|SAM[ND][0-9]{8}))$"),
    STUDY("study_accession", "(^(PRJ[A-Z]{2}[0-9]+))$"),
    RUN("run_accession", "^[ESDR]RR[0-9]{6,}$"),
    ANALYSIS("analysis_accession", "([ESDR]RZ[0-9]{6,})");

    private final static Map<String, AccessionTypeEnum> map = new HashMap<>();

    static {
        Arrays.stream(AccessionTypeEnum.values()).forEach(k -> map.put(k.accessionField, k));
    }

    private final String accessionField;
    private final String pattern;

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

}
