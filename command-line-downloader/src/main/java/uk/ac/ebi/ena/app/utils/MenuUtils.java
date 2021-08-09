/*
 * ******************************************************************************
 *  * Copyright 2021 EMBL-EBI, Hinxton outstation
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *****************************************************************************
 */

package uk.ac.ebi.ena.app.utils;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import uk.ac.ebi.ena.app.constants.Constants;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.ac.ebi.ena.app.utils.CommonUtils.printSeparatorLine;

public class MenuUtils {
    public static final String exitMessage = "To exit enter 0 (zero)";
    public static final String validEmailMessage = "The provided email is invalid. Please enter a valid email address.";
    public static final String emailMessage = "If you would like to receive an email when the downloads are complete," +
            " type your email address here and press Enter. Or just press Enter to skip";
    public static final String welcomeMessage = "Welcome to the Ena file downloader utility!";
    public static final String lineMessage = "----------------------------------------------";
    public static final String accessionsErrorMessage = "Please provide valid accessions! ";
    public static final String accessionsFileErrorMessage = "Please provide a valid path to accessions file! ";
    public static final String invalidAsperaConnectMessage = "The Aspera Connect/CLI location entered is incorrect. " +
            "Please enter a valid location";

    public static final String accessionsSameTypeErrorMessage = "Please provide valid accessions of the same type! ";

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

    public static Map<String, List<String>>  parseAccessions(String accessions) {
        Map<String, List<String>> accessionDetailsMap = new HashMap<>();
        if (StringUtils.isEmpty(accessions)) {

            return accessionDetailsMap;
        } else if (new File(accessions).exists()) {
            List<String> accessionList =  accsFromFile(accessions);

            return CommonUtils.processAccessions(accessionList);
        } else {
            List<String> accessionList = Arrays.asList(accessions.split(","));

            return CommonUtils.processAccessions(accessionList);
        }
    }

    @SneakyThrows
    public static List<String> accsFromFile(String inputValues) {
        Stream<String> lines = Files.lines(Paths.get(inputValues), StandardCharsets.US_ASCII);

        return lines.collect(Collectors.toList());
    }


}
