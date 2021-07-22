package uk.ac.ebi.ena.cv19fd.backend.service;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DataTypeEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DomainEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.cv19fd.backend.dto.FileDetail;
import uk.ac.ebi.ena.cv19fd.backend.enums.FileDownloadStatus;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class FileDownloaderServiceTest {

    private final static String downloadFolderPath = "downloads/";

    final Logger logger = LoggerFactory.getLogger(getClass());

    @InjectMocks
    FileDownloaderService fileDownloaderService;

    @Mock
    EnaBrowserService enaBrowserService;

    @Mock
    EmailService emailService;

    @BeforeEach
    public void cleanup() {
        try {
            Thread.sleep(3000);
            FileSystemUtils.deleteRecursively(Paths.get(downloadFolderPath));
        } catch (IOException | InterruptedException e) {
            logger.error("Unable to delete directory:{}", downloadFolderPath);
            e.printStackTrace();

        }
    }

    @Test
    public void testStartDownload() throws ExecutionException, InterruptedException {
        //ARRANGE
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        List<FileDetail> fileDetailList = new ArrayList<>();
        FileDetail fileDetail = createFileDetail();
        fileDetailList.add(fileDetail);
        DomainEnum domain = DomainEnum.VIRAL_SEQUENCES;
        DataTypeEnum dataType = DataTypeEnum.STUDIES;
        DownloadFormatEnum format = DownloadFormatEnum.FASTQ;
        File file = new File(downloadFolderPath);
        String downloadLoc = file.getPath();
        //ACT
        FileDownloadStatus fileDownloadStatusFuture =
                fileDownloaderService.startDownload(executorService, fileDetailList, downloadLoc, domain, dataType,
                        format, 1).get();
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        //ACT
        Assertions.assertEquals(1, fileDownloadStatusFuture.getSuccesssful());
        Assertions.assertEquals(0, fileDownloadStatusFuture.getFailedFiles().size());
    }

    /**
     * provide local aspera path and download location
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Disabled
    @Test
    public void testStartDownloadAspera() throws ExecutionException, InterruptedException {
        //ARRANGE
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        List<FileDetail> fileDetailList = new ArrayList<>();
        FileDetail fileDetail = createFileDetailAspera();
        fileDetailList.add(fileDetail);
        DomainEnum domain = DomainEnum.VIRAL_SEQUENCES;
        DataTypeEnum dataType = DataTypeEnum.STUDIES;
        DownloadFormatEnum format = DownloadFormatEnum.FASTQ;
        File file = new File("C:/data-files"); // provide local download location
        String asperaLocation = "C:/devtools/aspera-cli"; // provide local aspera location
        String downloadLoc = file.getPath();
        //ACT
        FileDownloadStatus fileDownloadStatusFuture =
                fileDownloaderService.startDownloadAspera(executorService, fileDetailList, asperaLocation, downloadLoc, domain, dataType,
                        format, 1).get();
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        //ACT
        Assertions.assertEquals(1, fileDownloadStatusFuture.getSuccesssful());
        Assertions.assertEquals(0, fileDownloadStatusFuture.getFailedFiles().size());
    }

    private FileDetail createFileDetail() {
        return new FileDetail("ERX4196985", "ERR4238191", "ftp.sra.ebi.ac.uk/vol1/fastq/ERR423/001/ERR4238191" +
                "/ERR4238191.fastq.gz", 10890775L, "38a88f0206e7cbf7531af137ea5b08e3");
    }

    private FileDetail createFileDetailAspera() {
        return new FileDetail("ERX4196985", "ERR4238191", "fasp.sra.ebi.ac.uk:/vol1/fastq/ERR423/001/ERR4238191" +
                "/ERR4238191.fastq.gz", 10890775L, "38a88f0206e7cbf7531af137ea5b08e3");
    }

    @Test
    public void testStartDownload_For_XML_EMBL_FASTA() throws IOException {
        //ARRANGE
        DomainEnum domain = DomainEnum.VIRAL_SEQUENCES;
        DataTypeEnum dataType = DataTypeEnum.STUDIES;
        DownloadFormatEnum format = DownloadFormatEnum.XML;
        File file = new File(downloadFolderPath);
        String downloadLoc = file.getPath();
        String emailId = "datasubs@ebi.ac.uk";
        Mockito.doNothing().when(emailService).sendEmailForOtherFormats(Mockito.anyString(), Mockito.any(DomainEnum.class),
                Mockito.any(DataTypeEnum.class), Mockito.any(DownloadFormatEnum.class), Mockito.anyString(),
                Mockito.anyBoolean(), Mockito.any(List.class));
        Mockito.when(enaBrowserService.getInputStreamForDownloadedFile(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).
                thenReturn(new ByteArrayInputStream("test File".getBytes()));
        //ACT
        fileDownloaderService.startDownload(downloadLoc, domain, dataType, format, emailId, new ArrayList<>());
    }

    @Test
    public void testStartDownload_for_XML_EMBL_FASTA_with_accessionList() throws IOException {
        //ARRANGE
        DomainEnum domain = DomainEnum.VIRAL_SEQUENCES;
        DataTypeEnum dataType = DataTypeEnum.STUDIES;
        DownloadFormatEnum format = DownloadFormatEnum.XML;
        File file = new File(downloadFolderPath);
        String downloadLoc = file.getPath();
        String emailId = "datasubs@ebi.ac.uk";
        List<String> accessions = Arrays.asList("MN908947", "LR991698");
        Mockito.doNothing().when(emailService).sendEmailForOtherFormats(Mockito.anyString(), Mockito.any(DomainEnum.class),
                Mockito.any(DataTypeEnum.class), Mockito.any(DownloadFormatEnum.class), Mockito.anyString(),
                Mockito.anyBoolean(), Mockito.any(List.class));
        Mockito.when(enaBrowserService.getInputStreamForDownloadedFile(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).
                thenReturn(new ByteArrayInputStream("test File".getBytes()));
        //ACT
        fileDownloaderService.startDownload(downloadLoc, domain, dataType, format, emailId, accessions);
    }
}
