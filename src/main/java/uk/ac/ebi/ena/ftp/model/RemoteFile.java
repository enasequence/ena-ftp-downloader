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
    private long size;
    private String path;

    private int waitTime = 100; // milliseconds
    private int pauseTime = 100; // milliseconds
    private String saveLocation;


    public RemoteFile(String name, long size, String path) {
        this.download = new SimpleBooleanProperty(false);
        this.name = name;
        this.size = size;
        this.path = path;
    }

    public RemoteFile(int waitTime, int pauseTime) {
        this.waitTime = waitTime;
        this.pauseTime = pauseTime;
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
        downloadService.downloadFile(this);
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
}
