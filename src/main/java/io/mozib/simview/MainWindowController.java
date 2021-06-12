package io.mozib.simview;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainWindowController implements Initializable {
    @FXML
    public HBox hBoxMain;
    @FXML
    public RadioMenuItem menuStretched;
    @FXML
    public ImageView imageViewMain;
    @FXML
    public RadioMenuItem menuFitToWindow;
    @FXML
    public RadioMenuItem menuOriginalSize;
    @FXML
    public RadioMenuItem menuFullScreen;
    @FXML
    public ScrollPane mainScrollPane;
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
    private final SimpleBooleanProperty isViewingFullScreen = new SimpleBooleanProperty(false);

    private enum ViewStyle {
        FIT_TO_WINDOW, ORIGINAL, STRETCHED
    }

    private final SimpleObjectProperty<ViewStyle> viewStyleProperty = new SimpleObjectProperty<>(ViewStyle.ORIGINAL);

    private void toggleFullScreen() {
        boolean setFullScreen = !isViewingFullScreen.get();
        ((Stage) mainScrollPane.getScene().getWindow()).setFullScreen(setFullScreen);
        menuBar.setVisible(!setFullScreen);
        toolBar.setVisible(!setFullScreen);
        statusBar.setVisible(!setFullScreen);
        isViewingFullScreen.set(setFullScreen);
        menuFullScreen.setSelected(setFullScreen);
    }

    private void centerImageView() {
        if (imageViewMain.getFitWidth() < mainScrollPane.getWidth()) {
            imageViewMain.setLayoutX((mainScrollPane.getWidth() - imageViewMain.getFitWidth()) / 2);
        }
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

        viewStyleProperty.addListener(((ObservableValue<? extends ViewStyle> observable, ViewStyle oldValue, ViewStyle newValue) -> {
            if (newValue == null) return;

            imageViewMain.fitWidthProperty().unbind();
            imageViewMain.fitHeightProperty().unbind();
            
            mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            
            mainScrollPane.setFitToHeight(false);
            mainScrollPane.setFitToWidth(false);

            if (imageViewMain.getImage() != null) {
                imageViewMain.setFitWidth(mainViewModel.getSelectedImage().getOriginalWidth());
                imageViewMain.setFitHeight(mainViewModel.getSelectedImage().getOriginalHeight());
            }

            imageViewMain.setPreserveRatio(true);

            switch (newValue) {
                case ORIGINAL:
                    menuOriginalSize.setSelected(true);
                    
                    mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    break;

                case FIT_TO_WINDOW:
                    menuFitToWindow.setSelected(true);
                    
                    mainScrollPane.setFitToHeight(true);
                    mainScrollPane.setFitToWidth(true);
                    
                    imageViewMain.fitWidthProperty().bind(mainScrollPane.widthProperty().subtract(8));
                    imageViewMain.fitHeightProperty().bind(mainScrollPane.heightProperty().subtract(8));
                    break;

                case STRETCHED:
                    menuStretched.setSelected(true);
                    
                    mainScrollPane.setFitToHeight(true);
                    mainScrollPane.setFitToWidth(true);
                    
                    imageViewMain.setPreserveRatio(false);
                    
                    imageViewMain.fitWidthProperty().bind(mainScrollPane.widthProperty().subtract(8));
                    imageViewMain.fitHeightProperty().bind(mainScrollPane.heightProperty().subtract(8));
                    break;
            }
        }));

        mainViewModel.selectedImageModelProperty().addListener((
                (ObservableValue<? extends ImageModel> observableValue, ImageModel imageModel, ImageModel t1) -> {
                    // force trigger change listener
                    var oldViewStyle = viewStyleProperty.get();
                    viewStyleProperty.set(null);
                    viewStyleProperty.set(oldViewStyle);
                }));
        viewStyleProperty.set(ViewStyle.FIT_TO_WINDOW);
    }

    @FXML
    public void imageViewMain_onClick(MouseEvent mouseEvent) {
    }

    @FXML
    public void mainScrollPane_onClick(MouseEvent mouseEvent) {
        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            mainScrollPane.requestFocus();
            if (mouseEvent.getClickCount() == 2) {
                toggleFullScreen();
            }
        } else if (mouseEvent.getButton().equals(MouseButton.MIDDLE)) {
            toggleFullScreen();
        }
    }

    @FXML
    public void mainScrollPane_onKeyPress(KeyEvent keyEvent) {
        if (keyEvent.isAltDown() && keyEvent.getCode() != KeyCode.ENTER && isViewingFullScreen.get()) {
            menuBar.setVisible(!menuBar.isVisible());
            if (menuBar.isVisible()) {
                menuBar.requestFocus();
            }
        } else {

            switch (keyEvent.getCode()) {
                case LEFT:
                case PAGE_DOWN:
                    mainViewModel.showPreviousImage();
                    break;
                case RIGHT:
                case PAGE_UP:
                    mainViewModel.showNextImage();
                    break;
                case HOME:
                    mainViewModel.showFirstImage();
                    break;
                case END:
                    mainViewModel.showLastImage();
                    break;
                case ENTER:
                    toggleFullScreen();
                    break;
                case ESCAPE:
                    if (isViewingFullScreen.get()) {
                        toggleFullScreen();
                        break;
                    } else {
                        Platform.exit();
                    }
            }
        }
    }

    @FXML
    public void mainScrollPane_onScroll(ScrollEvent scrollEvent) {
        if (scrollEvent.getDeltaY() > 0 || scrollEvent.getDeltaX() > 0) {
            mainViewModel.showPreviousImage();
        } else {
            mainViewModel.showNextImage();
        }
        scrollEvent.consume();
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
            mainViewModel.getSelectedImage().openInEditor();
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
        if (isViewingFullScreen.get() && menuBar.isVisible()) {
            if (keyEvent.getCode() == KeyCode.ESCAPE || keyEvent.getCode() == KeyCode.ALT) {
                menuBar.setVisible(false);
                imageViewMain.requestFocus();
            }
        }
    }

    @FXML
    public void menuOriginalSize_onAction(ActionEvent actionEvent) {
        viewStyleProperty.set(ViewStyle.ORIGINAL);
    }

    @FXML
    public void menuFitToWindow_onAction(ActionEvent actionEvent) {
        viewStyleProperty.set(ViewStyle.FIT_TO_WINDOW);
    }

    @FXML
    public void menuStretched_onAction(ActionEvent actionEvent) {
        viewStyleProperty.set(ViewStyle.STRETCHED);
    }

    @FXML
    public void menuOpen_onAction(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg;*.jpeg;*.png;*.gif")
        );
        File file = fileChooser.showOpenDialog(imageViewMain.getScene().getWindow());
        if (file != null) {
            mainViewModel.loadImage(new ImageModel(file.getPath()));
        }
    }

    @FXML
    public void menuOpenContainingFolder_onAction(ActionEvent actionEvent) {
        if (mainViewModel.getSelectedImage() != null) {
            mainViewModel.getSelectedImage().openContainingFolder();
        }
    }

    @FXML
    public void menuOpenInExternalEditor_onAction(ActionEvent actionEvent) {
        if (mainViewModel.getSelectedImage() != null) {
            mainViewModel.getSelectedImage().openInEditor();
        }
    }

}
