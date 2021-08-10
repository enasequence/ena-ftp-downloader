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

package uk.ac.ebi.ena.app.menu.services;


import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.ac.ebi.ena.app.MainRunner;
import uk.ac.ebi.ena.app.constants.Constants;
import uk.ac.ebi.ena.app.menu.enums.AccessionsEntryMethodEnum;
import uk.ac.ebi.ena.app.menu.enums.ActionEnum;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.app.utils.CommonUtils;
import uk.ac.ebi.ena.app.utils.FileUtils;
import uk.ac.ebi.ena.app.utils.MenuUtils;
import uk.ac.ebi.ena.app.utils.ScannerUtils;
import uk.ac.ebi.ena.backend.service.BackendService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


@Component
@NoArgsConstructor
@Slf4j
public class MenuService {


    public static final String NONE = "NONE";
    private ScannerUtils scannerUtils;
    private BackendService backendService;

    @Autowired
    public MenuService(ScannerUtils scannerUtils, BackendService backendService) {
        this.scannerUtils = scannerUtils;
        this.backendService = backendService;
    }

    private String showEmailOption() {
        CommonUtils.printSeparatorLine();
        MenuUtils.printEmptyline();
        MenuUtils.printEmailMessage();
        CommonUtils.printSeparatorLine();
        String emailId = scannerUtils.getNextString();
        if (StringUtils.isNotEmpty(emailId)) {
            boolean isValidId = MenuUtils.isValidEmailAddress(emailId);
            if (!isValidId) {
                MenuUtils.printValidEmailMessage();
                return showEmailOption();
            }
            return emailId;
        } else {
            return NONE;
        }
    }

    private Map<String, List<String>> a1GetAccessionListFromFilePath(AccessionsEntryMethodEnum accessionsEntry) {
        System.out.println("*** Please provide full path to the file containing accessions.");
        CommonUtils.printSeparatorLine();
        MenuUtils.printBackMessage();

        String inputValues = scannerUtils.getNextString();
        MenuUtils.printEmptyline();
        if ("0".equalsIgnoreCase(inputValues)) {
            MainRunner.exit();
        } else if (inputValues.equalsIgnoreCase("b")) { // back
            aBuildAccessionEntryMenu();
        } else {
            try {
                if (Files.exists(Paths.get(inputValues))) {
                    List<String> accessions = MenuUtils.accsFromFile(inputValues);

                    if (!(CollectionUtils.isEmpty(accessions))) {
                        Map<String, List<String>> accessionDetailsMap = CommonUtils.processAccessions(accessions);
                        if (accessionDetailsMap != null) {
                            accessionDetailsMap.put(accessionsEntry.toString(), Collections.singletonList(inputValues));
                            return accessionDetailsMap;
                        } else {
                            System.out.println(MenuUtils.accessionsSameTypeErrorMessage);
                            MenuUtils.printEmptyline();
                            return a1GetAccessionListFromFilePath(accessionsEntry);
                        }
                    } else {
                        if (accessions == null) {
                            System.out.println(" Invalid accession list.Please enter accessions in valid format ");
                            System.out.println(MenuUtils.validAccessionsErrorMessage);
                        } else {
                            System.out.println(" Empty accession list");
                            System.out.println(MenuUtils.accessionsErrorMessage);
                        }
                        MenuUtils.printEmptyline();
                        return a1GetAccessionListFromFilePath(accessionsEntry);
                    }
                } else {
                    System.out.println(MenuUtils.accessionsFileErrorMessage);
                    MenuUtils.printEmptyline();
                    a1GetAccessionListFromFilePath(accessionsEntry);
                }
            } catch (Exception e) {
                log.error("Exception occurred while reading accessions from file.", e);
            }
        }
        return null;
    }

    private Map<String, List<String>> a2GetAccessionListFromCommaSeparated(AccessionsEntryMethodEnum accessionsEntryMethodEnum) {
        System.out.println("*** Please provide the list of accessions separated by commas.");
        CommonUtils.printSeparatorLine();
        MenuUtils.printBackMessage();
        String inputValues = scannerUtils.getNextString();
        MenuUtils.printEmptyline();

        if ("0".equalsIgnoreCase(inputValues)) {
            MainRunner.exit();
        } else if (inputValues.equalsIgnoreCase("b")) { // back
            aBuildAccessionEntryMenu();
        } else {
            String[] accessions = inputValues.split(",");
            if (accessions.length > 0) {
                Map<String, List<String>> accessionDetailsMap = CommonUtils.processAccessions(Arrays.asList(accessions));

                if (accessionDetailsMap != null) {
                    accessionDetailsMap.put(accessionsEntryMethodEnum.getMessage(), null);
                    return accessionDetailsMap;
                } else {
                    System.out.println(MenuUtils.accessionsErrorMessage);
                    return a2GetAccessionListFromCommaSeparated(accessionsEntryMethodEnum);
                }
            } else {
                System.out.println(MenuUtils.accessionsErrorMessage);
                return a2GetAccessionListFromCommaSeparated(accessionsEntryMethodEnum);
            }
        }
        return null;
    }

