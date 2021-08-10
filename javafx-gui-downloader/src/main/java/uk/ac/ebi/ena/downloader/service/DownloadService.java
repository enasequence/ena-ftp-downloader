package uk.ac.ebi.ena.downloader.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ena.downloader.gui.custom.MD5TableCell;
import uk.ac.ebi.ena.downloader.model.DownloadSettings;
import uk.ac.ebi.ena.downloader.model.RemoteFile;
import uk.ac.ebi.ena.downloader.service.aspera.AsperaUtility;
import uk.ac.ebi.ena.downloader.service.ftp.CommonsFTPUtility;
import uk.ac.ebi.ena.downloader.service.ftp.FTP4JUtility;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;

/**
 * Created by suranj on 31/05/2016.
 */
public class DownloadService {

    private final static Logger log = LoggerFactory.getLogger(CommonsFTPUtility.class);

    private FTP4JUtility util = new FTP4JUtility();
    private AsperaUtility asperaUtility = new AsperaUtility();

    public Void downloadFileFtp(final RemoteFile remoteFile) throws Exception {
        util.connect();
        util.downloadFile(remoteFile);
        util.disconnect();
        log.debug(remoteFile.getName() + " download completed.");
        log.debug("end");
        return null;
    }

    public Void downloadFileAspera(final RemoteFile remoteFile, DownloadSettings downloadSettings) throws Exception {
        asperaUtility.downloadFile(remoteFile, downloadSettings);
        log.debug(remoteFile.getName() + " download completed.");
        log.debug("end");
        return null;
    }

    public boolean fileAlreadyDownloaded(RemoteFile remoteFile) throws Exception {
        final File downloadFile = new File(remoteFile.getSaveLocation() + File.separator + URLEncoder.encode(remoteFile.getName(), "UTF-8"));
        if (downloadFile.exists()) {
//            try {
            FileInputStream fis = new FileInputStream(downloadFile);
            String md5 = DigestUtils.md5Hex(fis);
            fis.close();
            if (!StringUtils.equals(md5, remoteFile.getMd5())) {
                if (remoteFile.getTransferred() == 0) {
                    // old file fragment
                    downloadFile.delete();
                    return false;
                } else {
                    // resume
                    log.debug("Resuming download.");
                    return false;
                }
            } else {
                log.debug("Existing file md5 matched");
                remoteFile.setSuccessIcon(MD5TableCell.SUCCESS_ICON);
                if (remoteFile.getSize() == 0) {
                    remoteFile.setSize(downloadFile.length());
                    remoteFile.setTransferred(downloadFile.length());
                }
                return true;
            }
        }
        return false;
    }

    public void abortDownload() throws Exception {
        try {
            util.abortDownload();
        } catch (Exception e) {
            log.debug("Aborted:" + e.getMessage());
        } finally {
            util.disconnect();
        }
    }
}
