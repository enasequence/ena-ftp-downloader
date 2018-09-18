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
import java.net.URLDecoder;
import java.util.*;

public class SearchController implements Initializable {

    public static final String ERA_ID_PATTERN = "([ESDR]R[ASPXRZ][0-9]{6,}|SAMEA[0-9]{6,}|SAM[ND][0-9]{8,}|PRJ[A-Z]{2}[0-9]+)";
    private final static Logger log = LoggerFactory.getLogger(SearchController.class);
    public static final String PLEASE_WAIT = "Please wait...";
    @FXML
    private TextField accession, report, asperaExe, asperaSsh, asperaParams, otherParams;

    @FXML
    private TextArea query;

    @FXML
    private Button accessionBtn, reportBtn, searchBtn, asperaExeBtn, asperaSshBtn, reportHelpBtn, reportLoadBtn, asperaSaveBtn, otherParamsHelpBtn;

    @FXML
    private RadioButton runFilesRadio, analysisFilesRadio, ftpRadio, asperaRadio;

    @FXML
    private TitledPane asperaConfig;

    @FXML
    private Hyperlink searchHelpLink;

    @FXML
    private TitledPane accTPane, reportTPane, searchTPane, settingsTPane;

    @FXML
    private Label fileErrorLabel, reportLoadingLabel, asperaLabel;

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
        boolean isWebstart = Main.parameters.getUnnamed().size() > 0;
//        searchLoadingImg.setVisible(false);
        setupSettingsPane(isWebstart);

        setupAccBtn();
        setupReportBtn();
        setupSearchBtn();

        // for webstart
        if (!isWebstart) {
            //nothing more to do here
            return;
        }

