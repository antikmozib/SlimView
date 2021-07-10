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
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.prefs.Preferences;

import static io.mozib.slimview.Common.tempDirectory;

public class SlimView extends Application {
    private static String[] cmdLineArgs;
    private final Preferences preferences = Preferences.userNodeForPackage(this.getClass());

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("mainWindow.fxml"));
        Parent root = fxmlLoader.load();
        MainWindowController controller = fxmlLoader.getController();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("style.css")).toExternalForm());
        stage.setScene(scene);
        stage.setTitle("SlimView");
        stage.getIcons().add(
                new Image(Objects.requireNonNull(this.getClass().getResourceAsStream("icons/slimview.png"))));
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setWidth(preferences.getDouble("MainWindowWidth", 960));
        stage.setHeight(preferences.getDouble("MainWindowHeight", 720));
        stage.show();
        stage.setOnCloseRequest(event -> {
            preferences.putDouble("MainWindowHeight", stage.getScene().getWindow().getHeight());
            preferences.putDouble("MainWindowWidth", stage.getScene().getWindow().getWidth());
        });

        if (cmdLineArgs != null && cmdLineArgs.length > 0) {
            controller.mainViewModel.loadImage(new ImageModel(cmdLineArgs[0]));
        }
    }

    @Override
    public void stop() {
        // clear caches...
        var files = new File(tempDirectory());
        for (File file : Objects.requireNonNull(files.listFiles())) {
            file.delete();
        }
    }

    public static void main(String[] args) {
        cmdLineArgs = args;
        launch();
    }
}