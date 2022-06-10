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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ena.app.menu.enums.AccessionTypeEnum;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.backend.config.BeanConfig;
import uk.ac.ebi.ena.backend.dto.DownloadJob;
import uk.ac.ebi.ena.backend.dto.EnaPortalResponse;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * This class will invoke the Portal API and fetch the {@value SEARCH_FIELDS_READ_FASTQ /SEARCH_FIELDS_SUBMITTED} for
 * the
 * accessionIdList and dataType
 */
@Component
@Slf4j
@AllArgsConstructor
public class EnaPortalService {

    public static final String SEARCH_FIELDS_READ_FASTQ = ",fastq_bytes,fastq_md5";
    public static final String SEARCH_FIELDS_SUBMITTED = ",submitted_bytes,submitted_md5";
    public static final String SEARCH_FIELDS_GENERATED = ",generated_bytes,generated_md5";

    public static final String FASTQ_FTP_FIELD = ",fastq_ftp";
    public static final String SUBMITTED_FTP_FIELD = ",submitted_ftp";
    public static final String GENERATED_FTP_FIELD = ",generated_ftp";

    public static final String GENERATED_ASPERA_FIELD = ",generated_aspera";
    public static final String FASTQ_ASPERA_FIELD = ",fastq_aspera";
    public static final String SUBMITTED_ASPERA_FIELD = ",submitted_aspera";

    private static final String PORTAL_API_READ_RUN_SEARCH_URL = "https://www.ebi.ac.uk/ena/portal/api/search?result" +
            "=read_run" +
            "&includeAccessionType=%s&fields=%s&format=json&limit=0";
    private static final String PORTAL_API_ANALYSIS_SEARCH_URL = "https://www.ebi.ac.uk/ena/portal/api/search?result" +
            "=analysis" +
            "&includeAccessionType=%s&fields=%s&format=json&limit=0";
    private static final String PORTAL_API_SUPPORT_URL = "https://www.ebi.ac.uk/ena/portal/api/support?email=datasubs" +
            "@ebi.ac.uk&message=%s&to=%s&subject=%s&name=%s";

    private static final String EXPERIMENT = "experiment";
    private static final String SAMPLE = "sample";
    private static final String STUDY = "study";
    private static final String ANALYSIS = "analysis";
    private static final String RUN = "run";

    private static final String COMMA = ",";
    private static final String URLENCODED = "application/x-www-form-urlencoded";
    private static final String APPLICATION_JSON = "application/json";
    private static final String MULTIPART_FORM_DATA = "multipart/form-data;boundary=%s";

    private final RestTemplate restTemplate;

    /**
     * This API will invoke the Portal API and fetch the {@value SEARCH_FIELDS_READ_FASTQ /SEARCH_FIELDS_SUBMITTED}
     * for the
     * accessionList and dataType
     *
     * @param accessionList       The experimentIds
     * @param format              The format provided by the user
     * @param protocol            The protocol for the download
     * @param downloadJob The map for accessionDetails
     * @return The details  for the accession Ids
     */
    public List<EnaPortalResponse> getPortalResponses(List<String> accessionList, DownloadFormatEnum format,
                                                      ProtocolEnum protocol,
                                                      DownloadJob downloadJob) {

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

        Assert.notNull(accessionList, "Accessions cannot be null");
        String includeAccs = String.join(COMMA, accessionList);
        URI uri = URI.create(Objects.requireNonNull(portalAPIEndpoint));
        String body = "includeAccessions=" + includeAccs;
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", URLENCODED);
        httpHeaders.add("Accept", APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, httpHeaders);
        log.info("url:{}", portalAPIEndpoint);
        log.info("request body:{}", body);
        while (retryCount <= BeanConfig.APP_RETRY) {
            try {
                EnaPortalResponse[] response = restTemplate.postForObject(uri, request, EnaPortalResponse[].class);
                if (ArrayUtils.isEmpty(response)) {
                    System.out.println("No data files of requested type found.");
                    return Collections.emptyList();
                }
                return Arrays.asList(Objects.requireNonNull(response));
            } catch (RestClientException rce) {
                log.error("Exception encountered while getting portalResponse for accession type:{}, format:{}",
                        accessionType, format, rce);
                retryCount++;
            }
        }
        log.error("Count not fetch get portalResponse for accession type:{}, format:{} even after {} retries",
                accessionType, format, BeanConfig.APP_RETRY);
        return Collections.emptyList();
    }

    public void sendEmail(String recipientEmail, String message, String subject, String name) {
        String supportApiEndpoint;
        Assert.notNull(recipientEmail, "Email recipient cannot be null");
        Assert.notNull(message, "Email Message cannot be null");

        supportApiEndpoint = String.format(PORTAL_API_SUPPORT_URL, message, recipientEmail, subject, name);
        HttpHeaders httpHeaders = new HttpHeaders();
        String boundary = Long.toHexString(System.currentTimeMillis());
        httpHeaders.add("Content-Type", String.format(MULTIPART_FORM_DATA, boundary));
        HttpEntity<String> request = new HttpEntity<>(null, httpHeaders);
        URI uri = URI.create(supportApiEndpoint);
        try {
            restTemplate.postForObject(uri, request, String.class);
        } catch (Exception exception) {
            log.error("Exception encountered while sending email to emailId:{}", recipientEmail, exception);
        }
        log.info("Email successfully sent to:{}", recipientEmail);

    }
}
