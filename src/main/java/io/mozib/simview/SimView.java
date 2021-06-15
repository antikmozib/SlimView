package io.mozib.simview;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static io.mozib.simview.Common.*;

public class SimView extends Application {
    private static String[] cmdLineArgs;
    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("mainWindow.fxml"));
        Parent root = fxmlLoader.load();
        MainWindowController controller = fxmlLoader.getController();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("style.css")).toExternalForm());
        stage.setScene(scene);
        stage.setTitle("SimView");
        stage.getIcons().add(
                new Image(Objects.requireNonNull(this.getClass().getResourceAsStream("icons/simview.png"))));
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.show();

        // load recent files
        RecentFiles recentFiles=loadRecentFiles();
        for (RecentFiles.RecentFile recentFile : recentFiles.recentFileList) {
            MenuItem menuItem = new MenuItem(recentFile.getPath());
            menuItem.setOnAction(event -> {
                controller.mainViewModel.loadImage(new ImageModel(menuItem.getText()));
            });
            controller.menuRecent.getItems().add(menuItem);
        }

        if (cmdLineArgs != null && cmdLineArgs.length > 0) {
            controller.mainViewModel.loadImage(new ImageModel(cmdLineArgs[0]));
        }
    }

    @Override
    public void stop() {
        // clear caches...
        var files = new File(cacheDirectory());
        for (File file : Objects.requireNonNull(files.listFiles())) {
            file.delete();
        }
    }

    public static void main(String[] args) {
        cmdLineArgs = args;
        launch();
    }

}