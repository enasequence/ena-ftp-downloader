package uk.ac.ebi.ena.ftp.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;

public class Main extends Application {

    public static Parameters parameters;
    public static Stage stage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            parameters = getParameters();
            stage = primaryStage;

            System.out.println("parameters:" + StringUtils.join(parameters));

            Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("gui.fxml"));
            String accession = Main.parameters.getUnnamed().size() > 0 ? Main.parameters.getUnnamed().get(0) : Main.parameters.getNamed().get("accession");

            primaryStage.setTitle("ENA File Downloader: " + accession);
            primaryStage.setScene(new Scene(root, 785, 520));
            primaryStage.setResizable(false);
            primaryStage.getIcons().add(new Image("http://www.ebi.ac.uk/web_guidelines/images/logos/ena/ena_100x100.png"));
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        System.exit(0);
    }
}
