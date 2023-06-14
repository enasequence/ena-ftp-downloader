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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.ac.ebi.ena.app.MainRunner;
import uk.ac.ebi.ena.app.constants.Constants;
import uk.ac.ebi.ena.app.menu.enums.*;
import uk.ac.ebi.ena.app.utils.CommonUtils;
import uk.ac.ebi.ena.app.utils.FileUtils;
import uk.ac.ebi.ena.app.utils.MenuUtils;
import uk.ac.ebi.ena.app.utils.ScannerUtils;
import uk.ac.ebi.ena.backend.dto.AuthenticationDetail;
import uk.ac.ebi.ena.backend.dto.DownloadJob;
import uk.ac.ebi.ena.backend.service.BackendService;
import uk.ac.ebi.ena.backend.service.EnaPortalService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static uk.ac.ebi.ena.app.MainRunner.ASPERA_PATH_MSG;


@Component
@NoArgsConstructor
@Slf4j
public class MenuService {

    final Logger console = LoggerFactory.getLogger("console");

    public static final String NONE = "NONE";
    private ScannerUtils scannerUtils;
    private BackendService backendService;

    private EnaPortalService portalService;


    @Autowired
    public MenuService(ScannerUtils scannerUtils, BackendService backendService, EnaPortalService portalService) {
        this.scannerUtils = scannerUtils;
        this.backendService = backendService;
        this.portalService = portalService;
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

    private DownloadJob a1GetAccessionListFromFilePath(AccessionsEntryMethodEnum accessionsEntry, AuthenticationDetail authenticationDetail) {
        System.out.println("*** Please provide full path to the file containing accessions.");
        CommonUtils.printSeparatorLine();
        MenuUtils.printBackMessage();

        String inputValues = scannerUtils.getNextString();
        MenuUtils.printEmptyline();
        if ("0".equalsIgnoreCase(inputValues)) {
            MainRunner.exit();
        } else if ("b".equalsIgnoreCase(inputValues)) { // back
            aBuildAccessionEntryMenu(authenticationDetail);
        } else {
            try {
                if (Files.exists(Paths.get(inputValues))) {
                    List<String> accessions = MenuUtils.accsFromFile(inputValues);

                    if (!(CollectionUtils.isEmpty(accessions))) {
                        DownloadJob downloadJob = CommonUtils.processAccessions(accessions);
                        if (downloadJob != null) {
                            downloadJob.setAccessionEntryMethod(accessionsEntry);
                            return downloadJob;
                        } else {
                            System.out.println(MenuUtils.accessionsSameTypeErrorMessage);
                            MenuUtils.printEmptyline();
                            return a1GetAccessionListFromFilePath(accessionsEntry, authenticationDetail);
                        }
                    } else {
                        if (accessions == null) {
                            System.out.println(" Unable to parse accessions. Are they in the correct format? ");
                            System.out.println(MenuUtils.validAccessionsErrorMessage);
                        } else {
                            System.out.println(" Empty accession list");
                            System.out.println(MenuUtils.accessionsErrorMessage);
                        }
                        MenuUtils.printEmptyline();
                        return a1GetAccessionListFromFilePath(accessionsEntry, authenticationDetail);
                    }
                } else {
                    System.out.println(MenuUtils.accessionsFileErrorMessage);
                    MenuUtils.printEmptyline();
                    a1GetAccessionListFromFilePath(accessionsEntry, authenticationDetail);
                }
            } catch (Exception e) {
                log.error("Exception occurred while reading accessions from file.", e);
            }
        }
        return null;
    }

    private DownloadJob a2GetAccessionListFromCommaSeparated(AccessionsEntryMethodEnum accessionsEntryMethodEnum, AuthenticationDetail authenticationDetail) {
        System.out.println("*** Please provide the list of accessions separated by commas.");
        CommonUtils.printSeparatorLine();
        MenuUtils.printBackMessage();
        String inputValues = scannerUtils.getNextString();
        MenuUtils.printEmptyline();

        if ("0".equalsIgnoreCase(inputValues)) {
            MainRunner.exit();
        } else if ("b".equalsIgnoreCase(inputValues)) { // back
            aBuildAccessionEntryMenu(authenticationDetail);
        } else {
            String[] accessions = inputValues.split(",");
            if (accessions.length > 0) {
                DownloadJob downloadJob = CommonUtils.processAccessions(Arrays.asList(accessions));

                if (downloadJob != null) {
                    downloadJob.setAccessionEntryMethod(accessionsEntryMethodEnum);
                    return downloadJob;
                } else {
                    System.out.println(MenuUtils.accessionsErrorMessage);
                    return a2GetAccessionListFromCommaSeparated(accessionsEntryMethodEnum, authenticationDetail);
                }
            } else {
                System.out.println(MenuUtils.accessionsErrorMessage);
                return a2GetAccessionListFromCommaSeparated(accessionsEntryMethodEnum, authenticationDetail);
            }
        }
        return null;
    }

    private DownloadJob a2GetAccessionListFromQuery(AccessionsEntryMethodEnum accessionsEntryMethodEnum, AuthenticationDetail authenticationDetail){
        System.out.println("*** Please provide the search query for downloads");
        CommonUtils.printSeparatorLine();
        MenuUtils.printBackMessage();
        String inputValues = scannerUtils.getNextString();
        MenuUtils.printEmptyline();

        if ("0".equalsIgnoreCase(inputValues)) {
            MainRunner.exit();
        } else if (inputValues.equalsIgnoreCase("b")) { // back
            aBuildAccessionEntryMenu(authenticationDetail);
        } else {
            boolean isValid = MenuUtils.validateSearchRequest(inputValues);

            if (isValid) {
                Long count = portalService.getCount(inputValues);
                if (count != null) {
                    if (count > 0) {
                        System.out.println("This query matches " + count + " records.");
                    } else if (count == 0) {
                        System.out.println("No records matching the query could be found.");
                        System.exit(0);
                    }

                    List<String> accessions = CommonUtils.getAccessionFromQuery(inputValues);
                    DownloadJob downloadJob;
                    if (!CollectionUtils.isEmpty(accessions)) {
                        downloadJob = CommonUtils.processAccessions(accessions);
                    } else {
                        downloadJob = CommonUtils.processQuery(inputValues);
                    }
                    downloadJob.setQuery(inputValues);
                    if (downloadJob != null) {
                        downloadJob.setAccessionEntryMethod(accessionsEntryMethodEnum);
                        return downloadJob;
                    }
                }
            }
            System.out.println(MenuUtils.queryErrorMessage);
            return a2GetAccessionListFromQuery(accessionsEntryMethodEnum, authenticationDetail);

        }
        return null;
    }

    public void showTypeOfDataMenu() {
        AuthenticationDetail authenticationDetail = null;
        CommonUtils.printSeparatorLine();
        System.out.println("\n*** What do you want to download? ***");

        for (TypeOfDataEnum typeOfDataEnum : TypeOfDataEnum.values()) {
            System.out.println(typeOfDataEnum.getMessage() + typeOfDataEnum.getValue());
        }

        String input = scannerUtils.getNextString();
        MenuUtils.printEmptyline();
        if (StringUtils.isEmpty(input)) {
            aBuildAccessionEntryMenu(authenticationDetail);
        } else if ("0".equals(input)) {
            MainRunner.exit();
        } else if ("1".equals(input)) {
            aBuildAccessionEntryMenu(authenticationDetail);
        } else if ("2".equals(input)) { // Flow to download the files from datahub
            requestForDataHubCredentials(authenticationDetail);
        }
    }

    public void requestForDataHubCredentials(AuthenticationDetail authenticationDetail) {

        CommonUtils.printSeparatorLine();
        MenuUtils.printEmptyline();
        MenuUtils.printUserNameMessage();
        String input = scannerUtils.getNextString();

        if ("b".equals(input)) {
            showTypeOfDataMenu();
        } else if (StringUtils.isNotEmpty(input) && StringUtils.startsWith(input, "dcc_")) {
            String password = requestForDataHubPassword();

            authenticationDetail = new AuthenticationDetail();

            authenticationDetail.setUserName(input);
            authenticationDetail.setPassword(password);

            if (portalService.authenticateUser(authenticationDetail)) {
                aBuildAccessionEntryMenu(authenticationDetail);
            } else {
                System.out.println("Data hub username and/or password is incorrect.");
                requestForDataHubCredentials(authenticationDetail);
            }
        } else {
            System.out.println("Please provide valid dcc username(starts with dcc_)");
            requestForDataHubCredentials(authenticationDetail);
        }
    }


    public String requestForDataHubPassword() {
        CommonUtils.printSeparatorLine();
        MenuUtils.printEmptyline();
        MenuUtils.printPasswordMessage();
        MenuUtils.printEmptyline();

        return scannerUtils.getNextString();
    }

    public void aBuildAccessionEntryMenu(AuthenticationDetail authenticationDetail) {
        System.out.println("*** How would you like to enter accessions?");
        CommonUtils.printSeparatorLine();
        for (AccessionsEntryMethodEnum accessionsEntryMethodEnum : AccessionsEntryMethodEnum.values()) {
            System.out.println(Constants.toMessage + accessionsEntryMethodEnum.getMessage() + Constants.enterMessage + accessionsEntryMethodEnum.getValue());
        }
        MenuUtils.printBackMessage();
        String input = scannerUtils.getNextString();
        MenuUtils.printEmptyline();
        if ("0".equals(input)) { // Exit
            MainRunner.exit();
        } else if ("b".equalsIgnoreCase(input)) { // back
            if (authenticationDetail != null) {
                requestForDataHubCredentials(authenticationDetail);
            } else {
                aBuildAccessionEntryMenu(authenticationDetail);
            }
        }

        AccessionsEntryMethodEnum accessionsEntryMethodEnum = AccessionsEntryMethodEnum.valueOf(Integer.parseInt(input));
        if (accessionsEntryMethodEnum != null) {
            DownloadJob downloadJob = new DownloadJob();
            switch (accessionsEntryMethodEnum) {
                case DOWNLOAD_FROM_FILE:
                    downloadJob = a1GetAccessionListFromFilePath(accessionsEntryMethodEnum, authenticationDetail);
                    break;
                case DOWNLOAD_FROM_LIST:
                    downloadJob = a2GetAccessionListFromCommaSeparated(accessionsEntryMethodEnum, authenticationDetail);
                    break;

                case DOWNLOAD_FROM_QUERY:
                    downloadJob = a2GetAccessionListFromQuery(accessionsEntryMethodEnum, authenticationDetail);
            }
            if (accessionsEntryMethodEnum == AccessionsEntryMethodEnum.DOWNLOAD_FROM_QUERY
                    || !CollectionUtils.isEmpty(downloadJob.getAccessionList())) {
                // proceed
                bShowDownloadFormatMenu(downloadJob, authenticationDetail);
            }
        } else {
            // replay
            MenuUtils.printInvalidMessage();
            aBuildAccessionEntryMenu(authenticationDetail);
        }

    }

    private void bShowDownloadFormatMenu(DownloadJob downloadJob, AuthenticationDetail authenticationDetail) {
        System.out.println("**** To download data " +
                (!CollectionUtils.isEmpty(downloadJob.getAccessionList()) ? " for provided accessions" : "") + ", select the format.");
        CommonUtils.printSeparatorLine();

        String accessionField = downloadJob.getAccessionField();
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
            aBuildAccessionEntryMenu(authenticationDetail);
        } else {
            if (input == 0) {
                MainRunner.exit();
            }
            final DownloadFormatEnum format = DownloadFormatEnum.getFormat(downloadFormatEnumList, input);

            if (format != null) {
                // proceed
                cRequestDownloadLocation(format, downloadJob, authenticationDetail);
            } else {
                // replay
                MenuUtils.printInvalidMessage();
                bShowDownloadFormatMenu(downloadJob, authenticationDetail);
            }
        }


    }

