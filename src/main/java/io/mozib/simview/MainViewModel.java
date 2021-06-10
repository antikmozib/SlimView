package io.mozib.simview;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MainViewModel {
    private List<ImageModel> imageModels = new ArrayList<>();
    private final ObservableList<ImageModel> images = FXCollections.observableList(imageModels);
    private Integer currentIndex = 0;

    private final ReadOnlyStringWrapper status = new ReadOnlyStringWrapper();
    private final SimpleBooleanProperty directoryScanCompleted = new SimpleBooleanProperty(false);
    private final ReadOnlyObjectWrapper<ImageModel> selectedImageModelWrapper = new ReadOnlyObjectWrapper<>();

    public ReadOnlyObjectProperty<ImageModel> selectedImageModelProperty() {
        return selectedImageModelWrapper.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty statusProperty() {
        return status.getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public void loadImage(ImageModel imageModel) {
        // first, show the image requested while the directory is being scanned
        setSelectedImage(imageModel);

        // now scan the rest of the directory
        LoadDirectory loadDirectory = new LoadDirectory(new File(imageModel.getPath()).getParent(), images);
        loadDirectory.setOnSucceeded(event -> {
            imageModels = (List<ImageModel>) event.getSource().getValue();
            setSelectedImage(imageModels.stream().filter(image -> image.getPath().equals(imageModel.getPath())).findFirst().orElseThrow());
            directoryScanCompleted.set(true);
        });
        status.bind(loadDirectory.messageProperty());
        loadDirectory.start();
    }

    public void showFirstImage() {
        if (imageModels.size() == 0) return;
        currentIndex = 0;
        setSelectedImage(imageModels.get(currentIndex));
    }

    public void showLastImage() {
        if (imageModels.size() == 0) return;
        currentIndex = imageModels.size() - 1;
        setSelectedImage(imageModels.get(currentIndex));
    }

    public void showNextImage() {
        if (imageModels.size() == 0) return;
        if (++currentIndex >= imageModels.size()) {
            currentIndex = 0;
        }
        setSelectedImage(imageModels.get(currentIndex));
        unloadInvisibleImages();
    }

    public void showPreviousImage() {
        if (imageModels.size() == 0) return;
        if (--currentIndex < 0) {
            currentIndex = imageModels.size() - 1;
        }
        setSelectedImage(imageModels.get(currentIndex));
        unloadInvisibleImages();
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

                    Iterator<File> iterator = FileUtils.iterateFiles(new File(directoryPath), new String[]{"jpg", "png", "gif"}, false);

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

    private void setSelectedImage(ImageModel imageModel) {
        selectedImageModelWrapper.set(imageModel);
        currentIndex = imageModels.indexOf(imageModel);
        status.unbind();
        status.set("Image " + (currentIndex + 1) + " of " + imageModels.size() + ": " + imageModel.getShortName());
    }

    private void unloadInvisibleImages() {
        if (currentIndex > 0) {
            imageModels.get(currentIndex - 1).unsetImage();
        }
        if (currentIndex < imageModels.size() - 1) {
            imageModels.get(currentIndex + 1).unsetImage();
        }
    }
}