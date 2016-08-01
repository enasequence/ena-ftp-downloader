package uk.ac.ebi.ena.ftp.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static Parameters parameters;

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            parameters = getParameters();

            Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("gui.fxml"));
            String acc = parameters.getRaw().get(0);
            primaryStage.setTitle("ENA File Downloader: " + acc);
            primaryStage.setScene(new Scene(root, 785, 520));
            primaryStage.setResizable(false);
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
