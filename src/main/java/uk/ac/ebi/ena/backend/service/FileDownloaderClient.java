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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.ebi.ena.app.constants.Constants;
import uk.ac.ebi.ena.app.menu.enums.AccessionTypeEnum;
import uk.ac.ebi.ena.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.app.utils.CommonUtils;
import uk.ac.ebi.ena.backend.dto.AuthenticationDetail;
import uk.ac.ebi.ena.backend.dto.FileDetail;
import uk.ac.ebi.ena.backend.enums.FileDownloadStatus;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static uk.ac.ebi.ena.app.constants.Constants.FTP_SRA_SERVER;
import static uk.ac.ebi.ena.app.utils.CommonUtils.getProgressBarBuilder;
import static uk.ac.ebi.ena.backend.config.BeanConfig.APP_RETRY;
import static uk.ac.ebi.ena.backend.service.FileDownloaderService.*;

@Component
@Slf4j
public class FileDownloaderClient {

    final Logger console = LoggerFactory.getLogger("console");

    AtomicLong verified = new AtomicLong(0), redownloading = new AtomicLong(0);

    private static final String DOWNLOAD_PARAMS_ASPERA = "-QT -l 300m -P 33001 ";
    public static final int THIRTYSECONDS = 30000;
    public static final int FIVEMINUTES = 300000;

    private final RequestConfig requestConfig;
    private final HttpRequestRetryHandler httpRequestRetryHandler;
    private final ServiceUnavailableRetryStrategy serviceUnavailableRetryStrategy;

    public FileDownloaderClient(RequestConfig requestConfig, HttpRequestRetryHandler httpRequestRetryHandler,
                                ServiceUnavailableRetryStrategy serviceUnavailableRetryStrategy) {
        this.requestConfig = requestConfig;
        this.httpRequestRetryHandler = httpRequestRetryHandler;
        this.serviceUnavailableRetryStrategy = serviceUnavailableRetryStrategy;
    }

    @SneakyThrows
    public long downloadHttpClient(URL url, Path remoteFilePath, long size, int retryCount) {
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
                    entity.getContent();
                 FileOutputStream fos = new FileOutputStream(outFile)) {
                return IOUtils.copyLarge(is, fos);
            }
        } catch (Exception e) {
            log.error(remoteFilePath + " failed to download", e);
            return 0;
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
            long copied = 0;
            try (InputStream in =
                         ProgressBar.wrap(ftp.retrieveFileStream(fileName), getBProgressBar(url +
                                 ":attempt " + (retryCount + 1), size));
                 FileOutputStream fos = new FileOutputStream(outFile)) {
                copied = IOUtils.copyLarge(in, fos);
            }
            return copied;
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

    @SneakyThrows
    public long downloadFTPUrlConnection(URL url, Path remoteFilePath, long size, int retryCount) {
        FTPClient ftp = null;
        try {
            File outFile = new File(remoteFilePath.toString());
            if (outFile.exists()) {
                outFile.delete();
            }
            String fileName = StringUtils.replace(url.toString(), "ftp.sra.ebi.ac.uk/vol1", "..");
            URLConnection conn = url.openConnection();
            long copied = 0;
            try (InputStream in =
                         ProgressBar.wrap(conn.getInputStream(), getBProgressBar(fileName +
                                 ":attempt " + (retryCount + 1), size));
                 FileOutputStream fos = new FileOutputStream(outFile)) {
                copied = IOUtils.copyLarge(in, fos);
            }
            return copied;
        } catch (Exception e) {
            log.error(remoteFilePath + " failed to download", e);
            return 0;
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
                                                          AccessionTypeEnum accessionType, DownloadFormatEnum format, int set,
                                                          AuthenticationDetail authenticationDetail) {
        FileDownloadStatus fileDownloadStatus = new FileDownloadStatus(fileDetails.size(), 0,
                new ArrayList<>());
        return executorService.submit(() -> {
            System.out.println("\nStarting set " + set + " of " + fileDetails.size() + " files.");
            final ProgressBar fileProgressBar =
                    getProgressBarBuilder("Downloading set " + set + " of " + fileDetails.size() + " files",
                            fileDetails.size()).build();
            String remoteFileName;
            for (FileDetail fileDetail : fileDetails) {
                log.debug("Downloading file {}", fileDetail.getFtpUrl());
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
                    long vl = verified.incrementAndGet();
                    if (vl % 1000 == 0) {
                        System.out.println(verified + " existing files verified. Last was " + remoteFilePath.toString());
                    }
                    continue;
                }
                long rl = redownloading.incrementAndGet();
                if (rl % 1000 == 0) {
                    System.out.println(redownloading + " files marked for download. Last was " + remoteFilePath.toString());
                }
                List<String> commands = getAsperaCommandParts(asperaLocation, fileDetail, remoteFilePath);

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
                                    log.debug("File {} downloaded {}%", remoteFileName
                                            , percentCompleted);
                                }
                            } else if (result.startsWith("Completed")) {
                                long bytesCopied = Files.size(remoteFilePath);
                                boolean isDownloaded = isDownloadSuccessful(fileDetail,
                                        fileDownloaderPath + File.separator + remoteFileName,
                                        bytesCopied);
                                if (isDownloaded) {
                                    log.debug("SUCCESSFUL download remoteFile:{}, " +
                                                    "parentId:{}", remoteFileName,
                                            fileDetail.getParentId());
                                    fileProgressBar.stepBy(1);
                                    fileDownloadStatus.setSuccesssful(fileDownloadStatus.getSuccesssful() + 1);
                                }
                            } else if (result.contains("Error")) {
                                log.error("Download for file {} could not be completed.", remoteFileName);
                            }
                        }
                    } catch (Exception exception) {
                        log.error("Exception occurred while downloading remote file:{}", fileDetail.getFtpUrl(),
                                exception);
                        fileDownloadStatus.getFailedFiles().add(fileDetail);
                    }

            }
            return fileDownloadStatus;
        });
    }

    private void deleteIfPartialFileExists(Path partialFilePath, String partialFileName) throws IOException {
        if (Files.exists(partialFilePath)) {
            console.info("Partial file {} exist, deleting it", partialFileName);
            Files.delete(partialFilePath);
        }
    }


}

