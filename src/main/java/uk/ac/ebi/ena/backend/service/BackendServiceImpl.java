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
import uk.ac.ebi.ena.backend.dto.DownloadJob;
import uk.ac.ebi.ena.backend.dto.FileDetail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
@Slf4j
@AllArgsConstructor
public class BackendServiceImpl implements BackendService {

    final Logger console = LoggerFactory.getLogger("console");
    
    private final EmailService emailService;
    private final AccessionDetailsService accessionDetailsService;

    @Override
    public void startDownload(DownloadFormatEnum format, String location, DownloadJob downloadJob,
                              ProtocolEnum protocol, String asperaConnectLocation, String emailId) {

        console.info("Starting download for format:{} at download location:{},protocol:{}, asperaLoc:{}, emailId:{}",
                format, location, protocol, asperaConnectLocation, emailId);
        long failedCount = 0;
        List<List<FileDetail>> listList = accessionDetailsService.fetchFileDetails(format, downloadJob, protocol);
        AtomicLong count = new AtomicLong();
        listList.forEach(fileDetails -> fileDetails.forEach(fileDetail -> count.getAndIncrement()));

        do {
            accessionDetailsService.doDownload(format, location, downloadJob, listList, protocol,
                    asperaConnectLocation);
            List<List<FileDetail>> newListList = new ArrayList<>();
            listList.forEach(fileDetails -> {
                final List<FileDetail> collect =
                        fileDetails.stream().filter(fileDetail -> !fileDetail.isSuccess()).collect(Collectors.toList());
                if (collect.size() > 0) {
                    newListList.add(collect);
                }
            });

            AtomicLong newCount = new AtomicLong();
            newListList.forEach(fileDetails -> fileDetails.forEach(fileDetail -> newCount.getAndIncrement()));
            failedCount = newCount.get();

            if (newListList.size() > 0) {
                log.warn("Number of files:{} successfully downloaded for accessionField:{}, format:{}",
                        count.get() - newCount.get(), downloadJob.getAccessionField(), format);
                log.warn("Number of files:{} failed downloaded for accessionField:{}, format:{}",
                        newCount.get(), downloadJob.getAccessionField(), format);
                if (newCount.get() > 0) {
                    System.out.println("Some files failed to download due to possible network issues. Please re-run the " +
                            "same script=" + FileUtils.getScriptPath(downloadJob, format) + " to re-attempt to download those files");
                }
                System.out.println("Automatically retrying failed downloads...");
                listList = newListList;
            }
        } while (failedCount > 0);

        console.info("{} files successfully downloaded for accessionField:{}, format:{} to {}",
                count.get(), downloadJob.getAccessionField(), format, location);

        emailService.sendEmailForFastqSubmitted(emailId, count.get(), 0,
                FileUtils.getScriptPath(downloadJob, format), downloadJob.getAccessionField(), format, location);

    }
}
