package uk.ac.ebi.ena.downloader.gui;

import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ena.downloader.gui.custom.MD5TableCell;
import uk.ac.ebi.ena.downloader.model.DownloadSettings;
import uk.ac.ebi.ena.downloader.model.RemoteFile;
import uk.ac.ebi.ena.downloader.service.DownloadService;

import java.io.File;
import java.util.concurrent.CountDownLatch;

/**
 * Created by suranj on 01/08/2016.
 */
public class DownloadTask extends Task<Void> {
    private final static int RETRY_COUNT = 5;
    private final static Logger log = LoggerFactory.getLogger(DownloadTask.class);

    private final RemoteFile file;
    private CountDownLatch latch;
    private DownloadSettings downloadSettings;
    private DownloadService downloadService = new DownloadService();
    private boolean countedDown;

    public DownloadTask(RemoteFile file, CountDownLatch latch, DownloadSettings downloadSettings) {
        this.file = file;
        this.latch = latch;
        this.downloadSettings = downloadSettings;
    }

    @Override
    protected Void call() throws Exception {
        try {

            try {
                if (!downloadService.fileAlreadyDownloaded(file)) {
                    int count = 0;
                    while (count < RETRY_COUNT) {
                        try {
                            if (downloadSettings.getMethod() == DownloadSettings.Method.FTP) {
                                downloadService.downloadFileFtp(file);
                            } else {
                                downloadService.downloadFileAspera(file, downloadSettings);
                            }
                            break;
                        } catch (Exception e) {
                            count++;
                            log.warn(file.getName() + " Timed out download attempt. Retry:" + count);
                            if (RETRY_COUNT == count) {
                                throw e;
                            }
                        }
                    }
                }
                if (file.getSize() == file.getTransferred()) {
                    file.updateProgress(1);
                    log.debug("calling success:" + file.getName());
                    file.setDownloaded(true);
                    succeeded();
                }

            } catch (Exception e) {
                log.error("Failed download", e);
                throw e;
            }
            return null;

        } catch (Exception e) {
            log.error("Failed download", e);
            file.updateProgress(0);
            file.setSuccessIcon(MD5TableCell.ERROR_ICON + ":" + e.getMessage());
            try {
                new File(file.getLocalPath()).delete();
            } catch (Exception ex) {
                log.error("Error deleting failed file:" + file.getLocalPath());
            }
            this.failed();
            throw e;
        } finally {
            countdownLatch();
        }
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        try {
            downloadService.abortDownload();
        } catch (Exception e) {
            log.error("Failed cancel:", e);
        } finally {
            countdownLatch();
        }
    }

    private void countdownLatch() {
        if (!countedDown) {
            countedDown = true;
            try {
                latch.countDown();
            } catch (Exception e) {
                log.error("latch error", e);
            }
        }
    }


}
