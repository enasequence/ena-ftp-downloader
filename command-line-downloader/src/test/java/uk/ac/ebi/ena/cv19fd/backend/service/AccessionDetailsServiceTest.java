package uk.ac.ebi.ena.cv19fd.backend.service;

import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DataTypeEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DomainEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.cv19fd.app.utils.FileUtils;
import uk.ac.ebi.ena.cv19fd.backend.dto.EnaPortalResponse;
import uk.ac.ebi.ena.cv19fd.backend.enums.FileDownloadStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class AccessionDetailsServiceTest {

    @Mock
    EbiSearchService ebiSearchService;

    @Mock
    EnaPortalService enaPortalService;

    @Mock
    FileDownloaderService fileDownloaderService;

    @Mock
    Future<FileDownloadStatus> mockFuture = CompletableFuture.completedFuture(new FileDownloadStatus(0, 0, null));

    @Mock
    EmailService emailService;

    @Captor
    ArgumentCaptor<List<String>> listArgumentCaptor;

    @InjectMocks
    AccessionDetailsService accessionDetailsService;

    @Test
    public void testFetchAccessionDetailsWhenSuccess() throws Exception {
        //ARRANGE
        DomainEnum domain = DomainEnum.VIRAL_SEQUENCES;
        DataTypeEnum dataType = DataTypeEnum.RAW_READS;
        DownloadFormatEnum format = DownloadFormatEnum.FASTQ;
        ProtocolEnum protocol = ProtocolEnum.FTP;
        when(ebiSearchService.getCounts(domain, dataType)).thenReturn(5000);
        when(ebiSearchService.getAccessionIds(domain, dataType, 5000)).thenReturn(getAcccessionIds());
        String scriptFileName = FileUtils.getScriptPath(domain, dataType, format, null);
        when(enaPortalService.getPortalResponses(Mockito.anyList(), Mockito.any(DataTypeEnum.class),
                Mockito.any(DownloadFormatEnum.class), Mockito.any(ProtocolEnum.class))).thenReturn(getPortalResponses());
        final Future<FileDownloadStatus> mockedFuture = Mockito.mock(Future.class);
        when(mockedFuture.get()).thenReturn(new FileDownloadStatus(0, 0, new ArrayList<>()));

        when(fileDownloaderService.startDownload(Mockito.any(ExecutorService.class), Mockito.any(List.class),
                Mockito.any(String.class), Mockito.any(DomainEnum.class), Mockito.any(DataTypeEnum.class),
                Mockito.any(DownloadFormatEnum.class), Mockito.anyInt())).thenReturn(mockedFuture);

        //ACT
        accessionDetailsService.fetchAccessionAndDownload(domain, dataType, format, "C:\\Users", "datasubs@ebi.ac.uk", new ArrayList<>(),
                ProtocolEnum.FTP, null);
        //ASSERT
        verify(enaPortalService, times(2)).getPortalResponses(listArgumentCaptor.capture(), Mockito.eq(dataType),
                Mockito.eq(format), Mockito.eq(protocol));
        verify(emailService, times(1)).sendEmailForFastqSubmitted("datasubs@ebi.ac.uk",
                0, 0, scriptFileName,
                dataType, format, "C:\\Users");
    }

    @Test
    public void testFetchAccessionDetailsWhenIncorrectFormat() throws Exception {
        //ARRANGE
        DomainEnum domain = DomainEnum.VIRAL_SEQUENCES;
        DataTypeEnum dataType = DataTypeEnum.RAW_READS;
        DownloadFormatEnum format = DownloadFormatEnum.EMBL;
        //ACT
        accessionDetailsService.fetchAccessionAndDownload(domain, dataType, format, "C:\\Users", "suman.ebi.ac.uk", new ArrayList<>(),
                ProtocolEnum.FTP, null);
        //ASSERT
        verify(ebiSearchService, times(0)).getCounts(domain, dataType);

    }

    @Test
    public void testFetchAccessionDetailsWhenJsonException() throws Exception {
        //ARRANGE
        DomainEnum domain = DomainEnum.VIRAL_SEQUENCES;
        DataTypeEnum dataType = DataTypeEnum.RAW_READS;
        ProtocolEnum protocol = ProtocolEnum.FTP;
        when(ebiSearchService.getCounts(domain, dataType)).thenThrow(new JSONException("Json Exeption " +
                "encountered"));
        //ACT AND ASSERT
        Assertions.assertThrows(JSONException.class, () -> accessionDetailsService.fetchAccessionAndDownload(domain,
                dataType, DownloadFormatEnum.FASTQ, "", null, null, protocol, null));
    }

    @Disabled
    @Test
    public void testFetchAccessionDetailsWhenEmptyFtpUrl() throws Exception {
        //ARRANGE
        DomainEnum domain = DomainEnum.VIRAL_SEQUENCES;
        DataTypeEnum dataType = DataTypeEnum.RAW_READS;
        DownloadFormatEnum format = DownloadFormatEnum.FASTQ;
        ProtocolEnum protocol = ProtocolEnum.FTP;
        when(ebiSearchService.getCounts(domain, dataType)).thenReturn(5000);
        when(ebiSearchService.getAccessionIds(domain, dataType, 5000)).thenReturn(getAcccessionIds());
        when(enaPortalService.getPortalResponses(Mockito.anyList(), Mockito.any(DataTypeEnum.class),
                Mockito.any(DownloadFormatEnum.class), Mockito.any(ProtocolEnum.class))).thenReturn(getPortalResponsesWithNoFileDetails());
        final Future<FileDownloadStatus> mockedFuture = Mockito.mock(Future.class);
        when(mockedFuture.get()).thenReturn(new FileDownloadStatus(0, 0, new ArrayList<>()));

        when(fileDownloaderService.startDownload(Mockito.any(ExecutorService.class), Mockito.any(List.class),
                Mockito.any(String.class), Mockito.any(DomainEnum.class), Mockito.any(DataTypeEnum.class),
                Mockito.any(DownloadFormatEnum.class), Mockito.anyInt())).thenReturn(mockedFuture);


        //ACT
        accessionDetailsService.fetchAccessionAndDownload(domain, dataType, format, "C:\\Users", null, null
                , protocol, null);
        //ASSERT
        verify(fileDownloaderService, times(1)).startDownload(Mockito.any(ExecutorService.class),
                Mockito.any(List.class), Mockito.any(String.class), Mockito.any(DomainEnum.class),
                Mockito.any(DataTypeEnum.class), Mockito.any(DownloadFormatEnum.class), eq(1));
    }


    private List<List<String>> getAcccessionIds() {
        List<List<String>> accessionIdList = new ArrayList<>();
        List<String> accId1 = Arrays.asList("ERX4231114", "ERX4231115");
        accessionIdList.add(accId1);
        return accessionIdList;
    }

    private List<EnaPortalResponse> getPortalResponses() {
        List<EnaPortalResponse> portalResponses = new ArrayList<>();
        EnaPortalResponse enaPortalResponse1 = new EnaPortalResponse();
        enaPortalResponse1.setParentId("ERX4231114");
        enaPortalResponse1.setRunId("ERR4276487");
        enaPortalResponse1.setBytes("284982107;89194660");
        enaPortalResponse1.setFtpUrl("ftp.sra.ebi.ac.uk/vol1/fastq/ERR427/007/ERR4276487/ERR4276487_1.fastq.gz;ftp" +
                ".sra.ebi.ac.uk/vol1/fastq/ERR427/007/ERR4276487/ERR4276487_2.fastq.gz");
        enaPortalResponse1.setMd5("5a718b406f4f76984e2bf229b5c822d8;10029431c51c5fefb0759ac5639b1595");
        portalResponses.add(enaPortalResponse1);
        EnaPortalResponse enaPortalResponse2 = new EnaPortalResponse();
        enaPortalResponse2.setParentId("ERX4231115");
        enaPortalResponse2.setRunId("ERR4276488");
        enaPortalResponse2.setMd5("5a718b406f4f76984e2bf229b5c822d9;10029431c51c5fefb0759ac5639b1596");
        portalResponses.add(enaPortalResponse2);
        return portalResponses;
    }

    private List<EnaPortalResponse> getPortalResponsesWithNoFileDetails() {
        List<EnaPortalResponse> portalResponses = new ArrayList<>();
        EnaPortalResponse enaPortalResponse1 = new EnaPortalResponse();
        enaPortalResponse1.setParentId("ERX4231114");
        enaPortalResponse1.setRunId("ERR4276487");
        enaPortalResponse1.setBytes("284982107;89194660");
        enaPortalResponse1.setFtpUrl(null);
        enaPortalResponse1.setMd5("5a718b406f4f76984e2bf229b5c822d8;10029431c51c5fefb0759ac5639b1595");
        portalResponses.add(enaPortalResponse1);
        EnaPortalResponse enaPortalResponse2 = new EnaPortalResponse();
        enaPortalResponse2.setParentId("ERX4231115");
        enaPortalResponse2.setRunId("ERR4276488");
        enaPortalResponse2.setMd5("5a718b406f4f76984e2bf229b5c822d9;10029431c51c5fefb0759ac5639b1596");
        portalResponses.add(enaPortalResponse2);
        return portalResponses;
    }


}
