package uk.ac.ebi.ena.app.utils;


import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.lang3.SystemUtils;
import uk.ac.ebi.ena.app.constants.Constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static uk.ac.ebi.ena.app.constants.Constants.ACCESSION_LIST;
import static uk.ac.ebi.ena.app.constants.Constants.ACCESSION_TYPE;


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

    public static Map<String, List<String>> getAccessionDetails(List<String> accessions) {
        String baseAccession = accessions.get(0);
        String regex = "";
        String accessionType = "";
        Map<String, List<String>> accessionDetailsMap = null;
        if (Pattern.matches(Constants.sraExperimentPattern, baseAccession)) {
            regex = Constants.sraExperimentPattern;
            accessionType = Constants.EXPERIMENT;
        } else if (Pattern.matches(Constants.sraSamplePattern, baseAccession)) {
            regex = Constants.sraSamplePattern;
            accessionType = Constants.SAMPLE;
        } else if (Pattern.matches(Constants.projectPattern, baseAccession)) {
            regex = Constants.projectPattern;
            accessionType = Constants.PROJECT;
        } else if (Pattern.matches(Constants.sraRunPattern, baseAccession)) {
            regex = Constants.sraRunPattern;
            accessionType = Constants.RUN;
        } else if (Pattern.matches(Constants.analysisPattern, baseAccession)) {
            regex = Constants.analysisPattern;
            accessionType = Constants.ANALYSIS;
        }
        String finalRegex = regex;

        boolean isValid = accessions.stream().allMatch(acc -> Pattern.matches(finalRegex, acc));
        if (isValid) {
            accessionDetailsMap = getAccessionDetailsMap(accessionType, accessions);
        }

        return accessionDetailsMap;
    }

    public static Map<String, List<String>> getAccessionDetailsMap(String type, List<String> accessions) {
        Map<String, List<String>> accessionDetailsMap = new HashMap<>();
        accessionDetailsMap.put(ACCESSION_TYPE, Collections.singletonList(type));
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
