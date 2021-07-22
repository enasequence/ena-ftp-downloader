package uk.ac.ebi.ena.cv19fd.app.menu.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by raheela on 20/04/2021.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum DomainEnum {
    VIRAL_SEQUENCES(1, "Viral Sequences",
            Arrays.asList(DataTypeEnum.SEQUENCES, DataTypeEnum.REFERENCE_SEQUENCES,
                    DataTypeEnum.RAW_READS,
                    DataTypeEnum.SEQUENCED_SAMPLES, DataTypeEnum.STUDIES)),
    HOST_SEQUENCES(2, "Host Sequences",
            Arrays.asList(DataTypeEnum.HUMAN_READS, DataTypeEnum.OTHER_SPECIES_READS)),
    HELP(3, "Help", null),
    PRIVACY(4, "Privacy Notice", null);

    private int value;
    private String message;
    private List<DataTypeEnum> dataTypeEnums;

    private static Map<Integer, DomainEnum> map = new HashMap<>();

    private static Map<String, DomainEnum> mapWithString = new HashMap<>();

    static {
        Arrays.stream(DomainEnum.values()).forEach(k -> map.put(k.value, k));
        Arrays.stream(DomainEnum.values()).forEach(k -> mapWithString.put(k.message, k));
    }

    public static DomainEnum valueOf(Integer i) {
        return map.get(i);
    }
}
