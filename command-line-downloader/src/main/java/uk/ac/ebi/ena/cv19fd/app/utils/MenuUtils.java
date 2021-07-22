package uk.ac.ebi.ena.cv19fd.app.utils;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import uk.ac.ebi.ena.cv19fd.app.constants.Constants;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.ac.ebi.ena.cv19fd.app.utils.CommonUtils.printSeparatorLine;

public class MenuUtils {
    public static final String exitMessage = "To exit enter 0 (zero)";
    public static final String validEmailMessage = "The provided email is invalid. Please enter a valid email address.";
    public static final String emailMessage = "If you would like to receive an email when the downloads are complete," +
            " type your email address here and press Enter. Or just press Enter to skip";
    public static final String zeroMessage = " (zero)";
    public static final String welcomeMessage = "Welcome to the Covid-19 Data Portal's data downloader utility!";
    public static final String lineMessage = "----------------------------------------------";
    public static final String helpMessage = "This is a standalone tool that allows you to download data from " +
            "EMBL-EBI ENA as seen on the Covid 19 Data Portal (https://www.covid19dataportal.org/). " +
            "This can be used to download data directly or to setup a script that can be invoked directly to get " +
            "up-to-date data.\n\n" +
            "First, select between viral and host data:\n" +
            "  1. Viral Sequences : Raw and assembled sequences and analyses of SARS-CoV-2 and other " +
            "coronaviruses. \n" +
            "  2. Host Sequences : Raw and assembled sequences and analyses of human and other hosts.\n\n" +
            "Next, select the type of data:\n" +
            "  1. Sequences : Assembled/annotated Sequences for SARS-CoV-2 and other coronaviruses.\n" +
            "  2. Reference Sequences : Reference genomes for SARS-CoV-2 and other coronaviruses.\n" +
            "  3. Raw Reads : Raw read datasets in SARS-CoV-2 and other coronaviruses.\n" +
            "  4. Sequenced Samples : Sequenced samples for SARS-CoV-2 and other coronaviruses.\n" +
            "  5. Studies : Studies (projects) dealing with SARS-CoV-2 and other coronaviruses.\n" +
            "  7. Human Reads : Raw read datasets from human hosts.\n" +
            "  8. Other Species Reads : All Other Species Reads in SARS-CoV-2 and other coronaviruses.\n\n" +
            "Next, select from the available formats for the data type:\n" +
            "  1. EMBL flatfile : Sequence and annotations in a flat text file. \n" +
            "  2. FASTA : Sequence in FASTA format.\n" +
            "  3. XML : Metadata objects in XML format.\n" +
            "  4. FASTQ : Raw reads processed into FASTQ format.\n" +
            "  5. SUBMITTED : Original format the read files were submitted in.\n\n" +
            "If you want to download read files in FASTQ/SUBMITTED format, select the protocol:\n" +
            "  1. Download files using FTP. \n" +
            "  2. Download files using ASPERA.\n" +
            "If the protocol selected is ASPERA, give the location of Local Aspera Connect/CLI folder.\n\n" +
            "With the above choices made, you can perform these actions:\n" +
            "  1. Create script : create a script with parameters based on your choices, which " +
            "can be then called directly without going through the above menus again.\n" +
            "  2. Create and download : create the script and also start downloading data immediately.\n\n";
    public static final String privacyMessage = "The execution of this tool may require limited processing of your " +
            "personal data to function. By using this tool you are agreeing to this as outlined in our\n" +
            "Privacy Notice: https://www.ebi.ac.uk/data-protection/privacy-notice/ena-presentation\n\n" +
            "Copyright © EMBL 2021 | EMBL-EBI is part of the European Molecular Biology Laboratory.\n\n" +
            "Terms of Use: https://www.ebi.ac.uk/about/terms-of-use.\n\n";
    public static final String accessionsErrorMessage = "Please provide valid accessions! ";
    public static final String invalidAsperaConnectMessage = "The Aspera Connect/CLI location entered is incorrect. " +
            "Please enter a valid location";

    public static void showHelpMessage() {
        System.out.println(MenuUtils.helpMessage);
        printSeparatorLine();
    }

    public static void showPrivacyMessage() {
        System.out.println(MenuUtils.privacyMessage);
        printSeparatorLine();
    }

    public static void printBackMessage() {
        System.out.println(Constants.backMessage);
        printExitMessage();
    }


    public static void printEmailMessage() {
        System.out.println(MenuUtils.emailMessage);
    }

    public static void printEmptyline() {
        System.out.println("");
    }


    public static void printInvalidMessage() {
        System.out.println("Invalid option entered. Please try again.");
    }
    public static void printInvalidAsperaConnectLocation() {
        System.out.println(MenuUtils.invalidAsperaConnectMessage);
    }

    public static boolean isValidAsperaConnectLoc(String asperaConnectLocation) {
        boolean isValidLocation = false;
        String ascpFile = Constants.ascpFileName + CommonUtils.getAscpExtension();
        if (Files.exists(Paths.get(asperaConnectLocation))) {
            if (Files.exists(Paths.get(asperaConnectLocation, Constants.binFolder))
                    && Files.exists(Paths.get(asperaConnectLocation, Constants.binFolder, ascpFile))
                    && Files.exists(Paths.get(asperaConnectLocation, Constants.etcFolder))
                    && Files.exists(Paths.get(asperaConnectLocation, Constants.etcFolder, Constants.asperaWebFile))) {
                isValidLocation = true;
            }
        }
        return isValidLocation;
    }


    public static void printValidEmailMessage() {
        System.out.println(MenuUtils.validEmailMessage);
    }

    public static void printExitMessage() {
        System.out.println(MenuUtils.exitMessage);
        printSeparatorLine();
    }

    public static boolean isValidEmailAddress(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException ex) {
            result = false;
        }
        return result;
    }

    public static List<String> parseAccessions(String accessions) {
        if (StringUtils.isEmpty(accessions)) {
            return new ArrayList<>();
        }
        if (new File(accessions).exists()) {
            return accsFromFile(accessions);
        } else {
            return Arrays.asList(accessions.split(","));
        }
    }

    @SneakyThrows
    public static List<String> accsFromFile(String inputValues) {
        Stream<String> lines = Files.lines(Paths.get(inputValues), StandardCharsets.US_ASCII);
        List<String> accessions = lines.collect(Collectors.toList());
        return accessions;
    }

}
