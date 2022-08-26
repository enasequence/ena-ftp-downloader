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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ena.app.MainRunner;
import uk.ac.ebi.ena.app.constants.Constants;
import uk.ac.ebi.ena.app.menu.enums.*;
import uk.ac.ebi.ena.app.utils.CommonUtils;
import uk.ac.ebi.ena.app.utils.FileUtils;
import uk.ac.ebi.ena.app.utils.MenuUtils;
import uk.ac.ebi.ena.app.utils.ScannerUtils;
import uk.ac.ebi.ena.backend.dto.DownloadJob;
import uk.ac.ebi.ena.backend.service.BackendService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
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

    private RestTemplate restTemplate;


    @Autowired
    public MenuService(ScannerUtils scannerUtils, BackendService backendService, RestTemplate restTemplate) {
        this.scannerUtils = scannerUtils;
        this.backendService = backendService;
        this.restTemplate = restTemplate;
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

    private DownloadJob a1GetAccessionListFromFilePath(AccessionsEntryMethodEnum accessionsEntry, String userName, String password) {
        System.out.println("*** Please provide full path to the file containing accessions.");
        CommonUtils.printSeparatorLine();
        MenuUtils.printBackMessage();

        String inputValues = scannerUtils.getNextString();
        MenuUtils.printEmptyline();
        if ("0".equalsIgnoreCase(inputValues)) {
            MainRunner.exit();
        } else if (inputValues.equalsIgnoreCase("b")) { // back
            aBuildAccessionEntryMenu(userName, password);
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
                            return a1GetAccessionListFromFilePath(accessionsEntry, userName, password);
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
                        return a1GetAccessionListFromFilePath(accessionsEntry, userName, password);
                    }
                } else {
                    System.out.println(MenuUtils.accessionsFileErrorMessage);
                    MenuUtils.printEmptyline();
                    a1GetAccessionListFromFilePath(accessionsEntry, userName, password);
                }
            } catch (Exception e) {
                log.error("Exception occurred while reading accessions from file.", e);
            }
        }
        return null;
    }

    private DownloadJob a2GetAccessionListFromCommaSeparated(AccessionsEntryMethodEnum accessionsEntryMethodEnum, String userName, String password) {
        System.out.println("*** Please provide the list of accessions separated by commas.");
        CommonUtils.printSeparatorLine();
        MenuUtils.printBackMessage();
        String inputValues = scannerUtils.getNextString();
        MenuUtils.printEmptyline();

        if ("0".equalsIgnoreCase(inputValues)) {
            MainRunner.exit();
        } else if (inputValues.equalsIgnoreCase("b")) { // back
            aBuildAccessionEntryMenu(userName, password);
        } else {
            String[] accessions = inputValues.split(",");
            if (accessions.length > 0) {
                DownloadJob downloadJob = CommonUtils.processAccessions(Arrays.asList(accessions));

                if (downloadJob != null) {
                    downloadJob.setAccessionEntryMethod(accessionsEntryMethodEnum);
                    return downloadJob;
                } else {
                    System.out.println(MenuUtils.accessionsErrorMessage);
                    return a2GetAccessionListFromCommaSeparated(accessionsEntryMethodEnum, userName, password);
                }
            } else {
                System.out.println(MenuUtils.accessionsErrorMessage);
                return a2GetAccessionListFromCommaSeparated(accessionsEntryMethodEnum, userName, password);
            }
        }
        return null;
    }

    public void showTypeOfDataMenu() {
        CommonUtils.printSeparatorLine();
        System.out.println("*** What do you want to download ?");

        for (TypeOfDataEnum typeOfDataEnum : TypeOfDataEnum.values()) {
            System.out.println(Constants.toMessage + typeOfDataEnum.getMessage() + Constants.enterMessage + typeOfDataEnum.getValue());
        }

        int input = scannerUtils.getNextInt();
        MenuUtils.printEmptyline();
        if (input == 0) {
            MainRunner.exit();
        } else if (input == 1) {
            aBuildAccessionEntryMenu(null, null);
        } else {
            requestForDataHubCredentials();
        }
    }

    public void requestForDataHubCredentials() {

        CommonUtils.printSeparatorLine();
        MenuUtils.printEmptyline();
        MenuUtils.printUserNameMessage();

        String userName = scannerUtils.getNextString();
        MenuUtils.printEmptyline();

        if (StringUtils.isNotEmpty(userName) && StringUtils.startsWith(userName, "dcc_")) {
            String password = requestForDataHubPassword();
            if (validateDataHubCredentials(userName, password, getDataPortalId(userName))) {
                aBuildAccessionEntryMenu(userName, password);
            } else {
                System.out.println("Data hub username and or password are not correct.");
                requestForDataHubCredentials();
            }
        } else {
            System.out.println("Please provide valid dcc username(starts with dcc_)");
            requestForDataHubCredentials();
        }
    }

    private String getDataPortalId(String userName) {
        if (StringUtils.isNotEmpty(userName) &&
                "metagenome".equalsIgnoreCase(StringUtils.substringAfter(userName, "_"))) {
            return "metagenome";
        } else {
            return "pathogen";
        }
    }

    public boolean validateDataHubCredentials(String userName, String password, String dataPortalId) {

        String portalAPIAuthEndpoint = Constants.PORTAL_API_EP + "/auth?dataPortal=" + dataPortalId;
        log.info("portalAPIAuthEndpoint: " + portalAPIAuthEndpoint);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Accept", Constants.APPLICATION_JSON);
        httpHeaders.setBasicAuth(userName, password);

        restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(userName, password));
        try {
            ResponseEntity<String> resp = restTemplate.exchange(portalAPIAuthEndpoint, HttpMethod.GET, null, String.class);
            if (resp.getStatusCode() == HttpStatus.OK) {
                return true;
            } else {
                return false;
            }
        } catch (RestClientException restClientException) {
            log.error(" Data hub authorization failed-  " + restClientException.getMessage());
            return false;
        }

    }

    public String requestForDataHubPassword() {
        CommonUtils.printSeparatorLine();
        MenuUtils.printEmptyline();
        MenuUtils.printPasswordMessage();

        String input = scannerUtils.getNextString();
        MenuUtils.printEmptyline();

        if (StringUtils.isNotEmpty(input)) {
            return input;
        }
        return null;
    }

    public void aBuildAccessionEntryMenu(String userName, String password) {
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
            DownloadJob downloadJob = new DownloadJob();
            switch (accessionsEntryMethodEnum) {
                case DOWNLOAD_FROM_FILE:
                    downloadJob = a1GetAccessionListFromFilePath(accessionsEntryMethodEnum, userName, password);
                    break;
                case DOWNLOAD_FROM_LIST:
                    downloadJob = a2GetAccessionListFromCommaSeparated(accessionsEntryMethodEnum, userName, password);
                    break;
            }
            if (!CollectionUtils.isEmpty(downloadJob.getAccessionList())) {
                // proceed
                bShowDownloadFormatMenu(downloadJob, userName, password);
            }
        } else {
            // replay
            MenuUtils.printInvalidMessage();
            aBuildAccessionEntryMenu(userName, password);
        }

    }

    private void bShowDownloadFormatMenu(DownloadJob downloadJob, String userName, String password) {
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
            aBuildAccessionEntryMenu(userName, password);
        } else {
            if (input == 0) {
                MainRunner.exit();
            }
            final DownloadFormatEnum format = DownloadFormatEnum.getFormat(downloadFormatEnumList, input);

            if (format != null) {
                // proceed
                cRequestDownloadLocation(format, downloadJob, userName, password);
            } else {
                // replay
                MenuUtils.printInvalidMessage();
                bShowDownloadFormatMenu(downloadJob, userName, password);
            }
        }


    }

    private String cRequestDownloadLocation(DownloadFormatEnum format, DownloadJob downloadJob, String userName, String password) {
        System.out.println("***** Provide the full path to where you want to save downloaded files.");
        CommonUtils.printSeparatorLine();
        MenuUtils.printBackMessage();
        String input = scannerUtils.getNextString();
        MenuUtils.printEmptyline();
        if (input.equalsIgnoreCase("b")) { // back
            bShowDownloadFormatMenu(downloadJob, userName, password);
        } else if (input.equalsIgnoreCase("0")) {
            MainRunner.exit();
        } else if (FileUtils.isDirectoryExists(input) && new File(input).canWrite()) {
            dRequestProtocolSelection(format, input, downloadJob, userName, password);
        } else {
            // replay
            return cRequestDownloadLocation(format, downloadJob, userName, password);
        }
        return input;
    }

    private String dRequestProtocolSelection(DownloadFormatEnum format, String location, DownloadJob downloadJob,
                                             String userName, String password) {
        System.out.println("***** Choose the method of downloading:");
        CommonUtils.printSeparatorLine();
        for (ProtocolEnum protocolEnum : ProtocolEnum.values()) {
            System.out.println(Constants.toMessage + protocolEnum.getMessage() + "," + Constants.enterMessage + protocolEnum.getValue());
        }
        MenuUtils.printBackMessage();
        int input = scannerUtils.getNextInt();
        MenuUtils.printEmptyline();
        if (input == -1) {
            return cRequestDownloadLocation(format, downloadJob, userName, password);
        }
        final ProtocolEnum protocolEnum = ProtocolEnum.valueOf(input);
        switch (protocolEnum) {
            case ASPERA:
                String asperaConnectLocation = d1RequestAsperaConnectOption(format, location, downloadJob, userName, password);
                return eRequestEmailId(format, location, downloadJob, protocolEnum, asperaConnectLocation, userName, password);
            case FTP:
                return eRequestEmailId(format, location, downloadJob, protocolEnum, null, userName, password);
        }
        return null;

    }

    private String eRequestEmailId(DownloadFormatEnum format, String location, DownloadJob downloadJob,
                                   ProtocolEnum protocolEnum, String asperaConnectLocation, String userName, String password) {
        CommonUtils.printSeparatorLine();
        MenuUtils.printEmptyline();
        MenuUtils.printEmailMessage();
        MenuUtils.printBackMessage();

        String input = scannerUtils.getNextString();
        MenuUtils.printEmptyline();
        if (input.equalsIgnoreCase("b")) { // back
            dRequestProtocolSelection(format, location, downloadJob, userName, password);
        } else if (StringUtils.isNotEmpty(input)) {
            boolean isValidId = MenuUtils.isValidEmailAddress(input);
            if (!isValidId) {
                MenuUtils.printValidEmailMessage();
                return showEmailOption();
            } else {
                fShowConfirmationAndPerformAction(format, location, downloadJob, protocolEnum, asperaConnectLocation, input, userName, password);
            }
        } else {
            fShowConfirmationAndPerformAction(format, location, downloadJob, protocolEnum, asperaConnectLocation, NONE, userName, password);
        }
        return null;
    }

    private void fShowConfirmationAndPerformAction(DownloadFormatEnum format, String location,
                                                   DownloadJob downloadJob,
                                                   ProtocolEnum protocol, String asperaConnectLocation,
                                                   String emailId, String userName, String password) {
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
            eRequestEmailId(format, location, downloadJob, protocol, asperaConnectLocation, userName, password);
        } else {
            if (input == 0) {
                MainRunner.exit();
            } else {
                actionEnumInput = ActionEnum.valueOf(input);
                if (actionEnumInput != null) {
                    switch (actionEnumInput) {
                        case CREATE_SCRIPT:
                            FileUtils.createDownloadScript(downloadJob, format, location, protocol,
                                    asperaConnectLocation, emailId);
                            CommonUtils.printSeparatorLine();
                            System.out.println("Script created=" + FileUtils.getScriptPath(downloadJob, format));
                            CommonUtils.printSeparatorLine();
                            break;
                        case CREATE_AND_DOWNLOAD:
                            CommonUtils.printSeparatorLine();
                            FileUtils.createDownloadScript(downloadJob, format, location, protocol,
                                    asperaConnectLocation, emailId);
                            CommonUtils.printSeparatorLine();
                            System.out.println("Script created at " + FileUtils.getScriptPath(downloadJob, format)
                                    + ". Download started.");
                            CommonUtils.printSeparatorLine();
                            startDownload(format, location, downloadJob, protocol, asperaConnectLocation, emailId, userName, password);
                            break;
                    }
                } else {
                    // replay
                    fShowConfirmationAndPerformAction(format, location, downloadJob, protocol,
                            asperaConnectLocation, emailId, userName, password);
                }
            }
        }

    }

    private void startDownload(DownloadFormatEnum format, String location, DownloadJob downloadJob
            , ProtocolEnum protocol, String asperaConnectLocation, String emailId,
                               String userName, String password) {
        try {
            console.info("Starting download at location {}", location);
            backendService.startDownload(format, location, downloadJob, protocol, asperaConnectLocation, emailId
                    , userName, password);
        } catch (Exception exception) {
            log.error("Exception encountered while starting download");
            exception.printStackTrace();
        }

    }

    private String d1RequestAsperaConnectOption(DownloadFormatEnum format, String location, DownloadJob downloadJob, String userName, String password) {
        System.out.println("***** " + ASPERA_PATH_MSG);
        CommonUtils.printSeparatorLine();
        MenuUtils.printBackMessage();
        String input = scannerUtils.getNextString();
        MenuUtils.printEmptyline();
        if (input.equalsIgnoreCase("b")) { // back
            return dRequestProtocolSelection(format, location, downloadJob, userName, password);
        } else if (input.equalsIgnoreCase("0")) {
            MainRunner.exit();
        } else if (StringUtils.isNotEmpty(input)) {
            boolean isValidLocation = MenuUtils.isValidAsperaConnectLoc(input);
            if (!isValidLocation) {
                MenuUtils.printInvalidAsperaConnectLocation();
                d1RequestAsperaConnectOption(format, location, downloadJob, userName, password);
            }
        } else {
            return d1RequestAsperaConnectOption(format, location, downloadJob, userName, password);
        }
        return input;
    }

}
