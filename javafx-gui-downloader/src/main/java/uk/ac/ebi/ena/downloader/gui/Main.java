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

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {

    static final Logger log = LoggerFactory.getLogger(Main.class);

    public static Parameters parameters;
    public static Stage stage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            parameters = getParameters();
            stage = primaryStage;

            System.out.println("parameters:" + StringUtils.join(parameters));
            log.info("parameters:" + StringUtils.join(parameters));

            FXMLLoader firstPaneLoader = new FXMLLoader(getClass().getClassLoader().getResource("search.fxml"));
            Parent searchPane = firstPaneLoader.load();
            Scene searchScene = new Scene(searchPane);

            FXMLLoader secondPaneLoader = new FXMLLoader(getClass().getClassLoader().getResource("results.fxml"));
            Parent resultsPane = secondPaneLoader.load();
            Scene resultScene = new Scene(resultsPane);

            // injecting second scene into the controller of the first scene
            SearchController firstPaneController = (SearchController) firstPaneLoader.getController();
            firstPaneController.setStage(stage);
            firstPaneController.setResultsScene(resultScene);
            firstPaneController.setHostServices(getHostServices());

            // injecting first scene into the controller of the second scene
            ResultsController secondPaneController = (ResultsController) secondPaneLoader.getController();
            secondPaneController.setSearchScene(searchScene);
            firstPaneController.setResultsController(secondPaneController);
            secondPaneController.setSearchController(firstPaneController);

            primaryStage.setTitle("ENA File Downloader");
            primaryStage.setScene(searchScene);
            primaryStage.setResizable(false);
            primaryStage.getIcons().add(new Image("http://www.ebi.ac.uk/web_guidelines/images/logos/ena/ena_100x100.png"));
            primaryStage.show();
            secondPaneController.setStage(primaryStage);

        } catch (Exception e) {
            log.error("Error in main", e);
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        log.info("Exiting downloader.");
        System.exit(0);
    }
}
