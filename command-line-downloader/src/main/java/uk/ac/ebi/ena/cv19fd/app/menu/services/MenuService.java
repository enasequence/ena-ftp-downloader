package uk.ac.ebi.ena.cv19fd.app.menu.services;


import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import uk.ac.ebi.ena.cv19fd.app.MainRunner;
import uk.ac.ebi.ena.cv19fd.app.constants.Constants;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.*;
import uk.ac.ebi.ena.cv19fd.app.utils.CommonUtils;
import uk.ac.ebi.ena.cv19fd.app.utils.FileUtils;
import uk.ac.ebi.ena.cv19fd.app.utils.MenuUtils;
import uk.ac.ebi.ena.cv19fd.app.utils.ScannerUtils;
import uk.ac.ebi.ena.cv19fd.backend.service.BackendService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.ac.ebi.ena.cv19fd.app.utils.CommonUtils.printSeparatorLine;
import static uk.ac.ebi.ena.cv19fd.app.utils.MenuUtils.*;

/**
 * Created by raheela on 20/04/2021.
 */
@Component
@NoArgsConstructor
@Slf4j
public class MenuService {


    public static final String NONE = "NONE";
    private ScannerUtils scannerUtils;
    private String email;
    private BackendService backendService;

    @Autowired
    public MenuService(ScannerUtils scannerUtils, BackendService backendService) {
        this.scannerUtils = scannerUtils;
        this.backendService = backendService;
    }

    public void bBuildDataTypeMenu(DomainEnum domainEnum) {
        System.out.println("** Within " + domainEnum.getMessage() + " select type of data");
        printSeparatorLine();
        if (domainEnum.getDataTypeEnums() != null) {
            for (DataTypeEnum dataType :
                    domainEnum.getDataTypeEnums()) {
                System.out.println(Constants.forMessage + dataType.getMessage() + Constants.enterMessage
                        + dataType.getValue());
            }
            printBackMessage();
            int input = scannerUtils.getNextInt();
            printEmptyline();
            if (input == -1) {
                // back
                aBuildDomainMenu();
            } else {
                if (input == 0) {
                    MainRunner.exit();
                }
                final DataTypeEnum dataTypeEnum = DataTypeEnum.valueOf(domainEnum.getValue(), input);
                if (dataTypeEnum != null) {
                    // proceed
                    c1BuildSelectionMenu(domainEnum, dataTypeEnum);
                } else {
                    // replay
                    printInvalidMessage();
                    bBuildDataTypeMenu(domainEnum);
                }
            }
        }

    }

    private String e1RequestDownloadLocation(DomainEnum domainEnum, DataTypeEnum dataTypeEnum,
                                             DownloadFormatEnum format, List<String> accessionList) {
        System.out.println("***** Provide the full path to where you want to save downloaded " +
                "files.");
        printSeparatorLine();
        printBackMessage();
        String input = scannerUtils.getNextString();
        printEmptyline();
        if (input.equalsIgnoreCase("b")) { // back
            dShowDownloadFormatMenu(domainEnum, dataTypeEnum, new ArrayList<>());
        } else if (input.equalsIgnoreCase("0")) {
            MainRunner.exit();
        } else if (FileUtils.isDirectoryExists(input)) {
            ProtocolEnum protocol = ProtocolEnum.FTP;
            String asperaConnectLocation = null;
            if (format == DownloadFormatEnum.FASTQ || format == DownloadFormatEnum.SUBMITTED) {
                asperaConnectLocation = e2BuildProtocolMenuAndGetSelectedProtocol(domainEnum, dataTypeEnum, format, accessionList);
                if (StringUtils.isNotBlank(asperaConnectLocation)) {
                    protocol = ProtocolEnum.ASPERA;
                }
            }
            fShowConfirmationAndPerformAction(domainEnum, dataTypeEnum, format, input, accessionList, protocol,
                    asperaConnectLocation);
        } else {
            // replay
            return e1RequestDownloadLocation(domainEnum, dataTypeEnum, format, accessionList);
        }
        return input;
    }

    public void aBuildDomainMenu() {
        System.out.println("* Select from the options below:");
        printSeparatorLine();
        for (DomainEnum domainEnum : DomainEnum.values()) {
            System.out.println(Constants.forMessage + domainEnum.getMessage() + Constants.enterMessage
                    + domainEnum.getValue());
        }
        printExitMessage();
        int choice = scannerUtils.getNextInt();
        printEmptyline();
        if (choice == 0) {
            MainRunner.exit();
        } else {
            DomainEnum selectedDomainEnum = DomainEnum.valueOf(choice);
            if (selectedDomainEnum != null) {
                if (DomainEnum.HELP == selectedDomainEnum) {
                    showHelpMessage();
                    aBuildDomainMenu();
                } else if (DomainEnum.PRIVACY == selectedDomainEnum) {
                    showPrivacyMessage();
                    aBuildDomainMenu();
                } else {
                    bBuildDataTypeMenu(selectedDomainEnum);
                }
            } else {
                printInvalidMessage();
                aBuildDomainMenu();
            }
        }
    }

