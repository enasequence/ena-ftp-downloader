package uk.ac.ebi.ena.ftp.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static Parameters parameters;

    @Override
    public void start(Stage primaryStage) throws Exception{

        parameters = getParameters();

        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("gui.fxml"));
        primaryStage.setTitle("ENA File Downloader");
        primaryStage.setScene(new Scene(root, 785, 520));
        primaryStage.setResizable(false);
        primaryStage.show();
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
