package uk.ac.ebi.ena.backend;

import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.backend.dto.FileDetail;
import uk.ac.ebi.ena.backend.enums.FileDownloadStatus;
import uk.ac.ebi.ena.backend.service.FileDownloaderClient;
import uk.ac.ebi.ena.backend.service.FileDownloaderService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class FileDownloaderServiceTest {

    private final static String downloadFolderPath = "downloads/";

    @InjectMocks
    FileDownloaderService fileDownloaderService;

    @InjectMocks
    FileDownloaderClient fileDownloaderClient;

    @Disabled
    @Test
    public void testStartDownload_UsingFtp() throws ExecutionException, InterruptedException {
        //ARRANGE
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        List<FileDetail> fileDetailList = new ArrayList<>();
        FileDetail fileDetail = createFileDetailFtp();
        fileDetailList.add(fileDetail);
        String accessionType = "EXPERIMENT";
        DownloadFormatEnum format = DownloadFormatEnum.READS_FASTQ;
        int set = 1;
        //ACT
        FileDownloadStatus fileDownloadStatus = fileDownloaderService.
                startDownload(executorService, fileDetailList, downloadFolderPath, accessionType, format, set).get();
        System.out.println(fileDownloadStatus);

    }

    @Disabled
    @Test
    public void testStartDownload_UsingAspera() throws ExecutionException, InterruptedException {
        //ARRANGE
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        List<FileDetail> fileDetailList = new ArrayList<>();
        FileDetail fileDetail = createFileDetailAspera();
        fileDetailList.add(fileDetail);
        String asperaLocation = "C:\\Users\\suman\\AppData\\Local\\Programs\\Aspera\\Aspera Connect\\";//local aspera connect folder
        String accessionType = "EXPERIMENT";
        DownloadFormatEnum format = DownloadFormatEnum.READS_FASTQ;
        int set = 1;
        //ACT
        FileDownloadStatus fileDownloadStatus = fileDownloaderClient.startDownloadAspera
                (executorService, fileDetailList, asperaLocation, downloadFolderPath, accessionType, format, set).get();
        Assert.assertEquals(1, fileDownloadStatus.getSuccesssful());

    }

    private FileDetail createFileDetailFtp() {
        return new FileDetail("SRX7264284", "SRR10583966", "ftp.sra.ebi.ac.uk/vol1/fastq/SRR105/066/SRR10583966/SRR10583966_1.fastq.gz",
                6424881L, "59383f5b12bbebc361eadd5ccc1ddaca");
    }

    private FileDetail createFileDetailAspera() {
        return new FileDetail("SRX7264284", "SRR10583966", "fasp.sra.ebi.ac.uk:/vol1/fastq/SRR105/066/SRR10583966/SRR10583966_1.fastq.gz",
                6424881L, "59383f5b12bbebc361eadd5ccc1ddaca");
    }
}