    /**
     * formats
     *
     * @param domain
     * @param dataType
     * @param accessionList
     */
    public void dShowDownloadFormatMenu(DomainEnum domain, DataTypeEnum dataType, List<String> accessionList) {
        System.out.println("**** To download " + dataType.getMessage() +
                (!CollectionUtils.isEmpty(accessionList) ? " for provided accessions" : "") + ", select the format.");
        printSeparatorLine();
        for (DownloadFormatEnum downloadFormatEnum :
                dataType.getFormats()) {
            System.out.println(Constants.forMessage + downloadFormatEnum.getMessage() + Constants.enterMessage
                    + downloadFormatEnum.getValue());

        }
        printBackMessage();
        int input = scannerUtils.getNextInt();
        printEmptyline();
        if (input == -1) { // back
            c1BuildSelectionMenu(domain, dataType);
        } else {
            if (input == 0) {
                MainRunner.exit();
            }
            final DownloadFormatEnum format = DownloadFormatEnum.getFormat(dataType.getFormats(), input);
            if (format != null) {
                // proceed
                e1RequestDownloadLocation(domain, dataType, format, accessionList);
            } else {
                // replay
                printInvalidMessage();
                dShowDownloadFormatMenu(domain, dataType, accessionList);
            }
        }
    }


    private String showEmailOption() {
        printSeparatorLine();
        printEmptyline();
        printEmailMessage();
        printSeparatorLine();
        String emailId = scannerUtils.getNextString();
        if (StringUtils.isNotEmpty(emailId)) {
            boolean isValidId = isValidEmailAddress(emailId);
            if (!isValidId) {
                printValidEmailMessage();
                return showEmailOption();
            }
            return emailId;
        } else {
            return NONE;
        }
    }

    public void fShowConfirmationAndPerformAction(DomainEnum domainEnum, DataTypeEnum dataTypeEnum,
                                                  DownloadFormatEnum format, String location, List<String> accessionList,
                                                  ProtocolEnum protocol, String asperaConnectLocation) {
        String msg = "You are ready to download " + format.getMessage()
                + " of " + dataTypeEnum.getMessage() + " under " + domainEnum.getMessage()
                + " to " + location + " using " + protocol + ".";
        if (protocol == ProtocolEnum.ASPERA) {
            msg += " Aspera connect/CLI folder location:" + asperaConnectLocation;
        }
        System.out.println(msg);
        if (StringUtils.isBlank(email)) {
            email = showEmailOption();
        }
        printEmptyline();

        System.out.println("****** Choose between downloading and creating a script to run later.");
        printSeparatorLine();
        ActionEnum actionEnumInput;
        for (ActionEnum actionEnum :
                ActionEnum.values()) {
            System.out.println(Constants.toMessage + actionEnum.getMessage() + "," + Constants.enterMessage + actionEnum.getValue());
        }
        printBackMessage();
        int input = scannerUtils.getNextInt();
        printEmptyline();
        if (input == -1) { // back
            e1RequestDownloadLocation(domainEnum, dataTypeEnum, format, accessionList);
        } else {
            if (input == 0) {
                MainRunner.exit();
            } else {
                actionEnumInput = ActionEnum.valueOf(input);
                if (actionEnumInput != null) {
                    switch (actionEnumInput) {
                        case CREATE_SCRIPT:
                            FileUtils.createDownloadScript(location, domainEnum, dataTypeEnum,
                                    format, email, accessionList, protocol, asperaConnectLocation);
                            printSeparatorLine();
                            System.out.println("Script created=" + FileUtils.getScriptPath(domainEnum,
                                    dataTypeEnum, format, accessionList));
                            printSeparatorLine();
                            break;
                        case CREATE_AND_DOWNLOAD:
                            printSeparatorLine();
                            FileUtils.createDownloadScript(location, domainEnum, dataTypeEnum,
                                    format, email, accessionList, protocol, asperaConnectLocation);
                            printSeparatorLine();
                            System.out.println("Script created at " + FileUtils.getScriptPath(domainEnum,
                                    dataTypeEnum, format, accessionList) + ". Download started.");
                            printSeparatorLine();
                            startDownload(domainEnum, dataTypeEnum, format, location, email, accessionList, protocol,
                                    asperaConnectLocation);
                            break;
                    }
                } else {
                    // replay
                    fShowConfirmationAndPerformAction(domainEnum, dataTypeEnum, format, location, accessionList,
                            protocol, asperaConnectLocation);
                }
            }
        }
    }

