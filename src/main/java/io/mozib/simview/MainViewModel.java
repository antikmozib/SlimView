package io.mozib.simview;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.mozib.simview.Common.*;

public class MainViewModel {

    private List<ImageModel> imageModels = new ArrayList<>();
    private Integer currentIndex = 0;
    private LoadDirectory loadDirectory;
    private final ReadOnlyStringWrapper status = new ReadOnlyStringWrapper();
    private final ReadOnlyObjectWrapper<ImageModel> selectedImageModelWrapper = new ReadOnlyObjectWrapper<>();

    public ReadOnlyObjectProperty<ImageModel> selectedImageModelProperty() {
        return selectedImageModelWrapper.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty statusProperty() {
        return status.getReadOnlyProperty();
    }

    private static class LoadDirectory extends Service<List<ImageModel>> {

        private final String directoryPath;
        private final AtomicInteger fileCount = new AtomicInteger(0);
        private final List<ImageModel> images = new ArrayList<>();

        public LoadDirectory(String directoryPath) {
            this.directoryPath = directoryPath;
        }

        public Integer getFileCount() {
            return fileCount.get();
        }

        @Override
        protected Task<List<ImageModel>> createTask() {

            return new Task<>() {

                @Override
                protected List<ImageModel> call() {

                    Iterator<File> iterator = FileUtils.iterateFiles(new File(directoryPath), new String[]{"jpg", "jpeg", "png", "gif"}, false);

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
        status.set((currentIndex + 1) + " / " + imageModels.size() + " | Resolution: "
                + imageModel.getResolution() + " | Format: " + imageModel.getFormat()
                + " | Size: " + imageModel.getFormattedFileSize());
        //editedImage = null;
    }

    private void unloadInvisibleImages() {
        if (currentIndex > 0) {
            imageModels.get(currentIndex - 1).unsetImage();
        }
        if (currentIndex < imageModels.size() - 1) {
            imageModels.get(currentIndex + 1).unsetImage();
        }
    }

    @SuppressWarnings("unchecked")
    public void loadImage(ImageModel imageModel) {
        // first, show the image requested while the directory is being scanned
        setSelectedImage(imageModel);

        // now scan the rest of the directory
        loadDirectory = new LoadDirectory(new File(imageModel.getPath()).getParent());
        loadDirectory.setOnSucceeded((WorkerStateEvent event) -> {
            imageModels = (List<ImageModel>) event.getSource().getValue();
            setSelectedImage(imageModels.stream().filter(image -> image.getPath().equals(imageModel.getPath())).findFirst().orElseThrow());
        });
        status.bind(loadDirectory.messageProperty());
        loadDirectory.start();
        addToRecent(imageModel.getPath());
    }

    public void showFirstImage() {
        if (directoryScanComplete() && imageModels.size() > 0) {
            currentIndex = 0;
            setSelectedImage(imageModels.get(currentIndex));
        }
    }

    public void showLastImage() {
        if (directoryScanComplete() && imageModels.size() > 0) {
            currentIndex = imageModels.size() - 1;
            setSelectedImage(imageModels.get(currentIndex));
        }
    }

    public void showNextImage() {
        if (directoryScanComplete() && imageModels.size() > 0) {
            if (++currentIndex >= imageModels.size()) {
                currentIndex = 0;
            }
            setSelectedImage(imageModels.get(currentIndex));
            unloadInvisibleImages();
        }
    }

    public void showPreviousImage() {
        if (directoryScanComplete() && imageModels.size() > 0) {
            if (--currentIndex < 0) {
                currentIndex = imageModels.size() - 1;
            }
            setSelectedImage(imageModels.get(currentIndex));
            unloadInvisibleImages();
        }
    }

    public ImageModel getSelectedImageModel() {
        return selectedImageModelWrapper.get();
    }

    public boolean directoryScanComplete() {
        return loadDirectory == null || !loadDirectory.isRunning();
    }

    public void rotateLeft() {
        rotateImage(getSelectedImageModel(), Scalr.Rotation.CW_270);
    }

    public void rotateRight() {
        rotateImage(getSelectedImageModel(), Scalr.Rotation.CW_90);
    }

    public void flipVertically() {
        rotateImage(getSelectedImageModel(), Scalr.Rotation.FLIP_VERT);
    }

    public void flipHorizontally() {
        rotateImage(getSelectedImageModel(), Scalr.Rotation.FLIP_HORZ);
    }

    private void rotateImage(ImageModel imageModel, Scalr.Rotation rotation) {
        var file = new File(Paths.get(cacheDirectory(), imageModel.getShortName()).toString());
        var rotated = Scalr.rotate(SwingFXUtils.fromFXImage(imageModel.getImage(), null), rotation);
        editImage(rotated, file);
    }

    public void resizeImage(ImageModel imageModel, int newWidth, int newHeight) {
        var file = new File(Paths.get(cacheDirectory(), imageModel.getShortName()).toString());
        var image = SwingFXUtils.fromFXImage(imageModel.getImage(), null);
        var resized = Scalr.resize(image, Scalr.Mode.FIT_EXACT, newWidth, newHeight);
        editImage(resized, file);
    }

    private void editImage(BufferedImage edited, File file) {
        String format = FilenameUtils.getExtension(file.getPath());
        try {
            file.createNewFile();
            ImageIO.write(edited, format, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        setSelectedImage(new ImageModel(file.getPath()));
    }

    public void saveImage(ImageModel imageModel, String destination) {
        File file = new File(destination);
        try {
            file.createNewFile();
            ImageIO.write(
                    SwingFXUtils.fromFXImage(imageModel.getImage(), null),
                    FilenameUtils.getExtension(destination),
                    file
            );
        } catch (IOException e) {

        }
    }

    public void trashImage(ImageModel imageModel) {
        try {
            Desktop.getDesktop().moveToTrash(new File(imageModel.getPath()));
            // remove from list
            imageModels.remove(imageModel);
            showNextImage();
        } catch (Exception e) {

        }
    }
}
