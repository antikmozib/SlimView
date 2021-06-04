package io.mozib.simview;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

public class MainWindowController implements Initializable {

	@FXML
	private ImageView imageViewMain;

	@FXML
	private Pane pane;

	@FXML
	private Label labelStatus;

	private ObservableList<ImageModel> images;
	private List<ImageModel> imageModels;
	private ObjectProperty<Image> selectedImage;

	@FXML
	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		imageModels = new ArrayList<>();
		images = FXCollections.observableList(imageModels);

		System.out.println("Initialized!");
	}

	private void displayImage(ImageModel imageModel) {
		Stage stage = (Stage) imageViewMain.getScene().getWindow();
		imageViewMain.setImage(imageModel.getImage());
		imageViewMain.setPreserveRatio(false);
		imageViewMain.fitWidthProperty().bind(stage.widthProperty());
		imageViewMain.fitHeightProperty().bind(pane.heightProperty());
		imageViewMain.requestFocus();
		stage.setTitle(imageModel.getShortName() + " - SimView");
		stage.setFullScreen(true);
	}

	public void loadImage(ImageModel imageModel) {
		/*
		 * Process: 1. Load all the images in the directory 2. Find and display the
		 * image requested from the list
		 */

		LoadDirectory loadDirectory = new LoadDirectory(new File(imageModel.getPath()).getParent(), images);
		loadDirectory.setOnSucceeded(event -> {
			imageModels = (List<ImageModel>) event.getSource().getValue();
			displayImage(imageModels.stream().filter(image -> image.getPath().equals(imageModel.getPath())).findFirst()
					.get());
		});
		labelStatus.textProperty().bind(loadDirectory.messageProperty());
		loadDirectory.start();
	}

	@FXML
	public void menuClose_onAction(ActionEvent actionEvent) {
		System.out.println("Clicked!");
		Platform.exit();
	}

	private static class LoadDirectory extends Service<List<ImageModel>> {

		private final String directoryPath;
		private AtomicInteger fileCount;
		private ObservableList<ImageModel> images;

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
					Iterator<File> iterator = FileUtils.iterateFiles(new File(directoryPath),
							new String[] { "jpg", "png" }, false);
					// List<ImageModel> images = new ArrayList<>();

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
