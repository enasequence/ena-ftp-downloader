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

import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;
import uk.ac.ebi.ena.app.constants.Constants;
import uk.ac.ebi.ena.app.menu.enums.AccessionTypeEnum;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.backend.dto.AuthenticationDetail;
import uk.ac.ebi.ena.backend.dto.FileDetail;
import uk.ac.ebi.ena.backend.enums.FileDownloadStatus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static uk.ac.ebi.ena.app.utils.CommonUtils.getProgressBarBuilder;

@Service
@Slf4j
public class FileDownloaderService {

    private final FileDownloaderClient fileDownloaderClient;

    public FileDownloaderService(FileDownloaderClient fileDownloaderClient) {
        this.fileDownloaderClient = fileDownloaderClient;
    }

    /**
     * API will check if the file is downloaded properly for FASTQ/SUBMITTED format by validating against md5 and size.
     *
     * @return true if file is downloaded in its entirety
     */
    public static boolean isDownloadSuccessful(FileDetail fileDetail, String fileDownloaderPath, long bytesCopied) {
        if (fileDetail.getBytes() == bytesCopied) {
            try (final FileInputStream fileInputStream = new FileInputStream(fileDownloaderPath)) {
                String md5Hex = DigestUtils.md5DigestAsHex(fileInputStream);
                if (fileDetail.getMd5().equals(md5Hex)) {
                    fileDetail.setSuccess(true);
                    log.debug("MD5 validation successful for file:{}", fileDetail.getFtpUrl());
                    return true;
                } else {
                    log.warn("MD5 failed for file:{}",
                            fileDetail.getFtpUrl());
                    return false;
                }
            } catch (Exception e) {
                log.error("IOException encountered while calculating MD5", e);
                throw new IllegalStateException("IOException encountered while calculating MD5", e);
            }
        }

        log.warn("Unsuccessful Validation for remoteFile:{}. Bytes downloaded does not match with given bytes",
                fileDetail.getFtpUrl());
        return false;
    }


    public static ProgressBarBuilder getBProgressBar(String s, long size) {
        return new ProgressBarBuilder()
                .setMaxRenderedLength(100)
                .setStyle(ProgressBarStyle.ASCII)
                .setTaskName(getShortThreadName()
                        + ":" + StringUtils.substringAfter(maskPassword(s), "://"))
                .setInitialMax(size)
                .setUnit("B", 1); // setting the progress bar to use MB as the unit
    }

    private static String maskPassword(String url) {
        String password = stripPassword(url);
        if (StringUtils.isNotBlank(password)) {
            return StringUtils.replace(url, password, "********");
        }
        return url;
    }

    private static String stripPassword(String url) {
        if (StringUtils.isNotBlank(url)) {
            String userNamePassword = StringUtils.substringBetween(url, "ftp://", "@");
            if (StringUtils.isNotBlank(userNamePassword) && userNamePassword.split(":").length == 2) {
                return userNamePassword.split(":")[1];
            }
        }

        return null;
    }

    public static String getFileDownloadPath(String downloadLoc, AccessionTypeEnum accessionType,
                                             DownloadFormatEnum format,
                                             FileDetail fileDetail) {
        String path = downloadLoc + File.separator
                + StringUtils.lowerCase(format.toString()) + File.separator;
        if (StringUtils.isNotBlank(fileDetail.getParentId())) {
            path += fileDetail.getParentId() + File.separator;
        }
        return path + fileDetail.getRecordId();
    }


