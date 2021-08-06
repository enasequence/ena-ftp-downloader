package uk.ac.ebi.ena.app.utils;


import lombok.SneakyThrows;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import uk.ac.ebi.ena.app.constants.Constants;
import uk.ac.ebi.ena.app.menu.enums.AccessionTypeEnum;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static uk.ac.ebi.ena.app.constants.Constants.ACCESSION_FIELD;
import static uk.ac.ebi.ena.app.constants.Constants.ACCESSION_LIST;


public class CommonUtils {

    public static int stringToInput(String str) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            System.out.println("Provide a valid input to proceed!");
        }
        return new ScannerUtils().getNextInt();
    }

    public static void printSeparatorLine() {
        System.out.println(MenuUtils.lineMessage);
    }

    public static ProgressBarBuilder getProgressBarBuilder(String s, int size) {
        return new ProgressBarBuilder()
                .setTaskName(s)
                .setStyle(ProgressBarStyle.ASCII)
                .setInitialMax(size);
    }

    @SneakyThrows
    public static Map<String, List<String>> processAccessions(List<String> accessions) {
        String baseAccession = accessions.get(0);
        Map<String, List<String>> accessionDetailsMap = null;
        AccessionTypeEnum type = AccessionTypeEnum.getAccessionTypeByPattern(baseAccession);
        if (type != null) {
            boolean isValid = accessions.stream().allMatch(acc -> Pattern.matches(type.getPattern(), acc));
            if (isValid) {
                accessionDetailsMap = getAccessionDetailsMap(type.getAccessionField(), accessions);
            }
            return accessionDetailsMap;
        } else {
            System.out.println("Unsupported accession type provided:" + baseAccession + " Supported types are "
                    + StringUtils.join(AccessionTypeEnum.values(), ", "));
            throw new Exception("Unsupported accession type provided:" + baseAccession);
        }
    }

    public static Map<String, List<String>> getAccessionDetailsMap(String accessionField, List<String> accessions) {
        Map<String, List<String>> accessionDetailsMap = new HashMap<>();
        accessionDetailsMap.put(ACCESSION_FIELD, Collections.singletonList(accessionField));
        accessionDetailsMap.put(ACCESSION_LIST, accessions);
        return accessionDetailsMap;
    }

    public static String getAscpExtension() {
        return SystemUtils.IS_OS_WINDOWS ? ".exe" : "";
    }

    public static String getAscpFileName() {
        return Constants.ascpFileName + CommonUtils.getAscpExtension();
    }
}
