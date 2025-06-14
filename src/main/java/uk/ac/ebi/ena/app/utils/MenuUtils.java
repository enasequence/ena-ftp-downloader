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

import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import uk.ac.ebi.ena.app.constants.Constants;
import uk.ac.ebi.ena.app.menu.enums.AccessionTypeEnum;
import uk.ac.ebi.ena.app.menu.enums.AccessionsEntryMethodEnum;
import uk.ac.ebi.ena.backend.dto.DownloadJob;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static uk.ac.ebi.ena.app.utils.CommonUtils.printSeparatorLine;

@Slf4j
public class MenuUtils {

    static final Logger console = LoggerFactory.getLogger("console");

    public static final String exitMessage = "To exit enter 0 (zero)";

    public static final String dataHubDownloadMessage = "Do you want to download the files from datahub. Or just press Enter to skip";

    public static final String downloadDccOnlyMessage = "Do you want to download only from dataHub data or include public data as well";


    public static final String dataHubUsernameInputMessage = "Enter data hub name (dcc username) or enter b to go back.";

    public static final String dataHubPasswordInputMessage = "Enter data hub password";

    public static final String welcomeMessage = "Welcome to the ENA File Downloader utility!";
    public static final String lineMessage = "----------------------------------------------";
    public static final String accessionsErrorMessage = "Please provide valid accessions! ";
    public static final String validAccessionsErrorMessage = "Accessions file should be plain-text, in TSV (tab" +
            " separated values) format. The first column must be the accessions. Header row is ignored if present." +
            " Values can be enclosed in double quotes or not.";
    public static final String accessionsFileErrorMessage = "Please provide a valid path to accessions file! ";
    public static final String invalidAsperaConnectMessage = "The Aspera Connect/CLI location entered is incorrect. " +
            "Please enter a valid location";

    public static final String accessionsSameTypeErrorMessage = "Please provide valid accessions of the same type! ";

    private static final String ACCESSION = "accession";

    public static final String queryErrorMessage = "Please provide valid search query! ";
    private static final String PORTAL_API_SEARCH_URL = "https://www.ebi.ac.uk/ena/portal/api/search?";

    public static void printBackMessage() {
        System.out.println(Constants.backMessage);
        printExitMessage();
    }

    public static void printUserNameMessage() {
        System.out.println(MenuUtils.dataHubUsernameInputMessage);
    }

    public static void printPasswordMessage() {
        System.out.println(MenuUtils.dataHubPasswordInputMessage);
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
        console.info("Assumed aspera client:{}", ascpFile);
        if (Files.exists(Paths.get(asperaConnectLocation))) {
            if (!Files.exists(Paths.get(asperaConnectLocation, Constants.binFolder))) {
                log.error("bin folder not found in path:{}", asperaConnectLocation);
                System.out.println("bin folder not found in path:{}" + asperaConnectLocation);
            } else if (!Files.exists(Paths.get(asperaConnectLocation, Constants.binFolder, ascpFile))) {
                log.error("{} not found.", Paths.get(asperaConnectLocation, Constants.binFolder, ascpFile));
                System.out.println(Paths.get(asperaConnectLocation, Constants.binFolder, ascpFile) + " not found.");
            } else if (!Files.exists(Paths.get(asperaConnectLocation, Constants.etcFolder)) || !Files.exists(Paths.get(asperaConnectLocation, Constants.etcFolder, Constants.asperaWebFile))) {
                log.error("etc folder not found in path:{}. We look for etc/asperaweb_id_dsa.openssh",
                        asperaConnectLocation);
                System.out.println("etc/asperaweb_id_dsa.openssh not found in " + asperaConnectLocation);
            } else {
                isValidLocation = true;
            }
        }
        return isValidLocation;
    }

    public static void printExitMessage() {
        System.out.println(MenuUtils.exitMessage);
        printSeparatorLine();
    }

    public static DownloadJob parseAccessions(String accessions) {
        DownloadJob job = new DownloadJob();
        if (StringUtils.isEmpty(accessions)) {

        } else if (new File(accessions).exists()) {
            List<String> accessionList = accsFromFile(accessions);

            job = CommonUtils.processAccessions(Objects.requireNonNull(accessionList));
            job.setAccessionEntryMethod(AccessionsEntryMethodEnum.DOWNLOAD_FROM_FILE);
        } else {
            List<String> accessionList = Arrays.asList(accessions.split(","));
            job = CommonUtils.processAccessions(accessionList);
            job.setAccessionEntryMethod(AccessionsEntryMethodEnum.DOWNLOAD_FROM_LIST);
        }
        return job;
    }

    public static List<String> accsFromFile(String inputValues) {
        try {
            String[] headers = Files.lines(Paths.get(inputValues))
                    .map(s -> s.split("\t"))
                    .findFirst()
                    .orElse(new String[1]);

            String firstColumn = headers[0];

            if (firstColumn == null) {
                return new ArrayList<>();
            } else {
                if (headers.length > 1) {
                    TsvParserSettings settings = new TsvParserSettings();
                    settings.getFormat().setLineSeparator("\n");
                    settings.selectFields(firstColumn);

                    TsvParser parser = new TsvParser(settings);
                    List<String[]> accessionIdColumn = parser.parseAll(new File(inputValues));
                    //removing the column header
                    if (firstColumn.contains(ACCESSION)) {
                        accessionIdColumn = accessionIdColumn.subList(1, accessionIdColumn.size());
                    }

                    return accessionIdColumn.stream().map(aRow -> aRow[0].replace("\"", "").trim())
                            .collect(Collectors.toList());
                } else {
                    //if multiple headers are not present then we expect the file to contain only the accessionIds
                    boolean isValidBaseAccession = validateAccession(firstColumn);
                    if (!isValidBaseAccession) {
                        return null;
                    }
                    List<String> accessionIds =
                            Files.lines(Paths.get(inputValues), StandardCharsets.US_ASCII).collect(Collectors.toList());

                    return new ArrayList<>(accessionIds.stream().map(aRow -> aRow.replace("\"", "").trim()).collect(Collectors.toCollection(LinkedHashSet::new)));
                }
            }
        } catch (IOException exception) {
            log.error("Exception occured while parsing accession list from file ", exception);
            return null;
        }

    }

    private static boolean validateAccession(String firstColumn) {
        firstColumn = firstColumn.replace("\"", "").trim();
        AccessionTypeEnum type = AccessionTypeEnum.getAccessionTypeByPattern(firstColumn);
        if (type != null) {
            return Pattern.matches(type.getPattern(), firstColumn);
        }
        return false;
    }

    public static boolean validateSearchRequest(String searchQuery) {
        //use utils to validate the whole http request and parse it
        String searchURL =  PORTAL_API_SEARCH_URL + searchQuery;
        try {
            new URI(searchURL).toURL();
        }catch (MalformedURLException | URISyntaxException e) {
            log.error("Search query is not valid "+e);
            return false;
        }
        MultiValueMap<String, String> parameters = CommonUtils.getParameters(searchURL);

        return parameters.get("result") != null;
    }

    public static DownloadJob parseQuery(String query) {
        List<String> accessions = CommonUtils.getAccessionFromQuery(query);
        DownloadJob downloadJob;
        if (!CollectionUtils.isEmpty(accessions)) {
            downloadJob = CommonUtils.processAccessions(accessions);
        } else {
            downloadJob = CommonUtils.processQuery(query);
        }
        downloadJob.setQuery(query);
        downloadJob.setAccessionEntryMethod(AccessionsEntryMethodEnum.DOWNLOAD_FROM_QUERY);

        return downloadJob;
    }

}
