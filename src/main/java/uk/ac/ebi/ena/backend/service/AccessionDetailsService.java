/*
 * ******************************************************************************
 *  * Copyright 2021 EMBL-EBI, Hinxton outstation
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *****************************************************************************
 */

package uk.ac.ebi.ena.backend.service;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.ena.app.constants.Constants;
import uk.ac.ebi.ena.app.menu.enums.AccessionTypeEnum;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.backend.dto.DownloadJob;
import uk.ac.ebi.ena.backend.dto.EnaPortalResponse;
import uk.ac.ebi.ena.backend.dto.FileDetail;
import uk.ac.ebi.ena.backend.enums.FileDownloadStatus;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static uk.ac.ebi.ena.app.constants.Constants.CHUNK_SIZE;
import static uk.ac.ebi.ena.app.utils.CommonUtils.getProgressBarBuilder;

/**
 * This class is responsible for getting the Experiment details and populating the respective dto classes
 */
@Service
@Slf4j
@AllArgsConstructor
public class AccessionDetailsService {

    final Logger console = LoggerFactory.getLogger("console");

    private final EnaPortalService enaPortalService;
    private final FileDownloaderService fileDownloaderService;
    private final FileDownloaderClient fileDownloaderClient;

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
                fileDetails.add(new FileDetail(enaPortalResponse.getParentId(), enaPortalResponse.getRecordId(),
                        ftpUrlsList.get(i), bytesList.get(i), md5List.get(i), false, 0));
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
    public List<List<FileDetail>> fetchFileDetails(DownloadFormatEnum format, DownloadJob downloadJob, ProtocolEnum protocol,
                                                   String userName, String password) {

        AccessionTypeEnum accessionType = AccessionTypeEnum.getAccessionType(downloadJob.getAccessionField());
        List<List<String>> accLists = Collections.synchronizedList(Lists.partition(downloadJob.getAccessionList(), 10000));
        long total = 0;
        long totalFiles = 0;
        for (List<String> accs : accLists) {
            total += accs.size();
        }
        console.info("Total {} {} {} records found", total, downloadJob.getAccessionField(), format);

        final ProgressBarBuilder portalPB = getProgressBarBuilder("Getting file details from ENA Portal API", -1);

        List<List<FileDetail>> listList = new ArrayList<>();
        for (List<String> accList : ProgressBar.wrap(accLists, portalPB)) {
            final List<List<String>> partitions = Lists.partition(accList,
                    accList.size() > CHUNK_SIZE * 5 ? CHUNK_SIZE : (int) Math.ceil(new Double(accList.size()) / 5));
            for (List<String> partition : partitions) {
                final List<EnaPortalResponse> portalResponses = enaPortalService.getPortalResponses(partition, format,
                        protocol, downloadJob, userName, password);
                final List<FileDetail> fileDetails = createFileDetails(portalResponses);
                totalFiles += fileDetails.size();
                listList.add(fileDetails);
            }
        }
        if (totalFiles == 0) {
            System.out.println("No records found for the accessions submitted under type=" + accessionType + " format=" + format);
        }

        // Compare the list of accessions provided by user and acessions returns from portal api

        if (StringUtils.isNotEmpty(userName)) {
            Set<String> missingAccessions = getMissingAccessions(downloadJob, listList);
            if (missingAccessions.size() > 0) {
                console.info("Below accessions not available in " + userName + " data hub \n"
                        + missingAccessions.stream().collect(Collectors.joining(",")));
            }

        }

        if (totalFiles > 0) {
            console.info("Downloading {} files in total", totalFiles);
        }
        return listList;
    }

    private Set<String> getMissingAccessions(DownloadJob downloadJob, List<List<FileDetail>> list) {

        Set<String> userAccessions = new HashSet<>();
        for (String acc : downloadJob.getAccessionList()) {
            userAccessions.add(acc);
        }

        for (List<FileDetail> fileDetails : list) {
            for (FileDetail fileDetail : fileDetails) {
                if (Objects.isNull(fileDetail.getParentId())) {
                    userAccessions.remove(fileDetail.getRecordId());
                } else {
                    userAccessions.remove(fileDetail.getParentId());
                }
                if (userAccessions.isEmpty()) {
                    break;
                }

            }
        }
        return userAccessions;

    }

    @SneakyThrows
    public long doDownload(DownloadFormatEnum format, String downloadLocation, DownloadJob downloadJob,
                           List<List<FileDetail>> partitions, ProtocolEnum protocol,
                           String asperaLocation,
                           String userName, String password) {
        final ExecutorService executorService = Executors.newFixedThreadPool(Constants.EXECUTOR_THREAD_COUNT);
        AccessionTypeEnum accessionType = AccessionTypeEnum.getAccessionType(downloadJob.getAccessionField());

        List<Future<FileDownloadStatus>> futures = new ArrayList<>();

        for (int thisSet = 0; thisSet < partitions.size(); thisSet++) {
            List<FileDetail> fileDetails = partitions.get(thisSet);
            if (fileDetails.size() == 0) {
                continue;
            }
                if (protocol == ProtocolEnum.FTP) {
                    final Future<FileDownloadStatus> listFuture =
                            fileDownloaderService.startDownload(executorService, fileDetails,
                                    downloadLocation, accessionType, format, thisSet, userName, password);
                    futures.add(listFuture);
                } else if (protocol == ProtocolEnum.ASPERA) {
                    final Future<FileDownloadStatus> listFuture =
                            fileDownloaderClient.startDownloadAspera(executorService, fileDetails,
                                    asperaLocation, downloadLocation, accessionType, format, thisSet, userName, password);
                    futures.add(listFuture);
                }
            }

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


        return failedDownloadsCount;
    }


}
