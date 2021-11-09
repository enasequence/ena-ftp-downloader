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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.stereotype.Component;
import uk.ac.ebi.ena.app.constants.Constants;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.utils.CommonUtils;
import uk.ac.ebi.ena.backend.dto.FileDetail;
import uk.ac.ebi.ena.backend.enums.FileDownloadStatus;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static uk.ac.ebi.ena.app.constants.Constants.FTP_SRA_SERVER;
import static uk.ac.ebi.ena.app.utils.CommonUtils.getProgressBarBuilder;
import static uk.ac.ebi.ena.backend.config.BeanConfig.APP_RETRY;
import static uk.ac.ebi.ena.backend.service.FileDownloaderService.*;

@Component
@Slf4j
@AllArgsConstructor
public class FileDownloaderClient {
    private static final String DOWNLOAD_PARAMS_ASPERA = "-QT -l 300m -P 33001 ";
    public static final int THIRTYSECONDS = 30000;
    public static final int FIVEMINUTES = 300000;

    private final RequestConfig requestConfig;
    private final HttpRequestRetryHandler httpRequestRetryHandler;
    private final ServiceUnavailableRetryStrategy serviceUnavailableRetryStrategy;


    @SneakyThrows
    public long downloadHttpClient(URL url, Path remoteFilePath, long size, int retryCount) {
        if (retryCount == APP_RETRY) {
            return 0;
        }
        try (CloseableHttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(httpRequestRetryHandler)
                .setServiceUnavailableRetryStrategy(serviceUnavailableRetryStrategy)
                .build()) {
            File outFile = new File(remoteFilePath.toString());
            if (outFile.exists()) {
                outFile.delete();
            }

            HttpGet request = new HttpGet(url.toURI());

            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();

            try (InputStream is = retryCount > 0 ?
                    ProgressBar.wrap(entity.getContent(),
                            getBProgressBar(url + ":attempt " + (retryCount + 1), size)) :
                    entity.getContent()
                 ;
                 FileOutputStream fos = new FileOutputStream(outFile)) {
                return IOUtils.copyLarge(is, fos);
            }
        } catch (Exception e) {
            log.error(remoteFilePath + " retry " + (retryCount + 1), e.getMessage());
            return downloadHttpClient(url, remoteFilePath, size, retryCount + 1);
        }
    }