    public void aBuildAccessionEntryMenu() {
        System.out.println("*** How would you like to enter accessions?");
        CommonUtils.printSeparatorLine();
        for (AccessionsEntryMethodEnum accessionsEntryMethodEnum : AccessionsEntryMethodEnum.values()) {
            System.out.println(Constants.toMessage + accessionsEntryMethodEnum.getMessage() + Constants.enterMessage + accessionsEntryMethodEnum.getValue());
        }
        int input = scannerUtils.getNextInt();
        MenuUtils.printEmptyline();
        if (input == 0) { // Exit
            MainRunner.exit();
        }
        AccessionsEntryMethodEnum accessionsEntryMethodEnum = AccessionsEntryMethodEnum.valueOf(input);
        if (accessionsEntryMethodEnum != null) {
            Map<String, List<String>> accessionDetailsMap = new HashMap<>();
            switch (accessionsEntryMethodEnum) {
                case DOWNLOAD_FROM_FILE:
                    accessionDetailsMap = a1GetAccessionListFromFilePath(accessionsEntryMethodEnum);
                    break;
                case DOWNLOAD_FROM_LIST:
                    accessionDetailsMap = a2GetAccessionListFromCommaSeparated(accessionsEntryMethodEnum);
                    break;
            }
            if (!CollectionUtils.isEmpty(accessionDetailsMap)) {
                // proceed
                bShowDownloadFormatMenu(accessionDetailsMap);
            }
        } else {
            // replay
            MenuUtils.printInvalidMessage();
            aBuildAccessionEntryMenu();
        }

    }

    private void bShowDownloadFormatMenu(Map<String, List<String>> accessionDetailsMap) {
        System.out.println("**** To download data " +
                (!CollectionUtils.isEmpty(accessionDetailsMap) ? " for provided accessions" : "") + ", select the format.");
        CommonUtils.printSeparatorLine();

        String accessionField = accessionDetailsMap.get(Constants.ACCESSION_FIELD).get(0);
        List<DownloadFormatEnum> downloadFormatEnumList = Arrays.stream(DownloadFormatEnum.values())
                .filter(format -> format.getAccessionTypes().contains(accessionField))
                .collect(Collectors.toList());

        for (DownloadFormatEnum formatEnum : downloadFormatEnumList) {
            System.out.println(Constants.forMessage + formatEnum.getMessage() + Constants.enterMessage + formatEnum.getValue());

        }

        MenuUtils.printBackMessage();
        int input = scannerUtils.getNextInt();
        MenuUtils.printEmptyline();
        if (input == -1) { // back
            aBuildAccessionEntryMenu();
        } else {
            if (input == 0) {
                MainRunner.exit();
            }
            final DownloadFormatEnum format = DownloadFormatEnum.getFormat(downloadFormatEnumList, input);

            if (format != null) {
                // proceed
                cRequestDownloadLocation(format, accessionDetailsMap);
            } else {
                // replay
                MenuUtils.printInvalidMessage();
                bShowDownloadFormatMenu(accessionDetailsMap);
            }
        }


    }

    private String cRequestDownloadLocation(DownloadFormatEnum format, Map<String, List<String>> accessionDetailsMap) {
        System.out.println("***** Provide the full path to where you want to save downloaded files.");
        CommonUtils.printSeparatorLine();
        MenuUtils.printBackMessage();
        String input = scannerUtils.getNextString();
        MenuUtils.printEmptyline();
        if (input.equalsIgnoreCase("b")) { // back
            bShowDownloadFormatMenu(accessionDetailsMap);
        } else if (input.equalsIgnoreCase("0")) {
            MainRunner.exit();
        } else if (FileUtils.isDirectoryExists(input) && new File(input).canWrite()) {
            dRequestProtocolSelection(format, input, accessionDetailsMap);
        } else {
            // replay
            return cRequestDownloadLocation(format, accessionDetailsMap);
        }
        return input;
    }

    private String dRequestProtocolSelection(DownloadFormatEnum format, String location, Map<String, List<String>> accessionDetailsMap) {
        System.out.println("***** Choose the method of downloading:");
        CommonUtils.printSeparatorLine();
        for (ProtocolEnum protocolEnum : ProtocolEnum.values()) {
            System.out.println(Constants.toMessage + protocolEnum.getMessage() + "," + Constants.enterMessage + protocolEnum.getValue());
        }
        MenuUtils.printBackMessage();
        int input = scannerUtils.getNextInt();
        MenuUtils.printEmptyline();
        if (input == -1) {
            return cRequestDownloadLocation(format, accessionDetailsMap);
        }
        final ProtocolEnum protocolEnum = ProtocolEnum.valueOf(input);
        switch (protocolEnum) {
            case ASPERA:
                String asperaConnectLocation = d1RequestAsperaConnectOption(format, location, accessionDetailsMap);
                return eRequestEmailId(format, location, accessionDetailsMap, protocolEnum, asperaConnectLocation);
            case FTP:
                return eRequestEmailId(format, location, accessionDetailsMap, protocolEnum, null);
        }
        return null;

    }

