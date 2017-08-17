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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.PopOver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ena.downloader.model.DownloadSettings;
import uk.ac.ebi.ena.downloader.model.Images;
import uk.ac.ebi.ena.downloader.model.RemoteFile;
import uk.ac.ebi.ena.downloader.service.ReportParser;
import uk.ac.ebi.ena.downloader.service.WarehouseQuery;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

public class SearchController implements Initializable {

    public static final String ERA_ID_PATTERN = "([ESDR]R[ASPXRZ][0-9]{6,}|SAMEA[0-9]{6,}|SAM[ND][0-9]{8,})";
    private final static Logger log = LoggerFactory.getLogger(SearchController.class);
    public static final String PLEASE_WAIT = "Please wait...";
    @FXML
    private TextField accession, report, asperaExe, asperaSsh, asperaParams;

    @FXML
    private TextArea query;

    @FXML
    private Button accessionBtn, reportBtn, searchBtn, asperaExeBtn, asperaSshBtn, reportHelpBtn, reportLoadBtn, asperaSaveBtn;

    @FXML
    private RadioButton runFilesRadio, analysisFilesRadio, ftpRadio, asperaRadio;

    @FXML
    private TitledPane asperaConfig;

    @FXML
    private Hyperlink searchHelpLink;

    @FXML
    private TitledPane accTPane, reportTPane, searchTPane, settingsTPane;

    @FXML
    private Label fileErrorLabel, reportLoadingLabel;

    @FXML
    private HBox errorPanel;

