/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */
package io.mozib.slimview;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
import javafx.scene.effect.BlendMode;
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
    private enum ViewStyle {
        FIT_TO_WINDOW, FIT_TO_DESKTOP, ORIGINAL, STRETCHED
    }

    public MainViewModel mainViewModel = new MainViewModel();
    private final Preferences preferences = Preferences.userNodeForPackage(this.getClass());

    private final double zoomStep = 0.1;

    // for favorite button
    private final Image favoriteOutline = new Image(getClass().getResourceAsStream("icons/favorite.png"));
    private final Image favoriteSolid = new Image(getClass().getResourceAsStream("icons/favorite-solid.png"));

    private final ToggleGroup toggleGroupViewStyle = new ToggleGroup();
    private final ToggleGroup toggleGroupSortStyle = new ToggleGroup();
    private final ToggleGroup toggleGroupSelectPan = new ToggleGroup();

    private final SimpleBooleanProperty isViewingFullScreen = new SimpleBooleanProperty(false);
    private final SimpleObjectProperty<ViewStyle> viewStyleProperty
            = new SimpleObjectProperty<>(ViewStyle.FIT_TO_DESKTOP);

    // the ViewStyle to reset to when switching between images after zooming
    private ViewStyle cachedViewStyleZoom = viewStyleProperty.get();

    // fields for SelectionRectangle
    private javafx.scene.shape.Rectangle selectionRectangle = null;
    private boolean selectionStarted = false;
    private boolean cursorInsideSelRect = false; // true if the cursor is inside the SelectionRectangle
    // initial point where mouse was clicked
    private double selectionPivotX = 0.0;
    private double selectionPivotY = 0.0;

    // Structure: ScrollPane > AnchorPane > Rectangle + [StackPane > ImageView]

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
    public ScrollPane scrollPaneMain;
    @FXML
    public Label labelStatus;
    @FXML
    public Label labelPoints;
    @FXML
    public Label labelResolution;
    @FXML
    public ToolBar toolBar;
    @FXML
    public AnchorPane statusBar;
    @FXML
    public AnchorPane anchorPaneMain;
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
    public ToggleButton tButtonPanMode;
    @FXML
    public ToggleButton tButtonSelectionMode;
    @FXML
    public ImageView tButtonFavoriteImageView;

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // initialize only UI control listeners in this method

        // reset control properties
        labelResolution.setText("");
        labelPoints.setText("");
        imageViewMain.setFitHeight(0);
        imageViewMain.setFitWidth(0);
        labelStatus.textProperty().bind(mainViewModel.statusProperty());

        // MenuBar ToggleGroup
        menuFitToDesktop.setToggleGroup(toggleGroupViewStyle);
        menuStretched.setToggleGroup(toggleGroupViewStyle);
        menuFitToWindow.setToggleGroup(toggleGroupViewStyle);
        menuOriginalSize.setToggleGroup(toggleGroupViewStyle);
        menuSortByName.setToggleGroup(toggleGroupSortStyle);
        menuSortByCreated.setToggleGroup(toggleGroupSortStyle);
        menuSortByModified.setToggleGroup(toggleGroupSortStyle);

        // Button ToggleGroup
        tButtonPanMode.setToggleGroup(toggleGroupSelectPan);
        tButtonSelectionMode.setToggleGroup(toggleGroupSelectPan);

        // bindings for fullscreen viewing
        toolBar.managedProperty().bind(toolBar.visibleProperty());
        statusBar.managedProperty().bind(statusBar.visibleProperty());
        menuBar.managedProperty().bind(menuBar.visibleProperty());

        // bind ImageView and FavoriteButton to the SelectedImage
        tButtonFavorite.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                tButtonFavoriteImageView.setImage(favoriteSolid);
            } else {
                tButtonFavoriteImageView.setImage(favoriteOutline);
            }
        });

        // bind SelectionRectangle/pan mode buttons
        scrollPaneMain.pannableProperty().bindBidirectional(tButtonPanMode.selectedProperty());

        // set focus on the ImageView whenever any Button on the Toolbar is actioned
        toolBar.getItems().forEach(node -> {
            if (node instanceof Button) {
                Button button = (Button) node;
                button.addEventHandler(ActionEvent.ACTION, (event -> {
                    imageViewMain.requestFocus();
                }));
            }
        });

        // start tracking resolution, zoom and SelectionRectangle
        imageViewMain.fitWidthProperty().addListener(new ImageSizeChangeListener());
        imageViewMain.fitHeightProperty().addListener(new ImageSizeChangeListener());

        // load recent files
        RecentFiles recentFiles = Util.readDataFile(RecentFiles.class, Util.DataFileLocation.RECENT_FILES);
        if (recentFiles == null) {
            recentFiles = new RecentFiles();
        }
        if (recentFiles.getRecentFiles() == null) {
            recentFiles.setRecentFiles(new ArrayList<>());
        } else {
            recentFiles.getRecentFiles().sort(((o1, o2) -> Long.compare(o2.getLastSeen(), o1.getLastSeen())));
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

            MenuItem menuClearRecent = new MenuItem("Clear History");
            menuClearRecent.setOnAction(event -> {
                File file = new File(getDataFile(Util.DataFileLocation.RECENT_FILES));
                if (file.exists()) {
                    file.delete();
                }
                menuRecent.getItems().clear();
            });
            menuRecent.getItems().add(menuClearRecent);
        }
    }

    /**
     * Sets up the listeners to various virtual (ViewModel) properties. Important to call this after the UI has loaded.
     */
    public void initUIListeners() {
        // remove SelectionRectangle if window size is changed
        // Window must be called after the stage has initialized
        Window window = imageViewMain.getScene().getWindow();
        window.widthProperty().addListener((observable -> clearSelectionRectangle()));
        window.heightProperty().addListener((observable -> clearSelectionRectangle()));

        // bind ChangeListeners
        mainViewModel.selectedImageModelProperty().addListener(new ImageChangeListener());
        viewStyleProperty.addListener(new ViewStyleChangeListener(
                menuBar.getHeight(), toolBar.getHeight(), statusBar.getHeight()));
        mainViewModel.selectedSortStyleProperty().addListener(new SortStyleChangeListener());

        // restore previous settings
        viewStyleProperty.set(ViewStyle.valueOf(
                preferences.get("LastViewStyle", ViewStyle.FIT_TO_DESKTOP.toString())));
        mainViewModel.sortImages(MainViewModel.SortStyle.valueOf(
                preferences.get("LastSortStyle", MainViewModel.SortStyle.DATE_MODIFIED.toString()))); // default sorting

        // force trigger ViewStyle if we're switching to fullscreen mode
        isViewingFullScreen.addListener(((observable, oldValue, newValue) -> {
            ViewStyle old = viewStyleProperty.get();
            viewStyleProperty.set(null);
            viewStyleProperty.set(old);
        }));
    }

    private void clearSelectionRectangle() {
        if (selectionRectangle != null) {
            anchorPaneMain.getChildren().remove(selectionRectangle);
            selectionRectangle = null;
        }
    }

    @FXML
    public void anchorPaneMain_onMousePress(MouseEvent mouseEvent) {
        if (!scrollPaneMain.isPannable() && mainViewModel.getSelectedImageModel() != null) {

            // don't start selecting if initial point is outside the ImageView
            if (mouseEvent.getX() < imageViewMain.getBoundsInParent().getMinX() ||
                    mouseEvent.getY() < imageViewMain.getBoundsInParent().getMinY() ||
                    mouseEvent.getX() > imageViewMain.getBoundsInParent().getMaxX() ||
                    mouseEvent.getY() > imageViewMain.getBoundsInParent().getMaxY()) {

                return;
            }

            selectionStarted = true;
        } else {
            // most likely panning started; disable selection mode
            selectionStarted = false;
        }

        if (!cursorInsideSelRect) {
            anchorPaneMain.getChildren().remove(selectionRectangle);
            selectionRectangle = null;
        }
    }

    @FXML
    public void anchorPaneMain_onMouseRelease(MouseEvent mouseEvent) {
        // stop selecting but don't clear the rectangle yet as we need it for copying, zooming etc.
        if (selectionStarted) {
            selectionStarted = false;
        }
    }

    @FXML
    public void anchorPaneMain_onMouseDrag(MouseEvent mouseEvent) {
        if (selectionStarted) {
            if (selectionRectangle == null) {

                // save this point for selecting in the reverse direction
                selectionPivotX = mouseEvent.getX();
                selectionPivotY = mouseEvent.getY();

                selectionRectangle = new javafx.scene.shape.Rectangle();
                selectionRectangle.getStyleClass().add("selectionRectangle");
                selectionRectangle.setBlendMode(BlendMode.EXCLUSION);
                selectionRectangle.setOnMouseMoved(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        // add 1 to account for SelectionRectangle's borders
                        refreshCoordinates(
                                event.getX() - imageViewMain.getBoundsInParent().getMinX() + 1,
                                event.getY() - imageViewMain.getBoundsInParent().getMinY() + 1);
                    }
                });
                selectionRectangle.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        // zoom selection into viewport

                        double selectedLeft = selectionRectangle.getX() - imageViewMain.getBoundsInParent().getMinX();
                        double selectedTop = selectionRectangle.getY() - imageViewMain.getBoundsInParent().getMinY();
                        double selectedWidth = selectionRectangle.getWidth();
                        double selectedHeight = selectionRectangle.getHeight();
                        double scaleFactor = scrollPaneMain.getViewportBounds().getWidth() / selectedWidth;

                        double targetWidth = scaleFactor * getViewingWidth();
                        double targetHeight = scaleFactor * getViewingHeight();

                        mainViewModel.resizeImage(mainViewModel.getSelectedImageModel(),
                                (int) targetWidth, (int) targetHeight);
                        viewStyleProperty.set(ViewStyle.ORIGINAL);

                        scrollPaneMain.layout();
                        double targetLeft = (1/(scrollPaneMain.getViewportBounds().getWidth()/2) ) * ((selectedLeft + selectedWidth / 2) * scaleFactor);
                        double targetTop = (selectedTop + selectedHeight) * scaleFactor / targetHeight;
                        scrollPaneMain.setHvalue(targetLeft);
                        scrollPaneMain.setVvalue(targetTop);
                    }
                });
                selectionRectangle.setOnMouseEntered(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        cursorInsideSelRect = true;
                    }
                });
                selectionRectangle.setOnMouseExited(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        cursorInsideSelRect = false;
                    }
                });

                anchorPaneMain.getChildren().add(selectionRectangle);
            } else {
                double startX = selectionPivotX;
                double startY = selectionPivotY;
                double endX = mouseEvent.getX();
                double endY = mouseEvent.getY();
                double topBoundary = imageViewMain.getBoundsInParent().getMinY();
                double leftBoundary = imageViewMain.getBoundsInParent().getMinX();
                double rightBoundary = imageViewMain.getBoundsInParent().getMaxX();
                double bottomBoundary = imageViewMain.getBoundsInParent().getMaxY();
                double width, height;

                // we can only be outside a maximum of two boundaries at the same time, e.g. top-right

                if (endX < leftBoundary)
                    endX = leftBoundary;

                if (endX > rightBoundary)
                    endX = rightBoundary;

                if (endY < topBoundary)
                    endY = topBoundary;

                if (endY > bottomBoundary)
                    endY = bottomBoundary;

                width = Math.max(startX, endX) - Math.min(startX, endX);
                height = Math.max(startY, endY) - Math.min(startY, endY);

                selectionRectangle.setX(Math.min(startX, endX));
                selectionRectangle.setY((Math.min(startY, endY)));
                selectionRectangle.setWidth(width);
                selectionRectangle.setHeight(height);

                // show the coordinates of the SelectionRectangle
                labelPoints.setText("(" + (int) (selectionRectangle.getX() -
                        imageViewMain.getBoundsInParent().getMinX()) + ", " +
                        (int) (selectionRectangle.getY() -
                                imageViewMain.getBoundsInParent().getMinY()) + "), (" +
                        (int) (width + selectionRectangle.getX()
                                - imageViewMain.getBoundsInParent().getMinX()) + ", " +
                        (int) (height + selectionRectangle.getY()
                                - imageViewMain.getBoundsInParent().getMinY()) + ")");
            }
        }
    }

    @FXML
    public void imageViewMain_onMouseExit(MouseEvent mouseEvent) {
        labelPoints.setText("");
    }

    @FXML
    public void imageViewMain_onMouseMove(MouseEvent mouseEvent) {
        refreshCoordinates(mouseEvent.getX(), mouseEvent.getY());
    }

    @FXML
    public void menuResize_onAction(ActionEvent actionEvent) throws IOException {
        if (mainViewModel.getSelectedImageModel() == null) {
            return;
        }

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("fxml/resizeWindow.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        Stage resizeWindow = new Stage();
        ResizeViewModel resizeViewModel = new ResizeViewModel(
                mainViewModel.getSelectedImageModel().getWidth(),
                mainViewModel.getSelectedImageModel().getHeight());
        resizeWindow.setScene(scene);
        resizeWindow.setTitle("Resize");
        resizeWindow.initModality(Modality.WINDOW_MODAL);
        resizeWindow.getIcons().add(((Stage) imageViewMain.getScene().getWindow()).getIcons().get(0));
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
        FXMLLoader fxmlLoader = new FXMLLoader(this.getClass().getResource("fxml/aboutWindow.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        AboutWindowController controller = fxmlLoader.getController();
        Stage aboutWindow = new Stage();
        aboutWindow.setScene(scene);
        aboutWindow.initModality(Modality.WINDOW_MODAL);
        aboutWindow.initOwner(imageViewMain.getScene().getWindow());
        aboutWindow.getIcons().add(((Stage) imageViewMain.getScene().getWindow()).getIcons().get(0));
        aboutWindow.setTitle("About");
        aboutWindow.show();
        controller.buttonOK.requestFocus();
    }

    @FXML
    public void menuFullScreen_onAction(ActionEvent actionEvent) {
        toggleFullScreen();
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
    public void scrollPaneMain_onClick(MouseEvent mouseEvent) {
        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            scrollPaneMain.requestFocus();
            if (mouseEvent.getClickCount() == 2) {
                toggleFullScreen();
            }
        } else if (mouseEvent.getButton().equals(MouseButton.MIDDLE)) {
            toggleFullScreen();
        }
    }

    @FXML
    public void scrollPaneMain_onKeyPress(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            // don't switch images if the scrollbar is visible
            case LEFT:
            case PAGE_DOWN:
                if (getViewingWidth() * 0.95 < scrollPaneMain.getWidth()) {
                    showPrevious();
                }
                break;

            case RIGHT:
            case PAGE_UP:
                if (getViewingWidth() * 0.95 < scrollPaneMain.getWidth()) {
                    showNext();
                }
                break;

            case HOME:
                showFirst();
                break;

            case END:
                showLast();
                break;

            case ENTER:
                toggleFullScreen();
                break;

            case ESCAPE:
                if (isViewingFullScreen.get()) {
                    toggleFullScreen();
                } else {
                    Platform.exit();
                }
                break;

            default:
                break;
        }
    }

    @FXML
    public void scrollPaneMain_onScroll(ScrollEvent scrollEvent) {
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
        deleteFile();
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
    }

    @FXML
    public void menuViewFavorites_onAction(ActionEvent actionEvent) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("fxml/favoritesWindow.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("styles/favoritesWindowStyle.css").toExternalForm());
        FavoritesWindowController controller = fxmlLoader.getController();
        Stage favoritesWindow = new Stage();
        favoritesWindow.setScene(scene);
        favoritesWindow.initModality(Modality.WINDOW_MODAL);
        favoritesWindow.getIcons().add(((Stage) imageViewMain.getScene().getWindow()).getIcons().get(0));
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

    private void toggleFullScreen() {
        if (mainViewModel.getSelectedImageModel() == null) {
            return;
        }

        boolean setFullScreen = !isViewingFullScreen.get();
        ((Stage) scrollPaneMain.getScene().getWindow()).setFullScreen(setFullScreen);
        menuBar.setVisible(!setFullScreen);
        toolBar.setVisible(!setFullScreen);
        statusBar.setVisible(!setFullScreen);
        isViewingFullScreen.set(setFullScreen);
        menuFullScreen.setSelected(setFullScreen);
    }

    /**
     * @return The width of the image as it's being displayed on the screen.
     */
    private double getViewingWidth() {
        double width = imageViewMain.getFitHeight() * mainViewModel.getSelectedImageModel().getAspectRatio();
        if (width > imageViewMain.getFitWidth()) {
            width = imageViewMain.getFitWidth();
        }
        return width;
    }

    /**
     * @return The height of the image as it's being displayed on the screen.
     */
    private double getViewingHeight() {
        double height = imageViewMain.getFitWidth() / mainViewModel.getSelectedImageModel().getAspectRatio();
        if (height > imageViewMain.getFitHeight()) {
            height = imageViewMain.getFitHeight();
        }
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
            title = mainViewModel.getSelectedImageModel().getName() + " - SlimView [Zoom: "
                    + (int) getViewingWidth() + " x " + (int) getViewingHeight() + " px]";
        }
        stage.setTitle(title);
    }

    /**
     * Main method to load an image into the application. Don't use ViewModel's function directly.
     *
     * @param path Path to the image file.
     */
    public void openImage(String path) {
        try {
            mainViewModel.loadImage(path);
        } catch (IOException e) {
            Util.showCustomErrorDialog(
                            "Loading failed",
                            "The requested file doesn't exist or is unreadable.",
                            imageViewMain.getScene().getWindow(), e)
                    .show();
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

    private void deleteFile() {
        if (mainViewModel.getSelectedImageModel() == null) return;
        try {
            mainViewModel.trashImage(mainViewModel.getSelectedImageModel());
        } catch (Exception e) {
            Util.showCustomErrorDialog(
                    "Deletion failed",
                    "The requested file couldn't be deleted.",
                    imageViewMain.getScene().getWindow(), e);
        }
    }

    private void zoomIn() {
        double originalWidth = mainViewModel.getSelectedImageModel().hasOriginal()
                ? mainViewModel.getSelectedImageModel().getOriginal().getWidth()
                : mainViewModel.getSelectedImageModel().getWidth();
        double originalHeight = mainViewModel.getSelectedImageModel().hasOriginal()
                ? mainViewModel.getSelectedImageModel().getOriginal().getHeight()
                : mainViewModel.getSelectedImageModel().getHeight();
        double screenWidth = Screen.getPrimary().getVisualBounds().getWidth();
        double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();
        double targetWidth = getViewingWidth() + getViewingWidth() * zoomStep;
        double targetHeight = getViewingHeight() + getViewingHeight() * zoomStep;
        double maxAllowedWidth = Math.min(5 * originalWidth, 3 * screenWidth);
        double maxAllowedHeight = Math.min(5 * originalHeight, 3 * screenHeight);

        // apply zoom in limit
        if (targetWidth > maxAllowedWidth || targetHeight > maxAllowedHeight) {
            return;
        }

        // latch onto the original size if we're around it
        if ((targetWidth >= 0.95 * originalWidth && targetWidth <= 1.05 * originalWidth)
                || (targetHeight >= 0.95 * originalHeight && targetHeight <= 1.05 * originalHeight)) {
            targetWidth = originalWidth;
            targetHeight = originalHeight;
        }

        mainViewModel.resizeImage(mainViewModel.getSelectedImageModel(), (int) targetWidth, (int) targetHeight);
        viewStyleProperty.set(ViewStyle.ORIGINAL);
    }

    private void zoomOut() {
        double originalWidth = mainViewModel.getSelectedImageModel().hasOriginal()
                ? mainViewModel.getSelectedImageModel().getOriginal().getWidth()
                : mainViewModel.getSelectedImageModel().getWidth();
        double originalHeight = mainViewModel.getSelectedImageModel().hasOriginal()
                ? mainViewModel.getSelectedImageModel().getOriginal().getHeight()
                : mainViewModel.getSelectedImageModel().getHeight();
        double targetWidth = getViewingWidth() - getViewingWidth() * zoomStep;
        double targetHeight = getViewingHeight() - getViewingHeight() * zoomStep;
        double minAllowedWidth = Math.max(0.1 * originalWidth, 8.0);
        double minAllowedHeight = Math.max(0.1 * originalHeight, 8.0);

        // apply zoom out limit
        if (targetWidth < minAllowedWidth || targetHeight < minAllowedHeight) {
            return;
        }

        // latch onto the original size if we're around it
        if ((targetWidth >= 0.95 * originalWidth && targetWidth <= 1.05 * originalWidth)
                || (targetHeight >= 0.95 * originalHeight && targetHeight <= 1.05 * originalHeight)) {
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

            // In Windows, put all extensions in one line
            StringBuilder stringBuilder = new StringBuilder();
            for (String ext : mainViewModel.getSupportedExtensions()) {
                stringBuilder.append("*").append(".").append(ext.toLowerCase()).append(";");
            }
            String extensions = stringBuilder.substring(0, stringBuilder.length() - 1);

            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("All Images", extensions),
                    new FileChooser.ExtensionFilter("All Files", "*.*"));

        } else {

            // *nix doesn't like Windows-style extension filters
            fileChooser.getExtensionFilters().addAll(
                    getExtensionFilters());
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("All Files", "*.*"));
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
        if (mainViewModel.getSelectedImageModel() == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(getExtensionFilters());

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
            if (selectionRectangle == null) {
                mainViewModel.copyToClipboard(mainViewModel.getSelectedImageModel().getBufferedImage());
            } else {
                copySelection();
            }
        }
    }

    /**
     * Copies to clipboard the part of the image bounded by the SelectionRectangle
     */
    private void copySelection() {
        if (mainViewModel.getSelectedImageModel() == null || selectionRectangle == null) return;

        double x = selectionRectangle.getBoundsInParent().getMinX() - imageViewMain.getBoundsInParent().getMinX();
        double y = selectionRectangle.getBoundsInParent().getMinY() - imageViewMain.getBoundsInParent().getMinY();
        double width =
                selectionRectangle.getBoundsInParent().getMaxX() - selectionRectangle.getBoundsInParent().getMinX();
        double height =
                selectionRectangle.getBoundsInParent().getMaxY() - selectionRectangle.getBoundsInParent().getMinY();
        double scaleFactor = getViewingWidth() / mainViewModel.getSelectedImageModel().getWidth();

        mainViewModel.copyToClipboard(
                mainViewModel.cropImage(mainViewModel.getSelectedImageModel(), x, y, width, height, scaleFactor));
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
        if (mainViewModel.getSelectedImageModel() == null) {
            return;
        }

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("fxml/imageInfoWindow.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root);
        ImageInfoWindowController controller = fxmlLoader.getController();
        Stage imageInfoWindow = new Stage();
        scene.getStylesheets().add(getClass().getResource("styles/imageInfoWindowStyle.css").toExternalForm());
        imageInfoWindow.setScene(scene);
        imageInfoWindow.initModality(Modality.WINDOW_MODAL);
        imageInfoWindow.getIcons().add(((Stage) imageViewMain.getScene().getWindow()).getIcons().get(0));
        imageInfoWindow.initOwner(imageViewMain.getScene().getWindow());
        imageInfoWindow.setTitle("Image Properties");
        controller.loadInfo(mainViewModel.getSelectedImageModel());
        imageInfoWindow.show();
    }

    private void copyFileTo() throws IOException {
        if (mainViewModel.getSelectedImageModel() == null) {
            return;
        }

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("fxml/copyFileToWindow.fxml"));
        Parent root = fxmlLoader.load();
        CopyFileToWindowController controller = fxmlLoader.getController();
        Scene scene = new Scene(root);
        Stage copyFileToWindow = new Stage();
        copyFileToWindow.setScene(scene);
        copyFileToWindow.initModality(Modality.WINDOW_MODAL);
        copyFileToWindow.getIcons().add(((Stage) imageViewMain.getScene().getWindow()).getIcons().get(0));
        copyFileToWindow.initOwner(imageViewMain.getScene().getWindow());
        copyFileToWindow.setTitle("Copy File To");
        controller.setViewModel(new CopyFileToViewModel(mainViewModel.getSelectedImageModel()));
        copyFileToWindow.show();
    }

    private FileChooser.ExtensionFilter[] getExtensionFilters() {
        ArrayList<FileChooser.ExtensionFilter> filters = new ArrayList<>();
        String[] extensions = mainViewModel.getSupportedExtensions();

        // separate item for each extension
        for (String ext : extensions) {
            ext = ext.replace("*", "").replace(".", "");
            filters.add(new FileChooser.ExtensionFilter(ext.toUpperCase() + " Image", "*." + ext.toLowerCase()));
        }

        return filters.toArray(FileChooser.ExtensionFilter[]::new);
    }

    /**
     * Shows the coordinates of the cursor point when the mouse is hovering over the image
     */
    private void refreshCoordinates(double x, double y) {
        if (mainViewModel.getSelectedImageModel() == null) {
            labelPoints.setText("");
        } else {
            labelPoints.setText("[ " + (int) x + ", " + (int) y + " ]");
        }
    }

    /**
     * Triggered when the image is changed
     */
    private class ImageChangeListener implements ChangeListener<ImageModel> {

        @Override
        public void changed(ObservableValue<? extends ImageModel> observable,
                            ImageModel oldValue,
                            ImageModel newValue) {

            tButtonFavorite.setSelected(newValue.getIsFavorite());
            imageViewMain.setImage(newValue.getImage());

            // reset the ViewStyle if we've zoomed image
            ViewStyle oldViewStyle;
            if (newValue.hasOriginal() || oldValue == null) {
                oldViewStyle = viewStyleProperty.get();
            } else {
                oldViewStyle = cachedViewStyleZoom;
            }
            viewStyleProperty.set(null); // force trigger ChangeListener
            viewStyleProperty.set(oldViewStyle);

            clearSelectionRectangle();
            imageViewMain.requestFocus();

            try {
                updateTitle();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Triggered with the ViewStyle is changed
     */
    private class ViewStyleChangeListener implements ChangeListener<ViewStyle> {

        private final double menuBarHeight, toolBarHeight, statusBarHeight;

        public ViewStyleChangeListener(double menuBarHeight, double toolBarHeight, double statusBarHeight) {
            this.menuBarHeight = menuBarHeight;
            this.toolBarHeight = toolBarHeight;
            this.statusBarHeight = statusBarHeight;
        }

        @Override
        public void changed(ObservableValue<? extends ViewStyle> observable,
                            ViewStyle oldValue,
                            ViewStyle newValue) {

            if (mainViewModel.getSelectedImageModel() == null) {
                return;
            }

            if (newValue == null) {
                newValue = Objects.requireNonNullElse(oldValue, ViewStyle.FIT_TO_WINDOW);
            }

            imageViewMain.fitWidthProperty().unbind();
            imageViewMain.fitHeightProperty().unbind();

            scrollPaneMain.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPaneMain.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

            scrollPaneMain.setFitToHeight(true);
            scrollPaneMain.setFitToWidth(true);

            if (imageViewMain.getImage() != null) {
                imageViewMain.setFitWidth(mainViewModel.getSelectedImageModel().getWidth());
                imageViewMain.setFitHeight(mainViewModel.getSelectedImageModel().getHeight());
            }

            imageViewMain.setPreserveRatio(true);

            switch (newValue) {
                case ORIGINAL:
                    menuOriginalSize.setSelected(true);
                    scrollPaneMain.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    scrollPaneMain.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    break;

                case FIT_TO_WINDOW:
                    menuFitToWindow.setSelected(true);
                    imageViewMain.fitWidthProperty().bind(scrollPaneMain.widthProperty());
                    imageViewMain.fitHeightProperty().bind(scrollPaneMain.heightProperty());
                    break;

                case FIT_TO_DESKTOP:
                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    Rectangle desktopSize
                            = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                    double taskBarHeight = screenSize.height - desktopSize.height;
                    double titleBarHeight = taskBarHeight;
                    double screenWidth = screenSize.getWidth();
                    double screenHeight = screenSize.getHeight();
                    double aspectRatio = mainViewModel.getSelectedImageModel().getAspectRatio();
                    double fixedHeight = menuBarHeight + toolBarHeight + statusBarHeight;

                    menuFitToDesktop.setSelected(true);
                    scrollPaneMain.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    scrollPaneMain.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

                    double targetWidth, targetHeight;

                    if (isViewingFullScreen.get()) {
                        targetWidth = screenWidth;
                    } else {
                        targetWidth = screenWidth - 16; // window border size * 2
                    }

                    targetHeight = targetWidth / aspectRatio;

                    if (isViewingFullScreen.get()) {
                        if (targetHeight > screenHeight) {
                            targetHeight = screenHeight;
                            targetWidth = aspectRatio * targetHeight;
                        }
                    } else {
                        double viewableHeight = screenHeight - taskBarHeight - titleBarHeight - fixedHeight;
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

                        // add 8 pixels of gutting on each side of ImageView to account for window borders
                        window.setWidth(targetWidth + 16);
                        window.setHeight(targetHeight + titleBarHeight + fixedHeight);
                        window.setX(0);
                        window.setY(0);
                    }
                    break;

                case STRETCHED:
                    menuStretched.setSelected(true);
                    imageViewMain.setPreserveRatio(false);
                    imageViewMain.fitWidthProperty().bind(scrollPaneMain.widthProperty());
                    imageViewMain.fitHeightProperty().bind(scrollPaneMain.heightProperty());
                    break;
            }

            // cache ViewStyle for use after zooming
            if (!mainViewModel.getSelectedImageModel().hasOriginal()
                    || mainViewModel.getSelectedImageModel() == null) {
                cachedViewStyleZoom = newValue;
            }

            preferences.put("LastViewStyle", newValue.toString());
            clearSelectionRectangle();
            imageViewMain.requestFocus();
        }
    }

    /**
     * Triggered when the file SortStyle is changed
     */
    private class SortStyleChangeListener implements ChangeListener<MainViewModel.SortStyle> {

        @Override
        public void changed(ObservableValue<? extends MainViewModel.SortStyle> observable,
                            MainViewModel.SortStyle oldValue,
                            MainViewModel.SortStyle newValue) {

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
    }

    /**
     * Triggered when the image size is changed
     */
    private class ImageSizeChangeListener implements ChangeListener<Number> {

        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {

            updateTitle();
            labelResolution.setText("");
            clearSelectionRectangle();

            if (mainViewModel.getSelectedImageModel() != null) {
                double currentWidth = mainViewModel.getSelectedImageModel().hasOriginal()
                        ? mainViewModel.getSelectedImageModel().getOriginal().getWidth()
                        : mainViewModel.getSelectedImageModel().getWidth();
                double zoom = getViewingWidth() / currentWidth * 100;
                labelResolution.setText(
                        (mainViewModel.getSelectedImageModel().hasOriginal()
                                ? mainViewModel.getSelectedImageModel().getOriginal().getResolution()
                                : mainViewModel.getSelectedImageModel().getResolution()) + " (" + Math.round(zoom) + "%)");
            }
        }
    }
}
