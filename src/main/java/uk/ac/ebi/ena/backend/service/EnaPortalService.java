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

package uk.ac.ebi.ena.backend.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ena.app.constants.Constants;
import uk.ac.ebi.ena.app.menu.enums.AccessionTypeEnum;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.app.utils.CommonUtils;
import uk.ac.ebi.ena.backend.config.BeanConfig;
import uk.ac.ebi.ena.backend.dto.AuthenticationDetail;
import uk.ac.ebi.ena.backend.dto.DownloadJob;
import uk.ac.ebi.ena.backend.dto.EnaPortalResponse;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class will invoke the Portal API and fetch the {@value SEARCH_FIELDS_READ_FASTQ /SEARCH_FIELDS_SUBMITTED} for
 * the
 * accessionIdList and dataType
 */
@Component
@Slf4j
@AllArgsConstructor
public class EnaPortalService {

    final Logger console = LoggerFactory.getLogger("console");


    public static final String SEARCH_FIELDS_READ_FASTQ = ",fastq_bytes,fastq_md5";
    public static final String SEARCH_FIELDS_SUBMITTED = ",submitted_bytes,submitted_md5";
    public static final String SEARCH_FIELDS_GENERATED = ",generated_bytes,generated_md5";
    public static final String SEARCH_FIELDS_READ_BAM = ",bam_bytes,bam_md5";

    public static final String FASTQ_FTP_FIELD = ",fastq_ftp";
    public static final String SUBMITTED_FTP_FIELD = ",submitted_ftp";
    public static final String GENERATED_FTP_FIELD = ",generated_ftp";
    public static final String FASTQ_BAM_FIELD = ",bam_ftp";

    public static final String GENERATED_ASPERA_FIELD = ",generated_aspera";
    public static final String FASTQ_ASPERA_FIELD = ",fastq_aspera";
    public static final String SUBMITTED_ASPERA_FIELD = ",submitted_aspera";
    public static final String BAM_ASPERA_FIELD = ",bam_aspera";

    private static final String PORTAL_API_READ_RUN_SEARCH_URL = "https://www.ebi.ac.uk/ena/portal/api/search?result" +
            "=read_run" + "&includeAccessionType=%s&fields=%s&format=json&limit=0"+ Constants.CLIENT_PARAM;
    private static final String PORTAL_API_ANALYSIS_SEARCH_URL = "https://www.ebi.ac.uk/ena/portal/api/search?result" +
            "=analysis" + "&includeAccessionType=%s&fields=%s&format=json&limit=0"+ Constants.CLIENT_PARAM;

    private static final String EXPERIMENT = "experiment";
    private static final String SAMPLE = "sample";
    private static final String STUDY = "study";
    private static final String ANALYSIS = "analysis";
    private static final String RUN = "run";

    private static final String COMMA = ",";

    private static final String MULTIPART_FORM_DATA = "multipart/form-data;boundary=%s";

    private final RestTemplate restTemplate;

    private static final String PORTAL_API_SEARCH_URL = "https://www.ebi.ac.uk/ena/portal/api/search?";
    private static final String JSON_FORMAT = "&format=json";
    private static final String PORTAL_API_COUNT_URL = "https://www.ebi.ac.uk/ena/portal/api/count?";

