package uk.ac.ebi.ena.ftp.service.ftp;


import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferListener;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ena.ftp.gui.custom.MD5TableCell;
import uk.ac.ebi.ena.ftp.model.RemoteFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A utility class that provides functionality for downloading files from a FTP
 * server.
 *
 * @author www.codejava.net
 */
public class FTP4JUtility {

    private final static Logger log = LoggerFactory.getLogger(FTP4JUtility.class);

    // FTP server information
    private String host = "ftp.sra.ebi.ac.uk";
    private int port = 21;
    private String username = "anonymous";
    private String password = "1234";

    private FTPClient ftpClient;
    private int replyCode;

    private InputStream inputStream;

    public FTP4JUtility() {
        ftpClient = new FTPClient();
    }

    /**
     * Connect and login to the server.
     *
     * @throws FTPException
     */
    public void connect() throws Exception {
//        try {
        log.debug("host:" + host);
        if (ftpClient.isConnected() && ftpClient.isAuthenticated()) {
            log.info("Client is aleady connected.");
            return;
        }
        String[] connect = ftpClient.connect(host);
        log.debug("connected:" + StringUtils.join(connect));
        ftpClient.login(username, password);
        log.debug("logged in");
    }


    /**
     * Start downloading a file from the server
     *
     * @throws FTPException if client-server communication error occurred
     */
    public void downloadFile(final RemoteFile remoteFile) throws Exception {
        try {

            final File downloadFile = new File(remoteFile.getSaveLocation() + File.separator + remoteFile.getName());
            log.debug(downloadFile.getAbsolutePath() + ":downloadFile.canWrite():" + downloadFile.canWrite());

            ftpClient.setType(FTPClient.TYPE_AUTO);
            String path = StringUtils.substringAfter(remoteFile.getPath(), this.host);
            String dir = StringUtils.substringAfter(StringUtils.substringBeforeLast(path, "/"), "/");
            ftpClient.changeDirectory(dir);
//            long fileSize = getFileSize(dir, StringUtils.substringAfterLast(path, "/"));
//            log.debug(fileSize);
            log.debug("path:" + path);
            remoteFile.setLocalPath(downloadFile.getAbsolutePath());
            ftpClient.download(path, downloadFile, remoteFile.getTransferred(), new FTPDataTransferListener() {
                @Override
                public void started() {
                }

                @Override
                public void transferred(int i) {
                    if (remoteFile.getSize() > 0) {
                        remoteFile.setTransferred(remoteFile.getTransferred() + i);
                        double percentCompleted = (double) remoteFile.getTransferred() / (double) remoteFile.getSize();
                        remoteFile.updateProgress(percentCompleted);
                    }
                }

                @Override
                public void completed() {
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                if (StringUtils.isNotBlank(remoteFile.getMd5())) {
                                    remoteFile.setSuccessIcon(MD5TableCell.LOADING_ICON);
                                    FileInputStream fis = new FileInputStream(downloadFile);
                                    String md5 = DigestUtils.md5Hex(fis);
                                    fis.close();
                                    if (!StringUtils.equals(md5, remoteFile.getMd5())) {
                                        log.debug("MD5 Error");
                                        remoteFile.updateProgress(0);
                                        remoteFile.setSuccessIcon(MD5TableCell.ERROR_ICON);
                                        try {
                                            new File(remoteFile.getLocalPath()).delete();
                                        } catch (Exception e) {
                                            log.error("Error deleting failed file:" + remoteFile.getLocalPath());
                                        }
                                        return;
                                    } else {
                                        log.debug("calling success after md5:" + remoteFile.getName());
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
                                remoteFile.setSuccessIcon(MD5TableCell.SUCCESS_ICON);
                            } catch (IOException e) {
                                log.error("Error", e);
                            }
                        }
                    }.start();
                }

                @Override
                public void aborted() {
                    log.debug(remoteFile.getPath() + " aborted at " + remoteFile.getTransferred());
//                    remoteFile.updateProgress(0);
//                    remoteFile.cancel(true);
                }

                @Override
                public void failed() {
                    remoteFile.updateProgress(0);
                    remoteFile.setSuccessIcon(MD5TableCell.ERROR_ICON);
                    try {
                        new File(remoteFile.getLocalPath()).delete();
                    } catch (Exception e) {
                        log.error("Error deleting failed file:" + remoteFile.getLocalPath());
                    }
                    disconnect();
//                    remoteFile.cancel(true);
                }
            });

        } catch (IOException ex) {
            log.error("IO error", ex);
            throw new FTPException("Error downloading file: " + ex.getMessage());
        } catch (FTPAbortedException e) {
            log.debug("Data transfer aborted.");
        }
    }


    /**
     * Log out and disconnect from the server
     */
    public void disconnect() {
        if (ftpClient.isConnected()) {
            try {
                try {
                    ftpClient.logout();
                } catch (it.sauronsoftware.ftp4j.FTPException e) {
                }
                ftpClient.disconnect(false);
            } catch (Exception ex) {
                log.error("Error disconnecting from the server: "
                        + ex.getMessage());
            }
        }
    }

    /**
     * Return InputStream of the remote file on the server.
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    public void abortDownload() throws Exception {
        try {
            ftpClient.abortCurrentDataTransfer(true);
        } catch (Exception e) {
            throw e;
        }
    }
}
