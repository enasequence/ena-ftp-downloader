
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

package uk.ac.ebi.ena.downloader.gui;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ena.downloader.gui.custom.MD5TableCell;
import uk.ac.ebi.ena.downloader.gui.custom.ProgressBarTableCell;
import uk.ac.ebi.ena.downloader.model.DownloadSettings;
import uk.ac.ebi.ena.downloader.model.Images;
import uk.ac.ebi.ena.downloader.model.RemoteFile;
import uk.ac.ebi.ena.downloader.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static uk.ac.ebi.ena.downloader.utils.Utils.UNITS;

public class ResultsController implements Initializable {

    private final static Logger log = LoggerFactory.getLogger(ResultsController.class);
    public static final int SIZE_COLUMN_INDEX = 3;
    public static final int DOWNLOAD_COLUMN_INDEX = 0;
    public static final int MD5_COLUMN_INDEX = 5;
    public static final int PROGRESS_COLUMN_INDEX = 4;

    @FXML
    private TextField localDownloadDir;

    @FXML
    private Button localDownloadDirBtn, selectAllBtn, startDownloadBtn, stopDownloadBtn, backBtn;

    @FXML
    private TableView<RemoteFile> fastqFileTable, submittedFileTable, sraFileTable;

    @FXML
    private Label selectionLabel;

    @FXML
    private TabPane fileTabPane;

    @FXML
    private CheckBox accSubCheckBox;

    @FXML
    private Tab fastqTab, submittedTab, sraTab;

    @FXML
    private ImageView labelImage;

    @FXML
    private ObservableList<RemoteFile> fastqFiles, submittedFiles, sraFiles;

