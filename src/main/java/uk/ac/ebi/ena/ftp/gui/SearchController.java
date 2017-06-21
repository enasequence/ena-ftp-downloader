package uk.ac.ebi.ena.ftp.gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ena.ftp.model.RemoteFile;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.*;

public class SearchController implements Initializable {

    public static final String ERA_ID_PATTERN = "[ESDR]R[ASPXRZ][0-9]{6,}";
    private final static Logger log = LoggerFactory.getLogger(SearchController.class);
    @FXML
    private TextField accession, report;
    @FXML
    private Button accessionBtn, reportBtn;
    @FXML
    private Label fileErrorLabel;
    private Scene resultsScene;
    private ResultsController resultsController;

    @Override // This method is called by the FXMLLoader when initialization is complete
    public void initialize(URL fxmlFileLocation, ResourceBundle resources) {
        log.debug("initialize");

        setupAccessionBtn();
        setupReportBtn();
    }

    private void setupAccessionBtn() {
        accessionBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                /*Platform.runLater(new Runnable() {
                    @Override public void run() {
                        accessionBtn.setText("Loading...");
                        accessionBtn.setGraphic(getLoadingImage());
                        accessionBtn.setDisable(true);

                    }
                });*/
                String acc = accession.getText();
                if (StringUtils.isBlank(acc)) {
                    showError("Please enter accession.");
                    return;
                }
                if (!acc.matches(ERA_ID_PATTERN)) {
                    showError("Please enter a valid accession.");
                    return;
                }

                Stage primaryStage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
                resultsController.renderResults(new Search(acc));
                primaryStage.setScene(resultsScene);

            }
        });
    }

    private void setupReportBtn() {
        reportBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select report file");
                File reportFile = fileChooser.showOpenDialog(reportBtn.getScene().getWindow());
                if (reportFile != null) {
                    report.setText(reportFile.getAbsolutePath());
//                    reportBtn.setGraphic(getLoadingImage());
                    Map<String, List<RemoteFile>> fileListMap = parseReportFile(reportFile);
                    if (fileListMap.size() == 0) {
                        showError("File is not in the desired format, or does not contain necessary information.");
                        reportBtn.setText("Load Report File");
                        return;
                    }
                    Stage primaryStage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
                    resultsController.renderResults(new Search(fileListMap));
                    primaryStage.setScene(resultsScene);
                }
            }
        });
    }

    private Node getLoadingImage() {
        ImageView imageView = new ImageView();
        imageView.setFitHeight(20);
        imageView.setFitWidth(20);
        imageView.setImage(new Image("http://www.ebi.ac.uk/ena/data/images/loading.gif"));
        Tooltip.install(imageView, new Tooltip("Loading results."));
        return imageView;
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
            }

            submittedIndex = headers.indexOf("submitted_ftp");
            if (submittedIndex > -1) {
                map.put("submitted", new ArrayList<RemoteFile>());
                submittedBytesIndex = headers.indexOf("submitted_bytes");
                submittedMd5Index = headers.indexOf("submitted_md5");
            }

            sraIndex = headers.indexOf("sra_ftp");
            if (sraIndex > -1) {
                map.put("sra", new ArrayList<RemoteFile>());
                sraBytesIndex = headers.indexOf("sra_bytes");
                sraMd5Index = headers.indexOf("sra_md5");
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
                                RemoteFile remoteFile = new RemoteFile(StringUtils.substringAfterLast(fastqFile, "/"),
                                        fastqBytesIndex > -1 ? Long.parseLong(bytes[f]) : 0, fastqFile,
                                        fastqMd5Index > -1 ? md5s[f] : null);
                                log.info(remoteFile.toString());
                                map.get("fastq").add(remoteFile);
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

    public void showError(String message) {
        this.fileErrorLabel.setText(message);
        this.fileErrorLabel.setTextFill(Color.RED);
    }

    public void clearFields() {
        accession.clear();
//        accessionBtn.setGraphic(null);
//        accessionBtn.setDisable(false);
//        reportBtn.setGraphic(null);
        report.clear();
        fileErrorLabel.setText("");
    }
}



