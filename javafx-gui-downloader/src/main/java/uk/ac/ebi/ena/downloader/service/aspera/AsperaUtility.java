package uk.ac.ebi.ena.downloader.service.aspera;


import it.sauronsoftware.ftp4j.FTPClient;
import javafx.scene.control.ProgressIndicator;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ena.downloader.gui.custom.MD5TableCell;
import uk.ac.ebi.ena.downloader.model.DownloadSettings;
import uk.ac.ebi.ena.downloader.model.RemoteFile;
import uk.ac.ebi.ena.downloader.service.ftp.FTPException;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A utility class that provides functionality for downloading files from a FTP
 * server.
 *
 * @author www.codejava.net
 */
public class AsperaUtility {

    private final static Logger log = LoggerFactory.getLogger(AsperaUtility.class);

    // FTP server information
    private String host = "fasp.sra.ebi.ac.uk";
    private int port = 33001;

    private FTPClient ftpClient;
    private int replyCode;

    private InputStream inputStream;

    public AsperaUtility() {
        ftpClient = new FTPClient();
    }


    /**
     * Start downloading a file from the server
     *
     * @throws FTPException if client-server communication error occurred
     */
    public void downloadFile(final RemoteFile remoteFile, DownloadSettings downloadSettings) throws Exception {
        try {

            // aspera doesn't support url encoded file paths
            // e.g. fasp.sra.ebi.ac.uk/vol1/ERA413/ERA413485/tab/TARA_018_DCM_%3C-0.22.gene.tsv for fasp.sra.ebi.ac.uk/vol1/ERA413/ERA413485/tab/TARA_018_DCM_<-0.22.gene.tsv
            final File downloadFile = new File(remoteFile.getSaveLocation() + File.separator + remoteFile.getName());

            remoteFile.setLocalPath(downloadFile.getAbsolutePath());

            List<String> commands = new ArrayList<>();
            commands.add(downloadSettings.getExecutable());
            commands.addAll(Arrays.asList(downloadSettings.getParameters().split("\\s")));
            commands.add("-P");
            commands.add("33001");
            commands.add("-i");
            commands.add(downloadSettings.getCertificate());
            commands.add("era-fasp@" + remoteFile.getPath());
            commands.add(remoteFile.getSaveLocation());

            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            log.info(StringUtils.join(processBuilder.command(), " "));

            Process process = processBuilder.start();

            // Get input streams
            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String s = null;
            while ((s = reader.readLine()) != null) {
                log.info(s);
                if (s.contains("Error")) {
                    remoteFile.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                    throw new AsperaException(s);
                }
                if (StringUtils.contains(s, "Store key in cache? (y/n)")) {
                    process.getOutputStream().write("y".getBytes());
                }
                if (remoteFile.getSize() > 0) {
                    if (s.startsWith(remoteFile.getName()) && remoteFile.getProgress() < 100) {
                        int progress = Integer.valueOf(StringUtils.strip(StringUtils.split(s)[1], "%"));
                        remoteFile.setTransferred(remoteFile.getSize() * (progress / 100l));
                        remoteFile.updateProgress(progress / 100.0);
                    } else if (s.startsWith("Completed")) {
                        new Thread(() ->  {
                            try {
                                log.info(remoteFile.getName() + ":" +  remoteFile.getMd5());
                                if (StringUtils.isNotBlank(remoteFile.getMd5())) {
                                    remoteFile.setSuccessIcon(MD5TableCell.LOADING_ICON);
                                    FileInputStream fis = new FileInputStream(downloadFile);
                                    String md5 = DigestUtils.md5Hex(fis);
                                    fis.close();
                                    if (!StringUtils.equals(md5, remoteFile.getMd5())) {
                                        log.debug("MD5 Error");
                                        remoteFile.updateProgress(0);
                                        remoteFile.setSuccessIcon(MD5TableCell.ERROR_ICON + ":" + "MD5 Checksum verification failed.");
                                        try {
                                            new File(remoteFile.getLocalPath()).delete();
                                        } catch (Exception e) {
                                            log.error("Error deleting failed file:" + remoteFile.getLocalPath());
                                        }
                                        return;
                                    } else {
                                        log.info("calling success after md5:" + remoteFile.getName());
                                        remoteFile.setSuccessIcon(MD5TableCell.SUCCESS_ICON);
                                    }
                                    log.debug("md5 matched");
                                }
                                if (remoteFile.getSize() == 0) {
                                    remoteFile.setSize(downloadFile.length());
                                }
                                remoteFile.updateProgress(1);
                                remoteFile.setDownloaded(true);
                                log.debug("calling success after end:" + remoteFile.getName());
                            } catch (IOException e) {
                                log.error("Error", e);
                            }
                        }).start();

                    }

                }
            }
            reader.close();

        } catch (IOException ex) {
            log.error("IO error", ex);
            throw new AsperaException("Error downloading file: " + ex.getMessage());
        }
    }

}
