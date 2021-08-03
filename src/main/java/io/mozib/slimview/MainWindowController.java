/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Window;
import javafx.stage.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import static io.mozib.slimview.Util.getDataFile;
import static io.mozib.slimview.Util.getOSType;

public class MainWindowController implements Initializable {

    @FXML
    public RadioMenuItem menuStretched;
    @FXML
    public ImageView imageViewMain;
    @FXML
    public RadioMenuItem menuFitToWindow;
    @FXML
    public RadioMenuItem menuFitToDesktop;
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
    public Label labelResolution;
    @FXML
    public ToolBar toolBar;
    @FXML
    public AnchorPane statusBar;
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
    @FXML
    public ToggleButton tButtonFavorite;
    @FXML
    public ImageView tButtonFavoriteImageView;

    private final double zoomStep = 0.1;
    private final Image favoriteOutline = new Image(getClass().getResourceAsStream("icons/favorite.png"));
    private final Image favoriteSolid = new Image(getClass().getResourceAsStream("icons/favorite-solid.png"));
    private final int scrollPaneOffset = 2; // to force correct clipping of scroll pane
    private final ToggleGroup toggleGroupViewStyle = new ToggleGroup();
    private final ToggleGroup toggleGroupSortStyle = new ToggleGroup();
    public MainViewModel mainViewModel = new MainViewModel();
    private final Preferences preferences = Preferences.userNodeForPackage(this.getClass());
    private final SimpleBooleanProperty isViewingFullScreen = new SimpleBooleanProperty(false);
    private final SimpleObjectProperty<ViewStyle> viewStyleProperty = new SimpleObjectProperty<>(ViewStyle.ORIGINAL);

    // the ViewStyle to reset to when switching between images after zooming
    private ViewStyle cachedViewStyleZoom = viewStyleProperty.get();

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
        labelResolution.setText("");

        // start tracking resolution and zoom
        ChangeListener<Number> imageViewSizeChangeListener = (observable, oldValue, newValue) -> {
            updateTitle();
            labelResolution.setText("");

            if (mainViewModel.getSelectedImageModel() != null) {
                double currentWidth = mainViewModel.getSelectedImageModel().hasOriginal() ?
                        mainViewModel.getSelectedImageModel().getOriginalImageModel().getWidth() :
                        mainViewModel.getSelectedImageModel().getWidth();
                double zoom = getViewingWidth() / currentWidth * 100;
                labelResolution.setText(
                        (mainViewModel.getSelectedImageModel().hasOriginal() ?
                                mainViewModel.getSelectedImageModel().getOriginalImageModel().getResolution() :
                                mainViewModel.getSelectedImageModel().getResolution()) + " (" + Math.round(zoom) + "%)");
            }
        };
        imageViewMain.fitHeightProperty().addListener(imageViewSizeChangeListener);
        imageViewMain.fitWidthProperty().addListener(imageViewSizeChangeListener);

        // menubar toggle group
        menuFitToDesktop.setToggleGroup(toggleGroupViewStyle);
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

        // bind ImageView and Favorite Button to selectedImage
        mainViewModel.selectedImageModelProperty().addListener((observable, oldValue, newValue) -> {
            tButtonFavorite.setSelected(newValue.getIsFavorite());
            imageViewMain.setImage(newValue.getImage());

            // reset the ViewStyle if we've zoomed image
            ViewStyle oldViewStyle;
            if (newValue.hasOriginal() || oldValue == null) {
                oldViewStyle = viewStyleProperty.get();
            } else {
                oldViewStyle = cachedViewStyleZoom;
            }
            viewStyleProperty.set(null); // force trigger change listener
            viewStyleProperty.set(oldViewStyle);

            imageViewMain.requestFocus();

            try {
                updateTitle();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        tButtonFavorite.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                tButtonFavoriteImageView.setImage(favoriteSolid);
            } else {
                tButtonFavoriteImageView.setImage(favoriteOutline);
            }
        });

