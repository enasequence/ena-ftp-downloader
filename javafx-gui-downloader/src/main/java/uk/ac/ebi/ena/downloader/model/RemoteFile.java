/*
 * Copyright (c) 2017  EMBL-EBI.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package uk.ac.ebi.ena.downloader.model;

import javafx.application.Platform;
import javafx.beans.property.*;
import org.apache.commons.lang3.StringUtils;
import uk.ac.ebi.ena.downloader.utils.Utils;

/**
 * Created by suranj on 27/05/2016.
 */
public class RemoteFile {
    private SimpleBooleanProperty download;
    private String name;
    private long size, transferred = 0;
    private StringProperty hrSize;

    private String path;
    private String accession;
    private String saveLocation;
    private String md5;
    private DoubleProperty progress;
    private boolean downloaded;
    private StringProperty successIcon;
    private String localPath;


    public RemoteFile(String name, long size, String path, String md5, String accession) {
        this.download = new SimpleBooleanProperty(false);
        this.name = name;
        this.size = size;
        if (size == 0) {
            this.hrSize = new SimpleStringProperty("N/A");
        } else {
            this.hrSize = new SimpleStringProperty(Utils.getHumanReadableSize(size));
        }
        this.path = path;
        this.md5 = md5;
        if (StringUtils.isBlank(md5)) {
            this.successIcon = new SimpleStringProperty("N/A");
        } else {
            this.successIcon = new SimpleStringProperty();
        }
        this.progress = new SimpleDoubleProperty(0);
        this.accession = accession;
    }


    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
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
        setHrSize(Utils.getHumanReadableSize(size));
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

    public void updateProgress(final double percentCompleted) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                progress.setValue(percentCompleted);
            }
        });
    }

    public String getSaveLocation() {
        return saveLocation;
    }

    public void setSaveLocation(String saveLocation) {
        this.saveLocation = saveLocation;
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

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public double getProgress() {
        return progress.get();
    }

    public void setProgress(double progress) {
        this.progress.set(progress);
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public StringProperty successIconProperty() {
        return successIcon;
    }

    public void setSuccessIcon(String icon) {
        Platform.runLater(() -> this.successIcon.set(icon));
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }


    public String getHrSize() {
        return hrSize.get();
    }

    public void setHrSize(String hrSize) {
        this.hrSize.set(hrSize);
    }

    public StringProperty hrSizeProperty() {
        return hrSize;
    }

    @Override
    public String toString() {
        return "RemoteFile{" +
                "download=" + download +
                ", name='" + name + '\'' +
                ", size=" + size +
                ", transferred=" + transferred +
                ", hrSize=" + hrSize +
                ", path='" + path + '\'' +
                ", accession='" + accession + '\'' +
                ", saveLocation='" + saveLocation + '\'' +
                ", md5='" + md5 + '\'' +
                ", progress=" + progress +
                ", downloaded=" + downloaded +
                ", successIcon=" + successIcon +
                ", localPath='" + localPath + '\'' +
                '}';
    }

}
