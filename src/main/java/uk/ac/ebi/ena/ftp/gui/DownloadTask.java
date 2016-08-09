package uk.ac.ebi.ena.ftp.gui;

import javafx.concurrent.Task;
import uk.ac.ebi.ena.ftp.model.RemoteFile;
import uk.ac.ebi.ena.ftp.service.DownloadService;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Created by suranj on 01/08/2016.
 */
public class DownloadTask extends Task<Void> {
    private final RemoteFile file;
    private DownloadService downloadService = new DownloadService();

    public DownloadTask(RemoteFile file) {
        this.file = file;
    }

    @Override
    protected Void call() throws Exception {
        try {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    try {
                        if (!downloadService.fileAlreadyDownloaded(file)) {
                            downloadService.downloadFileFtp4J(file);
                        } else {
                            file.updateProgress(1);
                            file.setDownloaded(true);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            });
//            file.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
            e.printStackTrace();
        }
    }
}
