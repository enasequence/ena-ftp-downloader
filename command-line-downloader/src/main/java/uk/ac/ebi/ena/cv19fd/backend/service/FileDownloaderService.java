package uk.ac.ebi.ena.cv19fd.backend.service;

import com.google.common.collect.Lists;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import uk.ac.ebi.ena.cv19fd.app.constants.Constants;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DataTypeEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DomainEnum;
import uk.ac.ebi.ena.cv19fd.app.menu.enums.DownloadFormatEnum;
import uk.ac.ebi.ena.cv19fd.app.utils.CommonUtils;
import uk.ac.ebi.ena.cv19fd.app.utils.FileUtils;
import uk.ac.ebi.ena.cv19fd.backend.dto.FileDetail;
import uk.ac.ebi.ena.cv19fd.backend.enums.FileDownloadStatus;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static uk.ac.ebi.ena.cv19fd.app.constants.Constants.DATE_FORMAT;
import static uk.ac.ebi.ena.cv19fd.app.constants.Constants.FTP_SRA_SERVER;
import static uk.ac.ebi.ena.cv19fd.app.utils.CommonUtils.getProgressBarBuilder;
import static uk.ac.ebi.ena.cv19fd.backend.config.BeanConfig.APP_RETRY;

@Service
@Slf4j
@AllArgsConstructor
public class FileDownloaderService {


    private final EnaBrowserService enaBrowserService;
    private final EmailService emailService;
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

    /**
     * It will download xml ,EMBL and fasta files.
     *
     * @param downloadLoc The download location provided by the user
     * @param domain      The domain provided by the user
     * @param dataType    The dataType provided by the user
     * @param format      The format provided by the user
     */
    public void startDownload(String downloadLoc, DomainEnum domain, DataTypeEnum dataType, DownloadFormatEnum format
            , String emailId, List<String> accessions) {
        int retryCount = 0;
        String outputPath = getFileDownloadPath(downloadLoc, domain, dataType, format);
        boolean isSuccess = false;
        while (retryCount <= Constants.TOTAL_RETRIES) {
            try {
                Path outputFile = Paths.get(outputPath);
                if (Files.exists(outputFile)) {
                    org.apache.commons.io.FileUtils.forceDelete(outputFile.toFile());
                }
                org.apache.commons.io.FileUtils.forceMkdirParent(outputFile.toFile());

                try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputPath))) {
                    if (!CollectionUtils.isEmpty(accessions)) {
                        final List<List<String>> partitions = Lists.partition(accessions, 10000);
                        for (List<String> partition : partitions) {
                            try (final InputStream inputStream =
                                         ProgressBar.wrap(enaBrowserService.getInputStreamForDownloadedFile(partition
                                                 , domain
                                                 , dataType, format), getMBProgressBar(format.name()))) {
                                IOUtils.copyLarge(inputStream, outputStream);
                            }
                        }
                    } else {
                        try (final InputStream inputStream =
                                     ProgressBar.wrap(enaBrowserService.getInputStreamForDownloadedFile(null, domain
                                             , dataType, format), getMBProgressBar(format.name()))) {
                            IOUtils.copyLarge(inputStream, outputStream);
                        }
                    }
                }

