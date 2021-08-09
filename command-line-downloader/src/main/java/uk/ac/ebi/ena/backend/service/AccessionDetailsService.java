package uk.ac.ebi.ena.backend.service;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.ac.ebi.ena.app.constants.Constants;
import uk.ac.ebi.ena.app.menu.enums.AccessionTypeEnum;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.app.utils.FileUtils;
import uk.ac.ebi.ena.backend.dto.EnaPortalResponse;
import uk.ac.ebi.ena.backend.dto.FileDetail;
import uk.ac.ebi.ena.backend.enums.FileDownloadStatus;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static uk.ac.ebi.ena.app.constants.Constants.*;
import static uk.ac.ebi.ena.app.utils.CommonUtils.getProgressBarBuilder;

/**
 * This class is responsible for getting the Experiment details and populating the respective dto classes
 */
@Service
@Slf4j
@AllArgsConstructor
public class AccessionDetailsService {
    private final EnaPortalService enaPortalService;
    private final FileDownloaderService fileDownloaderService;
    private final FileDownloaderClient fileDownloaderClient;
    private final EmailService emailService;

    /**
     * @param enaPortalResponses The responses from Portal API
     * @return List of FileDetails from EnaPortalResponse
     */
    private List<FileDetail> createFileDetails(List<EnaPortalResponse> enaPortalResponses) {
        List<FileDetail> fileDetails = new ArrayList<>();
        for (EnaPortalResponse enaPortalResponse : enaPortalResponses) {
            List<String> ftpUrlsList = getMd5OrUrl(enaPortalResponse.getUrl());
            List<String> md5List = getMd5OrUrl(enaPortalResponse.getMd5());
            List<Long> bytesList = getBytes(enaPortalResponse.getBytes());

            for (int i = 0; i < ftpUrlsList.size(); i++) {
                fileDetails.add(new FileDetail(enaPortalResponse.getParentId(), enaPortalResponse.getRunId(),
                        ftpUrlsList.get(i), bytesList.get(i), md5List.get(i)));
            }
        }
        return fileDetails;
    }

    /**
     * This API will convert the String delimited by ; to a Long list
     *
     * @param bytes as String
     * @return the bytes as Long list
     */
    private List<Long> getBytes(String bytes) {
        if (StringUtils.isNotEmpty(bytes)) {
            return Arrays.stream(bytes.split(Constants.SEMICOLON)).map(Long::valueOf).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * This API parses the string delimited by ; to a String list
     *
     * @param details as String
     * @return the String List
     */
    private List<String> getMd5OrUrl(String details) {
        if (StringUtils.isNotEmpty(details)) {
            return Arrays.stream(details.split(Constants.SEMICOLON)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @SneakyThrows
    public void fetchAccessionAndDownload(DownloadFormatEnum format, String downloadLocation,
                                          Map<String, List<String>> accessionDetailsMap, ProtocolEnum protocol,
                                          String asperaLocation, String recipientEmailId) {
        final ExecutorService executorService = Executors.newFixedThreadPool(Constants.EXECUTOR_THREAD_COUNT);

        String accessionField = accessionDetailsMap.get(ACCESSION_FIELD).get(0);
        String accessionType = AccessionTypeEnum.getAccessionType(accessionField).name();
        List<String> accessions = accessionDetailsMap.get(ACCESSION_LIST);
        List<List<String>> accLists = Collections.synchronizedList(Lists.partition(accessions, 10000));
        long total = 0;
        long totalFiles = 0;
        List<Future<FileDownloadStatus>> futures = new ArrayList<>();
        for (List<String> accs : accLists) {
            total += accs.size();
        }
        log.info("Total {} {} {} records found", total, accessionField, format);


        final ProgressBarBuilder portalPB = getProgressBarBuilder("Getting file details from ENA Portal API", -1);

        int set = 1;
        for (List<String> accList : ProgressBar.wrap(accLists, portalPB)) {
            final List<List<String>> partitions = Lists.partition(accList,
                    accList.size() > CHUNK_SIZE * 5 ? CHUNK_SIZE : (int) Math.ceil(new Double(accList.size()) / 5));
            for (List<String> partition : partitions) {
                final List<EnaPortalResponse> portalResponses = enaPortalService.getPortalResponses(partition, format,
                        protocol, accessionDetailsMap);
                final List<FileDetail> fileDetails = createFileDetails(portalResponses);
                if (fileDetails.size() == 0) {
                    continue;
                }
                totalFiles += fileDetails.size();
                int thisSet = set++;
                if (protocol == ProtocolEnum.FTP) {
                    final Future<FileDownloadStatus> listFuture =
                            fileDownloaderService.startDownload(executorService, fileDetails,
                                    downloadLocation, accessionType, format, thisSet);
                    futures.add(listFuture);
                } else if (protocol == ProtocolEnum.ASPERA) {
                    final Future<FileDownloadStatus> listFuture =
                            fileDownloaderClient.startDownloadAspera(executorService, fileDetails,
                                    asperaLocation, downloadLocation, accessionType, format, thisSet);
                    futures.add(listFuture);
                }
            }
        }
        if (totalFiles == 0) {
            System.out.println("No records found for the accessions submitted under type=" + accessionType + " format=" + format);
        }

        log.info("Downloading {} files in total", totalFiles);
        System.out.println("Downloading " + totalFiles + " files in total.");
        long successfulDownloadsCount = 0, failedDownloadsCount = 0;
        for (Future<FileDownloadStatus> f : futures) {
            final FileDownloadStatus fileDownloadStatus = f.get();
            successfulDownloadsCount += fileDownloadStatus.getSuccesssful();
            failedDownloadsCount += fileDownloadStatus.getFailedFiles().size();
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            log.error("Handling Interrupted exception received during await termination");
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("Shutdown complete");

        log.info("Number of files:{} successfully downloaded for accessionField:{}, format:{}",
                successfulDownloadsCount, accessionField, format);
        log.info("Number of files:{} failed downloaded for accessionField:{}, format:{}",
                failedDownloadsCount, accessionField, format);
        String scriptFileName = FileUtils.getScriptPath(accessionDetailsMap, format);
        System.out.println("Downloads completed!");
        if (failedDownloadsCount > 0) {
            System.out.println("Some files failed to download due to possible network issues. Please re-run the " +
                    "same script=" + scriptFileName + " to re-attempt to download those files");
        }
        emailService.sendEmailForFastqSubmitted(recipientEmailId, successfulDownloadsCount, failedDownloadsCount,
                scriptFileName, accessionField, format, downloadLocation);
    }

}