    private List<RemoteFile> notDoneFiles;
    private List<Task> downloadTasks;
    private ExecutorService executor;
    private ResultsController self = this;
    private long totalSize;
    private Scene searchScene;
    private SearchController searchController;
    private Stage stage;
    private DownloadSettings downloadSettings;


    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        log.debug("initialize");
        assert startDownloadBtn != null : "fx:id=\"startDownloadBtn\" was not injected: check your FXML file 'results.fxml'.";

    }

    public void renderResults(Map<String, List<RemoteFile>> results, DownloadSettings downloadSettings) {
        this.downloadSettings = downloadSettings;
        setupDownloadDirBtn();
        setupTables(results);
        setupSelectAllBtn();
        setupBackBtn();
        setupDownloadButtons();
    }

    private void setupTables(Map<String, List<RemoteFile>> fileListMap) {

        boolean tabHasFiles = false;
        List<RemoteFile> queryFastq = fileListMap.get("fastq");
        if (queryFastq != null && queryFastq.size() > 0) {
            fastqFiles = FXCollections.observableArrayList(new Callback<RemoteFile, Observable[]>() {
                @Override
                public Observable[] call(RemoteFile param) {
                    return new Observable[]{param.downloadProperty()};
                }
            });
            fastqFiles.addAll(queryFastq);
            setupTable(fastqFileTable, fastqFiles);
            fastqTab.setDisable(false);
            tabHasFiles = true;
            fileTabPane.getSelectionModel().select(fastqTab);
        } else {
            fastqTab.setDisable(true);
        }
        List<RemoteFile> querySubmitted = fileListMap.get("submitted");
        if (querySubmitted != null && querySubmitted.size() > 0) {
            submittedFiles = FXCollections.observableArrayList(new Callback<RemoteFile, Observable[]>() {
                @Override
                public Observable[] call(RemoteFile param) {
                    return new Observable[]{param.downloadProperty()};
                }
            });
            submittedFiles.addAll(querySubmitted);
            setupTable(submittedFileTable, submittedFiles);
            submittedTab.setDisable(false);
            if (!tabHasFiles) {
                tabHasFiles = true;
                fileTabPane.getSelectionModel().select(submittedTab);
            }
        } else {
            submittedTab.setDisable(true);
        }
        List<RemoteFile> querySra = fileListMap.get("sra");
        if (querySra != null && querySra.size() > 0) {
            sraFiles = FXCollections.observableArrayList(new Callback<RemoteFile, Observable[]>() {
                @Override
                public Observable[] call(RemoteFile param) {
                    return new Observable[]{param.downloadProperty()};
                }
            });
            sraFiles.addAll(querySra);
            setupTable(sraFileTable, sraFiles);
            sraTab.setDisable(false);
            if (!tabHasFiles) {
                tabHasFiles = true;
                fileTabPane.getSelectionModel().select(sraTab);
            }
        } else {
            sraTab.setDisable(true);
        }
    }

    /*private void clearTables() {
        if (fastqFiles != null) {
            fastqFiles.removeAll();
            fastqFileTable.refresh();
        }
        if (submittedFiles != null) {
            submittedFiles.removeAll();
            submittedFileTable.refresh();
        }
        if (sraFiles != null) {
            sraFiles.removeAll();
        }
    }
*/
    private void setupTable(TableView<RemoteFile> tableView, ObservableList<RemoteFile> tableFiles) {

        ObservableList<TableColumn<RemoteFile, ?>> columns =
                tableView.getColumns();
        TableColumn<RemoteFile, Boolean> downloadColumn = (TableColumn<RemoteFile, Boolean>) columns.get(DOWNLOAD_COLUMN_INDEX);
        downloadColumn.setCellValueFactory(
                new Callback<TableColumn.CellDataFeatures<RemoteFile, Boolean>, ObservableValue<Boolean>>() {
                    @Override
                    public ObservableValue<Boolean> call(TableColumn.CellDataFeatures<RemoteFile, Boolean> p) {
                        return p.getValue().isDownload();
                    }
                });
        downloadColumn.setCellFactory(
                new Callback<TableColumn<RemoteFile, Boolean>, TableCell<RemoteFile, Boolean>>() {
                    @Override
                    public TableCell<RemoteFile, Boolean> call(TableColumn<RemoteFile, Boolean> p) {
                        CheckBoxTableCell<RemoteFile, Boolean> checkBoxTableCell = new CheckBoxTableCell<RemoteFile, Boolean>();
                        return checkBoxTableCell;
                    }
                });
        downloadColumn.setEditable(true);

        setupSizeColumn(tableView);

        final ObservableList<RemoteFile> finalTableFiles = tableFiles;// copying to final var use inside inner class
        tableFiles.addListener(new ListChangeListener<RemoteFile>() {
            @Override
            public void onChanged(Change<? extends RemoteFile> change) {
                updateSelectionMessage();
            }
        });
        tableView.setItems(tableFiles);
        addProgressColumn(tableView);
        addIconColumn(tableView);

        tableView.setOnScroll(new EventHandler() {
            @Override
            public void handle(Event event) {
                tableView.refresh();
            }

        });
        tableView.setOnScrollFinished(new EventHandler() {
            @Override
            public void handle(Event event) {
                tableView.refresh();
            }

        });
    }


    private void setupSizeColumn(TableView<RemoteFile> tableView) {
        TableColumn<RemoteFile, String> sizeColumn = (TableColumn<RemoteFile, String>) tableView.getColumns().get(SIZE_COLUMN_INDEX);
        PropertyValueFactory<RemoteFile, String> size = new PropertyValueFactory<>("hrSize");
        sizeColumn.setCellValueFactory(size);
        sizeColumn.setComparator(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int unit1 = ArrayUtils.indexOf(UNITS, StringUtils.substringAfterLast(o1, " "));
                int unit2 = ArrayUtils.indexOf(UNITS, StringUtils.substringAfterLast(o2, " "));

                if (unit1 == unit2 && unit1 != -1) {
                    int size1 = (int) ((Float.parseFloat(StringUtils.replace(StringUtils.substringBeforeLast(o1, " "), ",", ""))) * 100);
                    int size2 = (int) (Float.parseFloat(StringUtils.replace(StringUtils.substringBeforeLast(o2, " "), ",", "")) * 100);
                    return (size1 - size2);
                }
                return unit1 - unit2;
            }
        });
    }

    private void addIconColumn(TableView<RemoteFile> tableView) {
        ObservableList columns = tableView.getColumns();

        if (columns.size() < 6) {
            TableColumn<RemoteFile, String> iconCol = new TableColumn<>("MD5 OK");
            iconCol.setPrefWidth(60);
            iconCol.setResizable(false);
            PropertyValueFactory<RemoteFile, String> successIcon = new PropertyValueFactory<>("successIcon");
            iconCol.setCellValueFactory(successIcon);
            iconCol.setCellFactory(MD5TableCell.<RemoteFile>forTableColumn());
            columns.add(MD5_COLUMN_INDEX, iconCol);
        }
    }

    private void addProgressColumn(TableView<RemoteFile> tableView) {
        ObservableList columns = tableView.getColumns();
        if (columns.size() < MD5_COLUMN_INDEX) {

            TableColumn<RemoteFile, Double> progressCol = new TableColumn<>("Progress");
            progressCol.setPrefWidth(295);
            progressCol.setResizable(false);
            PropertyValueFactory<RemoteFile, Double> progress = new PropertyValueFactory<>("progress");
            progressCol.setCellValueFactory(progress);
            progressCol.setCellFactory(ProgressBarTableCell.<RemoteFile>forTableColumn());
            columns.add(PROGRESS_COLUMN_INDEX, progressCol);
        }
    }

    private void updateSelectionMessage() {
        int count = 0;
        long size = 0;
        String type = "";
        int tabIndex = fileTabPane.getSelectionModel().getSelectedIndex();
        ObservableList<RemoteFile> files = getRemoteFiles(tabIndex);
        switch (tabIndex) {
            case 0:
                type = "FASTQ";
                break;
            case 1:
                type = "Submitted";
                break;
            case 2:
                type = "SRA";
        }

        for (RemoteFile file : files) {
            if (file.isDownload().get()) {
                count++;
                size += file.getSize();
            }
        }
        showMessage(count + " " + type + " files selected. " + (size > 0 ? ("Total size: " + Utils.getHumanReadableSize(size)) : ""), null);
        totalSize = size;
    }

    private void setupDownloadButtons() {
        startDownloadBtn.setOnAction(new StartDownloadHandler());
        stopDownloadBtn.setOnAction(new StopDownloadHandler());
        stopDownloadBtn.setDisable(true);
    }

    private void showLoginPopup() {
        // Create the custom dialog.
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Login Dialog");
        dialog.setHeaderText("Look, a Custom Login Dialog");

// Set the icon (must be included in the project).
//        dialog.setGraphic(new ImageView(this.getClass().getResource("login.png").toString()));

// Set the button types.
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

// Create the username and password labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);

// Enable/Disable login button depending on whether a username was entered.
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

// Do some validation (using the Java 8 lambda syntax).
        username.textProperty().addListener((observable, oldValue, newValue) -> {
            loginButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

// Request focus on the username field by default.
        Platform.runLater(() -> username.requestFocus());

// Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(username.getText(), password.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(usernamePassword -> {
//            System.out.println("Username=" + usernamePassword.getKey() + ", Password=" + usernamePassword.getValue());
        });
    }

    public Scene getSearchScene() {
        return searchScene;
    }

    public void setSearchScene(Scene searchScene) {
        this.searchScene = searchScene;
    }

    public SearchController getSearchController() {
        return searchController;
    }

    public void setSearchController(SearchController searchController) {
        this.searchController = searchController;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void setupDownloadDirBtn() {
        localDownloadDirBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                DirectoryChooser fileChooser = new DirectoryChooser();
                fileChooser.setTitle("Select download location");
                File localDir = fileChooser.showDialog(localDownloadDirBtn.getScene().getWindow());
                if (localDir != null) {
                    localDownloadDir.setText(localDir.getAbsolutePath());
                }
                updateSelectionMessage();
            }
        });
    }

    private void setupSelectAllBtn() {
        selectAllBtn.setOnAction(new SelectAllHandler());
    }

    private ObservableList<RemoteFile> getRemoteFiles(int tabIndex) {
        switch (tabIndex) {
            case 0:
                return fastqFiles;
            case 1:
                return submittedFiles;
            default:
                return sraFiles;
        }
    }

    private void setupBackBtn() {
        backBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                selectAllBtn.setOnAction(new SelectAllHandler());
                selectAllBtn.setText("Select All");
                hideMessage();
                searchController.clearFields();
                Stage primaryStage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
                primaryStage.setScene(searchScene);
            }
        });
    }



    public class StartDownloadHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent actionEvent) {
            if (StringUtils.isBlank(localDownloadDir.getText())) {
                showMessage("Please select a download location.", Images.WARNING);
                return;
            }
            File downloadDir = new File(localDownloadDir.getText());
            try {
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs();
                }
            } catch (Exception e) {
                showMessage("The location \""+  downloadDir.getAbsolutePath()+ "\" does not exist, and could not be created.", Images.EXCLAMATION);
                return;
            }
            log.debug("downloadDir.isDirectory():" + downloadDir.isDirectory());
            log.debug("downloadDir.canWrite():" + downloadDir.canWrite());
            if (!downloadDir.isDirectory() || !downloadDir.canWrite()) {
                showMessage("Unable to save to selected download location \"" + downloadDir.getAbsolutePath()
                        + "\".", Images.EXCLAMATION);
                return;
            }
            File testFile = new File(downloadDir.getAbsolutePath() + File.separator + "delete_me");
            try {
                testFile.createNewFile();
                testFile.delete();
            } catch (IOException e) {
                log.error("Can't create files", e);
                showMessage("Not allowed to save to selected download location \"" + downloadDir.getAbsolutePath()
                        + "\". Please select a different location.", Images.EXCLAMATION);
                return;
            }
            long usableSpace = downloadDir.getUsableSpace();
            if (usableSpace < totalSize) {
                showMessage("Not enough space in selected location to save all files. An additional "
                        + Utils.getHumanReadableSize(totalSize - usableSpace) + " is required.", Images.WARNING);
                return;
            }

            int tabIndex = fileTabPane.getSelectionModel().getSelectedIndex();
            ObservableList<RemoteFile> files = getRemoteFiles(tabIndex);
            List<RemoteFile> checkedFiles = new ArrayList<RemoteFile>();
            notDoneFiles = new ArrayList<RemoteFile>();

            for (RemoteFile file : files) {
                if (file.isDownload().get()) {
                    if (file.getTransferred() == 0 && file.getProgress() < 1) {
                        file.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                    }
                    String saveLocation = localDownloadDir.getText();
                    if (accSubCheckBox.isSelected()) {
                        saveLocation+= File.separator + file.getAccession();
                        new File(saveLocation).mkdir();
                    }

                    file.setSaveLocation(saveLocation);
                    checkedFiles.add(file);
                }
            }
            if (checkedFiles.size() == 0) {
                showMessage("No files selected for download.", Images.WARNING);
                return;
            } else {
                updateSelectionMessage();
                for (RemoteFile file : checkedFiles) {
                    if (!file.isDownloaded()) {
//                        file.setFileList(notDoneFiles);
//                        file.setController(self);
                        notDoneFiles.add(file);
                    }
                }
                if (notDoneFiles.isEmpty()) {
                    showMessage("All selected files have already been downloaded.", Images.TICK);
                    return;
                }
            }
