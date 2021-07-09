package io.mozib.slimview;

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
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import static io.mozib.slimview.Common.loadRecentFiles;

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
    public RadioMenuItem menuFullScreen;
    @FXML
    public RadioMenuItem menuSortByName;
    @FXML
    public RadioMenuItem menuSortByCreated;
    @FXML
    public RadioMenuItem menuSortByModified;
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
    public Menu menuRecent;
    @FXML
    public Button buttonPrevious;
    @FXML
    public Button buttonNext;

    private final int scrollPaneOffset = 6; // to force correct clipping of scroll pane
    private final ToggleGroup toggleGroupViewStyle = new ToggleGroup();
    private final ToggleGroup toggleGroupSortStyle = new ToggleGroup();
    public MainViewModel mainViewModel = new MainViewModel();
    private final Preferences preferences = Preferences.userNodeForPackage(this.getClass());
    private final SimpleBooleanProperty isViewingFullScreen = new SimpleBooleanProperty(false);
    private final SimpleObjectProperty<ViewStyle> viewStyleProperty = new SimpleObjectProperty<>(ViewStyle.ORIGINAL);

    @FXML
    public void menuResize_onAction(ActionEvent actionEvent) {
        if (mainViewModel.getSelectedImageModel() == null) return;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("resizeWindow.fxml"));
        Parent root = null;
        try {
            root = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Scene scene = new Scene(root);
        Stage resizeWindow = new Stage();
        ResizeViewModel resizeViewModel = new ResizeViewModel(
                mainViewModel.getSelectedImageModel().getWidth(),
                mainViewModel.getSelectedImageModel().getHeight());
        resizeWindow.setScene(scene);
        resizeWindow.setTitle("Resize");
        resizeWindow.initModality(Modality.WINDOW_MODAL);
        resizeWindow.initStyle(StageStyle.UTILITY);
        resizeWindow.initOwner(imageViewMain.getScene().getWindow());
        ResizeWindowController controller = fxmlLoader.getController();
        controller.setViewModel(resizeViewModel);
        resizeWindow.showAndWait();

        if (resizeViewModel.useNewValues.get()) {
            viewStyleProperty.set(ViewStyle.ORIGINAL);
            mainViewModel.resizeImage(
                    mainViewModel.getSelectedImageModel(),
                    Integer.parseInt(resizeViewModel.newWidthProperty.get()),
                    Integer.parseInt(resizeViewModel.newHeightProperty.get()));
        }

    }

    @FXML
    public void menuSaveAs_onAction(ActionEvent actionEvent) {
        saveAs();
    }

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        labelStatus.textProperty().bind(mainViewModel.statusProperty());
        imageViewMain.requestFocus();

        // menubar toggle group
        menuStretched.setToggleGroup(toggleGroupViewStyle);
        menuFitToWindow.setToggleGroup(toggleGroupViewStyle);
        menuOriginalSize.setToggleGroup(toggleGroupViewStyle);
        menuSortByName.setToggleGroup(toggleGroupSortStyle);
        menuSortByCreated.setToggleGroup(toggleGroupSortStyle);
        menuSortByModified.setToggleGroup(toggleGroupSortStyle);

        // bindings for fullscreen viewing
        toolBar.managedProperty().bind(toolBar.visibleProperty());
        statusBar.managedProperty().bind(statusBar.visibleProperty());
        menuBar.managedProperty().bind(menuBar.visibleProperty());

        // bind ImageView to selectedImage
        mainViewModel.selectedImageModelProperty().addListener((observable, oldValue, newValue) -> {
            imageViewMain.setImage(newValue.getImage());
            imageViewMain.requestFocus();
            try {
                ((Stage) imageViewMain.getScene().getWindow()).setTitle(newValue.getShortName() + " - SlimView");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        viewStyleProperty.addListener(
                ((ObservableValue<? extends ViewStyle> observable, ViewStyle oldValue, ViewStyle newValue) -> {
                    if (newValue == null) {
                        newValue = Objects.requireNonNullElse(oldValue, ViewStyle.FIT_TO_WINDOW);
                    }

                    imageViewMain.fitWidthProperty().unbind();
                    imageViewMain.fitHeightProperty().unbind();

                    mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                    mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

                    mainScrollPane.setFitToHeight(true);
                    mainScrollPane.setFitToWidth(true);

                    if (imageViewMain.getImage() != null) {
                        imageViewMain.setFitWidth(mainViewModel.getSelectedImageModel().getWidth());
                        imageViewMain.setFitHeight(mainViewModel.getSelectedImageModel().getHeight());
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
                            imageViewMain.fitWidthProperty().bind(mainScrollPane.widthProperty().subtract(
                                    scrollPaneOffset));
                            imageViewMain.fitHeightProperty().bind(mainScrollPane.heightProperty().subtract(
                                    scrollPaneOffset));
                            break;

                        case STRETCHED:
                            menuStretched.setSelected(true);
                            imageViewMain.setPreserveRatio(false);
                            imageViewMain.fitWidthProperty().bind(mainScrollPane.widthProperty().subtract(
                                    scrollPaneOffset));
                            imageViewMain.fitHeightProperty().bind(mainScrollPane.heightProperty().subtract(
                                    scrollPaneOffset));
                            break;
                    }

                    preferences.put("LastViewStyle", newValue.toString());
                })
        );

        mainViewModel.selectedImageModelProperty().addListener(
                ((ObservableValue<? extends ImageModel> observableValue, ImageModel imageModel, ImageModel t1) -> {
                    // force trigger change listener
                    var oldViewStyle = viewStyleProperty.get();
                    viewStyleProperty.set(null);
                    viewStyleProperty.set(oldViewStyle);
                })
        );

        mainViewModel.selectedSortStyleProperty().addListener(
                ((observable, oldValue, newValue) -> {
                    switch (newValue) {
                        case NAME:
                            menuSortByName.setSelected(true);
                            break;
                        case DATE_CREATED:
                            menuSortByCreated.setSelected(true);
                            break;
                        case DATE_MODIFIED:
                            menuSortByModified.setSelected(true);
                            break;
                    }
                    preferences.put("LastSortStyle", newValue.toString());
                })
        );

        viewStyleProperty.set(ViewStyle.valueOf(preferences.get("LastViewStyle", ViewStyle.FIT_TO_WINDOW.toString())));
        mainViewModel.sortImages(MainViewModel.SortStyle.valueOf(
                preferences.get("LastSortStyle", MainViewModel.SortStyle.DATE_MODIFIED.toString()))); // default sorting

        // load recent files
        RecentFiles recentFiles = loadRecentFiles();
        for (RecentFiles.RecentFile recentFile : recentFiles.recentFileList) {
            MenuItem menuItem = new MenuItem(recentFile.getPath());
            menuItem.setOnAction(event -> {
                mainViewModel.loadImage(new ImageModel(menuItem.getText()));
            });
            menuRecent.getItems().add(menuItem);
        }
    }

    @FXML
    public void menuRotateLeft_onAction(ActionEvent actionEvent) {
        rotateLeft();
    }

    @FXML
    public void menuRotateRight_onAction(ActionEvent actionEvent) {
        rotateRight();
    }

    @FXML
    public void menuFlipVertically_onAction(ActionEvent actionEvent) {
        mainViewModel.flipVertically();
    }

    @FXML
    public void menuFlipHorizontally_onAction(ActionEvent actionEvent) {
        mainViewModel.flipHorizontally();
    }

    @FXML
    public void menuClose_onAction(ActionEvent actionEvent) {
        Platform.exit();
    }

    @FXML
    public void menuAbout_onAction(ActionEvent actionEvent) throws IOException {
        Scene scene = new Scene(loadParent("aboutWindow"));
        Stage aboutWindow = new Stage();
        aboutWindow.setScene(scene);
        aboutWindow.initModality(Modality.WINDOW_MODAL);
        aboutWindow.initStyle(StageStyle.UTILITY);
        aboutWindow.initOwner(imageViewMain.getScene().getWindow());
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
        open();
    }

    @FXML
    public void menuOpenContainingFolder_onAction(ActionEvent actionEvent) {
        if (mainViewModel.getSelectedImageModel() != null) {
            mainViewModel.openContainingFolder(mainViewModel.getSelectedImageModel());
        }
    }

    @FXML
    public void menuOpenInExternalEditor_onAction(ActionEvent actionEvent) {
        if (mainViewModel.getSelectedImageModel() != null) {
            mainViewModel.openInEditor(mainViewModel.getSelectedImageModel());
        }
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
                case ADD:
                case EQUALS:
                    zoomIn();
                    break;
                case UNDERSCORE:
                case SUBTRACT:
                    zoomOut();
                    break;
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
        if (mainViewModel.getSelectedImageModel() != null) {
            mainViewModel.openInEditor(mainViewModel.getSelectedImageModel());
        }
    }

    @FXML
    public void menuDelete_onAction(ActionEvent actionEvent) {
        mainViewModel.trashImage(mainViewModel.getSelectedImageModel());
    }

    @FXML
    public void menuCopy_onAction(ActionEvent actionEvent) {
        copy();
    }

    @FXML
    public void menuZoomIn_onAction(ActionEvent actionEvent) {
        zoomIn();
    }

    @FXML
    public void menuZoomOut_onAction(ActionEvent actionEvent) {
        zoomOut();
    }

    @FXML
    public void menuResetZoom_onAction(ActionEvent actionEvent) {
        resetZoom();
    }

    @FXML
    public void buttonResetZoom_onAction(ActionEvent actionEvent) {
        resetZoom();
    }

    @FXML
    public void buttonZoomOut_onAction(ActionEvent actionEvent) {
        zoomOut();
    }

    @FXML
    public void buttonZoomIn_onAction(ActionEvent actionEvent) {
        zoomIn();
    }

    @FXML
    public void buttonSave_onAction(ActionEvent actionEvent) {
        saveAs();
    }

    @FXML
    public void buttonOpen_onAction(ActionEvent actionEvent) {
        open();
    }

    @FXML
    public void buttonCopy_onAction(ActionEvent actionEvent) {
        copy();
    }

    @FXML
    public void buttonRotateRight_onAction(ActionEvent actionEvent) {
        rotateLeft();
    }

    @FXML
    public void buttonRotateLeft_onAction(ActionEvent actionEvent) {
        rotateRight();
    }

    @FXML
    public void menuSortByName_onAction(ActionEvent actionEvent) {
        sortByName();
    }

    @FXML
    public void menuSortByCreated_onAction(ActionEvent actionEvent) {
        sortByDateCreated();
    }

    @FXML
    public void menuSortByModified_onAction(ActionEvent actionEvent) {
        sortByDateModified();
    }

    @FXML
    public void menuImageInfo_onAction(ActionEvent actionEvent) throws IOException {
        viewImageInfo();
    }

    @FXML
    public void buttonImageInfo_onAction(ActionEvent actionEvent) throws IOException {
        viewImageInfo();
    }

    @FXML
    public void menuCopyFileTo_onAction(ActionEvent actionEvent) throws IOException {
        copyFileTo();
    }

    private enum ViewStyle {
        FIT_TO_WINDOW, ORIGINAL, STRETCHED
    }

    private void toggleFullScreen() {
        boolean setFullScreen = !isViewingFullScreen.get();
        ((Stage) mainScrollPane.getScene().getWindow()).setFullScreen(setFullScreen);
        menuBar.setVisible(!setFullScreen);
        toolBar.setVisible(!setFullScreen);
        statusBar.setVisible(!setFullScreen);
        isViewingFullScreen.set(setFullScreen);
        menuFullScreen.setSelected(setFullScreen);
    }

    private void zoomIn() {
        viewStyleProperty.set(ViewStyle.ORIGINAL);
        mainViewModel.zoomIn();
    }

    private void zoomOut() {
        viewStyleProperty.set(ViewStyle.ORIGINAL);
        mainViewModel.zoomOut();
    }

    private void resetZoom() {
        viewStyleProperty.set(ViewStyle.ORIGINAL);
        mainViewModel.resetZoom();
    }

    private void open() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg;*.jpeg;*.png;*.gif")
        );
        if (mainViewModel.getSelectedImageModel() != null) {
            if (mainViewModel.getSelectedImageModel().hasOriginal()) {
                fileChooser.setInitialDirectory(
                        new File(mainViewModel.getSelectedImageModel().getResamplePath()).getParentFile());
            } else {
                fileChooser.setInitialDirectory(
                        new File(mainViewModel.getSelectedImageModel().getPath()).getParentFile());
            }
        }
        File file = fileChooser.showOpenDialog(imageViewMain.getScene().getWindow());
        if (file != null) {
            mainViewModel.loadImage(new ImageModel(file.getPath()));
        }
    }

    private void saveAs() {
        if (mainViewModel.getSelectedImageModel() == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JPEG Image", "*.jpg"),
                new FileChooser.ExtensionFilter("PNG Image", "*.png"),
                new FileChooser.ExtensionFilter("GIF Image", "*.gif")
        );
        fileChooser.setInitialFileName(mainViewModel.getSelectedImageModel().getShortName());
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        File file = fileChooser.showSaveDialog(imageViewMain.getScene().getWindow());
        if (file != null) {
            mainViewModel.saveImage(mainViewModel.getSelectedImageModel(), file.getPath());
        }
    }

    private void copy() {
        if (mainViewModel.getSelectedImageModel() != null) {
            mainViewModel.copyToClipboard(mainViewModel.getSelectedImageModel());
        }
    }

    private void rotateLeft() {
        mainViewModel.rotateLeft();
    }

    private void rotateRight() {
        mainViewModel.rotateRight();
    }

    private void sortByName() {
        mainViewModel.sortImages(MainViewModel.SortStyle.NAME);
    }

    private void sortByDateCreated() {
        mainViewModel.sortImages(MainViewModel.SortStyle.DATE_CREATED);
    }

    private void sortByDateModified() {
        mainViewModel.sortImages(MainViewModel.SortStyle.DATE_MODIFIED);
    }

    private void viewImageInfo() throws IOException {
        if (mainViewModel.getSelectedImageModel() == null) return;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("imageInfoWindow.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        ImageInfoWindowController controller = fxmlLoader.getController();
        Stage imageInfoWindow = new Stage();
        imageInfoWindow.setScene(scene);
        imageInfoWindow.initModality(Modality.WINDOW_MODAL);
        imageInfoWindow.initStyle(StageStyle.UTILITY);
        imageInfoWindow.initOwner(imageViewMain.getScene().getWindow());
        imageInfoWindow.setTitle("Image Information - " + mainViewModel.getSelectedImageModel().getShortName());
        controller.loadInfo(mainViewModel.getSelectedImageModel());
        imageInfoWindow.show();
    }

    private void copyFileTo() throws IOException {
        if (mainViewModel.getSelectedImageModel() == null) return;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("copyFileToWindow.fxml"));
        Parent root = fxmlLoader.load();
        CopyFileToWindowController controller = fxmlLoader.getController();
        Scene scene = new Scene(root);
        Stage copyFileToWindow = new Stage();
        copyFileToWindow.setScene(scene);
        copyFileToWindow.initModality(Modality.WINDOW_MODAL);
        copyFileToWindow.initStyle(StageStyle.UTILITY);
        copyFileToWindow.initOwner(imageViewMain.getScene().getWindow());
        copyFileToWindow.setTitle("Copy \"" + mainViewModel.getSelectedImageModel().getShortName() + "\" To");
        controller.setViewModel(new CopyFileToViewModel(mainViewModel.getSelectedImageModel()));
        copyFileToWindow.show();
    }

    private Parent loadParent(String fxmlName) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource(fxmlName + ".fxml"));
        return fxmlLoader.load();
    }
}