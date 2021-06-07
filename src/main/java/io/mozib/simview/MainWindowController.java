package io.mozib.simview;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
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
    @FXML
    private RadioMenuItem menuFullScreen;

    public MainViewModel mainViewModel = new MainViewModel();
    private final SimpleBooleanProperty isViewingFullscreen = new SimpleBooleanProperty(false);

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        labelStatus.textProperty().bind(mainViewModel.statusProperty());
        imageViewMain.fitWidthProperty().bind(pane.widthProperty());
        imageViewMain.fitHeightProperty().bind(pane.heightProperty());
        imageViewMain.requestFocus();

        // bindings for full screen viewing
        toolBar.managedProperty().bind(toolBar.visibleProperty());
        statusBar.managedProperty().bind(statusBar.visibleProperty());
        menuBar.managedProperty().bind(menuBar.visibleProperty());

        // bind ImageView to selectedImage
        mainViewModel.selectedImageModelProperty().addListener((observable, oldValue, newValue) -> {
            imageViewMain.setImage(newValue.getImage());
            imageViewMain.requestFocus();
            try {
                ((Stage) imageViewMain.getScene().getWindow()).setTitle(newValue.getShortName() + " - SimView");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

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
                toggleFullscreen();
            }
        }
    }

    @FXML
    public void imageViewMain_onClick(MouseEvent mouseEvent) {
        //System.out.println("imageViewMain clicked!");
    }

    @FXML
    public void pane_onKeyPress(KeyEvent keyEvent) {
        if (keyEvent.isAltDown() &&
                keyEvent.getCode() != KeyCode.ENTER &&
                isViewingFullscreen.get()) {
            menuBar.setVisible(!menuBar.isVisible());
            if (menuBar.isVisible()) {
                menuBar.requestFocus();
            }
        }

        switch (keyEvent.getCode()) {
            case LEFT:
                mainViewModel.showPreviousImage();
                break;
            case RIGHT:
                mainViewModel.showNextImage();
                break;
            case ENTER:
                toggleFullscreen();
                break;
            case ESCAPE:
                if (isViewingFullscreen.get()) {
                    toggleFullscreen();
                    break;
                } else {
                    Platform.exit();
                }
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

    @FXML
    public void buttonNext_onAction(ActionEvent actionEvent) {
        mainViewModel.showNextImage();
    }

    @FXML
    public void buttonPrevious_onAction(ActionEvent actionEvent) {
        mainViewModel.showPreviousImage();
    }

    @FXML
    public void buttonFirst_onAction(ActionEvent actionEvent) {
        mainViewModel.showFirstImage();
    }

    @FXML
    public void buttonLast_onAction(ActionEvent actionEvent) {
        mainViewModel.showLastImage();
    }

    private void toggleFullscreen() {
        boolean setFullscreen;
        Stage stage = (Stage) pane.getScene().getWindow();

        stage.setFullScreen(!stage.isFullScreen());
        setFullscreen = stage.isFullScreen();

        isViewingFullscreen.set(setFullscreen);
        toolBar.setVisible(!setFullscreen);
        statusBar.setVisible(!setFullscreen);
        menuBar.setVisible(!setFullscreen);
        menuFullScreen.setSelected(setFullscreen);
    }

    @FXML
    public void menuFullScreen_onAction(ActionEvent actionEvent) {
        toggleFullscreen();
    }

    @FXML
    public void menuBar_onKeyPress(KeyEvent keyEvent) {
        if (isViewingFullscreen.get() && menuBar.isVisible()) {
            if (keyEvent.getCode() == KeyCode.ESCAPE || keyEvent.getCode() == KeyCode.ALT) {
                menuBar.setVisible(false);
                imageViewMain.requestFocus();
            }
        }
    }
}
