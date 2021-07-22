package uk.ac.ebi.ena.cv19fd.backend.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DataTypeEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DomainEnum;
import uk.ac.ebi.ena.cv19fd.backend.config.BeanConfig;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static uk.ac.ebi.ena.cv19fd.app.utils.CommonUtils.getProgressBarBuilder;

/**
 * This class will invoke the ebisearch API and get the total number of experiments and accession IDs for a given domain,datatype
 */
@Component
@Slf4j
@AllArgsConstructor
public class EbiSearchService {

    private static final int PAGE_SIZE = 10000;

    private static final String EBISEARCH_URL = "https://www.ebi.ac.uk/ebisearch/ws/rest";
    private static final String RAW_READS_ENDPOINT = EBISEARCH_URL + "/sra-experiment-covid19";
    private static final String SEQUENCED_SAMPLE_ENDPOINT = EBISEARCH_URL + "/sra-sample-covid19";
    private static final String STUDIES_ENDPOINT = EBISEARCH_URL + "/project-covid19";
    private static final String HUMAN_READS_ENDPOINT = EBISEARCH_URL + "/sra-experiment-covid19-host";
    private static final String OTHER_SPECIES_READ_ENDPOINT = EBISEARCH_URL + "/sra-experiment-covid19-host";

    private static final String QUERY_PARAM = "?format=JSON&query=id%3A%5B*%20TO%20*%5D&size=1";
    private static final String HUMAN_READS_QUERY_PARAM = "?format=JSON&query=TAXON%3A9606&size=1";
    private static final String OTHER_READS_QUERY_PARAM = "?format=JSON&query=id%3A%5B*%20TO%20*%5D%20NOT%20TAXON%3A9606&size=1";
    private static final String SEQUENCED_SAMPLE_QUERY_PARAM = "?format=JSON&query=TAXONOMY%3A2697049&size=1";
    private static final String ACCESSION_IDLIST_QUERY_PARAM = "?start=%d&size=10000&format=idlist&query=%s";

    private static final String ACCESSION_IDLIST_QUERY_RAW_READS = "id%3A%5B*%20TO%20*%5D";
    private static final String ACCESSION_IDLIST_QUERY_SEQUENCED_SAMPLE = "TAXONOMY%3A2697049";
    private static final String ACCESSION_IDLIST_QUERY_HUMAN_READS = "TAXON%3A9606";
    private static final String ACCESSION_IDLIST_QUERY_OTHER_SPECIES = "id%3A%5B*%20TO%20*%5D%20NOT%20TAXON%3A9606";
    private final RestTemplate restTemplate;

    /**
     * This API will return total number of experiments for a given domain and dataType
     *
     * @param domain   The domain provided by the user
     * @param dataType The dataType provided by the user
     * @return The total number of experiments for a given domain and dataType
     * @throws JSONException in case of issues while parsing the JSON
     */
    public int getCounts(DomainEnum domain, DataTypeEnum dataType) throws JSONException {
        switch (domain) {
            case VIRAL_SEQUENCES:
                switch (dataType) {
                    case RAW_READS:
                        return getTotalCount(RAW_READS_ENDPOINT + QUERY_PARAM);
                    case SEQUENCED_SAMPLES:
                        return getTotalCount(SEQUENCED_SAMPLE_ENDPOINT + SEQUENCED_SAMPLE_QUERY_PARAM);
                    case STUDIES:
                        return getTotalCount(STUDIES_ENDPOINT + QUERY_PARAM);
                }
            case HOST_SEQUENCES:
                switch (dataType) {
                    case HUMAN_READS:
                        return getTotalCount(HUMAN_READS_ENDPOINT + HUMAN_READS_QUERY_PARAM);
                    case OTHER_SPECIES_READS:
                        return getTotalCount(OTHER_SPECIES_READ_ENDPOINT + OTHER_READS_QUERY_PARAM);
                }
        }
        return 0;
    }

