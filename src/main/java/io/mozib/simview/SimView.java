package io.mozib.simview;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class SimView extends Application {
    private static Scene scene;
    private static String[] cmdLineArgs;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fXMLLoader = new FXMLLoader(getClass().getResource("mainWindow.fxml"));
        Parent root = fXMLLoader.load();
        MainWindowController controller = fXMLLoader.getController();

        scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("SimView");
        stage.getIcons().add(new Image(this.getClass().getResourceAsStream("icons/simview.png")));
        stage.setFullScreenExitHint("");
        stage.show();

        if (cmdLineArgs != null && cmdLineArgs.length > 0) {
            controller.loadImage(new ImageModel(cmdLineArgs[0]));
        }
    }

    public static void main(String[] args) {
        cmdLineArgs = args;
        launch();
    }
}