package uk.ac.ebi.ena.cv19fd.backend.service;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
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
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;
import uk.ac.ebi.ena.cv19fd.app.constants.Constants;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.cv19fd.app.utils.CommonUtils;
import uk.ac.ebi.ena.cv19fd.backend.dto.FileDetail;
import uk.ac.ebi.ena.cv19fd.backend.enums.FileDownloadStatus;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static uk.ac.ebi.ena.cv19fd.app.constants.Constants.*;
import static uk.ac.ebi.ena.cv19fd.app.utils.CommonUtils.getProgressBarBuilder;
import static uk.ac.ebi.ena.cv19fd.backend.config.BeanConfig.APP_RETRY;

@Service
@Slf4j
@AllArgsConstructor
public class FileDownloaderService {


    private final RequestConfig requestConfig;
    private final HttpRequestRetryHandler httpRequestRetryHandler;
    private final ServiceUnavailableRetryStrategy serviceUnavailableRetryStrategy;

    private static final String DOWNLOAD_PARAMS_ASPERA = "-QT -l 100m -P 33001 ";

    /**
     * API will check if the file is downloaded properly for FASTQ/SUBMITTED format by validating against md5 and size.
     *
     * @return true if file is downloaded in its entirety
     */
    private boolean isDownloadSuccessful(FileDetail fileDetail, String fileDownloaderPath, long bytesCopied) {
        log.debug("Checking file validation for remoteFile:{}", fileDetail.getFtpUrl());
        if (fileDetail.getBytes() == bytesCopied) {
            try (final FileInputStream fileInputStream = new FileInputStream(fileDownloaderPath)) {
                String md5Hex = DigestUtils.md5DigestAsHex(fileInputStream);
                if (fileDetail.getMd5().equals(md5Hex)) {
                    log.debug("Validation successful for remoteFile:{}", fileDetail.getFtpUrl());
                    return true;
                } else {
                    log.info("Unsuccessful Validation for remoteFile:{}. MD5 checksum does not match",
                            fileDetail.getFtpUrl());
                    return false;
                }
            } catch (IOException e) {
                log.error("IOException encountered while calculating MD5");
                throw new IllegalStateException("IOException encountered while calculating MD5", e);
            }
        }

        log.info("Unsuccessful Validation for remoteFile:{}. Bytes downloaded does not match with given bytes",
                fileDetail.getFtpUrl());
        return false;
    }



    public ProgressBarBuilder getBProgressBar(String s, long size) {
        return new ProgressBarBuilder()
                .setMaxRenderedLength(100)
                .setStyle(ProgressBarStyle.ASCII)
                .setTaskName(StringUtils.replace(Thread.currentThread().getName(), "pool-1-", "")
                        + ":" + StringUtils.substringAfter(s, "://"))
                .setInitialMax(size)
                .setUnit("B", 1); // setting the progress bar to use MB as the unit
    }

    private String getFileDownloadPath(String downloadLoc, String accessionType, DownloadFormatEnum format, FileDetail fileDetail) {
        switch (accessionType) {
            case RUN:
                return downloadLoc + File.separator
                        + StringUtils.lowerCase(format.toString()) + File.separator + fileDetail.getRunId();
            case PROJECT:
            case EXPERIMENT:
            case SAMPLE:
                return downloadLoc + File.separator
                        + StringUtils.lowerCase(format.toString()) + File.separator + fileDetail.getParentId() +File.separator+ fileDetail.getRunId();
            case ANALYSIS:
                return downloadLoc + File.separator
                        + StringUtils.lowerCase(format.toString()) + File.separator + fileDetail.getParentId();
        }
        return "";
    }

