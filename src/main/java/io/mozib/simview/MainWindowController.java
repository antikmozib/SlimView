package io.mozib.simview;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ToolBar;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

public class MainWindowController implements Initializable {

    @FXML
    public ImageView imageViewMain;

    @FXML
    private Pane pane;

    @FXML
    private Label labelStatus;

    @FXML
    private ToolBar toolBar;

    @FXML
    private HBox statusBar;

    @FXML
    private MenuBar menuBar;

    public MainViewModel mainViewModel;

    private SimpleBooleanProperty isViewingFullscreen;

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        mainViewModel = new MainViewModel();
        isViewingFullscreen = new SimpleBooleanProperty(false);

        labelStatus.textProperty().bind(mainViewModel.statusProperty());
        imageViewMain.setPreserveRatio(false);
        imageViewMain.fitWidthProperty().bind(pane.widthProperty());
        imageViewMain.fitHeightProperty().bind(pane.heightProperty());
        imageViewMain.requestFocus();

        // bindings for full screen viewing
        toolBar.managedProperty().bind(toolBar.visibleProperty());
        statusBar.managedProperty().bind(statusBar.visibleProperty());
        menuBar.managedProperty().bind(menuBar.visibleProperty());
        toolBar.visibleProperty().bind(isViewingFullscreen.not());
        statusBar.visibleProperty().bind(isViewingFullscreen.not());
        menuBar.visibleProperty().bind(isViewingFullscreen.not());
        System.out.println("Initialized!");
    }

    @FXML
    public void menuClose_onAction(ActionEvent actionEvent) {
        System.out.println("Clicked!");
        Platform.exit();
    }

    @FXML
    public void pane_onClick(MouseEvent mouseEvent) {
        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            pane.requestFocus();
            if (mouseEvent.getClickCount() == 2) {
                Stage stage = (Stage) pane.getScene().getWindow();
                if (stage.isFullScreen()) {
                    stage.setFullScreen(false);
                    isViewingFullscreen.set(false);
                } else {
                    stage.setFullScreen(true);
                    isViewingFullscreen.set(true);
                }
            }
        }
    }

    @FXML
    public void imageViewMain_onClick(MouseEvent mouseEvent) {
        System.out.println("imageViewMain clicked!");
    }

    @FXML
    public void pane_onKeyPress(KeyEvent keyEvent) {
        if (keyEvent.isAltDown() && ((Stage) pane.getScene().getWindow()).isFullScreen()) {
            menuBar.setVisible(!menuBar.isVisible());
        }

        switch (keyEvent.getCode()) {
            case LEFT:
                mainViewModel.showPreviousImage();
                break;
            case RIGHT:
                mainViewModel.showNextImage();
                break;
        }
    }

    @FXML
    public void menuAbout_onAction(ActionEvent actionEvent) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource("aboutWindow.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        Stage aboutWindow = new Stage();
        aboutWindow.setScene(scene);
        aboutWindow.initModality(Modality.WINDOW_MODAL);
        aboutWindow.initStyle(StageStyle.UTILITY);
        aboutWindow.initOwner(imageViewMain.getScene().getWindow());
        aboutWindow.setResizable(false);
        aboutWindow.setIconified(false);
        aboutWindow.setTitle("About");
        aboutWindow.show();
    }


    public void buttonNext_onAction(ActionEvent actionEvent) {
        mainViewModel.showNextImage();
    }

    public void buttonPrevious_onAction(ActionEvent actionEvent) {
        mainViewModel.showPreviousImage();
    }

    public void buttonFirst_onAction(ActionEvent actionEvent) {
        mainViewModel.showFirstImage();
    }

    public void buttonLast_onAction(ActionEvent actionEvent) {
        mainViewModel.showLastImage();
    }
}
