package uk.ac.ebi.ena.app.menu.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


@Getter
@AllArgsConstructor
public enum ActionEnum {
    CREATE_AND_DOWNLOAD(1, "start downloading right now, and also create a script that can be invoked " +
            "directly"),
    CREATE_SCRIPT(2, "create a script that can be invoked directly (e.g. by a pipeline or a script)");

    private final int value;
    private final String message;
    private final static Map<Integer, ActionEnum> map = new HashMap<>();

    static {
        Arrays.stream(ActionEnum.values()).forEach(k -> map.put(k.value, k));
    }

    public static ActionEnum valueOf(Integer i) {
        return map.get(i);
    }

}
