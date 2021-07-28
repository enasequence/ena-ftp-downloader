package uk.ac.ebi.ena.cv19fd.backend.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ena.cv19fd.app.constants.Constants;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.cv19fd.backend.config.BeanConfig;
import uk.ac.ebi.ena.cv19fd.backend.dto.EnaPortalResponse;

import java.net.URI;
import java.util.*;

import static uk.ac.ebi.ena.cv19fd.app.constants.Constants.ACCESSION_TYPE;

/**
 * This class will invoke the Portal API and fetch the {@value SEARCH_FIELDS_READ_FASTQ /SEARCH_FIELDS_SUBMITTED} for the
 * accessionIdList and dataType
 */
@Component
@Slf4j
@AllArgsConstructor
public class EnaPortalService {

    public static final String EXPERIMENT_READS = "experiment_accession";
    public static final String SAMPLE_ACCESSION = "sample_accession";
    public static final String STUDY_ACCESSION = "study_accession";
    public static final String RUN_ACCESSION = "run_accession";
    public static final String ANALYSIS_ACCESSION = "analysis_accession";


    public static final String SEARCH_FIELDS_READ_FASTQ = ",fastq_bytes,fastq_md5";
    public static final String SEARCH_FIELDS_SUBMITTED = ",submitted_bytes,submitted_md5";
    public static final String SEARCH_FIELDS_GENERATED = ",generated_bytes,generated_md5";

    public static final String FASTQ_FTP_FIELD = ",fastq_ftp";
    public static final String SUBMITTED_FTP_FIELD = ",submitted_ftp";
    public static final String GENERATED_FTP_FIELD = ",generated_ftp";

    public static final String GENERATED_ASPERA_FIELD = ",generated_aspera";
    public static final String FASTQ_ASPERA_FIELD = ",fastq_aspera";
    public static final String SUBMITTED_ASPERA_FIELD = ",submitted_aspera";