    private void startDownload(DomainEnum domainEnum, DataTypeEnum dataTypeEnum, DownloadFormatEnum format,
                               String location, String receipientMailId, List<String> accessionList,
                               ProtocolEnum protocol, String asperaConnectLocation) {
        try {
            boolean isValid = backendService.isDownloadLocationValid(location);
            if (isValid) {
                log.info("Download location:{} is valid. Starting download..", location);
                backendService.startDownload(location, domainEnum, dataTypeEnum, format,
                        receipientMailId, accessionList, protocol, asperaConnectLocation);
            } else {
                log.info("Download location:{} is not valid. Will not start the download", location);
            }
        } catch (Exception exception) {
            log.error("Exception encountered while starting download");
            exception.printStackTrace();
        }
    }

    /**
     * all records or a list of accs
     *
     * @param domain
     * @param dataType
     */
    public void c1BuildSelectionMenu(DomainEnum domain, DataTypeEnum dataType) {
        System.out.println("*** Which records do you want to download?");
        printSeparatorLine();
        for (DownloadOptionMenuEnum downloadOptionMenuEnum : DownloadOptionMenuEnum.values()) {
            System.out.println(Constants.toMessage + downloadOptionMenuEnum.getMessage() + Constants.enterMessage + downloadOptionMenuEnum.getValue());
        }
        printBackMessage();
        int input = scannerUtils.getNextInt();
        printEmptyline();
        if (input == -1) {
            // back
            bBuildDataTypeMenu(domain);
        } else {
            if (input == 0) {
                MainRunner.exit();
            }
            DownloadOptionMenuEnum downloadOptionMenuEnum = DownloadOptionMenuEnum.valueOf(input);
            if (downloadOptionMenuEnum != null) {
                // proceed
                switch (downloadOptionMenuEnum) {
                    case DOWNLOAD_ALL:
                        dShowDownloadFormatMenu(domain, dataType, new ArrayList<>());
                        break;
                    case DOWNLOAD_LIST:
                        c2ShowAccessionEntryMethodMenu(domain, dataType);
                        break;
                }
            } else {
                // replay
                printInvalidMessage();
                c1BuildSelectionMenu(domain, dataType);
            }
        }
    }

