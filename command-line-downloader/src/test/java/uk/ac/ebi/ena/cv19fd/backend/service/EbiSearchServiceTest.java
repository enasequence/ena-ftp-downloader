package uk.ac.ebi.ena.cv19fd.backend.service;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DataTypeEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DomainEnum;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class EbiSearchServiceTest {

    ArgumentCaptor<URI> argumentCaptor = ArgumentCaptor.forClass(URI.class);

    @Mock
    RestTemplate restTemplate;

    @InjectMocks
    EbiSearchService ebiSearchService;

    @Test
    public void testGetCount_WithRawReads() throws URISyntaxException, JSONException {
        //ARRANGE
        URI uri = new URI("https://www.ebi.ac.uk/ebisearch/ws/rest/sra-experiment-covid19?format=JSON&query=id%3A%5B*%20TO%20*%5D&size=1");
        DomainEnum domainEnum = DomainEnum.VIRAL_SEQUENCES;
        DataTypeEnum dataTypeEnum = DataTypeEnum.RAW_READS;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hitCount", 12);
        Mockito.when(restTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(String.class))).thenReturn(jsonObject.toString());
        //ACT
        int count = ebiSearchService.getCounts(domainEnum, dataTypeEnum);
        //ASSERT
        verify(restTemplate, times(1)).getForObject(argumentCaptor.capture(), Mockito.eq(String.class));
        Assertions.assertEquals(12, count);
        Assertions.assertEquals(uri, argumentCaptor.getValue());

    }

    @Test
    public void testGetCount_WithSequencedSamples() throws URISyntaxException, JSONException {
        //ARRANGE
        URI uri = new URI("https://www.ebi.ac.uk/ebisearch/ws/rest/sra-sample-covid19?format=JSON&query=TAXONOMY%3A2697049&size=1");
        DomainEnum domainEnum = DomainEnum.VIRAL_SEQUENCES;
        DataTypeEnum dataTypeEnum = DataTypeEnum.SEQUENCED_SAMPLES;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hitCount", 14);
        Mockito.when(restTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(String.class))).thenReturn(jsonObject.toString());
        //ACT
        int count = ebiSearchService.getCounts(domainEnum, dataTypeEnum);
        //ASSERT
        verify(restTemplate, times(1)).getForObject(argumentCaptor.capture(), Mockito.eq(String.class));
        Assertions.assertEquals(14, count);
        Assertions.assertEquals(uri, argumentCaptor.getValue());

    }

    @Test
    public void testGetCount_WithStudies() throws URISyntaxException, JSONException {
        //ARRANGE
        URI uri = new URI("https://www.ebi.ac.uk/ebisearch/ws/rest/project-covid19?format=JSON&query=id%3A%5B*%20TO%20*%5D&size=1");
        DomainEnum domainEnum = DomainEnum.VIRAL_SEQUENCES;
        DataTypeEnum dataTypeEnum = DataTypeEnum.STUDIES;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hitCount", 16);
        Mockito.when(restTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(String.class))).thenReturn(jsonObject.toString());
        //ACT
        int count = ebiSearchService.getCounts(domainEnum, dataTypeEnum);
        //ASSERT
        verify(restTemplate, times(1)).getForObject(argumentCaptor.capture(), Mockito.eq(String.class));
        Assertions.assertEquals(16, count);
        Assertions.assertEquals(uri, argumentCaptor.getValue());

    }

    @Test
    public void testGetCount_ForHumanReads() throws URISyntaxException, JSONException {
        //ARRANGE
        URI uri = new URI("https://www.ebi.ac.uk/ebisearch/ws/rest/sra-experiment-covid19-host?format=JSON&query=TAXON%3A9606&size=1");
        DomainEnum domainEnum = DomainEnum.HOST_SEQUENCES;
        DataTypeEnum dataTypeEnum = DataTypeEnum.HUMAN_READS;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hitCount", 18);
        Mockito.when(restTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(String.class))).thenReturn(jsonObject.toString());
        //ACT
        int count = ebiSearchService.getCounts(domainEnum, dataTypeEnum);
        //ASSERT
        verify(restTemplate, times(1)).getForObject(argumentCaptor.capture(), Mockito.eq(String.class));
        Assertions.assertEquals(18, count);
        Assertions.assertEquals(uri, argumentCaptor.getValue());

    }

    @Test
    public void testGetCount_ForOtherSpeciesRead() throws URISyntaxException, JSONException {
        //ARRANGE
        URI uri = new URI("https://www.ebi.ac.uk/ebisearch/ws/rest/sra-experiment-covid19-host?format=JSON&query=id%3A%5B*%20TO%20*%5D%20NOT%20TAXON%3A9606&size=1");
        DomainEnum domainEnum = DomainEnum.HOST_SEQUENCES;
        DataTypeEnum dataTypeEnum = DataTypeEnum.OTHER_SPECIES_READS;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("hitCount", 20);
        Mockito.when(restTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(String.class))).thenReturn(jsonObject.toString());
        //ACT
        int count = ebiSearchService.getCounts(domainEnum, dataTypeEnum);
        //ASSERT
        verify(restTemplate, times(1)).getForObject(argumentCaptor.capture(), Mockito.eq(String.class));
        Assertions.assertEquals(20, count);
        Assertions.assertEquals(uri, argumentCaptor.getValue());
    }

    @Test
    public void testGetCount_WhenInvalidDomain() throws JSONException {
        //ARRANGE
        DomainEnum domainEnum = DomainEnum.HELP;
        DataTypeEnum dataTypeEnum = DataTypeEnum.OTHER_SPECIES_READS;

        //ACT
        int count = ebiSearchService.getCounts(domainEnum, dataTypeEnum);
        //ASSERT
        Assertions.assertEquals(0, count);
    }

    @Test
    public void testGetAccessionIds_ForRawReads() throws URISyntaxException {
        //ARRANGE
        DomainEnum domain = DomainEnum.VIRAL_SEQUENCES;
        DataTypeEnum dataType = DataTypeEnum.RAW_READS;
        int totalCount = 2;
        URI uri = new URI("https://www.ebi.ac.uk/ebisearch/ws/rest/sra-experiment-covid19?start=0&size=10000&format=idlist&query=id%3A%5B*%20TO%20*%5D");
        String str = "ERX4231114\nERX4231115";
        Mockito.when(restTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(String.class))).thenReturn(str);
        List<String> accIds = new ArrayList<>();
        accIds.add("ERX4231114");
        accIds.add("ERX4231115");
        //ACT
        List<List<String>> accessionIds = ebiSearchService.getAccessionIds(domain, dataType, totalCount);
        //ASSERT
        verify(restTemplate, times(1)).getForObject(argumentCaptor.capture(), Mockito.eq(String.class));
        Assertions.assertEquals(uri, argumentCaptor.getValue());

        Assertions.assertEquals(1, accessionIds.size());
        Assertions.assertEquals(accIds, accessionIds.get(0));
    }

    @Test
    public void testGetAccessionIds_ForSequencedSamples() throws URISyntaxException {
        //ARRANGE
        DomainEnum domain = DomainEnum.VIRAL_SEQUENCES;
        DataTypeEnum dataType = DataTypeEnum.SEQUENCED_SAMPLES;
        int totalCount = 2;
        URI uri = new URI("https://www.ebi.ac.uk/ebisearch/ws/rest/sra-sample-covid19?start=0&size=10000&format=idlist&query=TAXONOMY%3A2697049");
        String str = "ERX4231114\nERX4231115";
        Mockito.when(restTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(String.class))).thenReturn(str);
        List<String> accIds = new ArrayList<>();
        accIds.add("ERX4231114");
        accIds.add("ERX4231115");
        //ACT
        List<List<String>> accessionIds = ebiSearchService.getAccessionIds(domain, dataType, totalCount);
        //ASSERT
        verify(restTemplate, times(1)).getForObject(argumentCaptor.capture(), Mockito.eq(String.class));
        Assertions.assertEquals(uri, argumentCaptor.getValue());
        Assertions.assertEquals(1, accessionIds.size());
        Assertions.assertEquals(accIds, accessionIds.get(0));
    }

    @Test
    public void testGetAccessionIds_ForStudies() throws URISyntaxException {
        //ARRANGE
        DomainEnum domain = DomainEnum.VIRAL_SEQUENCES;
        DataTypeEnum dataType = DataTypeEnum.STUDIES;
        int totalCount = 2;
        URI uri = new URI("https://www.ebi.ac.uk/ebisearch/ws/rest/project-covid19?start=0&size=10000&format=idlist&query=id%3A%5B*%20TO%20*%5D");
        String str = "ERX4231114\nERX4231115";
        Mockito.when(restTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(String.class))).thenReturn(str);
        List<String> accIds = new ArrayList<>();
        accIds.add("ERX4231114");
        accIds.add("ERX4231115");
        //ACT
        List<List<String>> accessionIds = ebiSearchService.getAccessionIds(domain, dataType, totalCount);
        //ASSERT
        verify(restTemplate, times(1)).getForObject(argumentCaptor.capture(), Mockito.eq(String.class));
        Assertions.assertEquals(uri, argumentCaptor.getValue());
        Assertions.assertEquals(1, accessionIds.size());
        Assertions.assertEquals(accIds, accessionIds.get(0));
    }

    @Test
    public void testGetAccessionIds_ForHumanReads() throws URISyntaxException {
        //ARRANGE
        DomainEnum domain = DomainEnum.HOST_SEQUENCES;
        DataTypeEnum dataType = DataTypeEnum.HUMAN_READS;
        int totalCount = 2;
        URI uri = new URI("https://www.ebi.ac.uk/ebisearch/ws/rest/sra-experiment-covid19-host?start=0&size=10000&format=idlist&query=TAXON%3A9606");
        String str = "ERX4231114\nERX4231115";
        Mockito.when(restTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(String.class))).thenReturn(str);
        List<String> accIds = new ArrayList<>();
        accIds.add("ERX4231114");
        accIds.add("ERX4231115");
        //ACT
        List<List<String>> accessionIds = ebiSearchService.getAccessionIds(domain, dataType, totalCount);
        //ASSERT
        verify(restTemplate, times(1)).getForObject(argumentCaptor.capture(), Mockito.eq(String.class));
        Assertions.assertEquals(uri, argumentCaptor.getValue());
        Assertions.assertEquals(1, accessionIds.size());
        Assertions.assertEquals(accIds, accessionIds.get(0));
    }

    @Test
    public void testGetAccessionIds_ForOtherSpeciesReads() throws URISyntaxException {
        //ARRANGE
        DomainEnum domain = DomainEnum.HOST_SEQUENCES;
        DataTypeEnum dataType = DataTypeEnum.OTHER_SPECIES_READS;
        int totalCount = 2;
        URI uri = new URI("https://www.ebi.ac.uk/ebisearch/ws/rest/sra-experiment-covid19-host?start=0&size=10000&format=idlist&query=id%3A%5B*%20TO%20*%5D%20NOT%20TAXON%3A9606");
        String str = "ERX4231114\nERX4231115";
        Mockito.when(restTemplate.getForObject(Mockito.any(URI.class), Mockito.eq(String.class))).thenReturn(str);
        List<String> accIds = new ArrayList<>();
        accIds.add("ERX4231114");
        accIds.add("ERX4231115");
        //ACT
        List<List<String>> accessionIds = ebiSearchService.getAccessionIds(domain, dataType, totalCount);
        //ASSERT
        verify(restTemplate, times(1)).getForObject(argumentCaptor.capture(), Mockito.eq(String.class));
        Assertions.assertEquals(uri, argumentCaptor.getValue());
        Assertions.assertEquals(1, accessionIds.size());
        Assertions.assertEquals(accIds, accessionIds.get(0));
    }
}
