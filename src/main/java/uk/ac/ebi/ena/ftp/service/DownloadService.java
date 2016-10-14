package uk.ac.ebi.ena.ftp.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import uk.ac.ebi.ena.ftp.model.RemoteFile;
import uk.ac.ebi.ena.ftp.service.ftp.CommonsFTPUtility;
import uk.ac.ebi.ena.ftp.service.ftp.FTP4JUtility;

import java.io.File;
import java.io.FileInputStream;

/**
 * Created by suranj on 31/05/2016.
 */
public class DownloadService {

    private final static Logger log = Logger.getLogger(CommonsFTPUtility.class);

    private CommonsFTPUtility util = new CommonsFTPUtility();

    public Void downloadFileFtp(final RemoteFile remoteFile) throws Exception {
        util.connect();
        util.downloadFile(remoteFile);
        util.disconnect();
        log.debug(remoteFile.getName() + " download completed.");
        log.debug("end");
        return null;
    }

    public boolean fileAlreadyDownloaded(RemoteFile remoteFile) throws Exception {
        final File downloadFile = new File(remoteFile.getSaveLocation() + File.separator + remoteFile.getName());
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
                return true;
            }
//            } catch (IOException e) {
//                e.log.error();
//            }
        }
        return false;
    }

    public void abortDownload() throws Exception {
        try {
            util.abortDownload();
        } catch (Exception e) {
            log.debug("Aborted:" + e.getMessage());
        }
    }
}
