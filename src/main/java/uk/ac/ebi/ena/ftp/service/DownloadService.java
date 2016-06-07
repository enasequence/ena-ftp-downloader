package uk.ac.ebi.ena.ftp.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import uk.ac.ebi.ena.ftp.model.RemoteFile;
import uk.ac.ebi.ena.ftp.service.ftp.FTPException;
import uk.ac.ebi.ena.ftp.service.ftp.FTP4JUtility;
import uk.ac.ebi.ena.ftp.service.ftp.FTPUtility;

import java.io.*;
import java.util.List;

/**
 * Created by suranj on 31/05/2016.
 */
public class DownloadService {

    private static final int BUFFER_SIZE = 8192; //2097152;// 2MB

    public Void downloadFile(final RemoteFile remoteFile) throws Exception {
        FTPUtility util = new FTPUtility();
        BufferedOutputStream outputStream = null;
        try {
            util.connect();

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead = -1;
            long totalBytesRead = 0;
            int percentCompleted = 0;

            String fileName = remoteFile.getName();

            final File downloadFile = new File(remoteFile.getSaveLocation() + File.separator + fileName);
            outputStream = new BufferedOutputStream(new FileOutputStream(downloadFile));

            util.getInputStream(remoteFile.getPath());
            BufferedInputStream inputStream = new BufferedInputStream(util.getInputStream());

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
                percentCompleted = (int) (totalBytesRead * 100 / remoteFile.getSize());
                remoteFile.updateProgress(percentCompleted);
            }
            outputStream.close();
            util.finish();
            util.disconnect();
            System.out.println(remoteFile.getName() + " download completed.");

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
            System.out.println("end");

        } catch (FTPException ex) {
            ex.printStackTrace();
            remoteFile.updateProgress(0);
            remoteFile.cancel(true);
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }

        }

        return null;
    }

    public Void downloadFileFtp4J(final RemoteFile remoteFile) throws Exception {
        FTP4JUtility util = new FTP4JUtility();
        try {
            util.connect();

            util.downloadFile(remoteFile);
            util.disconnect();
            System.out.println(remoteFile.getName() + " download completed.");

            System.out.println("end");

        } catch (FTPException ex) {
            ex.printStackTrace();

        }

        return null;
    }

    public boolean fileAlreadyDownloaded(RemoteFile remoteFile) {
        final File downloadFile = new File(remoteFile.getSaveLocation() + File.separator + remoteFile.getName());
        if (downloadFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(downloadFile);
                String md5 = DigestUtils.md5Hex(fis);
                fis.close();
                if (!StringUtils.equals(md5, remoteFile.getMd5())) {
                    downloadFile.delete();
                    return false;
                } else {
                    System.out.println("Existing file md5 matched");
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