        viewStyleProperty.addListener((observable, oldValue, newValue) -> {
                    if (mainViewModel.getSelectedImageModel() == null) return;

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

                        case FIT_TO_DESKTOP:
                            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                            Rectangle desktopSize =
                                    GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                            double taskBarHeight = screenSize.height - desktopSize.height;
                            double titleBarHeight = taskBarHeight;
                            double screenWidth = screenSize.getWidth();
                            double screenHeight = screenSize.getHeight();
                            double aspectRatio = mainViewModel.getSelectedImageModel().getAspectRatio();
                            double fixedHeight = 25 + 42 + 33;

                            menuFitToDesktop.setSelected(true);
                            mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                            mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

                            double targetWidth, targetHeight;

                            if (isViewingFullScreen.get()) {
                                targetWidth = screenWidth;
                            } else {
                                targetWidth = screenWidth - 8;
                            }

                            targetHeight = targetWidth / aspectRatio;

                            if (isViewingFullScreen.get()) {
                                if (targetHeight > screenHeight - 8) {
                                    targetHeight = screenHeight - 8;
                                    targetWidth = aspectRatio * targetHeight;
                                }
                            } else {
                                double viewableHeight = screenHeight - taskBarHeight - titleBarHeight - fixedHeight - 8;
                                if (targetHeight > viewableHeight) {
                                    targetHeight = viewableHeight;
                                    targetWidth = aspectRatio * targetHeight;
                                }
                            }

                            imageViewMain.setPreserveRatio(false);
                            imageViewMain.setFitWidth(targetWidth);
                            imageViewMain.setFitHeight(targetHeight);

                            if (!isViewingFullScreen.get()) {
                                Window window = imageViewMain.getScene().getWindow();
                                window.setWidth(targetWidth + 16);
                                window.setHeight(targetHeight + titleBarHeight + fixedHeight + 8);
                                window.setX(0);
                                window.setY(0);
                            }
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

                    // cache ViewStyle for use after zooming
                    if (!mainViewModel.getSelectedImageModel().hasOriginal() ||
                            mainViewModel.getSelectedImageModel() == null) {
                        cachedViewStyleZoom = newValue;
                    }

                    preferences.put("LastViewStyle", newValue.toString());
                    imageViewMain.requestFocus();
                }
        );

        // force trigger view style if we're switching to fullscreen mode
        isViewingFullScreen.addListener(((observable, oldValue, newValue) -> {
            ViewStyle old = viewStyleProperty.get();
            viewStyleProperty.set(null);
            viewStyleProperty.set(old);
        }));

        mainViewModel.selectedSortStyleProperty().addListener((observable, oldValue, newValue) -> {
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
                }
        );

        viewStyleProperty.set(ViewStyle.valueOf(preferences.get("LastViewStyle", ViewStyle.FIT_TO_WINDOW.toString())));
        mainViewModel.sortImages(MainViewModel.SortStyle.valueOf(
                preferences.get("LastSortStyle", MainViewModel.SortStyle.DATE_MODIFIED.toString()))); // default sorting

