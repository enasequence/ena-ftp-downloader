package uk.ac.ebi.ena.ftp.gui;

import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ena.ftp.gui.custom.MD5TableCell;
import uk.ac.ebi.ena.ftp.model.RemoteFile;
import uk.ac.ebi.ena.ftp.service.DownloadService;

import java.io.File;

/**
 * Created by suranj on 01/08/2016.
 */
public class DownloadTask extends Task<Void> {
    private final static int RETRY_COUNT = 10;
    private final static Logger log = LoggerFactory.getLogger(DownloadTask.class);

    private final RemoteFile file;
    private DownloadService downloadService = new DownloadService();

    public DownloadTask(RemoteFile file) {
        this.file = file;
    }

    @Override
    protected Void call() throws Exception {
        try {
            /*AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {*/
                    try {
                        if (!downloadService.fileAlreadyDownloaded(file)) {
                            int count = 0;
                            while (count < RETRY_COUNT) {
                                try {
                                    downloadService.downloadFileFtp(file);
                                    break;
                                } catch (Exception e) {
                                    count++;
                                    log.warn(file.getName() + " Timed out download attempt. Retry:" + count);
                                    if (RETRY_COUNT == count) {
                                        throw e;
                                    }
                                }
                            }
                        } else {
                            file.updateProgress(1);
                            file.setSuccessIcon(MD5TableCell.SUCCESS_ICON);
                            file.setDownloaded(true);
                        }
                    } catch (Exception e) {
                        log.error("Failed download", e);
                        throw  e;
                    }
                    return null;
//                }
//            });
//            file.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        } catch (Exception e) {
            log.error("Failed download", e);
            file.updateProgress(0);
            file.setSuccessIcon(MD5TableCell.ERROR_ICON);
            try {
                new File(file.getLocalPath()).delete();
            } catch (Exception ex) {
                log.error("Error deleting failed file:" + file.getLocalPath());
            }
            throw e;
        }
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        try {
            double progress = this.getProgress();
//            if (!file.isDownloaded()) {
            downloadService.abortDownload();
//                File downloadFile = new File(getSaveLocation() + File.separator + getName());
//                boolean deleted = downloadFile.delete();
//                if (!deleted) {
//                    downloadFile.deleteOnExit();
//                }
//            } else if (progress < 0) {
//                this.updateProgress(0);
//            }
        } catch (Exception e) {
            log.error("Failed cancel:", e);
        }
    }
}