    @FXML
    private ImageView labelImage;

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
            asperaConfig.setExpanded(newValue);
            if (newValue) {
                settingsTPane.setText("Settings: Currently using Aspera");
            } else {
                settingsTPane.setText("Settings: Currently using FTP");
            }
        });
        asperaExeBtn.setOnAction(event -> {
            clearMessage();
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Aspera Connect executable (ascp)");
            File ascp = fileChooser.showOpenDialog(asperaExeBtn.getScene().getWindow());
            if (ascp != null) {
                asperaExe.setText(ascp.getAbsolutePath());
            }
        });
        asperaSshBtn.setOnAction(event -> {
            clearMessage();
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("asperaweb_id_dsa.openssh", "*.openssh"));
            fileChooser.setTitle("Select Aspera Connect certificate (asperaweb_id_dsa.openssh)");
            File ascp = fileChooser.showOpenDialog(asperaSshBtn.getScene().getWindow());
            if (ascp != null) {
                asperaSsh.setText(ascp.getAbsolutePath());
            }
        });
        asperaSaveBtn.setOnAction(event -> saveAsperaSettings());
        loadAsperaSettings();

       /* Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Pane title = (Pane) settingsTPane.lookup(".title");
                if (title != null) {
                    title.setVisible(false);
                }
            }
        });*/
        settingsTPane.heightProperty().addListener((obs, oldHeight, newHeight) -> stage.sizeToScene());
    }

    private void saveAsperaSettings() {
        try {
            DownloadSettings downloadSettings = getDownloadSettings();
            File jarDir = getJarDir();//new File(ClassLoader.getSystemClassLoader().getResource(".").getPath());
            System.out.println(jarDir.getAbsolutePath());
            if (!jarDir.canWrite()) {
                showMessage("Unable to save settings file to " + jarDir.getAbsolutePath() + ". Please make sure it has write access.", Images.EXCLAMATION);
            }
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(new File(jarDir.getAbsolutePath() + File.separator + "ena-file-downloader.settings")));
            objectOutputStream.writeObject(downloadSettings);
            objectOutputStream.close();
        } catch (Exception e) {
            showMessage(e.getMessage(), Images.EXCLAMATION);
            return;
        }
    }

    public File getJarDir() {
        URL url;
        String extURL;      //  url.toExternalForm();

        // get an url
        try {
            url = this.getClass().getProtectionDomain().getCodeSource().getLocation();
            // url is in one of two forms
            //        ./build/classes/   NetBeans test
            //        jardir/JarName.jar  froma jar
        } catch (SecurityException ex) {
            url = this.getClass().getResource(this.getClass().getSimpleName() + ".class");
            // url is in one of two forms, both ending "/com/physpics/tools/ui/PropNode.class"
            //          file:/U:/Fred/java/Tools/UI/build/classes
            //          jar:file:/U:/Fred/java/Tools/UI/dist/UI.jar!
        }

        // convert to external form
        extURL = url.toExternalForm();

        // prune for various cases
        if (extURL.endsWith(".jar"))   // from getCodeSource
            extURL = extURL.substring(0, extURL.lastIndexOf("/"));
        else {  // from getResource
            String suffix = "/"+(this.getClass().getName()).replace(".", "/")+".class";
            extURL = extURL.replace(suffix, "");
            if (extURL.startsWith("jar:") && extURL.endsWith(".jar!"))
                extURL = extURL.substring(4, extURL.lastIndexOf("/"));
        }

        // convert back to url
        try {
            url = new URL(extURL);
        } catch (MalformedURLException mux) {
            // leave url unchanged; probably does not happen
        }

        // convert url to File
        try {
            return new File(url.toURI());
        } catch(URISyntaxException ex) {
            return new File(url.getPath());
        }
    }

    private void loadAsperaSettings() {
        try {
            File jarDir = getJarDir();//new File(ClassLoader.getSystemClassLoader().getResource(".").getPath());
            System.out.println(jarDir.getAbsolutePath());
            File settings = new File(jarDir.getAbsolutePath() + File.separator + "ena-file-downloader.settings");
            if (settings.exists()) {
                ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(settings));
                DownloadSettings o = (DownloadSettings) objectInputStream.readObject();
                objectInputStream.close();
                if (o != null) {
                    asperaExe.setText(o.getExecutable());
                    asperaSsh.setText(o.getCertificate());
                    asperaParams.setText(o.getParameters());
                    downloadSettings = o;
                }
            }
        } catch (Exception e) {
            showMessage(e.getMessage(), Images.EXCLAMATION);
        }
    }


    private void setupAccBtn() {
        accessionBtn.setOnAction(new AcccessionSearchButtonHandler());
        accession.setOnKeyPressed(new AcccessionSearchEnterHandler());
        accTPane.heightProperty().addListener((obs, oldHeight, newHeight) -> stage.sizeToScene());
    }

    private void handleAccessionSearch(Event actionEvent) {
        clearMessage();
        String acc = accession.getText();
        if (StringUtils.isBlank(acc)) {
            showMessage("Please enter accession.", Images.WARNING);
            return;
        }
        if (!acc.matches(ERA_ID_PATTERN)) {
            showMessage("Please enter a valid accession.", Images.WARNING);
            return;
        }
        try {
            try {
                downloadSettings = getDownloadSettings();
            } catch (Exception e) {
                showMessage(e.getMessage(), Images.EXCLAMATION);
                return;
            }

            showMessage(PLEASE_WAIT, Images.LOADING);

            new Thread(() -> {
//                Map<String, List<RemoteFile>> stringListMap = null;
                /*try {
                    Future<Map<String, List<RemoteFile>>> stringListMapFuture = new WarehouseQuery().doWarehouseSearch(acc, downloadSettings.getMethod());
                    stringListMap = stringListMapFuture.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }*/
                Map<String, List<RemoteFile>> stringListMap = new WarehouseQuery().doWarehouseSearch(acc, downloadSettings.getMethod());
                if (stringListMap == null || stringListMap.size() == 0) {
                    showMessage("No downloadable files were found for the accession " + acc, Images.WARNING);
                    return;
                }
                Map<String, List<RemoteFile>> finalStringListMap = stringListMap;
                Platform.runLater(() -> {
                    Stage primaryStage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
                    resultsController.renderResults(finalStringListMap, downloadSettings);
                    primaryStage.setScene(resultsScene);
                });
            }).start();
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
        report.textProperty().addListener((observable, oldValue, newValue) -> {
            reportLoadBtn.setDisable(!new File(newValue).exists());
        });
        reportLoadBtn.setOnAction(event -> {
            showMessage(PLEASE_WAIT, Images.LOADING);

            File reportFile = new File(report.getText());
            new Thread(() -> {
//                Future<Map<String, List<RemoteFile>>> fileListMapFuture = new ReportParser().parseExternalReportFile(reportFile, downloadSettings.getMethod());
                Map<String, List<RemoteFile>> fileListMap = new ReportParser().parseExternalReportFile(reportFile, downloadSettings.getMethod());

                /*Map<String, List<RemoteFile>> fileListMap = null;
                try {
                    fileListMap = fileListMapFuture.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }*/
                if (fileListMap == null || fileListMap.size() == 0) {
                    showMessage("File is not in the desired format, or does not contain necessary information.", Images.WARNING);
                    return;
                }
                Map<String, List<RemoteFile>> finalFileListMap = fileListMap;
                Platform.runLater(() -> {
                    Stage primaryStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    resultsController.renderResults(finalFileListMap, downloadSettings);
                    primaryStage.setScene(resultsScene);
                });

            }).start();

        });
        reportBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                clearMessage();
                try {
                    downloadSettings = getDownloadSettings();
                } catch (Exception e) {
                    showMessage(e.getMessage(), Images.EXCLAMATION);
                    return;
                }
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select report file");
                File reportFile = fileChooser.showOpenDialog(reportBtn.getScene().getWindow());
                try {
                    if (reportFile != null) {
                        report.setText(reportFile.getAbsolutePath());
//                        Platform.runLater(() ->

//                        toggleBtnImage(reportBtn, true);


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
                showMessage(PLEASE_WAIT, Images.LOADING);
                if (StringUtils.isBlank(query.getText())) {
                    showMessage("Please enter search query string.", Images.WARNING);
                    return;
                }
                try {
                    try {
                        downloadSettings = getDownloadSettings();
                    } catch (Exception e) {
                        showMessage(e.getMessage(), Images.EXCLAMATION);
                        return;
                    }
//                    Platform.runLater(() -> {
                    new Thread(() -> {
                        Map<String, List<RemoteFile>> fileListMap = new HashMap<>();
                        try {
                            if (runFilesRadio.isSelected()) {
                                fileListMap = new WarehouseQuery().doPortalSearch("read_run", query.getText(), downloadSettings.getMethod()).get();
                            } else if (analysisFilesRadio.isSelected()) {
                                fileListMap = new WarehouseQuery().doPortalSearch("analysis", query.getText(), downloadSettings.getMethod()).get();
                            } else {
                                showMessage("Please select result type to search in.", Images.WARNING);
                                return;
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                        if (fileListMap == null || fileListMap.size() == 0) {
                            showMessage("No downloadable files were found for the given query and result type.", Images.WARNING);
                            return;
                        }

                        Map<String, List<RemoteFile>> finalFileListMap = fileListMap;
                        Platform.runLater(() -> {
                                    Stage primaryStage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
                                    resultsController.renderResults(finalFileListMap, downloadSettings);
                                    primaryStage.setScene(resultsScene);
                                }
                        );
                    }).start();
//                    }                    );


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

    public void showMessage(String message, Images image) {
        log.info(message);
        Platform.runLater(() -> {
            this.fileErrorLabel.setText(message);
            this.fileErrorLabel.setTextFill(image.getTextColor());
            labelImage.setImage(new Image(image.getImage()));
            errorPanel.setVisible(StringUtils.isNotBlank(message));
        });
    }

    public void clearMessage() {
        Platform.runLater(() -> {
            this.fileErrorLabel.setText(null);
            errorPanel.setVisible(false);
        });
    }

    public void clearFields() {
        accession.clear();
//        accessionBtn.setGraphic(null);
//        accessionBtn.setDisable(false);
//        reportBtn.setGraphic(null);
        report.clear();
        clearMessage();
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
                throw new Exception("Aspera Connect client executable not found.");
            }
            if (StringUtils.isBlank(asperaSsh.getText()) || !new File(asperaSsh.getText()).exists()) {
                throw new Exception("Aspera Connect public key certificate not found.");
            }
            if (StringUtils.isBlank(asperaParams.getText())) {
                throw new Exception("Aspera Connect parameters not found.");
            }
            return new DownloadSettings(DownloadSettings.Method.ASPERA, asperaExe.getText(), asperaSsh.getText(), asperaParams.getText());
        }
    }


}



