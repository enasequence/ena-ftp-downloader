package uk.ac.ebi.ena.ftp.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import uk.ac.ebi.ena.ftp.service.DownloadService;

/**
 * Created by suranj on 27/05/2016.
 */
public class RemoteFile extends Task<Void> {
    private SimpleBooleanProperty download;
    private String name;
    private long size, transferred=0;
    private String path;

    private String saveLocation;
    private String md5;


    public RemoteFile(String name, long size, String path, String md5) {
        this.download = new SimpleBooleanProperty(false);
        this.name = name;
        this.size = size;
        this.path = path;
        this.md5 = md5;
        this.updateProgress(0);
    }


    public SimpleBooleanProperty isDownload() {
        return download;
    }

    public void setDownload(SimpleBooleanProperty download) {
        this.download = download;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public BooleanProperty downloadProperty() {
        return download;
    }


    public static final int NUM_ITERATIONS = 100;


    @Override
    protected Void call() throws Exception {
        DownloadService downloadService = new DownloadService();
        this.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, 1);
        this.updateMessage("Waiting...");
        this.updateMessage("Running...");
        if (!downloadService.fileAlreadyDownloaded(this)) {
            downloadService.downloadFileFtp4J(this);
        }
        this.updateMessage("Done");
        this.updateProgress(1, 1);
        return null;
    }

    public void updateProgress(int percentCompleted) {
        this.updateProgress(percentCompleted, 100);
    }

    public void setSaveLocation(String saveLocation) {
        this.saveLocation = saveLocation;
    }

    public String getSaveLocation() {
        return saveLocation;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public long getTransferred() {
        return transferred;
    }

    public void setTransferred(long transferred) {
        this.transferred = transferred;
    }
}
