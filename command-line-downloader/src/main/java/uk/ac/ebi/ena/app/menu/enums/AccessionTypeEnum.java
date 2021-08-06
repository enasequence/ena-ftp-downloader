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
