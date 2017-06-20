package uk.ac.ebi.ena.ftp.service.ftp;


import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ena.ftp.model.RemoteFile;

import java.io.*;
import java.net.SocketTimeoutException;

/**
 * A utility class that provides functionality for downloading files from a FTP
 * server.
 *
 * @author www.codejava.net
 */
public class CommonsFTPUtility {
    // FTP server information
    public static final int BUFFER_SIZE = 8192; //2097152;// 2MB
    public static final int TIMEOUT = 5000;
    private final static Logger log = LoggerFactory.getLogger(CommonsFTPUtility.class);
    private String host = "ftp.sra.ebi.ac.uk";
    private int port = 21;
    private String username = "anonymous";
    private String password = "1234";

    private FTPClient ftpClient = null;
    private int replyCode;

    private InputStream inputStream;

    /*public FTP4JUtility() {
        FTPClientConfig conf = new FTPClientConfig(FTPClientConfig.SYST_L8);
        ftpClient.configure(conf);
    }*/

    /**
     * Connect and login to the server.
     *
     * @throws FTPException
     */
    public void connect() throws Exception {
        try {
            log.debug("host:" + host);
            ftpClient = new FTPClient();
            ftpClient.setBufferSize(BUFFER_SIZE);
            ftpClient.setDataTimeout(TIMEOUT);
            ftpClient.setConnectTimeout(TIMEOUT);
            ftpClient.setDefaultTimeout(TIMEOUT);

            ftpClient.connect(host);
            log.debug("connected:" + ftpClient.getReplyString());
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                log.error("FTP server refused connection.");
                System.exit(1);
            }
            ftpClient.enterLocalPassiveMode();
            ftpClient.setUseEPSVwithIPv4(true);
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.login(username, password);
            log.debug("logged in:" + ftpClient.getReplyString());

        } catch (IOException ex) {
            log.error("Connection error:", ex);
            throw new FTPException("I/O error: " + ex.getMessage());
        }
    }


    /**
     * Start downloading a file from the server
     *
     * @throws FTPException if client-server communication error occurred
     */
    public synchronized void downloadFile(final RemoteFile remoteFile) throws Exception {
        try {
            final File downloadFile = new File(remoteFile.getSaveLocation() + File.separator + remoteFile.getName());
            log.debug(downloadFile.getAbsolutePath() + ":downloadFile.canWrite():" + downloadFile.canWrite());
            log.debug("remote file:" + remoteFile.getPath());
//            ftpClient.setType(FTPClient.TYPE_AUTO);
            log.debug("files:" + StringUtils.join(ftpClient.listNames(), "\n"));
            String path = StringUtils.substringAfter(remoteFile.getPath(), this.host);
            String dir = StringUtils.substringAfter(StringUtils.substringBeforeLast(path, "/"), "/");
            ftpClient.changeWorkingDirectory(dir);
            log.debug("change dir:" + ftpClient.getReplyString());
            log.debug("files:" + StringUtils.join(ftpClient.listNames(), "\n"));
            log.debug("path:" + path);
            String remoteFileName = StringUtils.substringAfterLast(path, "/");
            log.debug("file:" + remoteFileName);

            CopyStreamListener copyStreamListener = new CopyStreamListener() {

                @Override
                public void bytesTransferred(CopyStreamEvent event) {

                }

                @Override
                public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                    remoteFile.setTransferred(remoteFile.getTransferred() + bytesTransferred);
                    double percentCompleted = (double) remoteFile.getTransferred() / (double) remoteFile.getSize();
                    remoteFile.updateProgress(percentCompleted);
                }
            };
            OutputStream outputStream2 = new BufferedOutputStream(new FileOutputStream(downloadFile));
            ftpClient.setCopyStreamListener(copyStreamListener);
            ftpClient.setRestartOffset(remoteFile.getTransferred());
            boolean completed = ftpClient.retrieveFile(remoteFileName, outputStream2);
            log.debug("input stream:" + ftpClient.getReplyString());
//            boolean success = ftpClient.completePendingCommand();
//            log.debug("completed:" + ftpClient.getReplyString());
            if (completed) {
                log.debug("File #2 has been downloaded successfully.");
            } else {
                remoteFile.updateProgress(0);
            }
            outputStream2.close();
            new Thread() {
                @Override
                public void run() {
                    try {
                        FileInputStream fis = new FileInputStream(downloadFile);
                        String md5 = DigestUtils.md5Hex(fis);
                        fis.close();
                        if (!StringUtils.equals(md5, remoteFile.getMd5())) {
                            log.debug("Error");
                            remoteFile.updateProgress(0);
//                                    remoteFile.cancel(true);
                        } else {
                            remoteFile.updateProgress(1);
                            log.debug("md5 matched");
                            remoteFile.setDownloaded(true);
                        }
                    } catch (IOException e) {
                        log.error("MD5 error", e);
                    }
                }
            }.start();

       /*
            InputStream inputStream = null;
            while (inputStream == null) {
                try {
                    inputStream = ftpClient.retrieveFileStream(path);
                    log.debug("input stream:" + ftpClient.getReplyString());
                } catch (Exception e) {
                    e.log.error();
                    log.debug("waiting 5 sec");
                    wait(5000);
                }
            }
            log.debug("input stream:" + ftpClient.getReplyString());*/
//            Util.copyStream(inputStream, outputStream2, (int) remoteFile.getTransferred(), remoteFile.getSize(), copyStreamListener);

        } catch (SocketTimeoutException ste) {
            throw ste;
        } catch (IOException ex) {
            log.error("Download error", ex);
            throw new FTPException("Error downloading file: " + ex.getMessage());
        } finally {

        }
    }


    /**
     * Log out and disconnect from the server
     */
    public void disconnect() throws Exception {
        if (ftpClient != null && ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                log.debug("logged out:" + ftpClient.getReplyString());
                ftpClient.disconnect();
                log.debug("disconnected:" + ftpClient.getReplyString());
            } catch (IOException ex) {
                log.error("Disconnect error", ex);
                throw new FTPException("Error disconnect from the server: "
                        + ex.getMessage());
            }
        }
    }

    public void abortDownload() throws Exception {
        new Thread() {
            @Override
            public void run() {
                try {
                    int count = 0;
                    count = 0;
                    while (count < 5) {
                        try {
                            if (ftpClient != null) {
                                ftpClient.abort();
                                log.debug("aborted:" + ftpClient.getReplyString());
                            }
                            break;
                        } catch (SocketTimeoutException ste) {
                            count++;
                            log.warn("aborted: timeout:" + count);
                        }
                    }
                } catch (Exception e) {
                    log.error("Abort fault:" + e.getMessage());
//            throw e;
                }
            }
        }.start();
    }
}
