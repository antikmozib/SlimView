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
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private boolean isCtrlDown = false;

    // for favorite button
    private final Image favoriteOutline = new Image(getClass().getResourceAsStream("icons/favorite.png"));
    private final Image favoriteSolid = new Image(getClass().getResourceAsStream("icons/favorite-solid.png"));

    private final ToggleGroup toggleGroupViewStyle = new ToggleGroup();
    private final ToggleGroup toggleGroupSortStyle = new ToggleGroup();
    private final ToggleGroup toggleGroupSelectPan = new ToggleGroup();

    private final SimpleBooleanProperty isViewingFullScreen = new SimpleBooleanProperty(false);
    private final SimpleObjectProperty<ViewStyle> viewStyleProperty
            = new SimpleObjectProperty<>(ViewStyle.FIT_TO_DESKTOP);

    // FIELDS FOR SELECTION RECTANGLE
    private javafx.scene.shape.Rectangle selectionRectangle = null;
    /**
     * Set to true if selection mode is active and pan mode is inactive.
     */
    private final SimpleBooleanProperty selectionModeActive = new SimpleBooleanProperty();
    /**
     * Set to true if and only if the mouse button is down. May be false even if the SelectionRectangle is visible.
     */
    private final SimpleBooleanProperty selectionStartedProperty = new SimpleBooleanProperty(false);
    private boolean cursorInsideSelRect = false; // true if the cursor is inside the SelectionRectangle
    private final ImageCursor zoomInCursor
            = new ImageCursor(new Image(getClass().getResourceAsStream("icons/zoom-in-cursor.png")));
    // initial point where mouse was clicked
    private double selectionPivotX = 0.0;
    private double selectionPivotY = 0.0;

    // Structure: AnchorPane (+FullScreenGrid) > ScrollPane > AnchorPane (+SelRect) > StackPane > ImageView

    @FXML
    public BorderPane borderPaneWindow;
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
    public GridPane gridPaneStatusBar;
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
    public GridPane gridPaneQuickInfo;
    @FXML
    public Label labelQuickInfo;
    @FXML
    public Label labelQuickInfo2;
    @FXML
    public Label labelQuickInfo3;

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // initialize only UI control listeners in this method

        // reset control properties
        labelResolution.setText("");
        labelPoints.setText("");
        labelQuickInfo.setText("");
        labelQuickInfo2.setText("");
        labelQuickInfo3.setText("");
        gridPaneQuickInfo.toFront();
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

        // Button ToggleGroup for selection and pan modes
        tButtonPanMode.setToggleGroup(toggleGroupSelectPan);
        tButtonSelectionMode.setToggleGroup(toggleGroupSelectPan);
        tButtonPanMode.selectedProperty().bindBidirectional(scrollPaneMain.pannableProperty());
        tButtonSelectionMode.selectedProperty().bindBidirectional(selectionModeActive);

        // bind SelectionRectangle properties
        selectionModeActive.set(preferences.getBoolean("CurrentSelectionMode", true));
        selectionModeActive.addListener(((observable, oldValue, newValue) -> {
            scrollPaneMain.setPannable(!newValue);
            if (!newValue) {
                clearSelectionRectangle();
            }
            preferences.putBoolean("CurrentSelectionMode", newValue);
        }));
        scrollPaneMain.pannableProperty().addListener(((observable, oldValue, newValue)
                -> selectionModeActive.set(!newValue)));

        // trigger change listener
        var currentSelectionMode = selectionModeActive.get();
        selectionModeActive.set(!currentSelectionMode);
        selectionModeActive.set(currentSelectionMode);

        // bindings for full screen viewing
        toolBar.managedProperty().bind(toolBar.visibleProperty());
        gridPaneStatusBar.managedProperty().bind(gridPaneStatusBar.visibleProperty());
        menuBar.managedProperty().bind(menuBar.visibleProperty());
        gridPaneQuickInfo.visibleProperty().bind(isViewingFullScreen);

        // bind ImageView and FavoriteButton to SelectedImage
        tButtonFavorite.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                tButtonFavoriteImageView.setImage(favoriteSolid);
            } else {
                tButtonFavoriteImageView.setImage(favoriteOutline);
            }
        });

        // common EventHandler for all toolbar elements; focus on the ImageView whenever any element is actioned
        EventHandler<ActionEvent> defaultToolbarEventHandler = event -> {
            imageViewMain.requestFocus();
        };
        // set focus on the ImageView whenever any Button on the Toolbar is actioned
        toolBar.getItems().forEach(node -> {
            if (node instanceof Button) {
                Button button = (Button) node;
                button.addEventHandler(ActionEvent.ACTION, defaultToolbarEventHandler);
            } else if (node instanceof ToggleButton) {
                ToggleButton toggleButton = (ToggleButton) node;
                toggleButton.addEventHandler(ActionEvent.ACTION, defaultToolbarEventHandler);
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
            menuItem.setOnAction(event -> openImage(menuItem.getText()));
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

        // *nix (esp Ubuntu) has a weird desktop bound measurement system; account for this
        double titleBarHeight = window.getHeight() - window.getScene().getHeight();
        double windowBorderLeftRight = window.getWidth() - window.getScene().getWidth();
        double fixedWidth, fixedHeight;

        if (getOSType() == Util.OSType.WINDOWS) {
            fixedWidth = windowBorderLeftRight;
            fixedHeight = titleBarHeight + menuBar.getHeight() + toolBar.getHeight() + gridPaneStatusBar.getHeight();
        } else {
            fixedWidth = 12;
            fixedHeight
                    = 12 + titleBarHeight + menuBar.getHeight() + toolBar.getHeight() + gridPaneStatusBar.getHeight();
        }

        // bind ChangeListeners
        mainViewModel.selectedImageModelProperty().addListener(new ImageChangeListener());
        viewStyleProperty.addListener(new ViewStyleChangeListener(fixedWidth, fixedHeight));
        mainViewModel.selectedSortStyleProperty().addListener(new SortStyleChangeListener());

        // restore previous settings
        viewStyleProperty.set(ViewStyle.valueOf( /* Default view style */
                preferences.get("LastViewStyle", ViewStyle.FIT_TO_DESKTOP.toString())));
        mainViewModel.sortImages(MainViewModel.SortStyle.valueOf( /* Default sort style */
                preferences.get("LastSortStyle", MainViewModel.SortStyle.DATE_MODIFIED.toString())));

        // refresh ViewStyle if we're switching to full screen mode
        isViewingFullScreen.addListener(((observable, oldValue, newValue) -> {
            ViewStyle old = viewStyleProperty.get();
            viewStyleProperty.set(null);
            viewStyleProperty.set(old);

            // don't show scroll bars if we're in full screen mode
            if (newValue) {
                scrollPaneMain.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                scrollPaneMain.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            } else {
                scrollPaneMain.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                scrollPaneMain.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            }
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
        switch (mouseEvent.getButton()) {
            case PRIMARY:
                if (selectionModeActive.get()
                        && !selectionStartedProperty.get()
                        && !cursorInsideSelRect
                        && mainViewModel.getSelectedImageModel() != null) {

                    clearSelectionRectangle();

                    // don't start selecting if initial point is outside the ImageView
                    if (mouseEvent.getX() < imageViewMain.getBoundsInParent().getMinX()
                            || mouseEvent.getY() < imageViewMain.getBoundsInParent().getMinY()
                            || mouseEvent.getX() > imageViewMain.getBoundsInParent().getMaxX()
                            || mouseEvent.getY() > imageViewMain.getBoundsInParent().getMaxY()) {
                        return;
                    }

                    selectionStartedProperty.set(true);
                }
                break;

            case SECONDARY:
                // if secondary mouse button is down, force into a temporary pan mode
                if (selectionModeActive.get()) {
                    selectionModeActive.set(false);
                }
                break;

            case BACK:
                showPrevious();
                break;

            case FORWARD:
                showNext();
                break;

            default:
                break;
        }

        // attempt to clear the selection rectangle on any mouse button press
        if (!cursorInsideSelRect && !selectionStartedProperty.get()) {
            clearSelectionRectangle();
        }
    }

    @FXML
    public void anchorPaneMain_onMouseRelease(MouseEvent mouseEvent) {
        imageViewMain.setCursor(Cursor.DEFAULT);

        switch (mouseEvent.getButton()) {
            case PRIMARY:
                // stop selecting but don't clear the rectangle yet as we need it for copying, zooming etc.
                selectionStartedProperty.set(false);
                break;

            case SECONDARY:
                selectionModeActive.set(true);
                break;

            default:
                break;
        }
    }

    @FXML
    public void anchorPaneMain_onMouseDrag(MouseEvent mouseEvent) {
        if (!selectionModeActive.get()) {
            imageViewMain.setCursor(Cursor.MOVE);
        } else {
            imageViewMain.setCursor(Cursor.CROSSHAIR);
        }

        if (selectionStartedProperty.get()) {
            if (selectionRectangle == null) {

                // save this point for selecting in the reverse direction
                selectionPivotX = mouseEvent.getX();
                selectionPivotY = mouseEvent.getY();

                selectionRectangle = new javafx.scene.shape.Rectangle();
                selectionRectangle.getStyleClass().add("selection-rect");
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
                selectionRectangle.setOnMouseMoved(new EventHandler<MouseEvent>() {
                    @Override
                    public void handle(MouseEvent event) {
                        // add 1 to account for SelectionRectangle's borders
                        refreshCoordinates(
                                event.getX() - imageViewMain.getBoundsInParent().getMinX() + 1,
                                event.getY() - imageViewMain.getBoundsInParent().getMinY() + 1);

                        if (!selectionStartedProperty.get()) {
                            selectionRectangle.setCursor(zoomInCursor);
                        }
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
                        double scaleFactor;

                        // fit the biggest selected side
                        if (selectedWidth >= selectedHeight) {
                            scaleFactor = scrollPaneMain.getViewportBounds().getWidth() / selectedWidth;
                        } else {
                            scaleFactor = scrollPaneMain.getViewportBounds().getHeight() / selectedHeight;
                        }

                        double targetWidth = Math.round(scaleFactor * getViewingWidth());
                        double targetHeight = Math.round(scaleFactor * getViewingHeight());

                        zoom(targetWidth, targetHeight);

                        // scroll to the new zoomed point
                        scrollPaneMain.layout();
                        double targetLeft = ((selectedLeft + selectedWidth / 2) * scaleFactor) / targetWidth;
                        double targetTop = ((selectedTop + selectedHeight / 2) * scaleFactor) / targetHeight;
                        scrollPaneMain.setHvalue(targetLeft);
                        scrollPaneMain.setVvalue(targetTop);

                        clearSelectionRectangle();
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

                // we can only be outside a maximum of two boundaries at the same time, e.g. top-right
                if (endX < leftBoundary) {
                    endX = leftBoundary;
                }

                if (endX > rightBoundary) {
                    endX = rightBoundary;
                }

                if (endY < topBoundary) {
                    endY = topBoundary;
                }

                if (endY > bottomBoundary) {
                    endY = bottomBoundary;
                }

                double width = Math.max(startX, endX) - Math.min(startX, endX);
                double height = Math.max(startY, endY) - Math.min(startY, endY);

                selectionRectangle.setX(Math.min(startX, endX));
                selectionRectangle.setY((Math.min(startY, endY)));
                selectionRectangle.setWidth(width);
                selectionRectangle.setHeight(height);

                // show the coordinates of the SelectionRectangle
                labelPoints.setText("("
                        + (int) (selectionRectangle.getX() - imageViewMain.getBoundsInParent().getMinX()) + ", "
                        + (int) (selectionRectangle.getY() - imageViewMain.getBoundsInParent().getMinY()) + "), ("
                        + (int) (width + selectionRectangle.getX() - imageViewMain.getBoundsInParent().getMinX()) + ", "
                        + (int) (height + selectionRectangle.getY() - imageViewMain.getBoundsInParent().getMinY()) + ")");
            }
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

            case LEFT:
                // don't switch images if the scrollbar is visible
                if (getViewingWidth() <= scrollPaneMain.getViewportBounds().getWidth()) {
                    showPrevious();
                }
                break;

            case RIGHT:
                if (getViewingWidth() <= scrollPaneMain.getViewportBounds().getWidth()) {
                    showNext();
                }
                break;

            case UP:
                if (getViewingHeight() <= scrollPaneMain.getViewportBounds().getHeight()) {
                    showNext();
                }
                break;

            case PAGE_UP:
                showNext();
                break;

            case DOWN:
                if (getViewingHeight() <= scrollPaneMain.getViewportBounds().getHeight()) {
                    showPrevious();
                }
                break;

            case PAGE_DOWN:
                showPrevious();
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

            case CONTROL:
                isCtrlDown = true;
                break;

            case SHIFT:
            case ALT:
                // prevent conflict with menu shortcuts
                isCtrlDown = false;
                break;

            case S: // toggle selection mode
                selectionModeActive.set(true);
                break;

            case P: // toggle pan mode
                selectionModeActive.set(false);
                break;

            case F: // toggle favorite
                toggleFavorite();
                break;

            default:
                break;
        }
    }

    @FXML
    public void scrollPaneMain_onKeyRelease(KeyEvent keyEvent) {
        isCtrlDown = false;
    }

    @FXML
    public void scrollPaneMain_onScroll(ScrollEvent scrollEvent) {
        scrollEvent.consume();

        if (isCtrlDown) {

            // ctrl is down; zoom image instead of switching
            if (scrollEvent.getDeltaY() > 0 || scrollEvent.getDeltaX() > 0) {
                zoomIn();
            } else if (scrollEvent.getDeltaY() < 0 || scrollEvent.getDeltaX() < 0) {
                zoomOut();
            }

        } else {

            // don't switch images if scrollbar is visible
            if (getViewingWidth() > scrollPaneMain.getViewportBounds().getWidth()
                    || getViewingHeight() > scrollPaneMain.getViewportBounds().getHeight()) {
                return;
            }

            if (scrollEvent.getDeltaY() > 0 || scrollEvent.getDeltaX() > 0) {
                showPrevious();
            } else if (scrollEvent.getDeltaY() < 0 || scrollEvent.getDeltaX() < 0) {
                showNext();
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
//        childWindowInCentre(imageViewMain.getScene().getWindow(), resizeWindow);
        resizeWindow.showAndWait();

        if (resizeViewModel.useNewValues.get()) {
            System.out.println(resizeViewModel.getSelectedQuality());
            viewStyleProperty.set(ViewStyle.ORIGINAL);
            mainViewModel.resizeImage(
                    mainViewModel.getSelectedImageModel(),
                    Integer.parseInt(resizeViewModel.newWidthProperty.get()),
                    Integer.parseInt(resizeViewModel.newHeightProperty.get()),
                    resizeViewModel.stringToMethod(resizeViewModel.getSelectedQuality()));
        }

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
//        childWindowInCentre(imageViewMain.getScene().getWindow(), aboutWindow);
        aboutWindow.show();
        controller.buttonOK.requestFocus();
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
                tButtonFavorite.isSelected());
        actionEvent.consume();
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
//        childWindowInCentre(imageViewMain.getScene().getWindow(), favoritesWindow);
        favoritesWindow.showAndWait();

        if (controller.getSelectedFavorite().get() != null) {
            openImage(controller.getSelectedFavorite().get().toString());
        }
    }

    private void toggleFullScreen() {
        if (mainViewModel.getSelectedImageModel() == null) {
            return;
        }

        boolean setFullScreen = !isViewingFullScreen.get();
        ((Stage) scrollPaneMain.getScene().getWindow()).setFullScreen(setFullScreen);
        menuBar.setVisible(!setFullScreen);
        toolBar.setVisible(!setFullScreen);
        gridPaneStatusBar.setVisible(!setFullScreen);
        isViewingFullScreen.set(setFullScreen);
        menuFullScreen.setSelected(setFullScreen);
    }

    /**
     * Sets/unsets the current image as a favorite
     */
    private void toggleFavorite() {
        if (mainViewModel.getSelectedImageModel() == null) {
            return;
        }

        tButtonFavorite.setSelected(!tButtonFavorite.isSelected());
        mainViewModel.setAsFavorite(mainViewModel.getSelectedImageModel(),
                tButtonFavorite.isSelected());
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
     * @return Size of the displayed image as a ratio of the size of the original image
     */
    private double getCurrentViewingZoom() {
        return BigDecimal.valueOf(100 * getViewingWidth()
                        / (mainViewModel.getSelectedImageModel().hasOriginal()
                        ? mainViewModel.getSelectedImageModel().getOriginal().getWidth()
                        : mainViewModel.getSelectedImageModel().getWidth()))
                .setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Updates the title of the application window.
     */
    private void updateTitle() {
        Stage stage = (Stage) imageViewMain.getScene().getWindow();
        String title;
        if (mainViewModel.getSelectedImageModel() == null) {
            title = "SlimView";
        } else {
            title = mainViewModel.getSelectedImageModel().getName() + " - SlimView [Zoom: "
                    + (int) getViewingWidth() + " x " + (int) getViewingHeight() + " px]";
        }
        stage.setTitle(title);
    }

    private void updateFullScreenInfo() {
        labelQuickInfo.setText(mainViewModel.getSelectedImageModel().getBestPath() + " ["
                + (mainViewModel.getIndex(mainViewModel.getSelectedImageModel()) + 1) + "/"
                + mainViewModel.getFileCount() + "]");
        labelQuickInfo2.setText(getCurrentViewingZoom() + "%");
        labelQuickInfo3.setText(mainViewModel.getSelectedImageModel().hasOriginal()
                ? mainViewModel.getSelectedImageModel().getOriginal().getResolution()
                : mainViewModel.getSelectedImageModel().getResolution());
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
            showLoadingFailedError(e);
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
        if (mainViewModel.getSelectedImageModel() == null) {
            return;
        }

        try {
            mainViewModel.trashImage(mainViewModel.getSelectedImageModel());
        } catch (Exception e) {
            Util.showCustomErrorDialog(
                    "Deletion failed",
                    "The requested file couldn't be deleted.",
                    imageViewMain.getScene().getWindow(), e);
        }
    }

    private void zoom(double targetWidth, double targetHeight) {
        double maxAllowedWidth = 5
                * (mainViewModel.getSelectedImageModel().hasOriginal()
                ? mainViewModel.getSelectedImageModel().getOriginal().getWidth()
                : mainViewModel.getSelectedImageModel().getWidth());
        double maxAllowedHeight = 5
                * (mainViewModel.getSelectedImageModel().hasOriginal()
                ? mainViewModel.getSelectedImageModel().getOriginal().getHeight()
                : mainViewModel.getSelectedImageModel().getHeight());
        double minAllowedWidth = Math.max(1, maxAllowedWidth / 5 * 0.1);
        double minAllowedHeight = Math.max(1, maxAllowedHeight / 5 * 0.1);

        if (targetWidth > maxAllowedWidth) {
            targetWidth = maxAllowedWidth;
        }
        if (targetWidth < minAllowedWidth) {
            targetWidth = minAllowedWidth;
        }
        if (targetHeight > maxAllowedHeight) {
            targetHeight = maxAllowedHeight;
        }
        if (targetHeight < minAllowedHeight) {
            targetHeight = minAllowedHeight;
        }

        imageViewMain.fitHeightProperty().unbind();
        imageViewMain.fitWidthProperty().unbind();
        imageViewMain.setFitWidth(targetWidth);
        imageViewMain.setFitHeight(targetHeight);

        // center new zoomed image
        scrollPaneMain.setVvalue(0.5);
        scrollPaneMain.setHvalue(0.5);
    }

    private void zoomIn() {
        double targetWidth = getViewingWidth() * (1 + zoomStep);
        double targetHeight = getViewingHeight() * (1 + zoomStep);
        zoom(targetWidth, targetHeight);
    }

    private void zoomOut() {
        double targetWidth = getViewingWidth() * (1 - zoomStep);
        double targetHeight = getViewingHeight() * (1 - zoomStep);
        zoom(targetWidth, targetHeight);
    }

    private void resetZoom() {
        viewStyleProperty.set(null);
        viewStyleProperty.set(ViewStyle.ORIGINAL);
    }

    private void bestFit() {
        viewStyleProperty.set(null);
        viewStyleProperty.set(ViewStyle.FIT_TO_WINDOW);
    }

    private void open() {
        FileChooser fileChooser = new FileChooser();

        if (getOSType() == Util.OSType.WINDOWS) {

            // In Windows, put all extensions in one line
            StringBuilder stringBuilder = new StringBuilder();
            for (String ext : mainViewModel.getSupportedReadExtensions()) {
                stringBuilder.append("*").append(".").append(ext.toLowerCase()).append(";");
            }
            String extensions = stringBuilder.substring(0, stringBuilder.length() - 1);

            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("All Supported Images", extensions),
                    new FileChooser.ExtensionFilter("All Files", "*.*"));

        } else {

            // *nix doesn't like Windows-style extension filters
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("All Files", "*"));
            fileChooser.getExtensionFilters().addAll(
                    getExtensionFilters(mainViewModel.getSupportedReadExtensions()));
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
        fileChooser.getExtensionFilters().addAll(getExtensionFilters(mainViewModel.getSupportedWriteExtensions()));

        fileChooser.setInitialFileName(mainViewModel.getSelectedImageModel().getName());

        File initialDirectory = new File(preferences.get("SaveAsLocation", System.getProperty("user.home")));
        if (!initialDirectory.exists() || !initialDirectory.isDirectory()) {
            initialDirectory = new File(System.getProperty("user.home"));
        }
        fileChooser.setInitialDirectory(initialDirectory);

        File file = fileChooser.showSaveDialog(imageViewMain.getScene().getWindow());
        if (file != null) {
            try {
                preferences.put("SaveAsLocation", file.getParentFile().getPath());
                mainViewModel.saveImage(mainViewModel.getSelectedImageModel(), file.getPath());
            } catch (IOException e) {
                Util.showCustomErrorDialog("Error saving file",
                        "The file couldn't be saved.",
                        imageViewMain.getScene().getWindow(), e);
            }
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
        if (mainViewModel.getSelectedImageModel() == null || selectionRectangle == null) {
            return;
        }

        double x = selectionRectangle.getBoundsInParent().getMinX() - imageViewMain.getBoundsInParent().getMinX();
        double y = selectionRectangle.getBoundsInParent().getMinY() - imageViewMain.getBoundsInParent().getMinY();
        double width
                = selectionRectangle.getBoundsInParent().getMaxX() - selectionRectangle.getBoundsInParent().getMinX();
        double height
                = selectionRectangle.getBoundsInParent().getMaxY() - selectionRectangle.getBoundsInParent().getMinY();
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
        imageInfoWindow.setScene(scene);
        imageInfoWindow.initModality(Modality.WINDOW_MODAL);
        imageInfoWindow.getIcons().add(((Stage) imageViewMain.getScene().getWindow()).getIcons().get(0));
        imageInfoWindow.initOwner(imageViewMain.getScene().getWindow());
        imageInfoWindow.setTitle("Image Properties");
        controller.loadInfo(mainViewModel.getSelectedImageModel());
//        childWindowInCentre(imageViewMain.getScene().getWindow(), imageInfoWindow);
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
//        childWindowInCentre(imageViewMain.getScene().getWindow(), copyFileToWindow);
        copyFileToWindow.show();
    }

    private FileChooser.ExtensionFilter[] getExtensionFilters(String[] extensions) {
        ArrayList<FileChooser.ExtensionFilter> filters = new ArrayList<>();

        // separate item for each extension
        for (String ext : extensions) {
            ext = ext.replace("*", "").replace(".", "");
            filters.add(new FileChooser.ExtensionFilter(
                    ext.toUpperCase() + " Image", "*." + ext.toLowerCase()));
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

    private void showLoadingFailedError(Exception e) {

        Util.showCustomErrorDialog(
                        "Loading failed",
                        "The requested file doesn't exist or is unreadable.",
                        imageViewMain.getScene().getWindow(), e)
                .show();
    }

    /**
     * Positions a child window at the center of a parent window
     *
     * @param owner The owning window
     * @param child The window to be centered
     */
    /*private void childWindowInCentre(Window owner, Window child) {
        child.setOnShown(event -> {
            child.setX(owner.getX() + owner.getWidth() / 2 - child.getWidth() / 2);
            child.setY(owner.getY() + owner.getHeight() / 3 - child.getHeight() / 2);
        });
    }*/

    /**
     * Triggered when the image is changed
     */
    private class ImageChangeListener implements ChangeListener<ImageModel> {

        @Override
        public void changed(ObservableValue<? extends ImageModel> observable,
                            ImageModel oldValue,
                            ImageModel newValue) {

            // check if image is corrupted
            if (newValue != null && newValue.getImage() == null) {
                showLoadingFailedError(null);
            }

            tButtonFavorite.setSelected(newValue.getIsFavorite());
            imageViewMain.setImage(newValue.getImage());

            // reset the ViewStyle if we've zoomed image
            ViewStyle currentViewStyle = viewStyleProperty.get();
            viewStyleProperty.set(null); // force trigger ChangeListener
            viewStyleProperty.set(currentViewStyle);

            clearSelectionRectangle();
            imageViewMain.requestFocus();
            updateFullScreenInfo();

            try {
                updateTitle();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Triggered when the ViewStyle is changed
     */
    private class ViewStyleChangeListener implements ChangeListener<ViewStyle> {

        private final double fixedWidth;
        private final double fixedHeight;

        /**
         * @param fixedWidth  Width of the fixed UI elements such as window borders
         * @param fixedHeight Height of the fixed UI elements such as title bar and menu bar
         */
        public ViewStyleChangeListener(double fixedWidth, double fixedHeight) {
            this.fixedWidth = fixedWidth;
            this.fixedHeight = fixedHeight;
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

            imageViewMain.setPreserveRatio(true);
            imageViewMain.fitWidthProperty().unbind();
            imageViewMain.fitHeightProperty().unbind();

            // reset image size first
            if (imageViewMain.getImage() != null) {
                imageViewMain.setFitWidth(mainViewModel.getSelectedImageModel().getWidth());
                imageViewMain.setFitHeight(mainViewModel.getSelectedImageModel().getHeight());
            }

            switch (newValue) {
                case ORIGINAL:

                    menuOriginalSize.setSelected(true);
                    break;

                case FIT_TO_WINDOW:

                    menuFitToWindow.setSelected(true);
                    imageViewMain.fitWidthProperty().bind(scrollPaneMain.widthProperty());
                    imageViewMain.fitHeightProperty().bind(scrollPaneMain.heightProperty());
                    break;

                case FIT_TO_DESKTOP:

                    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                    double screenWidth = screenSize.getWidth();
                    double screenHeight = screenSize.getHeight();
                    double desktopViewportWidth /* Takes screen insets into account */
                            = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getWidth();
                    double desktopViewportHeight
                            = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getHeight();
                    double aspectRatio = mainViewModel.getSelectedImageModel().getAspectRatio();

                    double viewportWidth = desktopViewportWidth - fixedWidth;
                    double viewportHeight = desktopViewportHeight - fixedHeight;

                    double finalWidth,
                            finalHeight;

                    if (!isViewingFullScreen.get()) {

                        finalWidth = viewportWidth;
                        finalHeight = finalWidth / aspectRatio;
                        if (finalHeight > viewportHeight) {
                            finalHeight = viewportHeight;
                            finalWidth = aspectRatio * finalHeight;
                        }

                        Window window = imageViewMain.getScene().getWindow();

                        ((Stage) window).setMaximized(false);
                        window.setWidth(finalWidth + fixedWidth);
                        window.setHeight(finalHeight + fixedHeight);

                        // ensure window remains within view
                        if (window.getX() + window.getWidth() > desktopViewportWidth) {
                            window.setX(desktopViewportWidth - window.getWidth());
                        } else if (window.getX() < 0) {
                            window.setX(0);
                        }

                        if (window.getY() + window.getHeight() > desktopViewportHeight) {
                            window.setY(desktopViewportHeight - window.getHeight());
                        } else if (window.getY() < 0) {
                            window.setY(0);
                        }

                    } else {

                        finalWidth = screenWidth;
                        finalHeight = finalWidth / aspectRatio;
                        if (finalHeight > screenHeight) {
                            finalHeight = screenHeight;
                            finalWidth = aspectRatio * finalHeight;
                        }
                    }

                    menuFitToDesktop.setSelected(true);
                    imageViewMain.setPreserveRatio(false);
                    imageViewMain.setFitWidth(finalWidth);
                    imageViewMain.setFitHeight(finalHeight);
                    break;

                case STRETCHED:

                    menuStretched.setSelected(true);
                    imageViewMain.setPreserveRatio(false);
                    imageViewMain.fitWidthProperty().bind(scrollPaneMain.widthProperty());
                    imageViewMain.fitHeightProperty().bind(scrollPaneMain.heightProperty());
                    break;
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
            updateFullScreenInfo();
            labelResolution.setText("");
            clearSelectionRectangle();

            if (mainViewModel.getSelectedImageModel() != null) {
                labelResolution.setText(
                        (mainViewModel.getSelectedImageModel().hasOriginal()
                                ? mainViewModel.getSelectedImageModel().getOriginal().getResolution()
                                : mainViewModel.getSelectedImageModel().getResolution())
                                + " (" + getCurrentViewingZoom() + "%)");
            }
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
    public void menuFitToDesktop_onAction(ActionEvent actionEvent) {
        viewStyleProperty.set(ViewStyle.FIT_TO_DESKTOP);
    }
}
