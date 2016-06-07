package uk.ac.ebi.ena.ftp.service.ftp;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.*;

import java.io.IOException;
import java.io.InputStream;

/**
 * A utility class that provides functionality for downloading files from a FTP
 * server.
 *
 * @author www.codejava.net
 */
public class FTPUtility {

    // FTP server information
    private String host = "ftp.sra.ebi.ac.uk";
    private int port = 21;
    private String username = "ftp";
    private String password = "1234";

    private FTPClient ftpClient = new FTPClient();
    private int replyCode;

    private InputStream inputStream;

    public FTPUtility() {
        FTPClientConfig conf = new FTPClientConfig(FTPClientConfig.SYST_L8);
        ftpClient.configure(conf);
    }

    /**
     * Connect and login to the server.
     *
     * @throws FTPException
     */
    public void connect() throws FTPException {
        try {
            ftpClient.connect(host);
            ftpClient.setBufferSize(8192);
            replyCode = ftpClient.getReplyCode();
//            System.out.print(ftpClient.getReplyString());
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                throw new FTPException("FTP serve refused connection.");
            }

            if (StringUtils.isNotBlank(username)) {
                boolean logged = ftpClient.login(username, password);
                if (!logged) {
                    // failed to login
                    ftpClient.disconnect();
                    throw new FTPException("Could not login to the server.");
                }
            }
//            System.out.println(ftpClient.getSystemType());
            /*FTPFile[] ftpFiles = ftpClient.listDirectories();
            for (FTPFile ftpFile : ftpFiles) {
                System.out.println(ftpFile.getName());
            }*/

//            ftpClient.enterRemotePassiveMode();

        } catch (IOException ex) {
            ex.printStackTrace();
            throw new FTPException("I/O error: " + ex.getMessage());
        }
    }

/*
    */
/**
     * Gets size (in bytes) of the file on the server.
     *
     * @param filePath Path of the file on server
     * @return file size in bytes
     * @throws FTPException
     *//*

    public long getFileSize(String dir, String filePath) throws FTPException {
        try {
//            System.out.println(StringUtils.join(ftpClient.listNames(), " "));
//            System.out.println(StringUtils.join(ftpClient.listNames(), " "));
            */
/*FTPFile[] ftpFiles = ftpClient.listFiles();
            for (FTPFile ftpFile : ftpFiles) {
                System.out.println(ftpFile.getName());
                if (StringUtils.equals(ftpFile.getName(), filePath)) {
                    return ftpFile.getSize();
                }
            }*//*


//            FTPFile file = ftpClient.mlistFile(filePath);
//            if (file == null) {
//                throw new FTPException("The file may not exist on the server!");
//            }
//            throw new FTPException("The file may not exist on the server!");

            return 0;
        } catch (IOException ex) {
            throw new FTPException("Could not determine size of the file: "
                    + ex.getMessage());
        }
    }
*/

    /**
     * Start downloading a file from the server
     *
     * @param downloadPath Full path of the file on the server
     * @throws FTPException if client-server communication error occurred
     */
    public void getInputStream(String downloadPath) throws FTPException {
        try {

            boolean success = ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            if (!success) {
                throw new FTPException("Could not set binary file type.");
            }

            String path = StringUtils.substringAfter(downloadPath, this.host);
            String dir = StringUtils.substringAfter(StringUtils.substringBeforeLast(path, "/"), "/");
            ftpClient.changeWorkingDirectory(dir);
//            long fileSize = getFileSize(dir, StringUtils.substringAfterLast(path, "/"));
//            System.out.println(fileSize);
            inputStream = ftpClient.retrieveFileStream(path);

            if (inputStream == null) {
                throw new FTPException(
                        "Could not open input stream. The file may not exist on the server.");
            }
        } catch (IOException ex) {
            throw new FTPException("Error downloading file: " + ex.getMessage());
        }
    }

    /**
     * Complete the download operation.
     */
    public void finish() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
        ftpClient.completePendingCommand();
    }

    /**
     * Log out and disconnect from the server
     */
    public void disconnect() throws FTPException {
        if (ftpClient.isConnected()) {
            try {
                if (!ftpClient.logout()) {
                    throw new FTPException("Could not log out from the server");
                }
                ftpClient.disconnect();
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