    private String cRequestDownloadLocation(DownloadFormatEnum format, DownloadJob downloadJob, AuthenticationDetail authenticationDetail) {
        System.out.println("***** Provide the full path to where you want to save downloaded files.");
        CommonUtils.printSeparatorLine();
        MenuUtils.printBackMessage();
        String input = scannerUtils.getNextString();
        MenuUtils.printEmptyline();
        if ("b".equalsIgnoreCase(input)) { // back
            bShowDownloadFormatMenu(downloadJob, authenticationDetail);
        } else if ("0".equalsIgnoreCase(input)) {
            MainRunner.exit();
        } else if (StringUtils.isNotEmpty(input)) {
            if (FileUtils.isDirectoryExists(input) && new File(input).canWrite()) {
                if (Objects.nonNull(authenticationDetail)) {
                    //Set FTP protocol selection and skip protocol selection menu
                    eRequestEmailId(format, input, downloadJob, ProtocolEnum.FTP, null, authenticationDetail);
                } else {
                    dRequestProtocolSelection(format, input, downloadJob, null);
                }
            }
        } else {
            // replay
            return cRequestDownloadLocation(format, downloadJob, authenticationDetail);
        }
        return input;
    }

    private String dRequestProtocolSelection(DownloadFormatEnum format, String location, DownloadJob downloadJob,
                                             AuthenticationDetail authenticationDetail) {
        System.out.println("***** Choose the method of downloading:");
        CommonUtils.printSeparatorLine();
        for (ProtocolEnum protocolEnum : ProtocolEnum.values()) {
            System.out.println(Constants.toMessage + protocolEnum.getMessage() + "," + Constants.enterMessage + protocolEnum.getValue());
        }
        MenuUtils.printBackMessage();
        int input = scannerUtils.getNextInt();
        MenuUtils.printEmptyline();
        if (input == -1) {
            return cRequestDownloadLocation(format, downloadJob, authenticationDetail);
        }
        final ProtocolEnum protocolEnum = ProtocolEnum.valueOf(input);
        switch (protocolEnum) {
            case ASPERA:
                String asperaConnectLocation = d1RequestAsperaConnectOption(format, location, downloadJob, null);
                return eRequestEmailId(format, location, downloadJob, protocolEnum, asperaConnectLocation, null);
            case FTP:
                return eRequestEmailId(format, location, downloadJob, protocolEnum, null, authenticationDetail);
        }
        return null;

    }

