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
import uk.ac.ebi.ena.backend.dto.FileDetail;
import uk.ac.ebi.ena.backend.enums.FileDownloadStatus;
import uk.ac.ebi.ena.backend.service.AccessionDetailsService;
import uk.ac.ebi.ena.backend.service.FileDownloaderService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class AccessionDetailsServiceTest {

    @Mock
    FileDownloaderService fileDownloaderService;

    @InjectMocks
    AccessionDetailsService accessionDetailsService;


    String path = System.getProperty("user.home");

    String accessionList = "SRX6415696,SRX2000905,SRX6415695";

    @Test
    public void testFetchAccessionAndDownloadWhenSuccess() throws ExecutionException, InterruptedException {
        //ARRANGE
        DownloadFormatEnum format = DownloadFormatEnum.READS_FASTQ;
        String downloadLocation = path;
        DownloadJob accessionDetailsMap = CommonUtils.processAccessions(Arrays.asList(accessionList.split(",")));
        ProtocolEnum protocol = ProtocolEnum.FTP;
        String asperaLocation = null;

        List<FileDetail> fileDetailList = createFileDetailFtp();

        final Future<FileDownloadStatus> mockedFuture = Mockito.mock(Future.class);
        when(mockedFuture.get()).thenReturn(new FileDownloadStatus(0, 0, new ArrayList<>()));
        Mockito.when(fileDownloaderService.startDownload(Mockito.any(ExecutorService.class), Mockito.any(List.class),
                Mockito.any(String.class), Mockito.any(AccessionTypeEnum.class), Mockito.any(DownloadFormatEnum.class),
                Mockito.anyInt(), null)).thenReturn(mockedFuture);
        //ACT
        accessionDetailsService.doDownload(format, downloadLocation, accessionDetailsMap, Collections.singletonList(fileDetailList), protocol, asperaLocation, null);
        //ASSERT
        verify(fileDownloaderService, times(1)).startDownload(Mockito.any(ExecutorService.class), Mockito.anyList(), Mockito.any(String.class),
                Mockito.any(AccessionTypeEnum.class), Mockito.any(DownloadFormatEnum.class), Mockito.anyInt(), null);
    }

    private List<FileDetail> createFileDetailFtp() {
        List<FileDetail> fileDetailList = new ArrayList<>();
        FileDetail fileDetail1 = new FileDetail("SRX2000905", "SRR4000583", "ftp.sra.ebi.ac.uk/vol1/fastq/SRR400/003/SRR4000583/SRR4000583.fastq.gz",
                1174738707L, "a991ce890047ffca760c6de2617b5fec", true, 0);
        FileDetail fileDetail2 = new FileDetail("SRX6415696", "SRR9654360", "ftp.sra.ebi.ac.uk/vol1/fastq/SRR965/000/SRR9654360/SRR9654360.fastq.gz",
                14139836L, "f3611f35a977b8b82a7adcf0a28c397d", true, 0);
        FileDetail fileDetail3 = new FileDetail("SRX6415695", "SRR9654361", "ftp.sra.ebi.ac.uk/vol1/fastq/SRR965/001/SRR9654361/SRR9654361.fastq.gz",
                15541843L, "1236b79cd93a63289841765aabacb880", true, 0);
        fileDetailList.add(fileDetail1);
        fileDetailList.add(fileDetail2);
        fileDetailList.add(fileDetail3);

        return fileDetailList;
    }
}
