package uk.ac.ebi.ena.backend;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.app.utils.CommonUtils;
import uk.ac.ebi.ena.backend.dto.DownloadJob;
import uk.ac.ebi.ena.backend.dto.EnaPortalResponse;
import uk.ac.ebi.ena.backend.service.EnaPortalService;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    public void testGetPortalResponse() {
        //ARRANGE
        String accessionList = "SRX6415696,SRX2000905,SRX6415695";
        List<String> accessionIdList = Arrays.asList(accessionList.split(","));
        DownloadFormatEnum format = DownloadFormatEnum.READS_FASTQ;
        ProtocolEnum protocol = ProtocolEnum.FTP;
        DownloadJob accessionDetailsMap = CommonUtils.processAccessions(Arrays.asList(accessionList.split(",")));
        Mockito.when(restTemplate.postForObject(Mockito.any(URI.class), Mockito.any(HttpEntity.class),
                Mockito.eq(EnaPortalResponse[].class))).thenReturn(getPortalResponses());
        //ACT
        List<EnaPortalResponse> portalResponses = enaPortalService.getPortalResponses(accessionIdList, format, protocol, accessionDetailsMap);
        //ASSERT
        Assertions.assertEquals(getPortalResponses().length, portalResponses.size());

    }

    @Test
    public void testSendEmail() throws UnsupportedEncodingException {
        //ARRANGE
        String recipientEmail = "yourmail@ebi.ac.uk";
        String message = URLEncoder.encode("Test email message", "UTF-8");
        String subject = URLEncoder.encode("Ena ANALYSIS ANALYSIS_SUBMITTED file download completed", "UTF-8");
        String name = URLEncoder.encode("For any issues please contact raise a support ticket to ENA", "UTF-8");
        //ACT
        enaPortalService.sendEmail(recipientEmail, message, subject, name);
        //ASSERT
        verify(restTemplate, times(1)).postForObject(Mockito.any(URI.class), Mockito.any(HttpEntity.class), Mockito.eq(String.class));
    }

    private EnaPortalResponse[] getPortalResponses() {
        List<EnaPortalResponse> portalResponses = new ArrayList<>();
        EnaPortalResponse enaPortalResponse1 = new EnaPortalResponse();
        enaPortalResponse1.setParentId("SRX2000905");
        enaPortalResponse1.setRecordId("SRR4000583");
        enaPortalResponse1.setBytes("1174738707");
        enaPortalResponse1.setUrl("ftp.sra.ebi.ac.uk/vol1/fastq/SRR400/003/SRR4000583/SRR4000583.fastq.gz");
        enaPortalResponse1.setMd5("a991ce890047ffca760c6de2617b5fec");
        portalResponses.add(enaPortalResponse1);
        EnaPortalResponse enaPortalResponse2 = new EnaPortalResponse();
        enaPortalResponse2.setParentId("SRX6415696");
        enaPortalResponse2.setRecordId("SRR9654360");
        enaPortalResponse2.setBytes("14139836");
        enaPortalResponse2.setUrl("ftp.sra.ebi.ac.uk/vol1/fastq/SRR965/000/SRR9654360/SRR9654360.fastq.gz");
        enaPortalResponse2.setMd5("f3611f35a977b8b82a7adcf0a28c397d");
        portalResponses.add(enaPortalResponse2);
        EnaPortalResponse enaPortalResponse3 = new EnaPortalResponse();
        enaPortalResponse3.setParentId("SRX6415695");
        enaPortalResponse3.setRecordId("SRR9654361");
        enaPortalResponse3.setBytes("15541843");
        enaPortalResponse3.setUrl("ftp.sra.ebi.ac.uk/vol1/fastq/SRR965/001/SRR9654361/SRR9654361.fastq.gz");
        enaPortalResponse3.setMd5("1236b79cd93a63289841765aabacb880");
        portalResponses.add(enaPortalResponse3);
        EnaPortalResponse[] portalResponse = new EnaPortalResponse[portalResponses.size()];
        return portalResponses.toArray(portalResponse);

    }
}
