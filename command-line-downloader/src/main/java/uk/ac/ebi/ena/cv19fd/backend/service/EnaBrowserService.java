package uk.ac.ebi.ena.cv19fd.backend.service;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DataTypeEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DomainEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.cv19fd.app.utils.HttpUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Vector;

@Component
@Slf4j
public class EnaBrowserService {

    private static final String ENABROWSER_URL = "https://www.ebi.ac.uk/ena/browser/api/";

    private static final String EMBL_ENDPOINT = ENABROWSER_URL + "embl/textsearch";
    private static final String XML_ENDPOINT = ENABROWSER_URL + "xml/textsearch";
    private static final String FASTA_ENDPOINT = ENABROWSER_URL + "fasta/textsearch";
    private static final String EMBL_ENDPOINT_WITH_LIST = ENABROWSER_URL + "embl/";
    private static final String XML_ENDPOINT_WITH_LIST = ENABROWSER_URL + "xml/";
    private static final String FASTA_ENDPOINT_WITH_LIST = ENABROWSER_URL + "fasta/";
    private String query = "";
    private String downloadDomain = "";
    private static final String emblDomain = "embl-covid19";
    private static final String sraExperimentDomain = "sra-experiment-covid19-host";
    private static final String projectDomain = "project-covid19";
    private static final String sraSampleDomain = "sra-sample-covid19";

    private static final String VS_SEQUENCES_EMBL_QUERY_PARAM = "id:[* TO *]";
    private static final String VS_RAW_READ_XML_QUERY_PARAM = "id:[* TO *]";
    private static final String VS_REFERENCE_SEQUENCES_EMBL_QUERY_PARAM = "id:MN908947^3 OR id:LR991698^2";
    private static final String VS_REFERENCE_SEQUENCES_FASTA_QUERY_PARAM = "id:MN908947^3 OR id:LR991698^2";
    private static final String VS_SAMPLE_SEQUENCES_XML_QUERY_PARAM = "id:[* TO *]";
    private static final String VS_STUDIES_XML_QUERY_PARAM = "id:[* TO *]";
    private static final String HS_HUMAN_READS_XML_QUERY_PARAM = "(TAXON:(9606))";
    private static final String HS_OTHER_SPECIES_READ_XML_QUERY_PARAM = "(id:[* TO *] NOT TAXON:9606)";
    private static final String URLENCODED = "application/x-www-form-urlencoded";
    private static final String APPLICATION_JSON = "application/json";
    private RestTemplate restTemplate;

    public InputStream getInputStreamForDownloadedFile(List<String> accessions, DomainEnum domain, DataTypeEnum dataType,
                                                       DownloadFormatEnum format) throws IOException {
        String enaBrowserAPIEndpoint = "";
        if (accessions != null && accessions.size() > 0) {
                switch (format) {
                    case XML:
                        enaBrowserAPIEndpoint = XML_ENDPOINT_WITH_LIST;
                        break;
                    case EMBL:
                        enaBrowserAPIEndpoint = EMBL_ENDPOINT_WITH_LIST;
                        break;
                    case FASTA:
                        enaBrowserAPIEndpoint = FASTA_ENDPOINT_WITH_LIST;
                        break;
                }

                HttpClient httpClient = HttpClientBuilder.create().build();
                HttpPost request = new HttpPost(enaBrowserAPIEndpoint);
                StringEntity params = new StringEntity("accessions=" + StringUtils.join(accessions, ','));
                request.addHeader("content-type", "application/x-www-form-urlencoded");
                request.setEntity(params);
                HttpResponse response = httpClient.execute(request);
                return response.getEntity().getContent();

        } else {
            switch (domain) {
                case VIRAL_SEQUENCES:
                    switch (dataType) {
                        case SEQUENCES:
                            query = VS_SEQUENCES_EMBL_QUERY_PARAM;
                            switch (format) {
                                case EMBL:
                                    enaBrowserAPIEndpoint = EMBL_ENDPOINT;
                                    downloadDomain = emblDomain;
                                    break;
                                case FASTA:
                                    enaBrowserAPIEndpoint = FASTA_ENDPOINT;
                                    downloadDomain = emblDomain;
                                    break;
                            }
                            break;
                        case RAW_READS:

                            if (DownloadFormatEnum.XML == format) {
                                enaBrowserAPIEndpoint = XML_ENDPOINT;
                                downloadDomain = sraExperimentDomain;
                                query = VS_RAW_READ_XML_QUERY_PARAM;
                            }
                            break;
                        case REFERENCE_SEQUENCES:
                            switch (format) {
                                case EMBL:
                                    enaBrowserAPIEndpoint = EMBL_ENDPOINT;
                                    downloadDomain = emblDomain;
                                    query = VS_REFERENCE_SEQUENCES_EMBL_QUERY_PARAM;
                                    break;
                                case FASTA:
                                    enaBrowserAPIEndpoint = FASTA_ENDPOINT;
                                    downloadDomain = emblDomain;
                                    query = VS_REFERENCE_SEQUENCES_FASTA_QUERY_PARAM;
                                    break;
                            }
                            break;
                        case SEQUENCED_SAMPLES:
                            if (DownloadFormatEnum.XML == format) {
                                enaBrowserAPIEndpoint = XML_ENDPOINT;
                                downloadDomain = sraSampleDomain;
                                query = VS_SAMPLE_SEQUENCES_XML_QUERY_PARAM;
                            }
                            break;
                        case STUDIES:
                            if (DownloadFormatEnum.XML == format) {
                                enaBrowserAPIEndpoint = XML_ENDPOINT;
                                downloadDomain = projectDomain;
                                query = VS_STUDIES_XML_QUERY_PARAM;
                            }
                            break;
                    }
                    break;
                case HOST_SEQUENCES:
                    switch (dataType) {
                        case HUMAN_READS:
                            if (DownloadFormatEnum.XML == format) {
                                enaBrowserAPIEndpoint = XML_ENDPOINT;
                                downloadDomain = sraExperimentDomain;
                                query = HS_HUMAN_READS_XML_QUERY_PARAM;
                            }
                            break;
                        case OTHER_SPECIES_READS:
                            if (DownloadFormatEnum.XML == format) {
                                enaBrowserAPIEndpoint = XML_ENDPOINT;
                                downloadDomain = sraExperimentDomain;
                                query = HS_OTHER_SPECIES_READ_XML_QUERY_PARAM;
                            }
                            break;
                    }
                    break;
            }

            Map<String, String> paramsMap;
            paramsMap = HttpUtils.asParamsMap("query", URLEncoder.encode(query),
                    "domain", downloadDomain);
            String url = HttpUtils.buildUrl(enaBrowserAPIEndpoint, paramsMap);
            URL urlConnection = new URL(url);
            URLConnection urlConnection1 = urlConnection.openConnection();
            urlConnection1.connect();
            log.info("Starting file download for url:{}", url);
            return urlConnection1.getInputStream();

        }
    }
}
