package uk.ac.ebi.ena.app.menu.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum HowToDownloadEnum {

    ACCESSIONS(1, "To download by providing accessions enter "),
    SEARCH_QUERY(2, "To download by providing query enter "),
    EXIT(0, "To Exit enter ");

    private static final Map<Integer, HowToDownloadEnum> map = new HashMap<>();

    static {
        Arrays.stream(HowToDownloadEnum.values()).forEach(k -> map.put(k.value, k));
    }

    private final int value;
    private final String message;
}