    private String eRequestEmailId(DownloadFormatEnum format, String location, Map<String, List<String>> accessionDetailsMap,
                                   ProtocolEnum protocolEnum, String asperaConnectLocation) {
        CommonUtils.printSeparatorLine();
        MenuUtils.printEmptyline();
        MenuUtils.printEmailMessage();
        MenuUtils.printBackMessage();

        String input = scannerUtils.getNextString();
        MenuUtils.printEmptyline();
        if (input.equalsIgnoreCase("b")) { // back
            dRequestProtocolSelection(format, location, accessionDetailsMap);
        } else if (StringUtils.isNotEmpty(input)) {
            boolean isValidId = MenuUtils.isValidEmailAddress(input);
            if (!isValidId) {
                MenuUtils.printValidEmailMessage();
                return showEmailOption();
            } else {
                fShowConfirmationAndPerformAction(format, location, accessionDetailsMap, protocolEnum, asperaConnectLocation, input);
            }
        } else {
            fShowConfirmationAndPerformAction(format, location, accessionDetailsMap, protocolEnum, asperaConnectLocation, NONE);
        }
        return null;
    }

    private void fShowConfirmationAndPerformAction(DownloadFormatEnum format, String location,
                                                   Map<String, List<String>> accessionDetailsMap,
                                                   ProtocolEnum protocol, String asperaConnectLocation,
                                                   String emailId) {
        String msg = "You are ready to download " + format.getMessage()
                + " to " + location + " using " + protocol + ".";
        if (protocol == ProtocolEnum.ASPERA) {
            msg += " Aspera connect/CLI folder location:" + asperaConnectLocation;
        }
        System.out.println(msg);

        System.out.println("****** Choose between downloading and creating a script to run later.");
        CommonUtils.printSeparatorLine();
        ActionEnum actionEnumInput;
        for (ActionEnum actionEnum :
                ActionEnum.values()) {
            System.out.println(Constants.toMessage + actionEnum.getMessage() + "," + Constants.enterMessage + actionEnum.getValue());
        }
        MenuUtils.printBackMessage();
        int input = scannerUtils.getNextInt();
        MenuUtils.printEmptyline();
        if (input == -1) { // back
            eRequestEmailId(format, location, accessionDetailsMap, protocol, asperaConnectLocation);
        } else {
            if (input == 0) {
                MainRunner.exit();
            } else {
                actionEnumInput = ActionEnum.valueOf(input);
                if (actionEnumInput != null) {
                    switch (actionEnumInput) {
                        case CREATE_SCRIPT:
                            FileUtils.createDownloadScript(accessionDetailsMap, format, location, protocol,
                                    asperaConnectLocation, emailId);
                            CommonUtils.printSeparatorLine();
                            System.out.println("Script created=" + FileUtils.getScriptPath(accessionDetailsMap, format));
                            CommonUtils.printSeparatorLine();
                            break;
                        case CREATE_AND_DOWNLOAD:
                            CommonUtils.printSeparatorLine();
                            FileUtils.createDownloadScript(accessionDetailsMap, format, location, protocol,
                                    asperaConnectLocation, emailId);
                            CommonUtils.printSeparatorLine();
                            System.out.println("Script created at " + FileUtils.getScriptPath(accessionDetailsMap, format)
                                    + ". Download started.");
                            CommonUtils.printSeparatorLine();
                            startDownload(format, location, accessionDetailsMap, protocol, asperaConnectLocation, emailId);
                            break;
                    }
                } else {
                    // replay
                    fShowConfirmationAndPerformAction(format, location, accessionDetailsMap, protocol,
                            asperaConnectLocation, emailId);
                }
            }
        }

    }

    private void startDownload(DownloadFormatEnum format, String location, Map<String, List<String>> accessionDetailsMap
            , ProtocolEnum protocol, String asperaConnectLocation, String emailId) {
        try {
            log.info("Starting download at location {}", location);
            backendService.startDownload(format, location, accessionDetailsMap, protocol, asperaConnectLocation, emailId);
        } catch (Exception exception) {
            log.error("Exception encountered while starting download");
            exception.printStackTrace();
        }

    }

    private String d1RequestAsperaConnectOption(DownloadFormatEnum format, String location, Map<String, List<String>> accessionDetailsMap) {
        System.out.println("***** Please enter the path to your local Aspera Connect/CLI installation");
        CommonUtils.printSeparatorLine();
        MenuUtils.printBackMessage();
        String input = scannerUtils.getNextString();
        MenuUtils.printEmptyline();
        if (input.equalsIgnoreCase("b")) { // back
            return dRequestProtocolSelection(format, location, accessionDetailsMap);
        } else if (input.equalsIgnoreCase("0")) {
            MainRunner.exit();
        } else if (StringUtils.isNotEmpty(input)) {
            boolean isValidLocation = MenuUtils.isValidAsperaConnectLoc(input);
            if (!isValidLocation) {
                MenuUtils.printInvalidAsperaConnectLocation();
                d1RequestAsperaConnectOption(format, location, accessionDetailsMap);
            }
        } else {
            return d1RequestAsperaConnectOption(format, location, accessionDetailsMap);
        }
        return input;
    }

}
