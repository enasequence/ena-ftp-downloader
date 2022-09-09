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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.menu.enums.ProtocolEnum;
import uk.ac.ebi.ena.app.utils.FileUtils;
import uk.ac.ebi.ena.backend.dto.AuthenticationDetail;
import uk.ac.ebi.ena.backend.dto.DownloadJob;
import uk.ac.ebi.ena.backend.dto.FileDetail;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static uk.ac.ebi.ena.app.constants.Constants.TOTAL_RETRIES;

@Component
@Slf4j
@AllArgsConstructor
public class BackendServiceImpl implements BackendService {

    final Logger console = LoggerFactory.getLogger("console");

    private final EmailService emailService;
    private final AccessionDetailsService accessionDetailsService;


    @Override
    public void startDownload(DownloadFormatEnum format, String location, DownloadJob downloadJob,
                              ProtocolEnum protocol, String asperaConnectLocation, String emailId,
                              AuthenticationDetail authenticationDetail) {

        console.info("Starting download for format:{} at download location:{},protocol:{}, asperaLoc:{}, emailId:{}, data hub:{}",
                format, location, protocol, asperaConnectLocation, emailId,
                Objects.nonNull(authenticationDetail) ? authenticationDetail.getUserName() : null);
        List<List<FileDetail>> fileDetailsList = accessionDetailsService.fetchFileDetails(format, downloadJob, protocol, authenticationDetail);
        final List<FileDetail> finallyFailedFiles = new ArrayList<>();
        AtomicLong count = new AtomicLong();
        fileDetailsList.forEach(fileDetails -> fileDetails.forEach(fileDetail -> count.getAndIncrement()));

        do {
            accessionDetailsService.doDownload(format, location, downloadJob, fileDetailsList, protocol,
                    asperaConnectLocation, authenticationDetail);
            List<List<FileDetail>> failedFileList = new ArrayList<>();
            final List<FileDetail> failedFiles = new ArrayList<>();
            fileDetailsList.forEach(fileDetails -> {
                for (FileDetail fileDetail : fileDetails) {
                    if (!fileDetail.isSuccess() && fileDetail.getRetryCount() < TOTAL_RETRIES) {
                        failedFiles.add(fileDetail);
                        fileDetail.incrementRetryCount();
                    } else if (fileDetail.getRetryCount() >= TOTAL_RETRIES) {
                        finallyFailedFiles.add(fileDetail);
                    }
                }
                if (failedFiles.size() > 0) {
                    failedFileList.add(failedFiles);
                }
            });

            AtomicLong newCount = new AtomicLong();
            failedFileList.forEach(fileDetails -> fileDetails.forEach(fileDetail -> newCount.getAndIncrement()));

            if (failedFileList.size() > 0) {
                log.warn("Number of files:{} successfully downloaded for accessionField:{}, format:{}",
                        count.get() - newCount.get(), downloadJob.getAccessionField(), format);
                log.warn("Number of files:{} failed downloaded for accessionField:{}, format:{}",
                        newCount.get(), downloadJob.getAccessionField(), format);
                if (newCount.get() > 0) {
                    System.out.println("Some files failed to download due to possible network issues. Please re-run the " +
                            "same script=" + FileUtils.getScriptPath(downloadJob, format) + " to re-attempt to download those files");
                }
                System.out.println("Automatically retrying failed downloads...");
            }
            fileDetailsList = failedFileList;

        } while (fileDetailsList.size() > 0);


        console.info("{} files successfully downloaded for accessionField:{}, format:{} to {}",
                count.get(), downloadJob.getAccessionField(), format, location);

        if (finallyFailedFiles.size() > 0) {
            console.info("{} files failed to be downloaded for accessionField:{}, format:{}",
                    finallyFailedFiles.size(), downloadJob.getAccessionField(), format);
            for (FileDetail fileDetail : finallyFailedFiles) {
                console.info("{}", fileDetail.getFtpUrl());
            }
        }

        emailService.sendEmailForFastqSubmitted(emailId, count.get(), 0,
                FileUtils.getScriptPath(downloadJob, format), downloadJob.getAccessionField(), format, location);

    }


}