    private int getTotalCount(String ebiSearchUrl) throws JSONException {
        int retryCount = 0;
        URI uri = URI.create(Objects.requireNonNull(ebiSearchUrl));
        while (retryCount <= BeanConfig.APP_RETRY) {
            try {
                String response = restTemplate.getForObject(uri, String.class);
                return getHitCountFromJson(response);
            } catch (RestClientException ex) {
                log.error("Exception encountered while getting total count from ebiSearchUrl:{}", ebiSearchUrl, ex);
                retryCount++;
            }
        }
        log.error("Count not fetch total count from ebiSearchUrl:{} even after {} retries", ebiSearchUrl, BeanConfig.APP_RETRY);
        return 0;

    }

    private static int getHitCountFromJson(String text) throws JSONException {
        JSONObject json = new JSONObject(text);
        return json.getInt("hitCount");
    }

    /**
     * This API will get the accession IDs  for a given domain and dataType
     *
     * @param domain     The domain provided by the user
     * @param dataType   The dataType provided by the user
     * @param totalCount The total number of experiments
     * @return the accession IDs grouped by page number
     */
    public List<List<String>> getAccessionIds(DomainEnum domain, DataTypeEnum dataType, int totalCount) {

        int totalPages = (totalCount % PAGE_SIZE == 0) ? (totalCount / PAGE_SIZE) : (totalCount / PAGE_SIZE) + 1;
        ProgressBarBuilder pbb = getProgressBarBuilder("Requesting accessions from EBISearch", totalPages);

        List<List<String>> list = Collections.synchronizedList(new ArrayList<>());
        ProgressBar.wrap(IntStream.range(0, totalPages).parallel(), pbb)
                .forEach(p -> list.add(getIds(domain, dataType, p)));
        return list;
    }

    private List<String> getIds(DomainEnum domain, DataTypeEnum dataType, int currentPage) {
        int retryCount = 0;
        int startId = currentPage * PAGE_SIZE;
        String endpoint = null;
        switch (domain) {
            case VIRAL_SEQUENCES:
                switch (dataType) {
                    case RAW_READS:
                        endpoint = String.format(RAW_READS_ENDPOINT + ACCESSION_IDLIST_QUERY_PARAM, startId, ACCESSION_IDLIST_QUERY_RAW_READS);
                        break;
                    case SEQUENCED_SAMPLES:
                        endpoint = String.format(SEQUENCED_SAMPLE_ENDPOINT + ACCESSION_IDLIST_QUERY_PARAM, startId, ACCESSION_IDLIST_QUERY_SEQUENCED_SAMPLE);
                        break;
                    case STUDIES:
                        endpoint = String.format(STUDIES_ENDPOINT + ACCESSION_IDLIST_QUERY_PARAM, startId, ACCESSION_IDLIST_QUERY_RAW_READS);
                        break;
                }
            case HOST_SEQUENCES:
                switch (dataType) {
                    case HUMAN_READS:
                        endpoint = String.format(HUMAN_READS_ENDPOINT + ACCESSION_IDLIST_QUERY_PARAM, startId, ACCESSION_IDLIST_QUERY_HUMAN_READS);
                        break;
                    case OTHER_SPECIES_READS:
                        endpoint = String.format(OTHER_SPECIES_READ_ENDPOINT + ACCESSION_IDLIST_QUERY_PARAM, startId, ACCESSION_IDLIST_QUERY_OTHER_SPECIES);
                        break;
                }
        }
        URI uri = URI.create(Objects.requireNonNull(endpoint));
        while (retryCount <= BeanConfig.APP_RETRY) {
            try {
                String response = restTemplate.getForObject(uri, String.class);
                return getAccessionIdsFromString(Objects.requireNonNull(response));
            } catch (RestClientException rce) {
                log.error("Exception encountered while getting accessionIds for domain:{}, dataType:{}, currentPage = {}", domain, dataType, currentPage, rce);
                retryCount++;
            }
        }
        log.error("Count not fetch accessionIds for domain:{}, dataType:{}, currentPage = {} even after {} retries", domain, dataType, currentPage, BeanConfig.APP_RETRY);
        return Collections.emptyList();
    }

    private static List<String> getAccessionIdsFromString(String idString) {
        return Arrays.stream(idString.split("\n")).collect(Collectors.toList());
    }

}
