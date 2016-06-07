package uk.ac.ebi.ena.ftp.gui;

import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;
import org.apache.commons.lang3.StringUtils;
import uk.ac.ebi.ena.ftp.gui.custom.ProgressBarTableCell;
import uk.ac.ebi.ena.ftp.model.RemoteFile;
import uk.ac.ebi.ena.ftp.service.WarehouseQuery;
import uk.ac.ebi.ena.ftp.utils.Utils;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Controller implements Initializable {
    @FXML
    private TextField localDownloadDir;

    @FXML
    private Button localDownloadDirBtn, selectAllBtn, startDownloadBtn;

    @FXML
    private TableView<RemoteFile> fastqFileTable, submittedFileTable;

    @FXML
    private Label selectionLabel;

    @FXML
    private TabPane fileTabPane;

    @FXML
    private ObservableList<RemoteFile> fastqFiles, submittedFiles;


    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {

        assert startDownloadBtn != null : "fx:id=\"startDownloadBtn\" was not injected: check your FXML file 'gui.fxml'.";
        // initialize your logic here: all @FXML variables will have been injected
        String accession = Main.parameters.getUnnamed().get(0);
        setupDownloadDirBtn();
        setupTables(accession);
        setupSelectAllBtn();
        setupDownloadButton();
        updateSelectionMessage();
    }

    private void setupTables(String accession) {
        WarehouseQuery warehouseQuery = new WarehouseQuery();
        List<RemoteFile> queryFastq = warehouseQuery.query(accession, "fastq");
        fastqFiles = FXCollections.observableArrayList(new Callback<RemoteFile, Observable[]>() {
            @Override
            public Observable[] call(RemoteFile param) {
                return new Observable[]{param.downloadProperty()};
            }
        });
        fastqFiles.addAll(queryFastq);
        setupTable(fastqFileTable, fastqFiles);
        List<RemoteFile> querySubmitted = warehouseQuery.query(accession, "submitted");
        submittedFiles = FXCollections.observableArrayList(new Callback<RemoteFile, Observable[]>() {
            @Override
            public Observable[] call(RemoteFile param) {
                return new Observable[]{param.downloadProperty()};
            }
        });
        submittedFiles.addAll(querySubmitted);
        setupTable(submittedFileTable, submittedFiles);
    }

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

        final ObservableList<RemoteFile> finalTableFiles = tableFiles;// copying to final var use inside inner class
        tableFiles.addListener(new ListChangeListener<RemoteFile>() {
            @Override
            public void onChanged(Change<? extends RemoteFile> change) {
                updateSelectionMessage();
            }
        });
        tableView.setItems(tableFiles);
        addProgressColumn(tableView);
    }

    private void addProgressColumn(TableView<RemoteFile> tableView) {
        ObservableList columns = tableView.getColumns();

        TableColumn<RemoteFile, Double> progressCol = new TableColumn<>("Progress");
        progressCol.setPrefWidth(311);
        progressCol.setResizable(false);
        PropertyValueFactory<RemoteFile, Double> progress = new PropertyValueFactory<>("progress");
        progressCol.setCellValueFactory(progress);
        progressCol.setCellFactory(ProgressBarTableCell.<RemoteFile>forTableColumn());
        columns.add(3, progressCol);
    }

    private void updateSelectionMessage() {
        int count = 0;
        long size = 0;
        String type = "";
        int tabIndex = fileTabPane.getSelectionModel().getSelectedIndex();
        ObservableList<RemoteFile> files = null;
        if (tabIndex == 0) {
            files = fastqFiles;
            type = "FASTQ";
        } else {
            files = submittedFiles;
            type = "Submitted";
        }
        for (RemoteFile file : files) {
            if (file.isDownload().get()) {
                count++;
                size += file.getSize();
            }
        }
        selectionLabel.setText(count + " " + type + " files selected. Total size: " + Utils.getHumanReadableSize(size));
    }

    private void setupDownloadButton() {
        startDownloadBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (StringUtils.isBlank(localDownloadDir.getText())) {
                    selectionLabel.setText("Please select a download location.");
                    return;
                }
                File downloadDir = new File(localDownloadDir.getText());
                if (!downloadDir.isDirectory() || !downloadDir.canWrite()) {
                    selectionLabel.setText("Unable to save to selected download location.");
                    return;
                }
                int tabIndex = fileTabPane.getSelectionModel().getSelectedIndex();
                ObservableList<RemoteFile> files = null;
                if (tabIndex == 0) {
                    files = fastqFiles;
                } else {
                    files = submittedFiles;
                }
                List<RemoteFile> checkedFiles = new ArrayList<RemoteFile>();
                List<RemoteFile> notDoneFiles = new ArrayList<RemoteFile>();

                for (RemoteFile file : files) {
                    if (file.isDownload().get()) {
                        file.setSaveLocation(localDownloadDir.getText());
                        checkedFiles.add(file);
                    }
                }
                if (checkedFiles.size() == 0) {
                    selectionLabel.setText("No files selected for download.");
                    return;
                } else {
                    for (RemoteFile file : checkedFiles) {
                        if (!file.isDone()) {
                            notDoneFiles.add(file);
                        }
                    }
                    if (notDoneFiles.isEmpty()) {
                        selectionLabel.setText("All selected files have already been downloaded.");
                        return;
                    }
                }

                ExecutorService executor = Executors.newSingleThreadExecutor();
                for (RemoteFile task : notDoneFiles) {
                    task.updateProgress(-1);
                    System.out.println("exec " + task.getPath());
                    executor.execute(task);
                }

            }
        });

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
        selectAllBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                int tabIndex = fileTabPane.getSelectionModel().getSelectedIndex();
                ObservableList<RemoteFile> files = null;
                if (tabIndex == 0) {
                    files = fastqFiles;
                } else {
                    files = submittedFiles;
                }
                for (RemoteFile file : files) {
                    file.downloadProperty().set(true);
                }
            }
        });
    }
}



