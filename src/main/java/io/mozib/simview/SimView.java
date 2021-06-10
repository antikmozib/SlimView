package io.mozib.simview;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import java.io.IOException;

public class SimView extends Application {
    private static String[] cmdLineArgs;

    public enum OSType {
        Windows, Mac, Linux
    }

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("mainWindow.fxml"));
        Parent root = fxmlLoader.load();
        MainWindowController controller = fxmlLoader.getController();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("SimView");
        stage.getIcons().add(new Image(this.getClass().getResourceAsStream("icons/simview.png")));
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.show();

        if (cmdLineArgs != null && cmdLineArgs.length > 0) {
            controller.mainViewModel.loadImage(new ImageModel(cmdLineArgs[0]));
        }
    }

    public static void main(String[] args) {
        cmdLineArgs = args;
        launch();
    }

    public static OSType getOSType() {
        String platform = System.getProperty("os.name").toLowerCase();
        if (platform.contains("win")) {
            return OSType.Windows;
        } else if (platform.contains("mac")) {
            return OSType.Mac;
        } else if (platform.contains("nux")) {
            return OSType.Linux;
        }
        return null;
    }
}