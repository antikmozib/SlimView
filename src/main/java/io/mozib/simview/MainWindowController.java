package io.mozib.simview;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainWindowController implements Initializable {
    @FXML
    public RadioMenuItem menuStretched;
    @FXML
    public ImageView imageViewMain;
    @FXML
    public RadioMenuItem menuFitToWindow;
    @FXML
    public RadioMenuItem menuOriginalSize;
    @FXML
    public RadioMenuItem menuFitHeight;
    @FXML
    public RadioMenuItem menuFitWidth;
    @FXML
    public RadioMenuItem menuFullScreen;
    @FXML
    public ScrollPane pane;
    @FXML
    public Label labelStatus;
    @FXML
    public ToolBar toolBar;
    @FXML
    public HBox statusBar;
    @FXML
    public MenuBar menuBar;
    @FXML
    public MenuItem menuClose;
    @FXML
    public Button buttonPrevious;
    @FXML
    public Button buttonNext;

    private final ToggleGroup toggleGroup = new ToggleGroup();
    public MainViewModel mainViewModel = new MainViewModel();
    private final SimpleBooleanProperty isViewingFullscreen = new SimpleBooleanProperty(false);

    private enum ViewStyle {
        FitToWindow, Original, Stretched
    }

    private final SimpleObjectProperty<ViewStyle> viewStyleProperty = new SimpleObjectProperty<>();

    private void toggleFullScreen() {
        boolean setFullScreen = !isViewingFullscreen.get();
        ((Stage) pane.getScene().getWindow()).setFullScreen(setFullScreen);
        menuBar.setVisible(!setFullScreen);
        toolBar.setVisible(!setFullScreen);
        statusBar.setVisible(!setFullScreen);
        isViewingFullscreen.set(setFullScreen);
        menuFullScreen.setSelected(setFullScreen);
    }

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        labelStatus.textProperty().bind(mainViewModel.statusProperty());
        imageViewMain.requestFocus();

        // menubar toggle group
        menuStretched.setToggleGroup(toggleGroup);
        menuFitToWindow.setToggleGroup(toggleGroup);
        menuOriginalSize.setToggleGroup(toggleGroup);

        // bindings for fullscreen viewing
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

        viewStyleProperty.addListener(((observable, oldValue, newValue) -> {
            imageViewMain.fitWidthProperty().unbind();
            imageViewMain.fitHeightProperty().unbind();
            pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

            if (imageViewMain.getImage() != null) {
                imageViewMain.setFitWidth(imageViewMain.getImage().getWidth());
                imageViewMain.setFitHeight(imageViewMain.getImage().getHeight());
            }

            imageViewMain.setPreserveRatio(true);

            switch (newValue) {
                case Original:
                    menuOriginalSize.setSelected(true);
                    pane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    pane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    break;

                case FitToWindow:
                    menuFitToWindow.setSelected(true);
                    imageViewMain.fitWidthProperty().bind(pane.widthProperty());
                    imageViewMain.fitHeightProperty().bind(pane.heightProperty());
                    break;

                case Stretched:
                    menuStretched.setSelected(true);
                    imageViewMain.setPreserveRatio(false);
                    imageViewMain.fitWidthProperty().bind(pane.widthProperty());
                    imageViewMain.fitHeightProperty().bind(pane.heightProperty());
                    break;
            }
        }));
        viewStyleProperty.set(ViewStyle.FitToWindow);

        System.out.println("Initialized!");
    }

    @FXML
    public void imageViewMain_onClick(MouseEvent mouseEvent) {
    }

    @FXML
    public void pane_onClick(MouseEvent mouseEvent) {
        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            pane.requestFocus();
            if (mouseEvent.getClickCount() == 2) {
                toggleFullScreen();
            }
        }
    }

    @FXML
    public void pane_onKeyPress(KeyEvent keyEvent) {
        System.out.println("Key pressed!");

        if (keyEvent.isAltDown() && keyEvent.getCode() != KeyCode.ENTER && isViewingFullscreen.get()) {
            menuBar.setVisible(!menuBar.isVisible());
            if (menuBar.isVisible()) {
                menuBar.requestFocus();
            }
        } else {

            switch (keyEvent.getCode()) {
                case LEFT:
                    mainViewModel.showPreviousImage();
                    break;
                case RIGHT:
                    mainViewModel.showNextImage();
                    break;
                case ENTER:
                    toggleFullScreen();
                    break;
                case ESCAPE:
                    if (isViewingFullscreen.get()) {
                        toggleFullScreen();
                        break;
                    } else {
                        Platform.exit();
                    }
            }
        }
    }

    @FXML
    public void pane_onScroll(ScrollEvent scrollEvent) {
        if (scrollEvent.getDeltaY() > 0 || scrollEvent.getDeltaX() > 0) {
            mainViewModel.showNextImage();
        } else {
            mainViewModel.showPreviousImage();
        }
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

    @FXML
    public void buttonEdit_onAction(ActionEvent actionEvent) {
        if (mainViewModel.getSelectedImage() != null) {
            mainViewModel.getSelectedImage().OpenInEditor();
        }
    }

    @FXML
    public void menuClose_onAction(ActionEvent actionEvent) {
        Platform.exit();
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
    public void menuFullScreen_onAction(ActionEvent actionEvent) {
        toggleFullScreen();
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

    @FXML
    public void menuOriginalSize_onAction(ActionEvent actionEvent) {
        viewStyleProperty.set(ViewStyle.Original);
    }

    @FXML
    public void menuFitToWindow_onAction(ActionEvent actionEvent) {
        viewStyleProperty.set(ViewStyle.FitToWindow);
    }

    @FXML
    public void menuStretched_onAction(ActionEvent actionEvent) {
        viewStyleProperty.set(ViewStyle.Stretched);
    }

    @FXML
    public void menuOpen_onAction(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files (jpg, png, gif)", "*.jpg;*.png;*.gif")
        );
        File file = fileChooser.showOpenDialog(imageViewMain.getScene().getWindow());
        if (file != null) {
            mainViewModel.loadImage(new ImageModel(file.getPath()));
        }
    }

    @FXML
    public void menuOpenContainingFolder_onAction(ActionEvent actionEvent) {
        if (mainViewModel.getSelectedImage() != null) {
            mainViewModel.getSelectedImage().OpenContainingFolder();
        }
    }

    @FXML
    public void menuOpenInExternalEditor_onAction(ActionEvent actionEvent) {
        if (mainViewModel.getSelectedImage() != null) {
            mainViewModel.getSelectedImage().OpenInEditor();
        }
    }

}
