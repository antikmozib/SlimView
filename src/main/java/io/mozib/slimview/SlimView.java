/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

import static io.mozib.slimview.Util.getTempDirectory;

public class SlimView extends Application {
    private static String[] cmdLineArgs;
    private final Preferences preferences = Preferences.userNodeForPackage(this.getClass());

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("fxml/mainWindow.fxml"));
        Parent root = fxmlLoader.load();
        MainWindowController controller = fxmlLoader.getController();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("styles/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("SlimView");
        stage.getIcons().add(new Image(this.getClass().getResourceAsStream("icons/slimview.png")));
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        stage.setWidth(preferences.getDouble("MainWindowWidth", 854));
        stage.setHeight(preferences.getDouble("MainWindowHeight", 640));
        stage.setX(preferences.getDouble("MainWindowLeft",
                Screen.getPrimary().getVisualBounds().getWidth() / 2 - stage.getWidth() / 2));
        stage.setY(preferences.getDouble("MainWindowTop",
                Screen.getPrimary().getVisualBounds().getHeight() / 2 - stage.getHeight() / 2));

        if (stage.getWidth() > 0.9 * Screen.getPrimary().getVisualBounds().getWidth() &&
                stage.getHeight() > 0.9 * Screen.getPrimary().getVisualBounds().getHeight()) {
            stage.setMaximized(true);
        }
        stage.show();

        // initialize listeners; must be called after UI has loaded
        controller.initUIListeners();

        stage.setOnCloseRequest(event -> {
            // save window positions
            preferences.putDouble("MainWindowHeight", stage.getScene().getWindow().getHeight());
            preferences.putDouble("MainWindowWidth", stage.getScene().getWindow().getWidth());
            preferences.putDouble("MainWindowTop", stage.getScene().getWindow().getY());
            preferences.putDouble("MainWindowLeft", stage.getScene().getWindow().getX());
        });

        if (cmdLineArgs != null && cmdLineArgs.length > 0) {
            controller.openImage(cmdLineArgs[0]);
        }
    }

    @Override
    public void stop() {
        // clear caches...
        var files = new File(getTempDirectory());
        for (File file : files.listFiles()) {
            file.delete();
        }
    }

    public static void main(String[] args) {
        cmdLineArgs = args;
        launch();
    }
}