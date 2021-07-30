package uk.ac.ebi.ena.app.menu.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


@Getter
@AllArgsConstructor
public enum DownloadOptionMenuEnum {
    DOWNLOAD_ALL(1, "download ALL records in this type"),
    DOWNLOAD_LIST(2, "provide a list of accessions (as a comma separated list or in a text file)");

    private final int value;
    private final String message;
    private final static Map<Integer, DownloadOptionMenuEnum> map = new HashMap<>();

    static {
        Arrays.stream(DownloadOptionMenuEnum.values()).forEach(k -> map.put(k.value, k));
    }

}