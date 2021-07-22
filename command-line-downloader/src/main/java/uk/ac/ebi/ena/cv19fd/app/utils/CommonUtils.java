package uk.ac.ebi.ena.cv19fd.app.utils;


import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.lang3.SystemUtils;
import uk.ac.ebi.ena.cv19fd.app.constants.Constants;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DataTypeEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DomainEnum;

import java.util.regex.Pattern;

/**
 * Created by raheela on 20/04/2021.
 */
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
        ProgressBarBuilder pbb = new ProgressBarBuilder()
                .setTaskName(s)
                .setStyle(ProgressBarStyle.ASCII)
                .setInitialMax(size);
        return pbb;
    }

    public static boolean isAccessionValid(String accession, DomainEnum domainName, DataTypeEnum dataTypeEnum) {
        switch (domainName) {
            case VIRAL_SEQUENCES:
                switch (dataTypeEnum) {
                    case SEQUENCES:
                    case REFERENCE_SEQUENCES:
                        return Pattern.matches(Constants.emblPattern, accession);
                    case RAW_READS:
                        return Pattern.matches(Constants.sraExperimentPattern, accession);
                    case SEQUENCED_SAMPLES:
                        return Pattern.matches(Constants.sraSamplePattern, accession);
                    case STUDIES:
                        return Pattern.matches(Constants.projectPattern, accession);
                }
            case HOST_SEQUENCES:
                switch (dataTypeEnum) {
                    case HUMAN_READS:
                    case OTHER_SPECIES_READS:
                        return Pattern.matches(Constants.sraExperimentPattern, accession);
                }
        }

        return false;
    }

    public static String getAscpExtension() {
        return SystemUtils.IS_OS_WINDOWS ? ".exe" : "";
    }

    public static String getAscpFileName() {
        return Constants.ascpFileName + CommonUtils.getAscpExtension();
    }
}
