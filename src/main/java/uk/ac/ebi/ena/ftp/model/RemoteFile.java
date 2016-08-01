package uk.ac.ebi.ena.ftp.model;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import uk.ac.ebi.ena.ftp.gui.Controller;
import uk.ac.ebi.ena.ftp.service.DownloadService;

import java.util.List;

/**
 * Created by suranj on 27/05/2016.
 */
public class RemoteFile {
    private SimpleBooleanProperty download;
    private String name;
    private long size, transferred=0;
    private String path;

    private String saveLocation;
    private String md5;
    private DownloadService downloadService;
    private List<RemoteFile> fileList;
    private Controller controller;
    private DoubleProperty progress;
    private boolean downloaded;


    public RemoteFile(String name, long size, String path, String md5) {
        this.download = new SimpleBooleanProperty(false);
        this.name = name;
        this.size = size;
        this.path = path;
        this.md5 = md5;
        this.progress = new SimpleDoubleProperty(0);
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


    /*@Override
    protected Void call() throws Exception {

    }


*/
    public void updateProgress(final double percentCompleted) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                progress.setValue(percentCompleted);
            }
        });
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

    public void setFileList(List<RemoteFile> fileList) {
        this.fileList = fileList;
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public double getProgress() {
        return progress.get();
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress.set(progress);
    }
}
