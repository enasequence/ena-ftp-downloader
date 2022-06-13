package uk.ac.ebi.ena.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.ena.app.menu.enums.AccessionTypeEnum;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.app.utils.CommonUtils;
import uk.ac.ebi.ena.backend.dto.DownloadJob;
import uk.ac.ebi.ena.backend.dto.EnaPortalResponse;
import uk.ac.ebi.ena.backend.enums.FileDownloadStatus;
import uk.ac.ebi.ena.backend.service.AccessionDetailsService;
import uk.ac.ebi.ena.backend.service.EmailService;
import uk.ac.ebi.ena.backend.service.EnaPortalService;
import uk.ac.ebi.ena.backend.service.FileDownloaderService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class AccessionDetailsServiceTest {

    @Mock
    EnaPortalService enaPortalService;

    @Mock
    FileDownloaderService fileDownloaderService;

    @Mock
    EmailService emailService;

    @InjectMocks
    AccessionDetailsService accessionDetailsService;


    String path = System.getProperty("user.home");

    String accessionList = "SRX6415696,SRX2000905,SRX6415695";

   /* @Test
    public void testFetchAccessionAndDownloadWhenSuccess() throws ExecutionException, InterruptedException {
        //ARRANGE
        DownloadFormatEnum format = DownloadFormatEnum.READS_FASTQ;
        String downloadLocation = path;
        DownloadJob accessionDetailsMap = CommonUtils.processAccessions(Arrays.asList(accessionList.split(",")));
        ProtocolEnum protocol = ProtocolEnum.FTP;
        String asperaLocation = null;
        String recipientEmailId = "datasubs@ebi.ac.uk";
        Mockito.when(enaPortalService.getPortalResponses(Mockito.anyList(), Mockito.any(DownloadFormatEnum.class), Mockito.any(ProtocolEnum.class), Mockito.any()))
                .thenReturn(getPortalResponses());
        final Future<FileDownloadStatus> mockedFuture = Mockito.mock(Future.class);
        when(mockedFuture.get()).thenReturn(new FileDownloadStatus(0, 0, new ArrayList<>()));
        Mockito.when(fileDownloaderService.startDownload(Mockito.any(ExecutorService.class), Mockito.any(List.class),
                Mockito.any(String.class), Mockito.any(AccessionTypeEnum.class), Mockito.any(DownloadFormatEnum.class),
                Mockito.anyInt())).thenReturn(mockedFuture);
        //ACT
        accessionDetailsService.doDownload(format, downloadLocation, accessionDetailsMap, protocol, asperaLocation, recipientEmailId);
        //ASSERT
        verify(enaPortalService, times(3)).getPortalResponses(Mockito.anyList(), Mockito.any(DownloadFormatEnum.class), Mockito.any(ProtocolEnum.class),
                Mockito.any());

        verify(fileDownloaderService, times(3)).startDownload(Mockito.any(ExecutorService.class), Mockito.anyList(), Mockito.any(String.class),
                Mockito.any(AccessionTypeEnum.class), Mockito.any(DownloadFormatEnum.class), Mockito.anyInt());


    }*/


    private List<EnaPortalResponse> getPortalResponses() {
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
        return portalResponses;
    }
}
