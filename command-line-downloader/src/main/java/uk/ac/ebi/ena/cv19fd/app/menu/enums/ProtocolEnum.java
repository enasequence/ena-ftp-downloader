package uk.ac.ebi.ena.cv19fd.app.menu.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum ProtocolEnum {
    FTP(1, "download files from ftp"),
    ASPERA(2, "download files using Aspera"),
    EXIT(0, "Exit");

    private final static Map<Integer, ProtocolEnum> map = new HashMap<>();

    static {
        Arrays.stream(ProtocolEnum.values()).forEach(k -> map.put(k.value, k));
    }

    private final int value;
    private final String message;

    public static ProtocolEnum valueOf(Integer i) {
        return map.get(i);
    }

}