    /**
     * This API will invoke the Portal API and fetch the {@value SEARCH_FIELDS_READ_FASTQ /SEARCH_FIELDS_SUBMITTED}
     * for the
     * accessionList and dataType
     *
     * @param accessionList The experimentIds
     * @param format        The format provided by the user
     * @param protocol      The protocol for the download
     * @param downloadJob   The map for accessionDetails
     * @return The details  for the accession Ids
     */
    public List<EnaPortalResponse> getPortalResponses(List<String> accessionList, DownloadFormatEnum format,
                                                      ProtocolEnum protocol,
                                                      DownloadJob downloadJob,
                                                      AuthenticationDetail authenticationDetail) {

        String accessionField = downloadJob.getAccessionField();
        String accessionType = AccessionTypeEnum.getAccessionType(accessionField).name().toLowerCase();
        int retryCount = 0;
        String portalAPIEndpoint = "";
        outer:
        switch (protocol) {
            case FTP:
                switch (accessionType) {
                    case SAMPLE:
                        switch (format) {
                            case READS_FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.SAMPLE.name().toLowerCase(),
                                        AccessionTypeEnum.SAMPLE.getAccessionField() + SEARCH_FIELDS_READ_FASTQ + FASTQ_FTP_FIELD);
                                break outer;
                            case READS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.SAMPLE.name().toLowerCase(),
                                        AccessionTypeEnum.SAMPLE.getAccessionField() + SEARCH_FIELDS_SUBMITTED + SUBMITTED_FTP_FIELD);
                                break outer;
                            case READS_BAM:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.SAMPLE.name().toLowerCase(),
                                        AccessionTypeEnum.SAMPLE.getAccessionField() + SEARCH_FIELDS_READ_BAM + FASTQ_BAM_FIELD);
                                break outer;
                            case ANALYSIS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL,
                                        AccessionTypeEnum.ANALYSIS.name().toLowerCase(),
                                        AccessionTypeEnum.SAMPLE.getAccessionField() + SEARCH_FIELDS_SUBMITTED + SUBMITTED_FTP_FIELD);
                                break outer;
                            case ANALYSIS_GENERATED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL,
                                        AccessionTypeEnum.ANALYSIS.name().toLowerCase(),
                                        AccessionTypeEnum.SAMPLE.getAccessionField() + SEARCH_FIELDS_GENERATED + GENERATED_FTP_FIELD);
                                break outer;
                        }
                    case STUDY:
                        switch (format) {
                            case READS_FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.STUDY.name().toLowerCase(),
                                        AccessionTypeEnum.STUDY.getAccessionField() + SEARCH_FIELDS_READ_FASTQ + FASTQ_FTP_FIELD);
                                break outer;
                            case READS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.STUDY.name().toLowerCase(),
                                        AccessionTypeEnum.STUDY.getAccessionField() + SEARCH_FIELDS_SUBMITTED + SUBMITTED_FTP_FIELD);
                                break outer;
                            case READS_BAM:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.STUDY.name().toLowerCase(),
                                        AccessionTypeEnum.STUDY.getAccessionField() + SEARCH_FIELDS_READ_BAM + FASTQ_BAM_FIELD);
                                break outer;
                            case ANALYSIS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL,
                                        AccessionTypeEnum.STUDY.name().toLowerCase(),
                                        AccessionTypeEnum.STUDY.getAccessionField() + SEARCH_FIELDS_SUBMITTED + SUBMITTED_FTP_FIELD);
                                break outer;
                            case ANALYSIS_GENERATED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL,
                                        AccessionTypeEnum.STUDY.name().toLowerCase(),
                                        AccessionTypeEnum.STUDY.getAccessionField() + SEARCH_FIELDS_GENERATED + GENERATED_FTP_FIELD);
                                break outer;
                        }
                    case EXPERIMENT:
                        switch (format) {
                            case READS_FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.EXPERIMENT.name().toLowerCase(),
                                        AccessionTypeEnum.EXPERIMENT.getAccessionField() + SEARCH_FIELDS_READ_FASTQ + FASTQ_FTP_FIELD);
                                break outer;
                            case READS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.EXPERIMENT.name().toLowerCase(),
                                        AccessionTypeEnum.EXPERIMENT.getAccessionField() + SEARCH_FIELDS_SUBMITTED + SUBMITTED_FTP_FIELD);
                                break outer;
                            case READS_BAM:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.EXPERIMENT.name().toLowerCase(),
                                        AccessionTypeEnum.EXPERIMENT.getAccessionField() + SEARCH_FIELDS_READ_BAM + FASTQ_BAM_FIELD);
                                break outer;
                        }
                    case RUN:
                        switch (format) {
                            case READS_FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.RUN.name().toLowerCase(),
                                        AccessionTypeEnum.RUN.getAccessionField() + SEARCH_FIELDS_READ_FASTQ + FASTQ_FTP_FIELD);
                                break outer;
                            case READS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.RUN.name().toLowerCase(),
                                        AccessionTypeEnum.RUN.getAccessionField() + SEARCH_FIELDS_SUBMITTED + SUBMITTED_FTP_FIELD);
                                break outer;
                            case READS_BAM:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.RUN.name().toLowerCase(),
                                        AccessionTypeEnum.RUN.getAccessionField() + SEARCH_FIELDS_READ_BAM + FASTQ_BAM_FIELD);
                                break outer;
                        }
                    case ANALYSIS:
                        switch (format) {
                            case ANALYSIS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL,
                                        AccessionTypeEnum.ANALYSIS.name().toLowerCase(),
                                        AccessionTypeEnum.ANALYSIS.getAccessionField() + SEARCH_FIELDS_SUBMITTED + SUBMITTED_FTP_FIELD);
                                break outer;
                            case ANALYSIS_GENERATED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL,
                                        AccessionTypeEnum.ANALYSIS.name().toLowerCase(),
                                        AccessionTypeEnum.ANALYSIS.getAccessionField() + SEARCH_FIELDS_GENERATED + GENERATED_FTP_FIELD);
                                break outer;
                        }
                }
            case ASPERA:
                switch (accessionType) {
                    case SAMPLE:
                        switch (format) {
                            case READS_FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.SAMPLE.name().toLowerCase(),
                                        AccessionTypeEnum.SAMPLE.getAccessionField() + SEARCH_FIELDS_READ_FASTQ + FASTQ_ASPERA_FIELD);
                                break outer;
                            case READS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.SAMPLE.name().toLowerCase(),
                                        AccessionTypeEnum.SAMPLE.getAccessionField() + SEARCH_FIELDS_SUBMITTED + SUBMITTED_ASPERA_FIELD);
                                break outer;
                            case READS_BAM:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.SAMPLE.name().toLowerCase(),
                                        AccessionTypeEnum.SAMPLE.getAccessionField() + SEARCH_FIELDS_READ_BAM + BAM_ASPERA_FIELD);
                                break outer;
                            case ANALYSIS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL,
                                        AccessionTypeEnum.SAMPLE.name().toLowerCase(),
                                        AccessionTypeEnum.SAMPLE.getAccessionField() + SEARCH_FIELDS_SUBMITTED + SUBMITTED_ASPERA_FIELD);
                                break outer;
                            case ANALYSIS_GENERATED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL,
                                        AccessionTypeEnum.SAMPLE.name().toLowerCase(),
                                        AccessionTypeEnum.SAMPLE.getAccessionField() + SEARCH_FIELDS_GENERATED + GENERATED_ASPERA_FIELD);
                                break outer;
                        }
                    case STUDY:
                        switch (format) {
                            case READS_FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.STUDY.name().toLowerCase(),
                                        AccessionTypeEnum.STUDY.getAccessionField() + SEARCH_FIELDS_READ_FASTQ + FASTQ_ASPERA_FIELD);
                                break outer;
                            case READS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.STUDY.name().toLowerCase(),
                                        AccessionTypeEnum.STUDY.getAccessionField() + SEARCH_FIELDS_SUBMITTED + SUBMITTED_ASPERA_FIELD);
                                break outer;
                            case READS_BAM:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.STUDY.name().toLowerCase(),
                                        AccessionTypeEnum.STUDY.getAccessionField() + SEARCH_FIELDS_READ_BAM + BAM_ASPERA_FIELD);
                                break outer;
                            case ANALYSIS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL,
                                        AccessionTypeEnum.STUDY.name().toLowerCase(),
                                        AccessionTypeEnum.STUDY.getAccessionField() + SEARCH_FIELDS_SUBMITTED + SUBMITTED_ASPERA_FIELD);
                                break outer;
                            case ANALYSIS_GENERATED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL,
                                        AccessionTypeEnum.STUDY.name().toLowerCase(),
                                        AccessionTypeEnum.STUDY.getAccessionField() + SEARCH_FIELDS_GENERATED + GENERATED_ASPERA_FIELD);
                                break outer;
                        }
                    case EXPERIMENT:
                        switch (format) {
                            case READS_FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.EXPERIMENT.name().toLowerCase(),
                                        AccessionTypeEnum.EXPERIMENT.getAccessionField() + SEARCH_FIELDS_READ_FASTQ + FASTQ_ASPERA_FIELD);
                                break outer;
                            case READS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.EXPERIMENT.name().toLowerCase(),
                                        AccessionTypeEnum.EXPERIMENT.getAccessionField() + SEARCH_FIELDS_SUBMITTED + SUBMITTED_ASPERA_FIELD);
                                break outer;
                            case READS_BAM:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.EXPERIMENT.name().toLowerCase(),
                                        AccessionTypeEnum.EXPERIMENT.getAccessionField() + SEARCH_FIELDS_READ_BAM + BAM_ASPERA_FIELD);
                                break outer;
                        }
                    case RUN:
                        switch (format) {
                            case READS_FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.RUN.name().toLowerCase(),
                                        AccessionTypeEnum.RUN.getAccessionField() + SEARCH_FIELDS_READ_FASTQ + FASTQ_ASPERA_FIELD);
                                break outer;
                            case READS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.RUN.name().toLowerCase(),
                                        AccessionTypeEnum.RUN.getAccessionField() + SEARCH_FIELDS_SUBMITTED + SUBMITTED_ASPERA_FIELD);
                                break outer;
                            case READS_BAM:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL,
                                        AccessionTypeEnum.RUN.name().toLowerCase(),
                                        AccessionTypeEnum.RUN.getAccessionField() + SEARCH_FIELDS_READ_BAM + BAM_ASPERA_FIELD);
                                break outer;
                        }
                    case ANALYSIS:
                        switch (format) {
                            case ANALYSIS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL,
                                        AccessionTypeEnum.ANALYSIS.name().toLowerCase(),
                                        AccessionTypeEnum.ANALYSIS.getAccessionField() + SEARCH_FIELDS_SUBMITTED + SUBMITTED_ASPERA_FIELD);
                                break outer;
                            case ANALYSIS_GENERATED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL,
                                        AccessionTypeEnum.ANALYSIS.name().toLowerCase(),
                                        AccessionTypeEnum.ANALYSIS.getAccessionField() + SEARCH_FIELDS_GENERATED + GENERATED_ASPERA_FIELD);
                                break outer;
                        }
                }
        }
        portalAPIEndpoint = portalAPIEndpoint + (Objects.nonNull(authenticationDetail) ? "&dccDataOnly=" + true : "");
        Assert.notNull(accessionList, "Accessions cannot be null");
        String includeAccs = String.join(COMMA, accessionList);
        URI uri = URI.create(Objects.requireNonNull(portalAPIEndpoint));
        log.info("portalAPIEndpoint: " + portalAPIEndpoint);
        String body = "includeAccessions=" + includeAccs;
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", Constants.URLENCODED);
        httpHeaders.add("Accept", Constants.APPLICATION_JSON);

        if (Objects.nonNull(authenticationDetail)) {
            httpHeaders.setBasicAuth(authenticationDetail.getUserName(), authenticationDetail.getPassword());
        }
        HttpEntity<String> request = new HttpEntity<>(body, httpHeaders);
        log.debug("url:{}, body:{}", portalAPIEndpoint, body);

        while (retryCount <= BeanConfig.APP_RETRY) {
            try {
                EnaPortalResponse[] response = restTemplate.postForObject(uri, request, EnaPortalResponse[].class);
                if (ArrayUtils.isEmpty(response)) {
                    System.out.println("No data files of requested type found.");
                    return Collections.emptyList();
                }
                return Arrays.asList(Objects.requireNonNull(response));
            } catch (RestClientResponseException rce) {
                if (rce.getRawStatusCode() == HttpStatus.UNAUTHORIZED.value()) {
                    console.info("User name and password for given data hub is not correct");
                    break;
                } else {
                    log.error("Exception encountered while getting portalResponse for accession type:{}, format:{}  @@" + rce.getMessage(),
                            accessionType, format, rce);
                    retryCount++;
                }

            }
        }
        log.error("Count not fetch get portalResponse for accession type:{}, format:{} even after {} retries",
                accessionType, format, BeanConfig.APP_RETRY);
        return Collections.emptyList();
    }

    public boolean authenticateUser(AuthenticationDetail authenticationDetail) {

        String userName = authenticationDetail.getUserName();
        String password = authenticationDetail.getPassword();
        authenticationDetail.setAuthenticated(false);
        if (StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(password)) {

            String portalAPIAuthEndpoint = Constants.PORTAL_API_EP;

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Accept", Constants.APPLICATION_JSON);
            httpHeaders.setBasicAuth(userName, password);

            HttpEntity<Void> requestEntity = new HttpEntity<>(httpHeaders);

            try {
                ResponseEntity<String> resp = restTemplate.exchange(portalAPIAuthEndpoint, HttpMethod.GET, requestEntity, String.class);
                if (resp.getStatusCode() == HttpStatus.OK) {
                    authenticationDetail.setAuthenticated(true);
                    return true;
                }
            } catch (RestClientException restClientException) {
                log.error(" Data hub authorization failed-  " + restClientException.getMessage());
                return false;
            }
        }
        return false;
    }

    public List<String> getAccessions(String query) {
        if (query != null) {
            String url = getModifiedUrl(query);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Content-Type", Constants.URLENCODED);
            httpHeaders.add("Accept", Constants.APPLICATION_JSON);
            URI uri = URI.create(Objects.requireNonNull(url));
            int retryCount = 0;

            while (retryCount <= BeanConfig.APP_RETRY) {
                try {
                    EnaPortalResponse[] response = restTemplate.postForObject(uri, httpHeaders, EnaPortalResponse[].class);
                    if (ArrayUtils.isEmpty(response)) {
                        System.out.println("No accessions for the given query found.");
                        return Collections.emptyList();
                    }
                    return Arrays.stream(response).map(resp -> resp.getRecordId() != null ? resp.getRecordId() :
                            resp.getParentId()).collect(Collectors.toList());
                } catch (RestClientResponseException rce) {
                    log.error("Exception encountered while getting accessions for query:{}, @@" + rce.getMessage(),
                            query, rce);
                    retryCount++;
                }

            }

            log.error("Could not fetch get accessions for query:{} even after {} retries", query, BeanConfig.APP_RETRY);
            return null;
        } else {
            return Collections.emptyList();
        }
    }

    private String getModifiedUrl(String query) {
        String searchURL = PORTAL_API_SEARCH_URL + query;

        MultiValueMap<String, String> parameters = CommonUtils.getParameters(searchURL);

        //ignore the format if specified
        String format = parameters.get("format") != null ? parameters.get("format").get(0) : null;

        if (format != null && !format.equals("json")) {
            searchURL = searchURL.replaceFirst(format, "json");
        } else {
            searchURL = searchURL + JSON_FORMAT;
        }

        return searchURL;
    }

    public Long getCount(String searchQuery) {
        String searchURL = PORTAL_API_COUNT_URL + searchQuery;

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", Constants.URLENCODED);
        httpHeaders.add("Accept", Constants.TEXT_PLAIN);
        URI uri = URI.create(Objects.requireNonNull(searchURL));
        int retryCount = 0;

        while (retryCount <= BeanConfig.APP_RETRY) {
            try {
                String count = restTemplate.postForObject(uri, httpHeaders, String.class);
                if (count != null) {
                    return Long.valueOf(count);
                }
            } catch (RestClientResponseException rce) {
                log.error("Exception encountered while getting count for query:{}, @@" + rce.getMessage(), searchQuery,
                        rce);
                retryCount++;
            }

        }

        log.error("Could not fetch get count for query:{} even after {} retries", searchQuery, BeanConfig.APP_RETRY);
        return null;
    }
}




