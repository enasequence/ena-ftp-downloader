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
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.ena.app.constants.Constants;
import uk.ac.ebi.ena.app.menu.enums.AccessionTypeEnum;
import uk.ac.ebi.ena.backend.dto.DownloadJob;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


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
    public static DownloadJob processAccessions(List<String> accessions) {
        String baseAccession = accessions.get(0);
        AccessionTypeEnum type = AccessionTypeEnum.getAccessionTypeByPattern(baseAccession);
        if (type == null) {
            System.out.println("Unsupported accession type provided:" + baseAccession + " Supported types are "
                    + AccessionTypeEnum.getDisplayTypes());
            throw new Exception("Unsupported accession type provided:" + baseAccession);
        }
        String invalid =
                accessions.stream().filter(acc -> !Pattern.matches(type.getPattern(), acc)).collect(Collectors.joining(","));
        if (StringUtils.isBlank(invalid)) {
            return makeDownloadJob(type.getAccessionField(), accessions);
        } else {
            System.out.println("Invalid accession patterns provided:" + invalid + ",\nSupported types are "
                    + AccessionTypeEnum.getDisplayTypes() + ".  All accessions should be of the same type.");
            throw new Exception("Invalid accessions provided:" + invalid);
        }
    }

    public static DownloadJob makeDownloadJob(String accessionField, List<String> accessions) {
        DownloadJob job = new DownloadJob();
        job.setAccessionField(accessionField);
        job.setAccessionList(accessions);
        return job;
    }

    public static String getAscpExtension() {
        return SystemUtils.IS_OS_WINDOWS ? ".exe" : "";
    }

    public static String getAscpFileName() {
        return Constants.ascpFileName + CommonUtils.getAscpExtension();
    }

    @SneakyThrows
    public static List<String> getAccessionFromQuery(String searchQuery) {
        String PORTAL_API_SEARCH_URL = "https://www.ebi.ac.uk/ena/portal/api/search?";
        String searchURL = PORTAL_API_SEARCH_URL + searchQuery;
        MultiValueMap<String, String> mvm = CommonUtils.getParameters(searchURL);
        List<String> includeAcc = mvm.get("includeAccessions");

        return includeAcc != null && !includeAcc.isEmpty() ? includeAcc : Collections.emptyList();
    }

    @SneakyThrows
    public static DownloadJob processQuery(String searchQuery) {
        String PORTAL_API_SEARCH_URL = "https://www.ebi.ac.uk/ena/portal/api/search?";
        String searchURL = PORTAL_API_SEARCH_URL + searchQuery;
        MultiValueMap<String, String> mvm = CommonUtils.getParameters(searchURL);

        String result = mvm.get("result") != null ? mvm.get("result").get(0) : null;
        AccessionTypeEnum type = AccessionTypeEnum.getAccessionTypeByResult(result);
        if (type == null) {
            System.out.println("Unsupported result provided:" + result);
            throw new Exception("Unsupported result provided:" + result);
        }
        return makeDownloadJob(type.getAccessionField(), null);
    }

    public static MultiValueMap<String, String> getParameters(String query) {
        return UriComponentsBuilder.fromUriString(query).build().getQueryParams();
    }
}