    private String eRequestEmailId(DownloadFormatEnum format, String location, DownloadJob downloadJob,
                                   ProtocolEnum protocolEnum, String asperaConnectLocation, AuthenticationDetail authenticationDetail) {
        CommonUtils.printSeparatorLine();
        MenuUtils.printEmptyline();
        MenuUtils.printEmailMessage();
        MenuUtils.printBackMessage();

        String input = scannerUtils.getNextString();
        MenuUtils.printEmptyline();
        if ("b".equalsIgnoreCase(input)) { // back
            dRequestProtocolSelection(format, location, downloadJob, authenticationDetail);
        } else if (StringUtils.isNotEmpty(input)) {
            boolean isValidId = MenuUtils.isValidEmailAddress(input);
            if (!isValidId) {
                MenuUtils.printValidEmailMessage();
                return showEmailOption();
            } else {
                fShowConfirmationAndPerformAction(format, location, downloadJob, protocolEnum, asperaConnectLocation, input, authenticationDetail);
            }
        } else {
            fShowConfirmationAndPerformAction(format, location, downloadJob, protocolEnum, asperaConnectLocation, NONE, authenticationDetail);
        }
        return null;
    }

    private void fShowConfirmationAndPerformAction(DownloadFormatEnum format, String location,
                                                   DownloadJob downloadJob,
                                                   ProtocolEnum protocol, String asperaConnectLocation,
                                                   String emailId, AuthenticationDetail authenticationDetail) {
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
            eRequestEmailId(format, location, downloadJob, protocol, asperaConnectLocation, authenticationDetail);
        } else {
            if (input == 0) {
                MainRunner.exit();
            } else {
                actionEnumInput = ActionEnum.valueOf(input);
                if (actionEnumInput != null) {
                    switch (actionEnumInput) {
                        case CREATE_SCRIPT:
                            FileUtils.createDownloadScript(downloadJob, format, location, protocol,
                                    asperaConnectLocation, emailId, authenticationDetail);
                            CommonUtils.printSeparatorLine();
                            System.out.println("Script created=" + FileUtils.getScriptPath(downloadJob, format));
                            CommonUtils.printSeparatorLine();
                            break;
                        case CREATE_AND_DOWNLOAD:
                            CommonUtils.printSeparatorLine();
                            FileUtils.createDownloadScript(downloadJob, format, location, protocol,
                                    asperaConnectLocation, emailId, authenticationDetail);
                            CommonUtils.printSeparatorLine();
                            System.out.println("Script created at " + FileUtils.getScriptPath(downloadJob, format)
                                    + ". Download started.");
                            CommonUtils.printSeparatorLine();
                            startDownload(format, location, downloadJob, protocol, asperaConnectLocation, emailId, authenticationDetail);
                            break;
                    }
                } else {
                    // replay
                    fShowConfirmationAndPerformAction(format, location, downloadJob, protocol,
                            asperaConnectLocation, emailId, authenticationDetail);
                }
            }
        }

    }

    private void startDownload(DownloadFormatEnum format, String location, DownloadJob downloadJob
            , ProtocolEnum protocol, String asperaConnectLocation, String emailId,
                               AuthenticationDetail authenticationDetail) {
        try {
            console.info("Starting download at location {}", location);
            backendService.startDownload(format, location, downloadJob, protocol, asperaConnectLocation, emailId
                    , authenticationDetail);
        } catch (Exception exception) {
            log.error("Exception encountered while starting download");
            exception.printStackTrace();
        }

    }

    private String d1RequestAsperaConnectOption(DownloadFormatEnum format, String location, DownloadJob downloadJob, AuthenticationDetail authenticationDetail) {
        System.out.println("***** " + ASPERA_PATH_MSG);
        CommonUtils.printSeparatorLine();
        MenuUtils.printBackMessage();
        String input = scannerUtils.getNextString();
        MenuUtils.printEmptyline();
        if ("b".equalsIgnoreCase(input)) { // back
            return dRequestProtocolSelection(format, location, downloadJob, authenticationDetail);
        } else if ("0".equalsIgnoreCase(input)) {
            MainRunner.exit();
        } else if (StringUtils.isNotEmpty(input)) {
            boolean isValidLocation = MenuUtils.isValidAsperaConnectLoc(input);
            if (!isValidLocation) {
                MenuUtils.printInvalidAsperaConnectLocation();
                d1RequestAsperaConnectOption(format, location, downloadJob, authenticationDetail);
            }
        } else {
            return d1RequestAsperaConnectOption(format, location, downloadJob, authenticationDetail);
        }
        return input;
    }

}
