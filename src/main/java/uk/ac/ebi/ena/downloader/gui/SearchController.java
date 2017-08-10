package uk.ac.ebi.ena.downloader.gui;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.PopOver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ena.downloader.model.DownloadSettings;
import uk.ac.ebi.ena.downloader.model.RemoteFile;
import uk.ac.ebi.ena.downloader.service.ReportParser;
import uk.ac.ebi.ena.downloader.service.WarehouseQuery;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SearchController implements Initializable {

    final ExecutorService pool = Executors.newFixedThreadPool(1);

    public static final String ERA_ID_PATTERN = "([ESDR]R[ASPXRZ][0-9]{6,}|SAMEA[0-9]{6,}|SAM[ND][0-9]{8,})";
    private final static Logger log = LoggerFactory.getLogger(SearchController.class);
    public static final String PLEASE_WAIT = "Please wait...";
    @FXML
    private TextField accession, report, asperaExe, asperaSsh, asperaParams;

    @FXML
    private TextArea query;

    @FXML
    private Button accessionBtn, reportBtn, searchBtn, asperaExeBtn, asperaSshBtn, reportHelpBtn;

    @FXML
    private RadioButton runFilesRadio, analysisFilesRadio, ftpRadio, asperaRadio;

    @FXML
    private VBox asperaConfig;

    @FXML
    private Hyperlink searchHelpLink;

    @FXML
    private TitledPane accTPane, reportTPane, searchTPane, settingsTPane;

    @FXML
    private Label fileErrorLabel, reportLoadingLabel;
    private Scene resultsScene;
    private ResultsController resultsController;
    private HostServices hostServices;
    private Stage stage;
    private DownloadSettings downloadSettings;

    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        log.debug("initialize");
//        searchLoadingImg.setVisible(false);
        setupSettingsPane();

        setupAccBtn();
        setupReportBtn();
        setupSearchBtn();
    }

    private void setupSettingsPane() {
        asperaRadio.selectedProperty().addListener((observable, oldValue, newValue) -> {
            asperaConfig.setVisible(newValue);
            if (newValue) {
                settingsTPane.setText("Settings: Currently using Aspera");
            } else {
                settingsTPane.setText("Settings: Currently using FTP");
            }
        });
        asperaExeBtn.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Aspera Connect executable (ascp)");
            File ascp = fileChooser.showOpenDialog(asperaExeBtn.getScene().getWindow());
            if (ascp != null) {
                asperaExe.setText(ascp.getAbsolutePath());
            }
        });
        asperaSshBtn.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("asperaweb_id_dsa.openssh", "*.openssh"));
            fileChooser.setTitle("Select Aspera Connect certificate (asperaweb_id_dsa.openssh)");
            File ascp = fileChooser.showOpenDialog(asperaSshBtn.getScene().getWindow());
            if (ascp != null) {
                asperaSsh.setText(ascp.getAbsolutePath());
            }
        });

        settingsTPane.heightProperty().addListener((obs, oldHeight, newHeight) -> stage.sizeToScene());
    }


    private void setupAccBtn() {
        accessionBtn.setOnAction(new AcccessionSearchButtonHandler());
        accession.setOnKeyPressed(new AcccessionSearchEnterHandler());
        accTPane.heightProperty().addListener((obs, oldHeight, newHeight) -> stage.sizeToScene());
    }

    private void handleAccessionSearch(Event actionEvent) {
        showMessage(null, false);
        String acc = accession.getText();
        if (StringUtils.isBlank(acc)) {
            showMessage("Please enter accession.", true);
            return;
        }
        if (!acc.matches(ERA_ID_PATTERN)) {
            showMessage("Please enter a valid accession.", true);
            return;
        }
        try {
            try {
                downloadSettings = getDownloadSettings();
            } catch (Exception e) {
                showMessage(e.getMessage(), true);
                return;
            }

            showMessage(PLEASE_WAIT, false);

            Platform.runLater(() -> {

                Map<String, List<RemoteFile>> stringListMap = null;
                try {
                    Future<Map<String, List<RemoteFile>>> stringListMapFuture = new WarehouseQuery().doWarehouseSearch(acc, downloadSettings.getMethod());
                    stringListMap = stringListMapFuture.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                if (stringListMap == null || stringListMap.size() == 0) {
                    showMessage("No downloadable files were found for the accession " + acc, true);
                    return;
                }

                Stage primaryStage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
                resultsController.renderResults(stringListMap, downloadSettings);
                primaryStage.setScene(resultsScene);
            });
        } finally {

        }
    }


    private void setupReportBtn() {
        reportHelpBtn.setOnAction(event -> {
            PopOver popOver = new PopOver();
            popOver.setTitle("help");
            popOver.setStyle("-fx-padding: 10");
            popOver.setContentNode(new Text(" Upload a pre-generated file report from \n the ENA Browser or the Pathogens portal "));
            popOver.show(reportHelpBtn);
            popOver.setAutoHide(true);
        });
        reportBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                try {
                    downloadSettings = getDownloadSettings();
                } catch (Exception e) {
                    showMessage(e.getMessage(), true);
                    return;
                }
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select report file");
                File reportFile = fileChooser.showOpenDialog(reportBtn.getScene().getWindow());
                try {
                    if (reportFile != null) {
                        report.setText(reportFile.getAbsolutePath());
                        showMessage(PLEASE_WAIT, false);
//                        toggleBtnImage(reportBtn, true);

                        Platform.runLater(() -> {
                            Future<Map<String, List<RemoteFile>>> fileListMapFuture = new ReportParser().parseExternalReportFile(reportFile, downloadSettings.getMethod());

                            Map<String, List<RemoteFile>> fileListMap = null;
                            try {
                                fileListMap = fileListMapFuture.get();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }
                            if (fileListMap == null || fileListMap.size() == 0) {
                                showMessage("File is not in the desired format, or does not contain necessary information.", true);
                                reportBtn.setText("Load Report File");
                                return;
                            }

                            Stage primaryStage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
                            resultsController.renderResults(fileListMap, downloadSettings);
                            primaryStage.setScene(resultsScene);
                        });


                    }
                } finally {
//                    toggleImage(reportLoadingLabel, false);
//                    toggleBtnImage(reportBtn, false);
                }
            }
        });

        reportTPane.heightProperty().addListener((obs, oldHeight, newHeight) -> stage.sizeToScene());

    }

    /*private void toggleBtnImage(Button reportBtn, boolean show) {
        Platform.runLater(() -> {
            if (show) {
                reportBtn.setGraphic(getLoadingImage());
            } else {
                reportBtn.setGraphic(null);
            }
        });
    }*/

    private void toggleWait(Button button, String message) {

        Platform.runLater(() -> {
            reportBtn.setText(message);

        });
    }


    private void setupSearchBtn() {
        searchBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                showMessage("", true);
                if (StringUtils.isBlank(query.getText())) {
                    showMessage("Please enter search query string.", true);
                    return;
                }
                try {
                    try {
                        downloadSettings = getDownloadSettings();
                    } catch (Exception e) {
                        showMessage(e.getMessage(), true);
                        return;
                    }

                    showMessage(PLEASE_WAIT, false);

                    Platform.runLater(() -> {
                        Map<String, List<RemoteFile>> fileListMap = new HashMap<>();
                        try {
                            if (runFilesRadio.isSelected()) {
                                fileListMap = new WarehouseQuery().doPortalSearch("read_run", query.getText(), downloadSettings.getMethod()).get();
                            } else if (analysisFilesRadio.isSelected()) {

                                fileListMap = new WarehouseQuery().doPortalSearch("analysis", query.getText(), downloadSettings.getMethod()).get();

                            } else {
                                showMessage("Please select result type to search in.", true);
                                return;
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                        if (fileListMap == null || fileListMap.size() == 0) {
                            showMessage("No downloadable files were found for the given query and result type.", true);
                            return;
                        }

                        Stage primaryStage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
                        resultsController.renderResults(fileListMap, downloadSettings);
                        primaryStage.setScene(resultsScene);
                    });


                } finally {

                }
            }
        });

        searchHelpLink.setOnAction(t -> {
            this.hostServices.showDocument("http://www.ebi.ac.uk/ena/browse/search-rest");
        });

        searchTPane.heightProperty().addListener((obs, oldHeight, newHeight) -> stage.sizeToScene());
    }


    public Scene getResultsScene() {
        return resultsScene;
    }

    public void setResultsScene(Scene resultsScene) {
        this.resultsScene = resultsScene;
    }

    public ResultsController getResultsController() {
        return resultsController;
    }

    public void setResultsController(ResultsController resultsController) {
        this.resultsController = resultsController;
    }

    public void showMessage(String message, boolean isError) {
        log.info(message);
//        Platform.runLater(() -> {
        this.fileErrorLabel.setText(message);
        if (isError) {
            this.fileErrorLabel.setTextFill(Color.RED);
        } else {
            this.fileErrorLabel.setTextFill(Color.GREEN);
        }
//        });

    }

    public void clearFields() {
        accession.clear();
//        accessionBtn.setGraphic(null);
//        accessionBtn.setDisable(false);
//        reportBtn.setGraphic(null);
        report.clear();
        showMessage(null, false);
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    public HostServices getHostServices() {
        return hostServices;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    class AcccessionSearchButtonHandler implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent actionEvent) {
            handleAccessionSearch(actionEvent);
        }
    }

    class AcccessionSearchEnterHandler implements EventHandler<KeyEvent> {
        @Override
        public void handle(KeyEvent actionEvent) {
            if (actionEvent.getCode() == KeyCode.ENTER) {
                handleAccessionSearch(actionEvent);
            }
        }
    }

    private DownloadSettings getDownloadSettings() throws Exception {
        if (ftpRadio.isSelected()) {
            return new DownloadSettings(DownloadSettings.Method.FTP);
        } else {
            if (StringUtils.isBlank(asperaExe.getText()) || !new File(asperaExe.getText()).exists()) {
                throw new Exception("Aspera Connect client not found.");
            }
            if (StringUtils.isBlank(asperaSsh.getText()) || !new File(asperaSsh.getText()).exists()) {
                throw new Exception("Aspera Connect certificate not found.");
            }
            if (StringUtils.isBlank(asperaParams.getText())) {
                throw new Exception("Aspera Connect parameters not found.");
            }
            return new DownloadSettings(DownloadSettings.Method.ASPERA, asperaExe.getText(), asperaSsh.getText(), asperaParams.getText());
        }
    }


}



