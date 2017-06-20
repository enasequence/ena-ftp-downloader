package uk.ac.ebi.ena.ftp.gui;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ena.ftp.gui.custom.MD5TableCell;
import uk.ac.ebi.ena.ftp.gui.custom.ProgressBarTableCell;
import uk.ac.ebi.ena.ftp.model.RemoteFile;
import uk.ac.ebi.ena.ftp.service.WarehouseQuery;
import uk.ac.ebi.ena.ftp.utils.Utils;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ResultsController implements Initializable {

    private final static Logger log = LoggerFactory.getLogger(ResultsController.class);

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
    private Tab fastqTab, submittedTab, sraTab;

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


    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        log.debug("initialize");
        assert startDownloadBtn != null : "fx:id=\"startDownloadBtn\" was not injected: check your FXML file 'results.fxml'.";

    }

    public void renderResults(Search search) {
        setupDownloadDirBtn();
        if (StringUtils.isNotBlank(search.getAccession())) {
            setupTables(search.getAccession());
        } else {
            setupTables(search.getReportFileMap());
        }
        setupSelectAllBtn();
        setupBackBtn();
        setupDownloadButtons();
    }


    private void setupTables(String acc) {
        WarehouseQuery warehouseQuery = new WarehouseQuery();

        List<RemoteFile> queryFastq = warehouseQuery.query(acc, "fastq");
        if (queryFastq.size() > 0) {
            fastqFiles = FXCollections.observableArrayList(new Callback<RemoteFile, Observable[]>() {
                @Override
                public Observable[] call(RemoteFile param) {
                    return new Observable[]{param.downloadProperty()};
                }
            });
            fastqFiles.addAll(queryFastq);
            setupTable(fastqFileTable, fastqFiles);
            fastqTab.setDisable(false);
        } else {
            fastqTab.setDisable(true);
        }

        List<RemoteFile> querySubmitted = warehouseQuery.query(acc, "submitted");
        if (querySubmitted.size() > 0) {
            submittedFiles = FXCollections.observableArrayList(new Callback<RemoteFile, Observable[]>() {
                @Override
                public Observable[] call(RemoteFile param) {
                    return new Observable[]{param.downloadProperty()};
                }
            });
            submittedFiles.addAll(querySubmitted);
            setupTable(submittedFileTable, submittedFiles);
            submittedTab.setDisable(false);
        } else {
            submittedTab.setDisable(true);
        }

        List<RemoteFile> querySra = warehouseQuery.query(acc, "sra");
        if (querySra.size() > 0) {
            sraFiles = FXCollections.observableArrayList(new Callback<RemoteFile, Observable[]>() {
                @Override
                public Observable[] call(RemoteFile param) {
                    return new Observable[]{param.downloadProperty()};
                }
            });
            sraFiles.addAll(querySra);
            setupTable(sraFileTable, sraFiles);
            sraTab.setDisable(false);
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
        TableColumn<RemoteFile, Boolean> downloadColumn = (TableColumn<RemoteFile, Boolean>) columns.get(0);
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

        TableColumn<RemoteFile, String> sizeColumn = (TableColumn<RemoteFile, String>) columns.get(2);

        sizeColumn.setCellValueFactory(
                file -> {
                    SimpleStringProperty property = new SimpleStringProperty();
                    property.setValue(file.getValue().getSize() > 0 ? Utils.getHumanReadableSize(file.getValue().getSize()) : "N/A");
                    return property;
                });

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
    }

    private void addIconColumn(TableView<RemoteFile> tableView) {
        ObservableList columns = tableView.getColumns();

        if (columns.size() < 5) {
            TableColumn<RemoteFile, String> progressCol = new TableColumn<>("MD5 OK");
            progressCol.setPrefWidth(60);
            progressCol.setResizable(false);
            PropertyValueFactory<RemoteFile, String> progress = new PropertyValueFactory<>("successIcon");
            progressCol.setCellValueFactory(progress);
            progressCol.setCellFactory(new Callback<TableColumn<RemoteFile, String>, TableCell<RemoteFile, String>>() {
                @Override
                public TableCell<RemoteFile, String> call(TableColumn<RemoteFile, String> param) {
                    TableCell<RemoteFile, String> cell = new MD5TableCell();
                    return cell;
                }
            });
            columns.add(4, progressCol);
        }
    }

    private void addProgressColumn(TableView<RemoteFile> tableView) {
        ObservableList columns = tableView.getColumns();
        if (columns.size() < 4) {

            TableColumn<RemoteFile, Double> progressCol = new TableColumn<>("Progress");
            progressCol.setPrefWidth(295);
            progressCol.setResizable(false);
            PropertyValueFactory<RemoteFile, Double> progress = new PropertyValueFactory<>("progress");
            progressCol.setCellValueFactory(progress);
            progressCol.setCellFactory(ProgressBarTableCell.<RemoteFile>forTableColumn());
            columns.add(3, progressCol);
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
        selectionLabel.setText(count + " " + type + " files selected. " + (size > 0 ? ("Total size: " + Utils.getHumanReadableSize(size)) : ""));
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

    private void setupTables(Map<String, List<RemoteFile>> fileListMap) {

        List<RemoteFile> queryFastq = fileListMap.get("fastq");
        if (queryFastq.size() > 0) {
            fastqFiles = FXCollections.observableArrayList(new Callback<RemoteFile, Observable[]>() {
                @Override
                public Observable[] call(RemoteFile param) {
                    return new Observable[]{param.downloadProperty()};
                }
            });
            fastqFiles.addAll(queryFastq);
            setupTable(fastqFileTable, fastqFiles);
            fastqTab.setDisable(false);
        } else {
            fastqTab.setDisable(true);
        }
        List<RemoteFile> querySubmitted = fileListMap.get("submitted");
        if (querySubmitted.size() > 0) {
            submittedFiles = FXCollections.observableArrayList(new Callback<RemoteFile, Observable[]>() {
                @Override
                public Observable[] call(RemoteFile param) {
                    return new Observable[]{param.downloadProperty()};
                }
            });
            submittedFiles.addAll(querySubmitted);
            setupTable(submittedFileTable, submittedFiles);
            submittedTab.setDisable(false);
        } else {
            submittedTab.setDisable(true);
        }
        List<RemoteFile> querySra = fileListMap.get("sra");
        if (querySra.size() > 0) {
            sraFiles = FXCollections.observableArrayList(new Callback<RemoteFile, Observable[]>() {
                @Override
                public Observable[] call(RemoteFile param) {
                    return new Observable[]{param.downloadProperty()};
                }
            });
            sraFiles.addAll(querySra);
            setupTable(sraFileTable, sraFiles);
            sraTab.setDisable(false);
        } else {
            sraTab.setDisable(true);
        }
    }

    private Map<String, List<RemoteFile>> parseReportFile(File reportFile) {
        Map<String, List<RemoteFile>> map = new HashMap<>();
        int fastqIndex = -1;
        int fastqBytesIndex = -1;
        int fastqMd5Index = -1;

        int submittedIndex = -1;
        int submittedBytesIndex = -1;
        int submittedMd5Index = -1;

        int sraIndex = -1;
        int sraBytesIndex = -1;
        int sraMd5Index = -1;

        try {
            List<String> lines = IOUtils.readLines(new FileReader(reportFile));
            String[] headersSplit = StringUtils.splitPreserveAllTokens(lines.get(0), "\t");
            List<String> headers = Arrays.asList(headersSplit);
            fastqIndex = headers.indexOf("fastq_ftp");
            if (fastqIndex > -1) {
                map.put("fastq", new ArrayList<RemoteFile>());
                fastqBytesIndex = headers.indexOf("fastq_bytes");
                fastqMd5Index = headers.indexOf("fastq_md5");
                ;
            }

            submittedIndex = headers.indexOf("submitted_ftp");
            if (submittedIndex > -1) {
                map.put("submitted", new ArrayList<RemoteFile>());
                submittedBytesIndex = headers.indexOf("submitted_bytes");
                submittedMd5Index = headers.indexOf("submitted_md5");
                ;
            }

            sraIndex = headers.indexOf("sra_ftp");
            if (sraIndex > -1) {
                map.put("sra", new ArrayList<RemoteFile>());
                sraBytesIndex = headers.indexOf("sra_bytes");
                sraMd5Index = headers.indexOf("sra_md5");
                ;
            }

            if (!map.isEmpty()) {
                for (int r = 1; r < lines.size(); r++) {
                    String[] fields = StringUtils.splitPreserveAllTokens(lines.get(r), "\t");
                    if (fastqIndex > -1) {
                        String fastqFilesStr = fields[fastqIndex];
                        if (StringUtils.isNotBlank(fastqFilesStr)) {
                            String[] files = StringUtils.split(fastqFilesStr, ";");
                            String[] bytes = null;
                            String[] md5s = null;
                            for (int f = 0; f < files.length; f++) {
                                String fastqFile = files[f];
                                if (fastqBytesIndex > -1) {
                                    bytes = StringUtils.split(fields[fastqBytesIndex], ";");
                                }
                                if (fastqMd5Index > -1) {
                                    md5s = StringUtils.split(fields[fastqMd5Index], ";");
                                }
                                map.get("fastq").add(new RemoteFile(StringUtils.substringAfterLast(fastqFile, "/"),
                                        fastqBytesIndex > -1 ? Long.parseLong(bytes[f]) : 0, fastqFile,
                                        fastqMd5Index > -1 ? md5s[f] : null));
                            }
                        }
                    }
                    if (submittedIndex > -1) {
                        String submittedFilesStr = fields[submittedIndex];
                        if (StringUtils.isNotBlank(submittedFilesStr)) {
                            String[] files = StringUtils.split(submittedFilesStr, ";");
                            String[] bytes = null;
                            String[] md5s = null;
                            for (int f = 0; f < files.length; f++) {
                                String submittedFile = files[f];
                                if (submittedBytesIndex > -1) {
                                    bytes = StringUtils.split(fields[submittedBytesIndex], ";");
                                }
                                if (submittedMd5Index > -1) {
                                    md5s = StringUtils.split(fields[submittedMd5Index], ";");
                                }
                                map.get("submitted").add(new RemoteFile(StringUtils.substringAfterLast(submittedFile, "/"),
                                        submittedBytesIndex > -1 ? Long.parseLong(bytes[f]) : 0, submittedFile,
                                        submittedMd5Index > -1 ? md5s[f] : null));
                            }
                        }
                        if (sraIndex > -1) {
                            String sraFilesStr = fields[sraIndex];
                            if (StringUtils.isNotBlank(sraFilesStr)) {
                                String[] files = StringUtils.split(sraFilesStr, ";");
                                String[] bytes = null;
                                String[] md5s = null;
                                for (int f = 0; f < files.length; f++) {
                                    String sraFile = files[f];
                                    if (sraBytesIndex > -1) {
                                        bytes = StringUtils.split(fields[sraBytesIndex], ";");
                                    }
                                    if (sraMd5Index > -1) {
                                        md5s = StringUtils.split(fields[sraMd5Index], ";");
                                    }
                                    map.get("sra").add(new RemoteFile(StringUtils.substringAfterLast(sraFile, "/"),
                                            sraBytesIndex > -1 ? Long.parseLong(bytes[f]) : 0, sraFile,
                                            sraMd5Index > -1 ? md5s[f] : null));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error", e);
        }
        return map;
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
                selectionLabel.setText("");
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
                selectionLabel.setText("Please select a download location.");
                return;
            }
            File downloadDir = new File(localDownloadDir.getText());
            log.debug("downloadDir.isDirectory():" + downloadDir.isDirectory());
            log.debug("downloadDir.canWrite():" + downloadDir.canWrite());
            if (!downloadDir.isDirectory()/* || !downloadDir.canWrite()*/) {
                selectionLabel.setText("Unable to save to selected download location.");
                return;
            }
            long usableSpace = downloadDir.getUsableSpace();
            if (usableSpace < totalSize) {
                selectionLabel.setText("Not enough space in selected location to save all files. An additional "
                        + Utils.getHumanReadableSize(totalSize - usableSpace) + " is required.");
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
                    file.setSaveLocation(localDownloadDir.getText());
                    checkedFiles.add(file);
                }
            }
            if (checkedFiles.size() == 0) {
                selectionLabel.setText("No files selected for download.");
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
                    selectionLabel.setText("All selected files have already been downloaded.");
                    return;
                }
            }
//            showLoginPopup();
            startDownloadBtn.setDisable(true);
            stopDownloadBtn.setDisable(false);
            executor = Executors.newSingleThreadExecutor();
            downloadTasks = new ArrayList<>();
            for (final RemoteFile file : notDoneFiles) {
                if (file.getTransferred() == 0) {
                    file.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                }
                Task task = new DownloadTask(file);
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
        }
    }

    private class StopDownloadHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent actionEvent) {
            startDownloadBtn.setDisable(false);
            stopDownloadBtn.setDisable(true);
            log.debug("Stopping downloads");
            selectionLabel.setText("Downloading stopped by user! Click Start Download to resume.");
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


}



