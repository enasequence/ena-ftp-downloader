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
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Callback;
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
import java.util.concurrent.ThreadFactory;

public class Controller implements Initializable {
    @FXML
    private Button startDownloadBtn;

    @FXML
    private TextField localDownloadDir;

    @FXML
    private Button localDownloadDirBtn;

    @FXML
    private TableView<RemoteFile> fastqFileTable;

    @FXML
    private TableView<RemoteFile> submittedFileTable;

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
        fillTable(accession);
    }

    private void fillTable(String accession) {
        WarehouseQuery warehouseQuery = new WarehouseQuery();

        ObservableList<TableColumn<RemoteFile, ?>> columns =
                fastqFileTable.getColumns();
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
                        return new CheckBoxTableCell<>();
                    }
                });
        downloadColumn.setEditable(true);

        fastqFiles = FXCollections.observableArrayList(new Callback<RemoteFile, Observable[]>() {

            @Override
            public Observable[] call(RemoteFile param) {
                return new Observable[]{param.downloadProperty()};
            }
        });

        fastqFiles.addAll(warehouseQuery.queryFastq(accession));
        fastqFiles.addListener(new ListChangeListener<RemoteFile>() {
            @Override
            public void onChanged(Change<? extends RemoteFile> change) {
                int count = 0;
                long size = 0;
                while (change.next()) {
                    if (change.wasUpdated()) {
                        for (RemoteFile file : fastqFiles) {
                            if (file.isDownload().get()) {
                                count++;
                                size += file.getSize();
                            }
                        }
                    }
                }
                selectionLabel.setText(count + " files selected. Total size: " + Utils.getHumanReadableSize(size));
            }
        });
        fastqFileTable.setItems(fastqFiles);
        addProgressColumn();
    }

    private void addProgressColumn() {
        ObservableList columns =
                fastqFileTable.getColumns();

        TableColumn<RemoteFile, Double> progressCol = new TableColumn<>("Progress");
        progressCol.setPrefWidth(311);
        progressCol.setCellValueFactory(new PropertyValueFactory<RemoteFile, Double>(
                "progress"));
        progressCol
                .setCellFactory(ProgressBarTableCell.<RemoteFile>forTableColumn());
        columns.add(3, progressCol);
        setupDownloadButton();
    }

    private void setupDownloadButton() {
        startDownloadBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                int tabIndex = fileTabPane.getSelectionModel().getSelectedIndex();
                ObservableList<RemoteFile> files = null;
                if (tabIndex == 0) {
                    files = fastqFiles;
                } else {
                    files = submittedFiles;
                }
                List<RemoteFile> checkedFiles = new ArrayList<RemoteFile>();
                for (RemoteFile file : files) {
                    if (file.isDownload().get()) {
                        file.setSaveLocation(localDownloadDir.getText());
                        checkedFiles.add(file);
                    }
                }

                if (checkedFiles.size() == 0) {
                    return;
                }
                ExecutorService executor = Executors.newFixedThreadPool(checkedFiles.size(), new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r);
                        t.setDaemon(false);
                        return t;
                    }
                });


                for (RemoteFile task : checkedFiles) {
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
                localDownloadDir.setText(localDir.getAbsolutePath());
            }
        });
    }
}



