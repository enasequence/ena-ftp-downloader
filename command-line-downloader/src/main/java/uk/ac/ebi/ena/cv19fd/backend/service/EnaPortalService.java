package uk.ac.ebi.ena.cv19fd.backend.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DataTypeEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.cv19fd.backend.config.BeanConfig;
import uk.ac.ebi.ena.cv19fd.backend.dto.EnaPortalResponse;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * This class will invoke the Portal API and fetch the {@value SEARCH_FIELDS_FASTQ/SEARCH_FIELDS_SUBMITTED} for the
 * accessionIdList and dataType
 */
@Component
@Slf4j
@AllArgsConstructor
public class EnaPortalService {

    public static final String EXPERIMENT_READS = ",experiment_accession";
    public static final String SEQUENCED_SAMPLE = ",sample_accession";
    public static final String STUDIES = ",study_accession";

    public static final String SEARCH_FIELDS_FASTQ = "fastq_bytes,fastq_md5";
    public static final String SEARCH_FIELDS_SUBMITTED = "submitted_bytes,submitted_md5";

    public static final String FASTQ_FTP_FIELD = ",fastq_ftp";
    public static final String FASTQ_SUBMITTED_FIELD = ",submitted_ftp";
    public static final String FASTQ_ASPERA_FIELD = ",fastq_aspera";

    private static final String PORTAL_API_SEARCH_URL = "https://www.ebi.ac.uk/ena/portal/api/search?result=read_run" +
            "&fields=%s&format=json";
    private static final String PORTAL_API_SUPPORT_URL = "https://www.ebi.ac.uk/ena/portal/api/support?email=datasubs" +
            "@ebi.ac.uk&message=%s&to=%s&subject=%s&name=%s";
    private static final String PORTAL_API_INCLUDE_ACCESSION = "&includeAccessionType=%s";
    private static final String EXPERIMENT = "experiment";
    private static final String SAMPLE = "sample";
    private static final String STUDY = "study";
    private static final String COMMA = ",";
    private static final String URLENCODED = "application/x-www-form-urlencoded";
    private static final String APPLICATION_JSON = "application/json";
    private static final String MULTIPART_FORM_DATA = "multipart/form-data;boundary=%s";

    private RestTemplate restTemplate;

