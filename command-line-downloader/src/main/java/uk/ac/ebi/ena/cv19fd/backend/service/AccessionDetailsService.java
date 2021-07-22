package uk.ac.ebi.ena.cv19fd.backend.service;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.ac.ebi.ena.cv19fd.app.constants.Constants;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DataTypeEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DomainEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.cv19fd.app.utils.FileUtils;
import uk.ac.ebi.ena.cv19fd.backend.dto.EnaPortalResponse;
import uk.ac.ebi.ena.cv19fd.backend.dto.FileDetail;
import uk.ac.ebi.ena.cv19fd.backend.enums.FileDownloadStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static uk.ac.ebi.ena.cv19fd.app.constants.Constants.CHUNK_SIZE;
import static uk.ac.ebi.ena.cv19fd.app.utils.CommonUtils.getProgressBarBuilder;

/**
 * This class is responsible for getting the Experiment details and populating the respective dto classes
 */
@Service
@Slf4j
@AllArgsConstructor
public class AccessionDetailsService {
    private final EnaPortalService enaPortalService;
    private final EbiSearchService ebiSearchService;
    private final FileDownloaderService fileDownloaderService;
    private final EmailService emailService;

    /**
     * Fetches the experiment details for a given domain,dataType,format and starts the download by invoking
     * {@link FileDownloaderService}
     *
     * @param domain           The domain provided by the user
     * @param dataType         The dataType provided by the user
     * @param format           The format provided by the user
     * @param downloadLocation The download location provided by the user
     * @param recipientEmailId The email ID at which the user wants to receive alerts
     */
    @SneakyThrows
    public void fetchAccessionAndDownload(DomainEnum domain, DataTypeEnum dataType, DownloadFormatEnum format,
                                          String downloadLocation, String recipientEmailId, List<String> accessions,
                                          ProtocolEnum protocol, String asperaLocation) {
        if (format == DownloadFormatEnum.FASTQ || format == DownloadFormatEnum.SUBMITTED) {

            final ExecutorService executorService = Executors.newFixedThreadPool(Constants.EXECUTOR_THREAD_COUNT);

            int totalCount = CollectionUtils.isEmpty(accessions) ? getTotalCount(domain, dataType) : accessions.size();
            final AtomicInteger counter = new AtomicInteger();
            List<List<String>> accLists = CollectionUtils.isEmpty(accessions) ?
                    ebiSearchService.getAccessionIds(domain, dataType, totalCount) :
                    Collections.synchronizedList(Lists.partition(accessions, 10000));
            long total = 0;
            long totalFiles = 0;
            List<Future<FileDownloadStatus>> futures = new ArrayList<>();
            for (List<String> accs : accLists) {
                total += accs.size();
            }
            log.info("Total {} {} records found", total, dataType);


            final ProgressBarBuilder portalPB = getProgressBarBuilder("Getting file details from ENA Portal API",
                    -1);

            int set = 1;
            for (List<String> accList : ProgressBar.wrap(accLists, portalPB)) {
                final List<List<String>> partitions = Lists.partition(accList,
                        accList.size() > CHUNK_SIZE * 5 ? CHUNK_SIZE: (int) Math.ceil(new Double(accList.size()) / 5));
                for (List<String> partition : partitions) {
                    final List<EnaPortalResponse> portalResponses = enaPortalService.getPortalResponses(partition,
                            dataType, format, protocol);
                    final List<FileDetail> fileDetails = createFileDetails(portalResponses);
                    if (fileDetails.size() == 0) {
                        continue;
                    }
                    totalFiles += fileDetails.size();
                    int thisSet = set++;
                    if (protocol == ProtocolEnum.FTP) {
                        final Future<FileDownloadStatus> listFuture =
                                fileDownloaderService.startDownload(executorService, fileDetails,
                                        downloadLocation, domain, dataType, format, thisSet);
                        futures.add(listFuture);
                    } else if (protocol == ProtocolEnum.ASPERA) {
                        final Future<FileDownloadStatus> listFuture =
                                fileDownloaderService.startDownloadAspera(executorService, fileDetails,
                                        asperaLocation, downloadLocation, domain, dataType, format, thisSet);
                        futures.add(listFuture);
                    }
                }
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

            log.info("Number of files:{} successfully downloaded for domain:{}, dataType:{}, format:{}",
                    successfulDownloadsCount, domain, dataType, format);
            log.info("Number of files:{} failed downloaded for domain:{}, dataType:{}, format:{}",
                    failedDownloadsCount, domain, dataType, format);
            String scriptFileName = FileUtils.getScriptPath(domain, dataType, format, accessions);
            if (failedDownloadsCount > 0) {
                System.out.println("Some files failed to download due to possible network issues. Please re-run the " +
                        "same script=" + scriptFileName + " to re-attempt to download those files");
            }
            emailService.sendEmailForFastqSubmitted(recipientEmailId, successfulDownloadsCount, failedDownloadsCount,
                    scriptFileName, dataType, format, downloadLocation);
        }
    }

    /**
     * @param enaPortalResponses The responses from Portal API
     * @return List of FileDetails from EnaPortalResponse
     */
    private List<FileDetail> createFileDetails(List<EnaPortalResponse> enaPortalResponses) {
        List<FileDetail> fileDetails = new ArrayList<>();
        for (EnaPortalResponse enaPortalResponse : enaPortalResponses) {
            List<String> ftpUrlsList = getMd5OrFtpUrl(enaPortalResponse.getFtpUrl());
            List<String> md5List = getMd5OrFtpUrl(enaPortalResponse.getMd5());
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
    private List<String> getMd5OrFtpUrl(String details) {
        if (StringUtils.isNotEmpty(details)) {
            return Arrays.stream(details.split(Constants.SEMICOLON)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * This API will return the total count of experiments for a domain and dataType
     *
     * @param domain   The domain provided by the user
     * @param dataType The dataType provided by the user
     * @return the total count of experiments for a domain and dataType
     * @throws JSONException if the total count is not a number
     */
    private int getTotalCount(DomainEnum domain, DataTypeEnum dataType) throws JSONException {
        return ebiSearchService.getCounts(domain, dataType);
    }

}
