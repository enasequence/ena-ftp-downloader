package uk.ac.ebi.ena.ftp.service.ftp;


import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferListener;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import uk.ac.ebi.ena.ftp.model.RemoteFile;

import java.io.*;

/**
 * A utility class that provides functionality for downloading files from a FTP
 * server.
 *
 * @author www.codejava.net
 */
public class FTP4JUtility {

    // FTP server information
    private String host = "ftp.sra.ebi.ac.uk";
    private int port = 21;
    private String username = "anonymous";
    private String password = "1234";

    private FTPClient ftpClient = new FTPClient();
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
            ftpClient.connect(host);
            ftpClient.login(username, password);

        } catch (IOException ex) {
            ex.printStackTrace();
            throw new FTPException("I/O error: " + ex.getMessage());
        }
    }



    /**
     * Start downloading a file from the server
     *
     * @throws FTPException if client-server communication error occurred
     */
    public void downloadFile(final RemoteFile remoteFile) throws Exception {
        try {

            final File downloadFile = new File(remoteFile.getSaveLocation() + File.separator + remoteFile.getName());
            final BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(downloadFile));

            ftpClient.setType(FTPClient.TYPE_AUTO);
            String path = StringUtils.substringAfter(remoteFile.getPath(), this.host);
            String dir = StringUtils.substringAfter(StringUtils.substringBeforeLast(path, "/"), "/");
            ftpClient.changeDirectory(dir);
//            long fileSize = getFileSize(dir, StringUtils.substringAfterLast(path, "/"));
//            System.out.println(fileSize);
            ftpClient.download(path, outputStream, 0, new FTPDataTransferListener() {

                @Override
                public void started() {
                }

                @Override
                public void transferred(int i) {
                    remoteFile.setTransferred(remoteFile.getTransferred() + i);
                    int percentCompleted = (int) (remoteFile.getTransferred() * 100 / remoteFile.getSize());
                    remoteFile.updateProgress(percentCompleted);
                }

                @Override
                public void completed() {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    new Thread() {
                        @Override
                        public void run() {
                            try {
                                FileInputStream fis = new FileInputStream(downloadFile);
                                String md5 = DigestUtils.md5Hex(fis);
                                fis.close();
                                if (!StringUtils.equals(md5, remoteFile.getMd5())) {
                                    System.out.println("Error");
                                    remoteFile.updateProgress(0);
                                    remoteFile.cancel(true);
                                } else {
                                    System.out.println("md5 matched");
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }

                @Override
                public void aborted() {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    remoteFile.updateProgress(0);
                    remoteFile.cancel(true);
                }

                @Override
                public void failed() {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    remoteFile.updateProgress(0);
                    remoteFile.cancel(true);
                }
            });

        } catch (IOException ex) {
            throw new FTPException("Error downloading file: " + ex.getMessage());
        }
    }


    /**
     * Log out and disconnect from the server
     */
    public void disconnect() throws Exception {
        if (ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect(false);
            } catch (IOException ex) {
                throw new FTPException("Error disconnect from the server: "
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
}