//            showLoginPopup();

            startDownloadBtn.setDisable(true);
            stopDownloadBtn.setDisable(false);
            CountDownLatch latch = new CountDownLatch(notDoneFiles.size());
            executor = Executors.newSingleThreadExecutor();
            downloadTasks = new ArrayList<>();
            for (final RemoteFile file : notDoneFiles) {
                if (file.getTransferred() == 0) {
                    file.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                }

                Task task = new DownloadTask(file, latch, downloadSettings);
                task.setOnFailed(new TaskFailedHandler());
                task.setOnSucceeded(new TaskSucceedHandler(file));
                Future<?> submit = executor.submit(task);
                downloadTasks.add(task);
                if (downloadTasks.size() == notDoneFiles.size()) {
                    new Thread() {
                        public void run() {
                            while (!file.isDownloaded() && !stopDownloadBtn.isDisabled()) {
                                try {
                                    sleep(500);
                                } catch (InterruptedException e) {
                                    log.warn("Interrupted");
                                }
                            }
                            Platform.runLater(() -> {
                                startDownloadBtn.setDisable(false);
                                stopDownloadBtn.setDisable(true);
                            });
                        }
                    }.start();
                }
            }
            new Thread(() -> {
                try {
                    latch.await();
                    int count = 0;
                    for (int r = 0; r < notDoneFiles.size(); r++) {
                        RemoteFile file = notDoneFiles.get(r);
                        log.info(file.getName() + " " + count);
                        if (file.isDownloaded()) {
                            count++;
                        }
                    }
                    if (count == notDoneFiles.size()) {
                        int finalCount = count;
                        Platform.runLater(() -> {
                            showMessage(finalCount + (finalCount == 1 ? " file has " : " files have ") + "been successfully downloaded.", Images.TICK);
                        });
                    }

                } catch (InterruptedException E) {
                    // handle
                }
            }).start();
        }
    }

    private class StopDownloadHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent actionEvent) {
            startDownloadBtn.setDisable(false);
            stopDownloadBtn.setDisable(true);
            log.debug("Stopping downloads");
            showMessage("Downloading stopped by user! Click Start Download to resume.", Images.EXCLAMATION);
            if (executor != null) {
                List<Runnable> runnables = executor.shutdownNow();
            }
            for (int r = 0; r < notDoneFiles.size(); r++) {
                RemoteFile file = notDoneFiles.get(r);
                if (file.getProgress() == ProgressIndicator.INDETERMINATE_PROGRESS) {
                    file.updateProgress(0);
                }
                if (!file.isDownloaded()) {
                    downloadTasks.get(r).cancel();
                }
            }
        }
    }

    private class SelectAllHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent actionEvent) {
            int tabIndex = fileTabPane.getSelectionModel().getSelectedIndex();
            ObservableList<RemoteFile> files = getRemoteFiles(tabIndex);
            for (RemoteFile file : files) {
                file.downloadProperty().set(true);
            }
            selectAllBtn.setOnAction(new DeselectAllHandler());
            selectAllBtn.setText("Deselect All");
        }
    }

    private class DeselectAllHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent actionEvent) {
            int tabIndex = fileTabPane.getSelectionModel().getSelectedIndex();
            ObservableList<RemoteFile> files = getRemoteFiles(tabIndex);
            for (RemoteFile file : files) {
                if (file.downloadProperty().get()) {
                    file.downloadProperty().setValue(false);
                }
            }
            selectAllBtn.setOnAction(new SelectAllHandler());
            selectAllBtn.setText("Select All");
        }
    }


    private class TaskFailedHandler implements EventHandler<javafx.concurrent.WorkerStateEvent> {
        @Override
        public void handle(WorkerStateEvent event) {
            startDownloadBtn.setDisable(false);
            stopDownloadBtn.setDisable(true);
            log.debug("Stopping downloads");
            showMessage("Downloading stopped due to an error.", Images.EXCLAMATION);
            if (executor != null) {
                List<Runnable> runnables = executor.shutdownNow();
            }
            for (int r = 0; r < notDoneFiles.size(); r++) {
                RemoteFile file = notDoneFiles.get(r);
                if (file.getProgress() == ProgressIndicator.INDETERMINATE_PROGRESS) {
                    file.updateProgress(0);
                }
                if (!file.isDownloaded()) {
                    downloadTasks.get(r).cancel();
                }
            }
        }
    }


    private class TaskSucceedHandler implements EventHandler<WorkerStateEvent> {
        private RemoteFile file;

        public TaskSucceedHandler(RemoteFile file) {
            this.file = file;
        }

        @Override
        public void handle(WorkerStateEvent event) {
            showMessage(file.getName() + " downloaded.", Images.TICK);
        }
    }

    public void showMessage(String message, Images image) {
        log.info(message);
        Platform.runLater(() -> {
            this.selectionLabel.setText(message);
            if (image != null) {
                this.selectionLabel.setTextFill(image.getTextColor());
                labelImage.setImage(new Image(image.getImage()));
                labelImage.setVisible(true);
            } else {
                this.selectionLabel.setTextFill(Color.BLACK);
                labelImage.setVisible(false);
            }
        });
    }

    private void hideMessage() {
        Platform.runLater(() -> {
            this.selectionLabel.setText(null);
            labelImage.setVisible(false);
        });
    }
}