    private List<String> c3GetAccessionListFromFilePath(DomainEnum domain, DataTypeEnum dataType) {
        List<String> accessionList = new ArrayList<>();
        System.out.println("*** Please provide full path to the file containing accessions.");
        printSeparatorLine();
        printBackMessage();

        String inputValues = scannerUtils.getNextString();
        printEmptyline();
        if ("b".equalsIgnoreCase(inputValues)) {
            return Arrays.asList("b");
        } else if ("0".equalsIgnoreCase(inputValues)) {
            MainRunner.exit();
        } else {
            try {
                boolean isValid = true;
                List<String> accessions = accsFromFile(inputValues);
                if (accessions.size() > 0) {
                    for (String s : accessions) {
                        if (CommonUtils.isAccessionValid(s, domain, dataType)) {
                            accessionList.add(s);
                        } else {
                            isValid = false;
                            break;
                        }
                    }
                    if (isValid) {
                        return accessionList;
                    } else {
                        System.out.println(MenuUtils.accessionsErrorMessage);
                        printEmptyline();
                        return c3GetAccessionListFromFilePath(domain, dataType);
                    }
                } else {
                    System.out.println(MenuUtils.accessionsErrorMessage);
                    printEmptyline();
                    return c3GetAccessionListFromFilePath(domain, dataType);
                }
            } catch (Exception e) {
                FileUtils.writeExceptionToFile("Exception occurred while downloading file: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
                log.error("Exception occurred while downloading file.", e);
            }
        }
        return null;
    }



    private List<String> c3GetAccessionListFromCommaSeparated(DomainEnum domain, DataTypeEnum dataType) {
        List<String> accessionList = new ArrayList<>();
        System.out.println("*** Please provide the list of accessions separated by commas.");
        printSeparatorLine();
        printBackMessage();
        boolean isValid = true;
        String inputValues = scannerUtils.getNextString();
        printEmptyline();

        if ("b".equalsIgnoreCase(inputValues)) {
            return Arrays.asList("b");
        } else if ("0".equalsIgnoreCase(inputValues)) {
            MainRunner.exit();
        } else {
            String[] accessions = inputValues.split(",");
            if (accessions.length > 0) {
                for (String accession : accessions) {
                    if (!CommonUtils.isAccessionValid(accession, domain, dataType)) {
                        isValid = false;
                        break;
                    } else {
                        accessionList.add(accession);
                    }
                }

                if (isValid) {
                    return accessionList;
                } else {
                    System.out.println(MenuUtils.accessionsErrorMessage);
                    return c3GetAccessionListFromCommaSeparated(domain, dataType);
                }
            } else {
                System.out.println(MenuUtils.accessionsErrorMessage);
                return c3GetAccessionListFromCommaSeparated(domain, dataType);
            }
        }
        return null;
    }

    public void c2ShowAccessionEntryMethodMenu(DomainEnum domain, DataTypeEnum dataType) {
        System.out.println("*** How would you like to enter accessions?");
        printSeparatorLine();
        for (AccessionsEntryMethodEnum accessionsEntryMethodEnum : AccessionsEntryMethodEnum.values()) {
            System.out.println(Constants.toMessage + accessionsEntryMethodEnum.getMessage() + Constants.enterMessage + accessionsEntryMethodEnum.getValue());
        }
        printBackMessage();
        int input = scannerUtils.getNextInt();
        printEmptyline();
        if (input == -1) { // back
            bBuildDataTypeMenu(domain);
        } else {
            if (input == 0) { // Exit
                MainRunner.exit();
            }
            AccessionsEntryMethodEnum accessionsEntryMethodEnum = AccessionsEntryMethodEnum.valueOf(input);
            if (accessionsEntryMethodEnum != null) {
                List<String> accessionList = null;
                switch (accessionsEntryMethodEnum) {
                    case DOWNLOAD_FROM_FILE:
                        accessionList = c3GetAccessionListFromFilePath(domain, dataType);
                        break;
                    case DOWNLOAD_FROM_LIST:
                        accessionList = c3GetAccessionListFromCommaSeparated(domain,
                                dataType);
                        break;
                }
                if (!CollectionUtils.isEmpty(accessionList)) {
                    if (accessionList.size() == 1 && "b".equals(accessionList.get(0))) {
                        c2ShowAccessionEntryMethodMenu(domain, dataType);
                    } else {
                        // proceed
                        dShowDownloadFormatMenu(domain, dataType, accessionList);
                    }
                }
            } else {
                // replay
                printInvalidMessage();
                c2ShowAccessionEntryMethodMenu(domain, dataType);
            }
        }
    }

    /**
     * choose from FTP or aspera
     * @param domainEnum
     * @param dataTypeEnum
     * @param format
     * @param accessionList
     */
    public String e2BuildProtocolMenuAndGetSelectedProtocol(DomainEnum domainEnum, DataTypeEnum dataTypeEnum, DownloadFormatEnum format, List<String> accessionList) {
        System.out.println("***** Choose the method of downloading:");
        printSeparatorLine();
        for (ProtocolEnum protocolEnum : ProtocolEnum.values()) {
            System.out.println(Constants.toMessage + protocolEnum.getMessage() + "," + Constants.enterMessage + protocolEnum.getValue());
        }
        printBackMessage();
        int input = scannerUtils.getNextInt();
        printEmptyline();
        if (input == -1) {
            return e1RequestDownloadLocation(domainEnum, dataTypeEnum, format, accessionList);
        }
        final ProtocolEnum protocolEnum = ProtocolEnum.valueOf(input);
        if (protocolEnum == ProtocolEnum.ASPERA) {
            String asperaConnectLocation = e3ShowAsperaConnectOptionAndGetSelectedAsperaLocation(domainEnum, dataTypeEnum, format, accessionList);
            if (StringUtils.isNotBlank(asperaConnectLocation)) {
                return asperaConnectLocation;
            }
        }
        return null;
    }


    private String e3ShowAsperaConnectOptionAndGetSelectedAsperaLocation(DomainEnum domainEnum,
                                                                         DataTypeEnum dataTypeEnum, DownloadFormatEnum format, List<String> accessionList) {
        System.out.println("***** Please enter the path to your local Aspera Connect/CLI installation");
        printSeparatorLine();
        printBackMessage();
        String input = scannerUtils.getNextString();
        printEmptyline();
        if (input.equalsIgnoreCase("b")) { // back
            return e2BuildProtocolMenuAndGetSelectedProtocol(domainEnum, dataTypeEnum, format, accessionList);
        } else if (input.equalsIgnoreCase("0")) {
            MainRunner.exit();
        } else if (StringUtils.isNotEmpty(input)) {
            boolean isValidLocation = isValidAsperaConnectLoc(input);
            if (!isValidLocation) {
                printInvalidAsperaConnectLocation();
                e3ShowAsperaConnectOptionAndGetSelectedAsperaLocation(domainEnum, dataTypeEnum, format, accessionList);
            }
        } else {
            return e3ShowAsperaConnectOptionAndGetSelectedAsperaLocation(domainEnum, dataTypeEnum, format, accessionList);
        }
        return input;
    }

}