                isSuccess = true;
                emailService.sendEmailForOtherFormats(emailId, domain, dataType, format, outputPath, isSuccess,
                        accessions);
                log.info("Download completed successfully!!.");
                System.out.println("Download completed to " + outputPath + ".");
                break;
            } catch (Exception e) {
                FileUtils.writeExceptionToFile("Exception occurred while downloading file: " + e.getMessage() + "\n" + Arrays.toString(e.getStackTrace()));
                log.error("Exception occurred while downloading file.", e);
                retryCount++;
            }
            if (retryCount > Constants.TOTAL_RETRIES) {
                System.out.println("File could not be downloaded due to possible network issues.");
                log.error("File could not be downloaded due to possible network issues.");
                emailService.sendEmailForOtherFormats(emailId, domain, dataType, format, outputPath, isSuccess,
                        accessions);
            }
        }
    }

    public ProgressBarBuilder getMBProgressBar(String s) {
        ProgressBarBuilder pbb = new ProgressBarBuilder()
                .setMaxRenderedLength(100)
                .setStyle(ProgressBarStyle.ASCII)
                .setTaskName("Downloading " + StringUtils.substringAfter(s, "://"))
                .setUnit("MB", 1048576); // setting the progress bar to use MB as the unit

        return pbb;
    }

    public ProgressBarBuilder getBProgressBar(String s, long size) {
        ProgressBarBuilder pbb = new ProgressBarBuilder()
                .setMaxRenderedLength(100)
                .setStyle(ProgressBarStyle.ASCII)
                .setTaskName("Downloading " + StringUtils.substringAfter(s, "://"))
                .setInitialMax(size)
                .setUnit("B", 1); // setting the progress bar to use MB as the unit

        return pbb;
    }

    /**
     * This API will fetch the download path and append the remoteFile name to it.
     * fileDownloaderPath=downloadLoc+domain+dataType+format+experimentId+runId+fileName
     */
    private String getFileDownloadPath(String downloadLoc, DomainEnum domain, DataTypeEnum dataType,
                                       DownloadFormatEnum format, FileDetail fileDetail) {
        return downloadLoc + File.separator
                + StringUtils.lowerCase(domain + File.separator + dataType + File.separator + format)
                + File.separator + fileDetail.getExperimentId().substring(0, 3) + File.separator +
                fileDetail.getExperimentId().substring(0, 6) + File.separator + fileDetail.getExperimentId() +
                File.separator + fileDetail.getRunId();
    }


    /**
     * This API will fetch the download path and append the remoteFile name to it.
     * <p>
     * fileDownloaderPath=downloadLoc+domain+dataType+format+experimentId+runId+fileName
     */
    private String getFileDownloadPath(String downloadLoc, DomainEnum domain, DataTypeEnum dataType,
                                       DownloadFormatEnum format) {
        return String.format(downloadLoc + File.separator + StringUtils.lowerCase(domain + File.separator
                + dataType + File.separator
                + format + File.separator + DATE_FORMAT.format(new Date()) + "." + format.getExtension()));
    }


    /**
     * API will start FASTQ/SUBMITTED download for each file and return a Future to the {@link FileDownloadStatus}.
     *
     * @param executorService The instance of executor service
     * @param fileDetails     The list of files to download
     * @param downloadLoc     The download location provided by the user
     * @param domain          The domain provided by the user
     * @param dataType        The dataType provided by the user
     * @param format          The format provided by the user
     * @param set
     * @return Future<FileDownloadStatus> Future of {@link FileDownloadStatus}
     */

    public Future<FileDownloadStatus> startDownload(ExecutorService executorService,
                                                    List<FileDetail> fileDetails, String downloadLoc,
                                                    DomainEnum domain, DataTypeEnum dataType,
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
                    String fileDownloaderPath = getFileDownloadPath(downloadLoc, domain, dataType, format, fileDetail);
                    remoteFileName = StringUtils.substringAfterLast(fileUrl, "/");
                    log.debug("Starting file download for remoteFile:{}, experimentId:{}", remoteFileName,
                            fileDetail.getExperimentId());
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
                    long bytesCopied = 0;
                    if (url.toString().startsWith("http")) {
                        bytesCopied = download(url, remoteFilePath, fileDetail.getBytes(), 0);
                    } else {
                        bytesCopied = downloadFTP(url, remoteFilePath, fileDetail.getBytes(), 0);
                    }
                    log.debug("Completed download for remoteFile:{}, experimentId:{}, bytesCopied:{}",
                            remoteFileName, fileDetail.getExperimentId(), bytesCopied);
                    boolean isDownloaded = isDownloadSuccessful(fileDetail,
                            fileDownloaderPath + File.separator + remoteFileName, bytesCopied);
                    if (isDownloaded) {
                        fileProgressBar.stepBy(1);
                        fileDownloadStatus.setSuccesssful(fileDownloadStatus.getSuccesssful() + 1);
                        continue;
                    } else {
                        log.error("Failed to download file:{}, experimentId:{}", remoteFileName,
                                fileDetail.getExperimentId());
                        System.out.println("Failed to download " + url);
                        fileDownloadStatus.getFailedFiles().add(fileDetail);
                    }

                } catch (Exception exception) {
                    log.error("Exception occurred while downloading file:{}, experimentId:{} ",
                            remoteFileName,
                            fileDetail.getExperimentId(), exception);
                    fileDownloadStatus.getFailedFiles().add(fileDetail);
                }
            }
            return fileDownloadStatus;

        });

    }

    @SneakyThrows
    private long download(URL url, Path remoteFilePath, long size, int retryCount) throws IOException {
//        org.apache.commons.io.FileUtils.copyURLToFile(url, new File(remoteFilePath.toString()),
//                30000, 10000);
        if (retryCount == APP_RETRY) {
            return 0;
        }
        CloseableHttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(httpRequestRetryHandler)
                .setServiceUnavailableRetryStrategy(serviceUnavailableRetryStrategy)
                .build();
        try {
            File outFile = new File(remoteFilePath.toString());
            if (outFile.exists()) {
                outFile.delete();
            }

            HttpGet request = new HttpGet(url.toURI());

            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();

            try (InputStream is = retryCount > 0 ?
                    ProgressBar.wrap(entity.getContent(),
                            getBProgressBar(url.toString() + ":attempt " + (retryCount + 1), size)) :
                    entity.getContent()
                 ;
                 FileOutputStream fos = new FileOutputStream(outFile)) {
                return IOUtils.copyLarge(is, fos);
            }
        } catch (Exception e) {
            log.error(remoteFilePath + " retry " + (retryCount + 1), e.getMessage());
            return download(url, remoteFilePath, size, retryCount + 1);
        } finally {
            client.close();
        }
    }

    @SneakyThrows
    private long downloadFTPClient(URL url, Path remoteFilePath, long size, int retryCount) throws IOException {
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
//                         retryCount > 0 ?
                         ProgressBar.wrap(ftp.retrieveFileStream(fileName), getBProgressBar(url.toString() +
                                 ":attempt " + (retryCount + 1), size))
//                                 : ftp.retrieveFileStream(fileName)
                 ;
                 FileOutputStream fos = new FileOutputStream(outFile)) {
                return IOUtils.copyLarge(in, fos);
            }
        } catch (Exception e) {
            log.error(remoteFilePath + " retry " + (retryCount + 1), e.getMessage());
            return downloadFTPClient(url, remoteFilePath, size, retryCount + 1);
        } finally {
            try {
                ftp.logout();
                ftp.disconnect();
            } catch (Exception e) {
            }
        }
    }

    @SneakyThrows
    private long downloadFTP(URL url, Path remoteFilePath, long size, int retryCount) {
        if (retryCount == APP_RETRY) {
            return 0;
        }

        try {
            File outFile = new File(remoteFilePath.toString());
            if (outFile.exists()) {
                outFile.delete();
            }
            URLConnection conn = url.openConnection();
            try (InputStream in =
                         retryCount > 0 ?
                                 ProgressBar.wrap(conn.getInputStream(),
                                         getBProgressBar(url.toString() + ":attempt " + (retryCount + 1), size))
                                 : conn.getInputStream()
                 ;
                 FileOutputStream fos = new FileOutputStream(outFile)) {
                return IOUtils.copyLarge(in, fos);
            }
        } catch (Exception e) {
            log.error(remoteFilePath + " retry " + (retryCount + 1), e.getMessage());
            return downloadFTP(url, remoteFilePath, size, retryCount + 1);
        }
    }

    public Future<FileDownloadStatus> startDownloadAspera(ExecutorService executorService,
                                                          List<FileDetail> fileDetails, String asperaLocation,
                                                          String downloadLocation, DomainEnum domain,
                                                          DataTypeEnum dataType, DownloadFormatEnum format,
                                                          int set) {
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
                String fileDownloaderPath = getFileDownloadPath(downloadLocation, domain, dataType, format,
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
                        inner:
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
                                                    "experimentId:{}", remoteFileName,
                                            fileDetail.getExperimentId());
                                    fileProgressBar.stepBy(1);
                                    fileDownloadStatus.setSuccesssful(fileDownloadStatus.getSuccesssful() + 1);
                                    break outer;
                                }
                                retryCount++;
                            } else if (result.contains("Error")) {
                                log.error("Download for file {} could not be completed. Will retry", remoteFileName);
                                retryCount++;
                                break inner;
                            }
                        }
                    } catch (Exception exception) {
                        log.error("Exception occurred while downloading remote file:{}", fileDetail.getFtpUrl(),
                                exception);
                        fileDownloadStatus.getFailedFiles().add(fileDetail);
                    }
                }

                if (retryCount > Constants.TOTAL_RETRIES) {
                    log.error("FAILED download remoteFile:{}, experimentId:{}, Retry Count:{}",
                            remoteFileName, fileDetail.getExperimentId(), --retryCount);
                    fileDownloadStatus.getFailedFiles().add(fileDetail);
                }

            }
            return fileDownloadStatus;
        });
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


}
