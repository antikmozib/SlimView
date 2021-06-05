package io.mozib.simview;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

public class MainWindowController implements Initializable {

    @FXML
    private ImageView imageViewMain;

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

    private ObservableList<ImageModel> images;
    private List<ImageModel> imageModels;
    private ObjectProperty<Image> selectedImage;
    private SimpleStringProperty status;
    private SimpleBooleanProperty directoryScanCompleted;

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        imageModels = new ArrayList<>();
        images = FXCollections.observableList(imageModels);
        selectedImage = new SimpleObjectProperty<>();
        status = new SimpleStringProperty();
        directoryScanCompleted = new SimpleBooleanProperty(false);

        labelStatus.textProperty().bind(status);
        imageViewMain.imageProperty().bind(selectedImage);
        imageViewMain.setPreserveRatio(false);
        imageViewMain.fitWidthProperty().bind(pane.widthProperty());
        imageViewMain.fitHeightProperty().bind(pane.heightProperty());
        imageViewMain.requestFocus();
        toolBar.managedProperty().bind(toolBar.visibleProperty());
        statusBar.managedProperty().bind(statusBar.visibleProperty());
        menuBar.managedProperty().bind(menuBar.visibleProperty());

        System.out.println("Initialized!");
    }

    private void displayImage(ImageModel imageModel) {
        Stage stage = (Stage) imageViewMain.getScene().getWindow();

        selectedImage.set(imageModel.getImage());
        stage.setTitle(imageModel.getShortName() + " - SimView");
    }

    @SuppressWarnings("unchecked")
    public void loadImage(ImageModel imageModel) {
        /*
         * Process:
         * 1. Load all the images in the directory
         * 2. Find and display the image requested from the list
         */

        // first, show the image loaded while the directory is being scanned
        displayImage(imageModel);

        // now scan the rest of the directory
        LoadDirectory loadDirectory = new LoadDirectory(new File(imageModel.getPath()).getParent(), images);
        loadDirectory.setOnSucceeded(event -> {
            imageModels = (List<ImageModel>) event.getSource().getValue();
            displayImage(imageModels.stream().filter(image -> image.getPath().equals(imageModel.getPath())).findFirst().orElseThrow());
            directoryScanCompleted.set(true);
        });
        status.bind(loadDirectory.messageProperty());
        loadDirectory.start();
    }

    @FXML
    public void menuClose_onAction(ActionEvent actionEvent) {
        System.out.println("Clicked!");
        Platform.exit();
    }

    @FXML
    public void pane_onClick(MouseEvent mouseEvent) {
        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            if (mouseEvent.getClickCount() == 2) {
                Stage stage = (Stage) pane.getScene().getWindow();
                if (stage.isFullScreen()) {
                    stage.setFullScreen(false);
                    toolBar.setVisible(true);
                    statusBar.setVisible(true);
                    menuBar.setVisible(true);
                } else {
                    stage.setFullScreen(true);
                    toolBar.setVisible(false);
                    statusBar.setVisible(false);
                    menuBar.setVisible(false);
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

    private static class LoadDirectory extends Service<List<ImageModel>> {

        private final String directoryPath;
        private final AtomicInteger fileCount;
        private final ObservableList<ImageModel> images;

        public LoadDirectory(String directoryPath, ObservableList<ImageModel> images) {
            this.directoryPath = directoryPath;
            this.images = images;
            fileCount = new AtomicInteger(0);
        }

        public Integer getFileCount() {
            return fileCount.get();
        }

        @Override
        protected Task<List<ImageModel>> createTask() {
            return new Task<>() {
                @Override
                protected List<ImageModel> call() {
                    Iterator<File> iterator = FileUtils.iterateFiles(new File(directoryPath), new String[]{"jpg", "png"}, false);
                    while (iterator.hasNext()) {
                        ImageModel image = new ImageModel(iterator.next().getPath());
                        images.add(image);
                        fileCount.addAndGet(1);
                        updateMessage("Scanning " + directoryPath + "... " + image.getShortName());
                    }
                    updateMessage("Found " + getFileCount() + " files.");
                    return images;
                }
            };
        }
    }
}