    @SneakyThrows
    private long download(URL url, Path remoteFilePath, long size, int retryCount) {
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
            return download(url, remoteFilePath, size, retryCount + 1);
        }
    }

    @SneakyThrows
    private long downloadFTPClient(URL url, Path remoteFilePath, long size, int retryCount) {
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
            log.error(remoteFilePath + " retry " + (retryCount + 1), e.getMessage());
            Thread.sleep(5000); // wait 5 sec before retrying
            return downloadFTPClient(url, remoteFilePath, size, retryCount + 1);
        } finally {
            try {
                if(ftp!=null) {
                    ftp.logout();
                    ftp.disconnect();
                }
            } catch (Exception e) {
                log.error("Exception encountered while calling logout and disconnect on ftpService");
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


    private boolean isFileDownloaded(Path directoryPath, Path remoteFilePath, String remoteFileName,
                                     FileDetail fileDetail,
                                     FileDownloadStatus fileDownloadStatus, ProgressBar fileProgressBar) throws IOException {
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
            return false;
        } else {
            if (Files.exists(remoteFilePath)) {
                log.debug("Remote file:{} already exists at the download location. Checking for size " +
                        "match", remoteFileName);
                if (fileDetail.getBytes() == Files.size(remoteFilePath)) {
                    log.debug("Remote file:{} size matches", remoteFileName);
                    fileProgressBar.stepBy(1);
                    fileDownloadStatus.setSuccesssful(fileDownloadStatus.getSuccesssful() + 1);
                    return true;
                }
            }
            return false;
        }
    }

    private void deleteIfPartialFileExists(Path partialFilePath, String partialFileName) throws IOException {
        if (Files.exists(partialFilePath)) {
            log.debug("Partial file {} exist, deleting it", partialFileName);
            Files.delete(partialFilePath);
        }
    }


    public Future<FileDownloadStatus> startDownload(ExecutorService executorService, List<FileDetail> fileDetails,
                                                    String downloadLoc, String accessionType,
                                                    DownloadFormatEnum format, int set) {
        FileDownloadStatus fileDownloadStatus = new FileDownloadStatus(fileDetails.size(), 0, new ArrayList<>());

        return executorService.submit(() -> {
            System.out.println("\nStarting set " + set + " of " + fileDetails.size() + " files.");
            final ProgressBar fileProgressBar =
                    getProgressBarBuilder("Downloading set " + set + " of " + fileDetails.size() + " files",
                            fileDetails.size()).build();

            String remoteFileName = null;
            log.info("Starting {} files starting with {}", fileDetails.size(), fileDetails.get(0).getFtpUrl());
            for (FileDetail fileDetail : fileDetails) {

                try {
                    String fileUrl = Constants.FTP + fileDetail.getFtpUrl();
                    String fileDownloaderPath = getFileDownloadPath(downloadLoc,accessionType, format, fileDetail);
                    remoteFileName = StringUtils.substringAfterLast(fileUrl, "/");
                    log.debug("Starting file download for remoteFile:{}, parentId:{}", remoteFileName,
                            fileDetail.getParentId());
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
                            log.debug("Remote file:{} already exists at the download location. Checking for size " +
                                    "match", remoteFileName);
                            if (fileDetail.getBytes() == Files.size(remoteFilePath)) {
                                log.info("File {} already exists and size matches {}. Skipping.", remoteFileName,
                                        fileDetail.getBytes());
                                fileProgressBar.stepBy(1);
                                fileDownloadStatus.setSuccesssful(fileDownloadStatus.getSuccesssful() + 1);
                                continue;
                            }
                        }
                    }
                    Assert.notNull(url, "FTP Url cannot be null");
                    long bytesCopied;
                    if (url.toString().startsWith("http")) {
                        bytesCopied = download(url, remoteFilePath, fileDetail.getBytes(), 0);
                    } else {
                        bytesCopied = downloadFTPClient(url, remoteFilePath, fileDetail.getBytes(), 0);
                    }
                    log.debug("Completed download for remoteFile:{}, experimentId:{}, bytesCopied:{}",
                            remoteFileName, fileDetail.getParentId(), bytesCopied);
                    boolean isDownloaded = isDownloadSuccessful(fileDetail,
                            fileDownloaderPath + File.separator + remoteFileName, bytesCopied);
                    if (isDownloaded) {
                        fileProgressBar.stepBy(1);
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
//                log.info("comd:{}", StringUtils.join(commands, " "));

                outer:
                while (retryCount < Constants.TOTAL_RETRIES) {
                    deleteIfPartialFileExists(partialFilePath, partialFileName);
                    ProcessBuilder processBuilder = new ProcessBuilder(commands);
                    Process process = processBuilder.start();
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
}
