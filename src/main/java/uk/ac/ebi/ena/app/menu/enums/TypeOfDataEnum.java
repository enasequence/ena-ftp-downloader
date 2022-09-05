package uk.ac.ebi.ena.app.menu.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum TypeOfDataEnum {
    PUBLIC_DATA(1, "For public data, just hit enter or press "),
    DATA_HUB(2, "From a specific data hub (dcc), enter "),
    EXIT(0, "To Exit enter ");

    private final static Map<Integer, TypeOfDataEnum> map = new HashMap<>();

    static {
        Arrays.stream(TypeOfDataEnum.values()).forEach(k -> map.put(k.value, k));
    }

    private final int value;
    private final String message;

    public static TypeOfDataEnum valueOf(Integer i) {
        return map.get(i);
    }
}