    private static final String PORTAL_API_READ_RUN_SEARCH_URL = "https://www.ebi.ac.uk/ena/portal/api/search?result=read_run" +
            "&includeAccessionType=%s&fields=%s&format=json";
    private static final String PORTAL_API_ANALYSIS_SEARCH_URL = "https://www.ebi.ac.uk/ena/portal/api/search?result=analysis" +
            "&includeAccessionType=%s&fields=%s&format=json";
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
     * This API will invoke the Portal API and fetch the {@value SEARCH_FIELDS_READ_FASTQ /SEARCH_FIELDS_SUBMITTED} for the
     * accessionIdList and dataType
     *
     * @param accessionIdList The experimentIds
     * @param format            The format provided by the user
     * @param protocol          The protocol for the download
     * @param accessionDetailsMap The map for accessionDetails
     * @return The details  for the accession Ids
     */
    public List<EnaPortalResponse> getPortalResponses(List<String> accessionIdList, DownloadFormatEnum format,
                                                      ProtocolEnum protocol, Map<String, List<String>> accessionDetailsMap) {

        String accessionType = accessionDetailsMap.get(ACCESSION_TYPE).get(0);
        int retryCount = 0;
        String portalAPIEndpoint = "";
        outer:
        switch (protocol) {
            case FTP:
                switch (accessionType) {
                    case Constants.SAMPLE:
                        switch (format) {
                            case READS_FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL, SAMPLE, SAMPLE_ACCESSION + SEARCH_FIELDS_READ_FASTQ + FASTQ_FTP_FIELD);
                                break outer;
                            case READS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL, SAMPLE, SAMPLE_ACCESSION + SEARCH_FIELDS_SUBMITTED + SUBMITTED_FTP_FIELD);
                                break outer;
                            case ANALYSIS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL, ANALYSIS, SAMPLE_ACCESSION + SEARCH_FIELDS_SUBMITTED + SUBMITTED_FTP_FIELD);
                                break outer;
                            case ANALYSIS_GENERATED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL, ANALYSIS, SAMPLE_ACCESSION + SEARCH_FIELDS_GENERATED + GENERATED_FTP_FIELD);
                                break outer;
                        }
                    case Constants.PROJECT:
                        switch (format) {
                            case READS_FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL, STUDY, STUDY_ACCESSION + SEARCH_FIELDS_READ_FASTQ + FASTQ_FTP_FIELD);
                                break outer;
                            case READS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL, STUDY, STUDY_ACCESSION + SEARCH_FIELDS_SUBMITTED + SUBMITTED_FTP_FIELD);
                                break outer;
                            case ANALYSIS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL, STUDY, STUDY_ACCESSION + SEARCH_FIELDS_SUBMITTED + SUBMITTED_FTP_FIELD);
                                break outer;
                            case ANALYSIS_GENERATED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL, STUDY, STUDY_ACCESSION + SEARCH_FIELDS_GENERATED + GENERATED_FTP_FIELD);
                                break outer;
                        }
                    case Constants.EXPERIMENT:
                        switch (format) {
                            case READS_FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL, EXPERIMENT, EXPERIMENT_READS + SEARCH_FIELDS_READ_FASTQ + FASTQ_FTP_FIELD);
                                break outer;
                            case READS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL, EXPERIMENT, EXPERIMENT_READS + SEARCH_FIELDS_SUBMITTED + SUBMITTED_FTP_FIELD);
                                break outer;
                        }
                    case Constants.RUN:
                        switch (format) {
                            case READS_FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL, RUN, RUN_ACCESSION + SEARCH_FIELDS_READ_FASTQ + FASTQ_FTP_FIELD);
                                break outer;
                            case READS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL, RUN, RUN_ACCESSION + SEARCH_FIELDS_SUBMITTED + SUBMITTED_FTP_FIELD);
                                break outer;
                        }
                    case Constants.ANALYSIS:
                        switch (format) {
                            case ANALYSIS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL, ANALYSIS, ANALYSIS_ACCESSION + SEARCH_FIELDS_SUBMITTED + SUBMITTED_FTP_FIELD);
                                break outer;
                            case ANALYSIS_GENERATED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL, ANALYSIS, ANALYSIS_ACCESSION + SEARCH_FIELDS_GENERATED + GENERATED_FTP_FIELD);
                                break outer;
                        }
                }
            case ASPERA:
                switch (accessionType) {
                    case Constants.SAMPLE:
                        switch (format) {
                            case READS_FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL, SAMPLE, SAMPLE_ACCESSION + SEARCH_FIELDS_READ_FASTQ + FASTQ_ASPERA_FIELD);
                                break outer;
                            case READS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL, SAMPLE, SAMPLE_ACCESSION + SEARCH_FIELDS_SUBMITTED + SUBMITTED_ASPERA_FIELD);
                                break outer;
                            case ANALYSIS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL, ANALYSIS, SAMPLE_ACCESSION + SEARCH_FIELDS_SUBMITTED + SUBMITTED_ASPERA_FIELD);
                                break outer;
                            case ANALYSIS_GENERATED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL, ANALYSIS, SAMPLE_ACCESSION + SEARCH_FIELDS_GENERATED + GENERATED_ASPERA_FIELD);
                                break outer;
                        }
                    case Constants.PROJECT:
                        switch (format) {
                            case READS_FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL, STUDY, STUDY_ACCESSION + SEARCH_FIELDS_READ_FASTQ + FASTQ_ASPERA_FIELD);
                                break outer;
                            case READS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL, STUDY, STUDY_ACCESSION + SEARCH_FIELDS_SUBMITTED + SUBMITTED_ASPERA_FIELD);
                                break outer;
                            case ANALYSIS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL, STUDY, STUDY_ACCESSION + SEARCH_FIELDS_SUBMITTED + SUBMITTED_ASPERA_FIELD);
                                break outer;
                            case ANALYSIS_GENERATED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL, STUDY, STUDY_ACCESSION + SEARCH_FIELDS_GENERATED + GENERATED_ASPERA_FIELD);
                                break outer;
                        }
                    case Constants.EXPERIMENT:
                        switch (format) {
                            case READS_FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL, EXPERIMENT, EXPERIMENT_READS + SEARCH_FIELDS_READ_FASTQ + FASTQ_ASPERA_FIELD);
                                break outer;
                            case READS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL, EXPERIMENT, EXPERIMENT_READS + SEARCH_FIELDS_SUBMITTED + SUBMITTED_ASPERA_FIELD);
                                break outer;
                        }
                    case Constants.RUN:
                        switch (format) {
                            case READS_FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL, RUN, RUN_ACCESSION + SEARCH_FIELDS_READ_FASTQ + FASTQ_ASPERA_FIELD);
                                break outer;
                            case READS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_READ_RUN_SEARCH_URL, RUN, RUN_ACCESSION + SEARCH_FIELDS_SUBMITTED + SUBMITTED_ASPERA_FIELD);
                                break outer;
                        }
                    case Constants.ANALYSIS:
                        switch (format) {
                            case ANALYSIS_SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL, ANALYSIS, ANALYSIS_ACCESSION + SEARCH_FIELDS_SUBMITTED + SUBMITTED_ASPERA_FIELD);
                                break outer;
                            case ANALYSIS_GENERATED:
                                portalAPIEndpoint = String.format(PORTAL_API_ANALYSIS_SEARCH_URL, ANALYSIS, ANALYSIS_ACCESSION + SEARCH_FIELDS_GENERATED + GENERATED_ASPERA_FIELD);
                                break outer;
                        }
                }
        }

        Assert.notNull(accessionIdList, "Accession IDs cannot be null");
        String experimentIds = String.join(COMMA, accessionIdList);
        URI uri = URI.create(portalAPIEndpoint);
        String body = "includeAccessions=" + experimentIds;
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", URLENCODED);
        httpHeaders.add("Accept", APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, httpHeaders);
        while (retryCount <= BeanConfig.APP_RETRY) {
            try {
                EnaPortalResponse[] response = restTemplate.postForObject(uri, request, EnaPortalResponse[].class);
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
