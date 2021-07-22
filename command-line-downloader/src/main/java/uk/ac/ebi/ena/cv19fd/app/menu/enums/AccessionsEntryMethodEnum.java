package uk.ac.ebi.ena.cv19fd.app.menu.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by raheela on 06/07/2021.
 */
@Getter
@AllArgsConstructor
public enum AccessionsEntryMethodEnum {
    DOWNLOAD_FROM_FILE(1, "provide a file path"),
    DOWNLOAD_FROM_LIST(2, "provide list of accessions separated by commas");

    private int value;
    private String message;
    private static Map<Integer, AccessionsEntryMethodEnum> map = new HashMap<>();

    static {
        Arrays.stream(AccessionsEntryMethodEnum.values()).forEach(k -> map.put(k.value, k));
    }

    public static AccessionsEntryMethodEnum valueOf(Integer i) {
        return map.get(i);
    }

}