    @SneakyThrows
    public long downloadFTPClient(URL url, Path remoteFilePath, long size, int retryCount) {
        if (retryCount == APP_RETRY) {
            return 0;
        }

        FTPClient ftp = null;
        try {
            File outFile = new File(remoteFilePath.toString());
            if (outFile.exists()) {
                outFile.delete();
            }
            ftp = new FTPClient();
            ftp.setConnectTimeout(THIRTYSECONDS);
            ftp.setDataTimeout(FIVEMINUTES);
            ftp.connect(Constants.FTP_SRA_SERVER);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new Exception("Failed ftp connection");
            }
            ftp.login("anonymous", "1234");
            ftp.changeWorkingDirectory(StringUtils.substringBeforeLast(StringUtils.substringAfter(url.toString(),
                    FTP_SRA_SERVER + "/"), "/"));
            String fileName = StringUtils.substringAfterLast(url.toString(), "/");
            try (InputStream in =
                         ProgressBar.wrap(ftp.retrieveFileStream(fileName), getBProgressBar(url +
                                 ":attempt " + (retryCount + 1), size));
                 FileOutputStream fos = new FileOutputStream(outFile)) {
                return IOUtils.copyLarge(in, fos);
            }
        } catch (Exception e) {
            log.error(remoteFilePath + " retry " + (retryCount + 1), e);
            Thread.sleep(5000); // wait 5 sec before retrying
            return downloadFTPClient(url, remoteFilePath, size, retryCount + 1);
        } finally {
            try {
                if (ftp != null) {
                    ftp.logout();
                    ftp.disconnect();
                }
            } catch (Exception e) {
                log.error("Exception encountered while calling logout and disconnect on ftpService", e);
            }
        }
    }

    private List<String> getAsperaCommandParts(String asperaLocation, FileDetail fileDetail, Path remoteFilePath) {
        String ascpFile = CommonUtils.getAscpFileName();
        List<String> commands = new ArrayList<>();
        commands.add(Paths.get(asperaLocation, Constants.binFolder, ascpFile).toString());
        commands.addAll(Arrays.asList(DOWNLOAD_PARAMS_ASPERA.split("\\s")));
        commands.add("-i");
        commands.add(Paths.get(asperaLocation, Constants.etcFolder, Constants.asperaWebFile).toString());
        commands.add("era-fasp@" + fileDetail.getFtpUrl());
        commands.add(remoteFilePath.toString());
        return commands;
    }

    public Future<FileDownloadStatus> startDownloadAspera(ExecutorService executorService, List<FileDetail> fileDetails,
                                                          String asperaLocation, String downloadLocation,
                                                          String accessionType, DownloadFormatEnum format, int set) {
        FileDownloadStatus fileDownloadStatus = new FileDownloadStatus(fileDetails.size(), 0,
                new ArrayList<>());
        return executorService.submit(() -> {
            System.out.println("\nStarting set " + set + " of " + fileDetails.size() + " files.");
            final ProgressBar fileProgressBar =
                    getProgressBarBuilder("Downloading set " + set + " of " + fileDetails.size() + " files",
                            fileDetails.size()).build();
            String remoteFileName;
            for (FileDetail fileDetail : fileDetails) {
                int retryCount = 0;
                log.info("Downloading file {}", fileDetail.getFtpUrl());
                String fileDownloaderPath = getFileDownloadPath(downloadLocation, accessionType, format,
                        fileDetail);
                remoteFileName = StringUtils.substringAfterLast(fileDetail.getFtpUrl(), "/");
                Path directoryPath = Paths.get(fileDownloaderPath);
                Path remoteFilePath = Paths.get(fileDownloaderPath, remoteFileName);
                String partialFileName = remoteFileName + ".partial";
                Path partialFilePath = Paths.get(fileDownloaderPath, partialFileName);
                boolean isFileDownloaded = isFileDownloaded(directoryPath, remoteFilePath, remoteFileName, fileDetail,
                        fileDownloadStatus, fileProgressBar);
                if (isFileDownloaded) {
                    continue;
                }
                List<String> commands = getAsperaCommandParts(asperaLocation, fileDetail, remoteFilePath);

                outer:
                while (retryCount < Constants.TOTAL_RETRIES) {
                    deleteIfPartialFileExists(partialFilePath, partialFileName);
                    ProcessBuilder processBuilder = new ProcessBuilder(commands);
                    Process process = processBuilder.start();
                    final ProgressBar progressBar =
                            getProgressBarBuilder(getShortThreadName() + ":" + StringUtils.substringAfter(fileDetail.getFtpUrl(), ":"), 100).build();

                    try (final BufferedReader reader =
                                 new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String result;
                        while ((result = reader.readLine()) != null) {
                            if (StringUtils.contains(result, "Store key in cache? (y/n)")) {
                                process.getOutputStream().write("y".getBytes());
                            }
                            if (result.startsWith(remoteFileName)) {
                                int percentCompleted = Integer.parseInt
                                        (StringUtils.strip(StringUtils.split(result)[1], "%"));
                                progressBar.stepTo(percentCompleted);

                                if (percentCompleted == 100) {
                                    log.info("File {} downloaded {}%", remoteFileName
                                            , percentCompleted);
                                }
                            } else if (result.startsWith("Completed")) {
                                long bytesCopied = Files.size(remoteFilePath);
                                boolean isDownloaded = isDownloadSuccessful(fileDetail,
                                        fileDownloaderPath + File.separator + remoteFileName,
                                        bytesCopied);
                                if (isDownloaded) {
                                    log.debug("File Detail for SUCCESSFUL download remoteFile:{}, " +
                                                    "parentId:{}", remoteFileName,
                                            fileDetail.getParentId());
                                    fileProgressBar.stepBy(1);
                                    fileDownloadStatus.setSuccesssful(fileDownloadStatus.getSuccesssful() + 1);
                                    break outer;
                                }
                                retryCount++;
                            } else if (result.contains("Error")) {
                                log.error("Download for file {} could not be completed. Will retry", remoteFileName);
                                retryCount++;
                            }
                        }
                    } catch (Exception exception) {
                        log.error("Exception occurred while downloading remote file:{}", fileDetail.getFtpUrl(),
                                exception);
                        fileDownloadStatus.getFailedFiles().add(fileDetail);
                    }
                }

                if (retryCount > Constants.TOTAL_RETRIES) {
                    log.error("FAILED download remoteFile:{}, parentId:{}, Retry Count:{}",
                            remoteFileName, fileDetail.getParentId(), --retryCount);
                    fileDownloadStatus.getFailedFiles().add(fileDetail);
                }

            }
            return fileDownloadStatus;
        });
    }

    private void deleteIfPartialFileExists(Path partialFilePath, String partialFileName) throws IOException {
        if (Files.exists(partialFilePath)) {
            log.debug("Partial file {} exist, deleting it", partialFileName);
            Files.delete(partialFilePath);
        }
    }


}

