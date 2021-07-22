package uk.ac.ebi.ena.cv19fd.backend.service;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DataTypeEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.cv19fd.backend.dto.EnaPortalResponse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class EnaPortalServiceTest {

    @Mock
    RestTemplate restTemplate;

    @InjectMocks
    EnaPortalService enaPortalService;


    @Test
    public void testGetPortalResponse_ForRawReadsFastq() throws URISyntaxException {
        //ARRANGE
        URI uri = new URI("https://www.ebi.ac.uk/ena/portal/api/search?result=read_run&fields=fastq_bytes,fastq_md5,fastq_ftp,experiment_accession&format=json&includeAccessionType=experiment");
        List<String> accessionIdList = new ArrayList<>();
        accessionIdList.add("ERX4231114");
        accessionIdList.add("ERX4231115");
        DataTypeEnum dataType = DataTypeEnum.RAW_READS;
        DownloadFormatEnum format = DownloadFormatEnum.FASTQ;
        ProtocolEnum protocol = ProtocolEnum.FTP;

        EnaPortalResponse[] portalResponses = createEnaPortalResponse();
        List<EnaPortalResponse> enaPortalResponses = Arrays.asList(portalResponses);
        Mockito.when(restTemplate.postForObject(Mockito.any(URI.class), Mockito.any(HttpEntity.class), Mockito.eq(EnaPortalResponse[].class))).thenReturn(portalResponses);
        //ACT
        List<EnaPortalResponse> portalResponseList = enaPortalService.getPortalResponses(accessionIdList, dataType, format, protocol);
        //ASSERT
        ArgumentCaptor<URI> uriArgumentCaptor = ArgumentCaptor.forClass(URI.class);

        ArgumentCaptor<Object> httpEntityArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        HttpEntity<String> request = createHttpEntity();
        verify(restTemplate, times(1)).postForObject(uriArgumentCaptor.capture(), httpEntityArgumentCaptor.capture(), Mockito.eq(EnaPortalResponse[].class));
        Assertions.assertEquals(uri, uriArgumentCaptor.getValue());
        Assertions.assertEquals(request, httpEntityArgumentCaptor.getValue());
        Assertions.assertEquals(enaPortalResponses, portalResponseList);
    }

    @Test
    public void testGetPortalResponse_ForSequencedSampleSubmitted() throws URISyntaxException {
        //ARRANGE
        URI uri = new URI("https://www.ebi.ac.uk/ena/portal/api/search?result=read_run&fields=submitted_bytes,submitted_md5,submitted_ftp,sample_accession&format=json&includeAccessionType=sample");
        List<String> accessionIdList = new ArrayList<>();
        accessionIdList.add("ERX4231114");
        accessionIdList.add("ERX4231115");
        DataTypeEnum dataType = DataTypeEnum.SEQUENCED_SAMPLES;
        DownloadFormatEnum format = DownloadFormatEnum.SUBMITTED;
        ProtocolEnum protocol = ProtocolEnum.FTP;

        EnaPortalResponse[] portalResponses = createEnaPortalResponse();
        List<EnaPortalResponse> enaPortalResponses = Arrays.asList(portalResponses);
        Mockito.when(restTemplate.postForObject(Mockito.any(URI.class), Mockito.any(HttpEntity.class), Mockito.eq(EnaPortalResponse[].class))).thenReturn(portalResponses);
        //ACT
        List<EnaPortalResponse> portalResponseList = enaPortalService.getPortalResponses(accessionIdList, dataType, format, protocol);
        //ASSERT
        HttpEntity<String> request = createHttpEntity();
        ArgumentCaptor<URI> uriArgumentCaptor = ArgumentCaptor.forClass(URI.class);

        ArgumentCaptor<Object> httpEntityArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(restTemplate, times(1)).postForObject(uriArgumentCaptor.capture(), httpEntityArgumentCaptor.capture(), Mockito.eq(EnaPortalResponse[].class));
        Assertions.assertEquals(uri, uriArgumentCaptor.getValue());
        Assertions.assertEquals(request, httpEntityArgumentCaptor.getValue());
        Assertions.assertEquals(enaPortalResponses, portalResponseList);
    }

    @Test
    public void testGetPortalResponse_ForStudiesFastq() throws URISyntaxException {
        //ARRANGE
        URI uri = new URI("https://www.ebi.ac.uk/ena/portal/api/search?result=read_run&fields=fastq_bytes,fastq_md5,fastq_ftp,study_accession&format=json&includeAccessionType=study");
        List<String> accessionIdList = new ArrayList<>();
        accessionIdList.add("ERX4231114");
        accessionIdList.add("ERX4231115");
        DataTypeEnum dataType = DataTypeEnum.STUDIES;
        DownloadFormatEnum format = DownloadFormatEnum.FASTQ;
        ProtocolEnum protocol = ProtocolEnum.FTP;

        EnaPortalResponse[] portalResponses = createEnaPortalResponse();
        List<EnaPortalResponse> enaPortalResponses = Arrays.asList(portalResponses);
        Mockito.when(restTemplate.postForObject(Mockito.any(URI.class), Mockito.any(HttpEntity.class), Mockito.eq(EnaPortalResponse[].class))).thenReturn(portalResponses);
        //ACT
        List<EnaPortalResponse> portalResponseList = enaPortalService.getPortalResponses(accessionIdList, dataType, format, protocol);
        //ASSERT
        HttpEntity<String> request = createHttpEntity();
        ArgumentCaptor<URI> uriArgumentCaptor = ArgumentCaptor.forClass(URI.class);

        ArgumentCaptor<Object> httpEntityArgumentCaptor = ArgumentCaptor.forClass(Object.class);
        verify(restTemplate, times(1)).postForObject(uriArgumentCaptor.capture(), httpEntityArgumentCaptor.capture(), Mockito.eq(EnaPortalResponse[].class));
        Assertions.assertEquals(uri, uriArgumentCaptor.getValue());
        Assertions.assertEquals(request, httpEntityArgumentCaptor.getValue());
        Assertions.assertEquals(enaPortalResponses, portalResponseList);
    }

    private EnaPortalResponse[] createEnaPortalResponse() {
        List<EnaPortalResponse> enaPortalResponses = new ArrayList<>();
        EnaPortalResponse enaPortalResponse1 = new EnaPortalResponse();
        enaPortalResponse1.setParentId("ERX4231114");
        enaPortalResponse1.setRunId("ERR4276487");
        enaPortalResponse1.setBytes("284982107;89194660");
        enaPortalResponse1.setMd5("5a718b406f4f76984e2bf229b5c822d8;10029431c51c5fefb0759ac5639b1595");
        enaPortalResponse1.setFtpUrl("ftp.sra.ebi.ac.uk/vol1/fastq/ERR427/007/ERR4276487/ERR4276487_1.fastq.gz;ftp.sra.ebi.ac.uk/vol1/fastq/ERR427/007/ERR4276487/ERR4276487_2.fastq.gz");
        enaPortalResponses.add(enaPortalResponse1);
        EnaPortalResponse enaPortalResponse2 = new EnaPortalResponse();
        enaPortalResponse2.setParentId("ERX4231115");
        enaPortalResponse2.setRunId("ERR4276488");
        enaPortalResponse2.setBytes("284982108;89194661");
        enaPortalResponse2.setMd5("5a718b406f4f76984e2bf229b5c822d9;10029431c51c5fefb0759ac5639b1596");
        enaPortalResponse2.setFtpUrl("ftp.sra.ebi.ac.uk/vol1/fastq/ERR427/007/ERR4276488/ERR4276488_1.fastq.gz;ftp.sra.ebi.ac.uk/vol1/fastq/ERR427/007/ERR4276488/ERR4276488_2.fastq.gz");
        enaPortalResponses.add(enaPortalResponse2);
        EnaPortalResponse[] portalResponses = new EnaPortalResponse[enaPortalResponses.size()];
        return enaPortalResponses.toArray(portalResponses);
    }

    private HttpEntity<String> createHttpEntity() {
        String body = "includeAccessions=" + "ERX4231114,ERX4231115";
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", "application/x-www-form-urlencoded");
        httpHeaders.add("Accept", "application/json");
        return new HttpEntity<>(body, httpHeaders);
    }

}
