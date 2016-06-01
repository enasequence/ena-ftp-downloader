package uk.ac.ebi.ena.ftp.service;

import org.apache.commons.lang3.StringUtils;
import uk.ac.ebi.ena.ftp.model.RemoteFile;
import uk.ac.ebi.ena.ftp.service.ftp.FTPException;
import uk.ac.ebi.ena.ftp.service.ftp.FTPUtility;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Created by suranj on 31/05/2016.
 */
public class DownloadService {

    private static final int BUFFER_SIZE = 4096;

    private String host;
    private int port;
    private String username;
    private String password;

    public Void downloadFile(RemoteFile remoteFile) throws Exception {
        FTPUtility util = new FTPUtility(host, port, username, password);
        try {
            util.connect();

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = -1;
            long totalBytesRead = 0;
            int percentCompleted = 0;

            String fileName = remoteFile.getName();

            File downloadFile = new File(remoteFile.getSaveLocation() + File.separator + fileName);
            FileOutputStream outputStream = new FileOutputStream(downloadFile);

            util.downloadFile(remoteFile.getPath());
            InputStream inputStream = util.getInputStream();

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                percentCompleted = (int) (totalBytesRead * 100 / remoteFile.getSize());
                remoteFile.updateProgress(percentCompleted);
            }

            outputStream.close();

            util.finish();
        } catch (FTPException ex) {
            ex.printStackTrace();
            remoteFile.updateProgress(0);
            remoteFile.cancel(true);
        } finally {
            util.disconnect();
        }

        return null;
    }
}