        // load recent files
        RecentFiles recentFiles = Util.readDataFile(RecentFiles.class, Util.DataFileLocation.RECENT_FILES);
        if (recentFiles == null) {
            recentFiles = new RecentFiles();
        }
        if (recentFiles.getRecentFiles() == null) {
            recentFiles.setRecentFiles(new ArrayList<>());
        } else {
            recentFiles.getRecentFiles().sort(((o1, o2) -> {
                return Long.compare(o2.getLastSeen(), o1.getLastSeen());
            }));
        }
        for (RecentFiles.RecentFile recentFile : recentFiles.getRecentFiles()) {
            MenuItem menuItem = new MenuItem(recentFile.getPath());
            menuItem.setOnAction(event -> {
                openImage(menuItem.getText());
            });
            menuRecent.getItems().add(menuItem);
        }
        // add a clear option
        if (recentFiles.getRecentFiles().size() > 0) {
            menuRecent.getItems().add(new SeparatorMenuItem());

            MenuItem menuClearRecent = new MenuItem("Clear Recent Files");
            menuClearRecent.setOnAction(event -> {
                File file = new File(getDataFile(Util.DataFileLocation.RECENT_FILES));
                if (file.exists()) file.delete();
                menuRecent.getItems().clear();
            });
            menuRecent.getItems().add(menuClearRecent);
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
        FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource("aboutWindow.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        AboutWindowController controller = fxmlLoader.getController();
        Stage aboutWindow = new Stage();
        aboutWindow.setScene(scene);
        aboutWindow.initModality(Modality.WINDOW_MODAL);
        aboutWindow.initStyle(StageStyle.UTILITY);
        aboutWindow.initOwner(imageViewMain.getScene().getWindow());
        aboutWindow.setTitle("About");
        aboutWindow.show();
        controller.buttonOK.requestFocus();
    }

    @FXML
    public void menuFullScreen_onAction(ActionEvent actionEvent) {
        toggleFullScreen();
    }

    @FXML
    public void menuBar_onKeyPress(KeyEvent keyEvent) {
    }

    @FXML
    public void menuOriginalSize_onAction(ActionEvent actionEvent) {
        viewStyleProperty.set(ViewStyle.ORIGINAL);
    }

    @FXML
    public void menuFitToWindow_onAction(ActionEvent actionEvent) {
        bestFit();
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
        switch (keyEvent.getCode()) {
            case LEFT:
            case PAGE_DOWN:
                if (getViewingWidth() < mainScrollPane.getWidth())
                    mainViewModel.showPreviousImage();
                break;
            case RIGHT:
            case PAGE_UP:
                if (getViewingWidth() < mainScrollPane.getWidth())
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

    @FXML
    public void mainScrollPane_onScroll(ScrollEvent scrollEvent) {
        if (scrollEvent.getDeltaY() > 0 || scrollEvent.getDeltaX() > 0) {
            showPrevious();
        } else if (scrollEvent.getDeltaY() < 0 || scrollEvent.getDeltaX() < 0) {
            showNext();
        }
        scrollEvent.consume();
    }

    @FXML
    public void buttonNext_onAction(ActionEvent actionEvent) {
        showNext();
    }

    @FXML
    public void buttonPrevious_onAction(ActionEvent actionEvent) {
        showPrevious();
    }

    @FXML
    public void buttonFirst_onAction(ActionEvent actionEvent) {
        showFirst();
    }

    @FXML
    public void buttonLast_onAction(ActionEvent actionEvent) {
        showLast();
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
        bestFit();
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
        rotateRight();
    }

    @FXML
    public void buttonRotateLeft_onAction(ActionEvent actionEvent) {
        rotateLeft();
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

    @FXML
    public void menuCheckForUpdates_onAction(ActionEvent actionEvent) {
        AppUpdateService appUpdateService = new AppUpdateService();
        appUpdateService.setOnSucceeded(event -> {
            boolean updateAvailable = (boolean) event.getSource().getValue();

            Alert alert;
            if (updateAvailable) {
                alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Update");
                alert.initOwner(imageViewMain.getScene().getWindow());
                alert.setHeaderText("An update is available.");
                alert.getDialogPane().setContentText("Would you like to download it now?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK) {
                    Util.browseUrl(appUpdateService.getUpdateUrl());
                }
            } else {
                alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Update");
                alert.initOwner(imageViewMain.getScene().getWindow());
                alert.setHeaderText("No updates are available.");
                alert.showAndWait();
            }
        });
        appUpdateService.start();
    }

    @FXML
    public void tButtonFavorite_onAction(ActionEvent actionEvent) {
        mainViewModel.setAsFavorite(mainViewModel.getSelectedImageModel(),
                ((ToggleButton) actionEvent.getSource()).isSelected());
        imageViewMain.requestFocus();
    }

    @FXML
    public void menuViewFavorites_onAction(ActionEvent actionEvent) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("favoritesWindow.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("favoritesWindowStyle.css")).toExternalForm());
        FavoritesWindowController controller = fxmlLoader.getController();
        Stage favoritesWindow = new Stage();
        favoritesWindow.setScene(scene);
        favoritesWindow.initModality(Modality.WINDOW_MODAL);
        favoritesWindow.initStyle(StageStyle.UTILITY);
        favoritesWindow.initOwner(imageViewMain.getScene().getWindow());
        favoritesWindow.setTitle("Favorites Manager");
        controller.setFavoritesController(mainViewModel.getFavoritesController());
        favoritesWindow.showAndWait();

        if (controller.getSelectedFavorite().get() != null) {
            openImage(controller.getSelectedFavorite().get().toString());
        }
    }

    @FXML
    public void menuFitToDesktop_onAction(ActionEvent actionEvent) {
        viewStyleProperty.set(ViewStyle.FIT_TO_DESKTOP);
    }

    private enum ViewStyle {
        FIT_TO_WINDOW, FIT_TO_DESKTOP, ORIGINAL, STRETCHED
    }

    private void toggleFullScreen() {
        if (mainViewModel.getSelectedImageModel() == null) return;

        boolean setFullScreen = !isViewingFullScreen.get();
        ((Stage) mainScrollPane.getScene().getWindow()).setFullScreen(setFullScreen);
        menuBar.setVisible(!setFullScreen);
        toolBar.setVisible(!setFullScreen);
        statusBar.setVisible(!setFullScreen);
        isViewingFullScreen.set(setFullScreen);
        menuFullScreen.setSelected(setFullScreen);
    }

    private void zoomIn() {
        double originalWidth = mainViewModel.getSelectedImageModel().hasOriginal() ?
                mainViewModel.getSelectedImageModel().getOriginalImageModel().getWidth() :
                mainViewModel.getSelectedImageModel().getWidth();
        double originalHeight = mainViewModel.getSelectedImageModel().hasOriginal() ?
                mainViewModel.getSelectedImageModel().getOriginalImageModel().getHeight() :
                mainViewModel.getSelectedImageModel().getHeight();
        double screenWidth = Screen.getPrimary().getVisualBounds().getWidth();
        double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();
        double targetWidth = getViewingWidth() + getViewingWidth() * zoomStep;
        double targetHeight = getViewingHeight() + getViewingHeight() * zoomStep;
        double maxAllowedWidth = Math.min(5 * originalWidth, 3 * screenWidth);
        double maxAllowedHeight = Math.min(5 * originalHeight, 3 * screenHeight);

        // apply zoom limit
        if (targetWidth > maxAllowedWidth || targetHeight > maxAllowedHeight) {
            return;
        }

        // latch onto the original size if we're around it
        if ((targetWidth >= 0.95 * originalWidth && targetWidth <= 1.05 * originalWidth) ||
                (targetHeight >= 0.95 * originalHeight && targetHeight <= 1.05 * originalHeight)) {
            targetWidth = originalWidth;
            targetHeight = originalHeight;
        }

        mainViewModel.resizeImage(mainViewModel.getSelectedImageModel(), (int) targetWidth, (int) targetHeight);
        viewStyleProperty.set(ViewStyle.ORIGINAL);
    }

    private void zoomOut() {
        double originalWidth = mainViewModel.getSelectedImageModel().hasOriginal() ?
                mainViewModel.getSelectedImageModel().getOriginalImageModel().getWidth() :
                mainViewModel.getSelectedImageModel().getWidth();
        double originalHeight = mainViewModel.getSelectedImageModel().hasOriginal() ?
                mainViewModel.getSelectedImageModel().getOriginalImageModel().getHeight() :
                mainViewModel.getSelectedImageModel().getHeight();
        double targetWidth = getViewingWidth() - getViewingWidth() * zoomStep;
        double targetHeight = getViewingHeight() - getViewingHeight() * zoomStep;
        double minAllowedWidth = 0.1 * originalWidth;
        double minAllowedHeight = 0.1 * originalHeight;

        // apply zoom out limit
        if (targetWidth < minAllowedWidth || targetHeight < minAllowedHeight) {
            return;
        }

        // latch onto the original size if we're around it
        if ((targetWidth >= 0.95 * originalWidth && targetWidth <= 1.05 * originalWidth) ||
                (targetHeight >= 0.95 * originalHeight && targetHeight <= 1.05 * originalHeight)) {
            targetWidth = originalWidth;
            targetHeight = originalHeight;
        }

        mainViewModel.resizeImage(mainViewModel.getSelectedImageModel(), (int) targetWidth, (int) targetHeight);
        viewStyleProperty.set(ViewStyle.ORIGINAL);
    }

    private void resetZoom() {
        viewStyleProperty.set(ViewStyle.ORIGINAL);
        mainViewModel.resetZoom();
    }

    private void bestFit() {
        viewStyleProperty.set(ViewStyle.FIT_TO_WINDOW);
    }

    private void open() {
        FileChooser fileChooser = new FileChooser();

        if (getOSType() == Util.OSType.WINDOWS) {

            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.jpg;*.jpeg;*.png;*.gif"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
        } else {

            // *nix doesn't like Windows-style extension filters
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("JPG Image", "*.jpg"),
                    new FileChooser.ExtensionFilter("JPEG Image", "*.jpeg"),
                    new FileChooser.ExtensionFilter("PNG Image", "*.png"),
                    new FileChooser.ExtensionFilter("GIF Image", "*.gif"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );
        }

        File initialDirectory = new File(preferences.get("OpenLocation", System.getProperty("user.home")));
        if (!initialDirectory.exists() || !initialDirectory.isDirectory()) {
            initialDirectory = new File(System.getProperty("user.home"));
        }
        fileChooser.setInitialDirectory(initialDirectory);

        File file = fileChooser.showOpenDialog(imageViewMain.getScene().getWindow());
        if (file != null) {
            preferences.put("OpenLocation", file.getParentFile().getPath());
            openImage(file.getPath());
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

        fileChooser.setInitialFileName(mainViewModel.getSelectedImageModel().getName());

        File initialDirectory = new File(preferences.get("SaveAsLocation", System.getProperty("user.home")));
        if (!initialDirectory.exists() || !initialDirectory.isDirectory()) {
            initialDirectory = new File(System.getProperty("user.home"));
        }
        fileChooser.setInitialDirectory(initialDirectory);

        File file = fileChooser.showSaveDialog(imageViewMain.getScene().getWindow());
        if (file != null) {
            preferences.put("SaveAsLocation", file.getParentFile().getPath());
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
        imageInfoWindow.setTitle("Image Properties");
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
        copyFileToWindow.setTitle("Copy File To");
        controller.setViewModel(new CopyFileToViewModel(mainViewModel.getSelectedImageModel()));
        copyFileToWindow.show();
    }

    /**
     * @return The width of the image as it's being displayed on the screen.
     */
    private double getViewingWidth() {
        double width = imageViewMain.getFitHeight() *
                (mainViewModel.getSelectedImageModel().hasOriginal() ?
                        mainViewModel.getSelectedImageModel().getOriginalImageModel().getAspectRatio() :
                        mainViewModel.getSelectedImageModel().getAspectRatio());
        if (width > imageViewMain.getFitWidth()) width = imageViewMain.getFitWidth();
        return width;
    }

    /**
     * @return The height of the image as it's being displayed on the screen.
     */
    private double getViewingHeight() {
        double height = imageViewMain.getFitWidth() /
                (mainViewModel.getSelectedImageModel().hasOriginal() ?
                        mainViewModel.getSelectedImageModel().getOriginalImageModel().getAspectRatio() :
                        mainViewModel.getSelectedImageModel().getAspectRatio());
        if (height > imageViewMain.getFitHeight()) height = imageViewMain.getFitHeight();
        return height;
    }

    /**
     * Updates the title of the application window.
     */
    private void updateTitle() {
        Stage stage = (Stage) imageViewMain.getScene().getWindow();
        String title = "";
        if (mainViewModel.getSelectedImageModel() == null) {
            title = "SlimView";
        } else {
            title = mainViewModel.getSelectedImageModel().getName() + " - SlimView [Zoom: " +
                    (int) getViewingWidth() + " x " + (int) getViewingHeight() + " px]";
        }
        stage.setTitle(title);
    }

    /**
     * Main method to load an image into the application. Don't use ViewModel's function directly.
     */
    public void openImage(String path) {
        try {
            mainViewModel.loadImage(new ImageModel(path));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Unable to open the requested file");
            alert.setContentText("The file doesn't exist or is unreachable.");
            alert.setTitle("Error");
            alert.initOwner(imageViewMain.getScene().getWindow());
            alert.show();
        }
    }

    private void showFirst() {
        mainViewModel.showFirstImage();
    }

    private void showPrevious() {
        mainViewModel.showPreviousImage();
    }

    private void showNext() {
        mainViewModel.showNextImage();
    }

    private void showLast() {
        mainViewModel.showLastImage();
    }
}