    /**
     * This API will invoke the Portal API and fetch the {@value SEARCH_FIELDS_FASTQ/SEARCH_FIELDS_SUBMITTED} for the
     * accessionIdList and dataType
     *
     * @param accessionIdList the experimentIds
     * @param dataType        The dataType provided by the user
     * @return The experiment details  for the accession Ids
     */
    public List<EnaPortalResponse> getPortalResponses(List<String> accessionIdList, DataTypeEnum dataType,
                                                      DownloadFormatEnum format, ProtocolEnum protocol) {
        int retryCount = 0;
        String portalAPIEndpoint = "";
        outer:
        switch (protocol) {
            case FTP:
                switch (dataType) {
                    case RAW_READS:
                    case HUMAN_READS:
                    case OTHER_SPECIES_READS:
                        switch (format) {
                            case FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_SEARCH_URL, SEARCH_FIELDS_FASTQ + FASTQ_FTP_FIELD + EXPERIMENT_READS);
                                break;
                            case SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_SEARCH_URL, SEARCH_FIELDS_SUBMITTED + FASTQ_SUBMITTED_FIELD + EXPERIMENT_READS);
                        }
                        portalAPIEndpoint = String.format(portalAPIEndpoint + PORTAL_API_INCLUDE_ACCESSION, EXPERIMENT);
                        break outer;
                    case SEQUENCED_SAMPLES:
                        switch (format) {
                            case FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_SEARCH_URL, SEARCH_FIELDS_FASTQ + FASTQ_FTP_FIELD + SEQUENCED_SAMPLE);
                                break;
                            case SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_SEARCH_URL, SEARCH_FIELDS_SUBMITTED + FASTQ_SUBMITTED_FIELD + SEQUENCED_SAMPLE);
                        }
                        portalAPIEndpoint = String.format(portalAPIEndpoint + PORTAL_API_INCLUDE_ACCESSION, SAMPLE);
                        break outer;
                    case STUDIES:
                        switch (format) {
                            case FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_SEARCH_URL, SEARCH_FIELDS_FASTQ + FASTQ_FTP_FIELD + STUDIES);
                                break;
                            case SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_SEARCH_URL, SEARCH_FIELDS_SUBMITTED + FASTQ_SUBMITTED_FIELD + STUDIES);
                        }
                        portalAPIEndpoint = String.format(portalAPIEndpoint + PORTAL_API_INCLUDE_ACCESSION, STUDY);
                        break outer;
                }
            case ASPERA:
                switch (dataType) {
                    case RAW_READS:
                    case HUMAN_READS:
                    case OTHER_SPECIES_READS:
                        switch (format) {
                            case FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_SEARCH_URL, SEARCH_FIELDS_FASTQ + FASTQ_ASPERA_FIELD + EXPERIMENT_READS);
                                break;
                            case SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_SEARCH_URL, SEARCH_FIELDS_SUBMITTED + FASTQ_ASPERA_FIELD + EXPERIMENT_READS);
                        }
                        portalAPIEndpoint = String.format(portalAPIEndpoint + PORTAL_API_INCLUDE_ACCESSION, EXPERIMENT);
                        break outer;
                    case SEQUENCED_SAMPLES:
                        switch (format) {
                            case FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_SEARCH_URL, SEARCH_FIELDS_FASTQ + FASTQ_ASPERA_FIELD + SEQUENCED_SAMPLE);
                                break;
                            case SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_SEARCH_URL, SEARCH_FIELDS_SUBMITTED + FASTQ_ASPERA_FIELD + SEQUENCED_SAMPLE);
                        }
                        portalAPIEndpoint = String.format(portalAPIEndpoint + PORTAL_API_INCLUDE_ACCESSION, SAMPLE);
                        break outer;
                    case STUDIES:
                        switch (format) {
                            case FASTQ:
                                portalAPIEndpoint = String.format(PORTAL_API_SEARCH_URL, SEARCH_FIELDS_FASTQ + FASTQ_ASPERA_FIELD + STUDIES);
                                break;
                            case SUBMITTED:
                                portalAPIEndpoint = String.format(PORTAL_API_SEARCH_URL, SEARCH_FIELDS_SUBMITTED + FASTQ_ASPERA_FIELD + STUDIES);
                        }
                        portalAPIEndpoint = String.format(portalAPIEndpoint + PORTAL_API_INCLUDE_ACCESSION, STUDY);
                        break outer;
                }
        }
        Assert.notNull(accessionIdList, "Accession IDs cannot be null");
        String experimentIds = String.join(COMMA, accessionIdList);
//        log.info("Calling Portal API URL:{} for dataType:{}", portalAPIEndpoint, dataType);
        URI uri = URI.create(portalAPIEndpoint);
        String body = "includeAccessions=" + experimentIds;
//        log.info("accs:{}", body);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", URLENCODED);
        httpHeaders.add("Accept", APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(body, httpHeaders);
        while (retryCount <= BeanConfig.APP_RETRY) {
            try {
                EnaPortalResponse[] response = restTemplate.postForObject(uri, request, EnaPortalResponse[].class);
                return Arrays.asList(Objects.requireNonNull(response));
            } catch (RestClientException rce) {
                log.error("Exception encountered while getting portalResponse for dataType:{}, format:{}", dataType,
                        format, rce);
                retryCount++;
            }
        }
        log.error("Count not fetch get portalResponse for dataType:{}, format:{} even after {} retries", dataType,
                format, BeanConfig.APP_RETRY);
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