        Map<String, String> webstartParams = parseWebstartParams(Main.parameters.getUnnamed().get(0));
        String accessionParam = webstartParams.get("accession");
        if (StringUtils.isNotBlank(accessionParam)) {
            accession.setText(accessionParam);
            accessionBtn.fire();
            return;
        }
        doAutoSearch(webstartParams);

    }

    private void doAutoSearch(Map<String, String> webstartParams) {
        String queryParam = webstartParams.get("query");
        String result = webstartParams.get("result");
        List<String> params = new ArrayList<>();
        String dataPortal = webstartParams.get("dataPortal");
        if (StringUtils.isNotBlank(dataPortal)) {
            params.add("dataPortal=" + dataPortal);
        }
        String limit = webstartParams.get("limit");
        if (StringUtils.isNotBlank(limit)) {
            params.add("limit=" + limit);
        }
        String includeMetagenomes = webstartParams.get("includeMetagenomes");
        if (StringUtils.isNotBlank(includeMetagenomes)) {
            params.add("includeMetagenomes=" + includeMetagenomes);
        }
        String dccDataOnly = webstartParams.get("dccDataOnly");
        if (StringUtils.isNotBlank(dccDataOnly)) {
            params.add("dccDataOnly=" + dccDataOnly);
        }
        if (StringUtils.isNotBlank(queryParam)) {
            try {
                query.setText(URLDecoder.decode(queryParam, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                showMessage("Error parsing query string. Please correct any invalid characters and retry", Images.EXCLAMATION);
            }
        }
        if ("read_run".equals(result)) {
            runFilesRadio.setSelected(true);
        } else if ("analysis".equals(result)) {
            analysisFilesRadio.setSelected(true);
        }
        if (params.size() > 0) {
            otherParams.setText(StringUtils.join(params, "&"));
        }
        searchBtn.fire();
        return;
    }

    private Map<String, String> parseWebstartParams(String text) {
        log.info(text);
        Map<String, String> map = new HashMap<>();
        String[] split = text.split("&");
        for (String s : split) {
            String[] split1 = s.split("=");
            map.put(split1[0], split1[1]);
        }
        return map;
    }

    private void setupSettingsPane(boolean isWebstart) {
        settingsTPane.heightProperty().addListener((obs, oldHeight, newHeight) -> stage.sizeToScene());

        if (isWebstart) {
            // no aspera
            asperaLabel.setVisible(false);
            asperaRadio.setVisible(false);
            return;
        }
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
    }

    private void saveAsperaSettings() {
        try {
            DownloadSettings downloadSettings = getDownloadSettings();
            File jarDir = getJarDir();//new File(ClassLoader.getSystemClassLoader().getResource(".").getPath());
            log.info(jarDir.getAbsolutePath());
            if (!jarDir.canWrite()) {
                showMessage("Unable to save settings file to " + jarDir.getAbsolutePath() + ". Please make sure it has write access.", Images.EXCLAMATION);
            }
            File file = new File(jarDir.getAbsolutePath() + File.separator + "ena-file-downloader.settings");
            log.info(file.getAbsolutePath());
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(file));
            objectOutputStream.writeObject(downloadSettings);
            objectOutputStream.close();
            showMessage("Configuration saved to " + file.getAbsolutePath(), Images.TICK);
        } catch (Exception e) {
            log.error("Error saving settings", e);
            showMessage("Error while saving Aspera settings:" + e.getMessage(), Images.EXCLAMATION);
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
            String suffix = "/" + (this.getClass().getName()).replace(".", "/") + ".class";
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
        } catch (URISyntaxException ex) {
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
                    showMessage("Configuration loaded from " + settings.getAbsolutePath(), Images.TICK);
                }
            }
        } catch (Exception e) {
            log.error("Error loading settings", e);
            showMessage("Error while loading Aspera settings:" + e.getMessage(), Images.EXCLAMATION);
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
                showMessage("Error loading download configuration:" + e.getMessage(), Images.EXCLAMATION);
                return;
            }

            showMessage(PLEASE_WAIT, Images.LOADING);

            new Thread(() -> {
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
            try {
                downloadSettings = getDownloadSettings();
            } catch (Exception e) {
                showMessage("Error loading download configuration:" + e.getMessage(), Images.EXCLAMATION);
                return;
            }
            File reportFile = new File(report.getText());
            new Thread(() -> {
                Map<String, List<RemoteFile>> fileListMap = null;
                try {
                    fileListMap = new ReportParser().parseExternalReportFile(reportFile, downloadSettings.getMethod());
                } catch (Exception e) {
                    log.error("Parsing error:", e);
                    showMessage("Error parsing report file:" + e.getMessage(), Images.WARNING);
                    return;
                }

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

                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select report file");
                File reportFile = fileChooser.showOpenDialog(reportBtn.getScene().getWindow());
                try {
                    if (reportFile != null) {
                        report.setText(reportFile.getAbsolutePath());

                    }
                } finally {
                }
            }
        });

        reportTPane.heightProperty().addListener((obs, oldHeight, newHeight) -> stage.sizeToScene());

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
                Map<String, String> otherParamsMap = new HashMap<>();
                if (!StringUtils.isBlank(otherParams.getText())) {
                    try {
                        otherParamsMap = parseOtherParams(otherParams.getText());
                    } catch (Exception e) {
                        log.error("Error in other params", e);
                        showMessage("Invalid parameters found in 'Other Parameters'", Images.EXCLAMATION);
                        return;
                    }

                }
                try {
                    try {
                        downloadSettings = getDownloadSettings();
                    } catch (Exception e) {
                        showMessage("Error loading download configuration:" + e.getMessage(), Images.EXCLAMATION);
                        return;
                    }
                    Map<String, String> finalOtherParamsMap = otherParamsMap;
                    new Thread(() -> {
                        Map<String, List<RemoteFile>> fileListMap = new HashMap<>();
                        if (runFilesRadio.isSelected()) {
                            fileListMap = new WarehouseQuery().doPortalSearch("read_run", query.getText(), finalOtherParamsMap, downloadSettings.getMethod());
                        } else if (analysisFilesRadio.isSelected()) {
                            fileListMap = new WarehouseQuery().doPortalSearch("analysis", query.getText(), finalOtherParamsMap, downloadSettings.getMethod());
                        } else {
                            showMessage("Please select result type to search in.", Images.WARNING);
                            return;
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


                } finally {

                }
            }
        });

        searchHelpLink.setOnAction(t -> {
            this.hostServices.showDocument("http://www.ebi.ac.uk/ena/browse/search-rest");
        });

        searchTPane.heightProperty().addListener((obs, oldHeight, newHeight) -> stage.sizeToScene());

        otherParamsHelpBtn.setOnAction(event -> {
            this.hostServices.showDocument("http://www.ebi.ac.uk/ena/portal/api/doc");
        });
    }

    private Map<String, String> parseOtherParams(String text) throws Exception {
        Map<String, String> map = new HashMap<>();
        String[] split = text.split("&");
        for (String s : split) {
            String[] split1 = s.split("=");
            if (!WarehouseQuery.PORTAL_SEARCH_PARAMETERS.contains(split1[0])) {
                throw new Exception("Unknown param:" + split1[0]);
            }
            map.put(split1[0], split1[1]);
        }
        return map;

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
//        accession.clear();
//        accessionBtn.setGraphic(null);
//        accessionBtn.setDisable(false);
//        reportBtn.setGraphic(null);
//        report.clear();
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
            if (StringUtils.isBlank(asperaExe.getText()) || (!"ascp".equals(asperaExe.getText()) && !(asperaExe.getText().contains("ascp") && new File(asperaExe.getText()).exists()))) {
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