    public static boolean isFileDownloaded(Path directoryPath, Path remoteFilePath, String remoteFileName,
                                           FileDetail fileDetail,
                                           FileDownloadStatus fileDownloadStatus, ProgressBar fileProgressBar) throws IOException {
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
            return false;
        } else {
            if (Files.exists(remoteFilePath)) {
                if (fileDetail.getBytes() == Files.size(remoteFilePath)) {
                    log.debug("file:{} size matches", remoteFilePath);
                    fileProgressBar.stepBy(1);
                    fileDownloadStatus.setSuccesssful(fileDownloadStatus.getSuccesssful() + 1);
                    fileDetail.setSuccess(true);
                    return true;
                }
                log.warn("file:{} already exists at the download location but size mismatched.", remoteFileName);
            }
            return false;
        }
    }

    private void deleteIfPartialFileExists(Path partialFilePath, String partialFileName) throws IOException {
        if (Files.exists(partialFilePath)) {
            log.warn("Partial file {} exist, deleting it", partialFileName);
            Files.delete(partialFilePath);
        }
    }

    private String prepareFTPUrl(FileDetail fileDetail, AuthenticationDetail authenticationDetail) {
        String filePath = fileDetail.getFtpUrl();
        if (Objects.nonNull(authenticationDetail) && authenticationDetail.isAuthenticated() &&
                StringUtils.isNotBlank(filePath) && filePath.contains(Constants.DCC_PRIVATE_FTP_FILE_PATH)) {
            return Constants.FTP + authenticationDetail.getUserName() + ":" + authenticationDetail.getPassword() + "@" + filePath;
        } else {
            return Constants.FTP + filePath;
        }
    }

    public Future<FileDownloadStatus> startDownload(ExecutorService executorService, List<FileDetail> fileDetails,
                                                    String downloadLoc, AccessionTypeEnum accessionType,
                                                    DownloadFormatEnum format, int set, AuthenticationDetail authenticationDetail) {
        FileDownloadStatus fileDownloadStatus = new FileDownloadStatus(fileDetails.size(), 0, new ArrayList<>());

        return executorService.submit(() -> {
            System.out.println("\nStarting set " + (set + 1) + " with " + fileDetails.size() + " files.");
            final ProgressBar fileProgressBar =
                    getProgressBarBuilder("Downloading set " + (set + 1) + " with " + fileDetails.size() + " files",
                            fileDetails.size()).build();

            String remoteFileName = null;
            log.debug("Starting {} files starting with {}", fileDetails.size(), fileDetails.get(0).getFtpUrl());
            for (FileDetail fileDetail : fileDetails) {

                try {
                    String fileUrl = prepareFTPUrl(fileDetail, authenticationDetail);
                    log.info("FileURL: " + StringUtils.replace(fileUrl, Objects.nonNull(authenticationDetail) ? authenticationDetail.getPassword() : null,
                            "********"));
                    String fileDownloaderPath = getFileDownloadPath(downloadLoc, accessionType, format, fileDetail);
                    remoteFileName = StringUtils.substringAfterLast(fileUrl, "/");

                    URL url;
                    try {
                        url = new URL(fileUrl);
                    } catch (MalformedURLException e) {
                        log.error("MalformedURLException encountered while starting download");
                        throw new IllegalStateException("MalformedURLException encountered  while starting download",
                                e);
                    }
                    Path directoryPath = Paths.get(fileDownloaderPath);
                    Path remoteFilePath = Paths.get(fileDownloaderPath, remoteFileName);

                    if (!Files.exists(directoryPath)) {
                        Files.createDirectories(directoryPath);
                    } else {
                        if (Files.exists(remoteFilePath)) {
                            if (fileDetail.getBytes() == Files.size(remoteFilePath)) {
                                log.debug("File {} already exists and size matches {}. Skipping.", remoteFileName,
                                        fileDetail.getBytes());
                                fileProgressBar.stepBy(1);
                                fileDownloadStatus.setSuccesssful(fileDownloadStatus.getSuccesssful() + 1);
                                fileDetail.setSuccess(true);
                                continue;
                            }
                            log.warn("File {} exists but size mismatch {}. Redownloading", remoteFileName,
                                    fileDetail.getBytes());

                        }
                    }
                    log.debug("Downloading {}, parentId:{}", remoteFileName, fileDetail.getParentId());
                    Assert.notNull(url, "FTP Url cannot be null");
                    long bytesCopied;
                    if (url.toString().startsWith("http")) {
                        bytesCopied = fileDownloaderClient.downloadHttpClient(url, remoteFilePath,
                                fileDetail.getBytes(), fileDetail.getRetryCount());
                    } else {
                        bytesCopied = fileDownloaderClient.downloadFTPUrlConnection(url, remoteFilePath,
                                fileDetail.getBytes(), fileDetail.getRetryCount());
                    }
                    log.debug("Completed download {}, parentId:{}, bytesCopied:{}",
                            remoteFileName, fileDetail.getParentId(), bytesCopied);
                    boolean isDownloaded = isDownloadSuccessful(fileDetail,
                            fileDownloaderPath + File.separator + remoteFileName, bytesCopied);
                    if (isDownloaded) {
                        fileProgressBar.stepBy(1);
                        log.debug("{} completed.", fileDownloaderPath + File.separator + remoteFileName);
                        fileDownloadStatus.setSuccesssful(fileDownloadStatus.getSuccesssful() + 1);
                    } else {
                        log.error("Failed to download file:{}, experimentId:{}", remoteFileName,
                                fileDetail.getParentId());
                        System.out.println("Failed to download " + url);
                        fileDownloadStatus.getFailedFiles().add(fileDetail);
                    }

                } catch (Exception exception) {
                    log.error("Exception occurred while downloading file:{}, experimentId:{} ",
                            remoteFileName,
                            fileDetail.getParentId(), exception);
                    fileDownloadStatus.getFailedFiles().add(fileDetail);
                }
            }
            return fileDownloadStatus;

        });
    }

    public static String getShortThreadName() {
        return "T-" + StringUtils.substringAfter(Thread.currentThread().getName(), "hread-");
    }

}
