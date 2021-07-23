/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

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
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static io.mozib.slimview.Util.*;

public class MainViewModel {

    private final FavoritesController favoritesController = new FavoritesController();
    private List<ImageModel> imageModels = new ArrayList<>();
    private LoadDirectory loadDirectory;
    private final ReadOnlyStringWrapper status = new ReadOnlyStringWrapper("Ready.");
    private final ReadOnlyObjectWrapper<ImageModel> selectedImageModelWrapper = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<SortStyle> selectedSortStyleWrapper = new ReadOnlyObjectWrapper<>();
    private final double zoomStep = 0.1; // how much to zoom on each step

    public MainViewModel() {
        selectedImageModelProperty().addListener(((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                // clear caches
                oldValue.unsetImage();
            }
        }));
    }

    public ReadOnlyObjectProperty<ImageModel> selectedImageModelProperty() {
        return selectedImageModelWrapper.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty statusProperty() {
        return status.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<SortStyle> selectedSortStyleProperty() {
        return selectedSortStyleWrapper.getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public void loadImage(ImageModel imageModel) {
        // first, show the image requested while the directory is being scanned
        setSelectedImage(imageModel);

        // now scan the rest of the directory
        loadDirectory = new LoadDirectory(new File(imageModel.getPath()).getParent());
        loadDirectory.setOnSucceeded((WorkerStateEvent event) -> {
            imageModels = (List<ImageModel>) event.getSource().getValue();
            sortImages(selectedSortStyleProperty().get());
            setSelectedImage(imageModels.stream().filter(image -> image.getPath().equals(imageModel.getPath()))
                    .findFirst().orElse(null));
        });
        status.bind(loadDirectory.messageProperty());
        loadDirectory.start();
        addToRecent(imageModel.getPath());
    }

    public void showFirstImage() {
        if (directoryScanComplete() && imageModels.size() > 0) {
            setSelectedImage(imageModels.get(0));
        }
    }

    public void showLastImage() {
        if (directoryScanComplete() && imageModels.size() > 0) {
            setSelectedImage(imageModels.get(imageModels.size() - 1));
        }
    }

    public void showNextImage() {
        if (directoryScanComplete() && imageModels.size() > 0) {
            if (getCurrentIndex() + 1 > imageModels.size() - 1) {
                setSelectedImage(imageModels.get(0));
            } else {
                setSelectedImage(imageModels.get(getCurrentIndex() + 1));
            }
        }
    }

    public void showPreviousImage() {
        if (directoryScanComplete() && imageModels.size() > 0) {
            if (getCurrentIndex() - 1 < 0) {
                setSelectedImage(imageModels.get(imageModels.size() - 1));
            } else {
                setSelectedImage(imageModels.get(getCurrentIndex() - 1));
            }
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

    public void zoomIn() {
        zoomImage(getSelectedImageModel(), zoomStep);
    }

    public void zoomOut() {
        zoomImage(getSelectedImageModel(), -zoomStep);
    }

    public void resetZoom() {
        if (getSelectedImageModel().getResamplePath() == null) {
            return;
        }
        setSelectedImage(
                imageModels.stream().filter(item -> item.getPath().equals(getSelectedImageModel().getResamplePath()))
                        .findFirst().orElse(null));
    }

    public void resizeImage(ImageModel imageModel, int newWidth, int newHeight) {
        var file = new File(Paths.get(tempDirectory(), imageModel.getShortName()).toString());
        BufferedImage image;

        // resample image to ensure best resizing quality
        if (!imageModel.hasOriginal()) {
            imageModel.setResamplePath(imageModel.getPath());
            image = SwingFXUtils.fromFXImage(imageModel.getImage(), null);
        } else {
            image = SwingFXUtils.fromFXImage(imageModel.getResampleImage(), null);
        }

        var resized = Scalr.resize(image, Scalr.Mode.FIT_EXACT, newWidth, newHeight);
        editImage(resized, file, imageModel.getResamplePath());
    }

    public void saveImage(ImageModel imageModel, String destination) {
        File file = new File(destination);
        try {
            file.createNewFile();
            ImageIO.write(SwingFXUtils.fromFXImage(imageModel.getImage(), null),
                    FilenameUtils.getExtension(destination), file);
        } catch (IOException ignored) {

        }
    }

    public void trashImage(ImageModel imageModel) {
        String path;

        if (imageModel.hasOriginal()) {
            path = imageModel.getResamplePath();
        } else {
            path = imageModel.getPath();
        }

        try {
            Desktop.getDesktop().moveToTrash(new File(path));

            // remove from list
            ImageModel remove;

            if (imageModel.hasOriginal()) {
                remove = findByPath(imageModel.getResamplePath());
            } else {
                remove = imageModel;
            }

            // item must be removed after showing next image to preserve index integrity
            showNextImage();
            imageModels.remove(remove);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openInEditor(ImageModel imageModel) {
        if (getOSType() == OSType.Windows) {
            try {
                Runtime.getRuntime().exec("mspaint \"" + imageModel.getBestPath() + "\"");
            } catch (IOException ignored) {

            }
        }
    }

    public void openContainingFolder(ImageModel imageModel) {
        if (getOSType() == OSType.Windows) {
            try {
                Runtime.getRuntime().exec("explorer.exe /select, \"\"" + imageModel.getBestPath() + "\"\"");
            } catch (IOException ignored) {

            }
        } else {
            try {
                Desktop.getDesktop().open(new File(imageModel.getBestPath()).getParentFile());
            } catch (IOException ignored) {

            }
        }
    }

    public void setAsFavorite(ImageModel imageModel, boolean value) {
        if (value && !favoritesController.exists(imageModel.getBestPath())) {
            favoritesController.add(imageModel.getBestPath());
        } else if (!value) {
            favoritesController.remove(imageModel.getBestPath());
        }
    }

    public void copyToClipboard(ImageModel imageModel) {
        if (imageModel.getImage() == null) {
            return;
        }
        var transformedImage = SwingFXUtils.fromFXImage(imageModel.getImage(), null);
        var transferableImage = new ImageTransferable(transformedImage);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferableImage, null);
    }

    public enum SortStyle {
        NAME, DATE_CREATED, DATE_MODIFIED
    }

    public void sortImages(SortStyle sortStyle) {
        if (imageModels.size() > 0) {
            switch (sortStyle) {
                case DATE_MODIFIED:
                    imageModels.sort((o1, o2) -> Long.compare(o2.getDateModified(), o1.getDateModified()));
                    break;
                case DATE_CREATED:
                    imageModels.sort((o1, o2) -> Long.compare(o2.getDateCreated(), o1.getDateCreated()));
                    break;
                case NAME:
                    imageModels.sort(Comparator.comparing(ImageModel::getShortName));
                    break;
                default:
                    break;
            }
            setSelectedImage(getSelectedImageModel()); // refresh status and index
        }
        selectedSortStyleWrapper.set(sortStyle);
    }

    public FavoritesController getFavoritesController() {
        return favoritesController;
    }

    private String formatTime(long time) {
        Date date = new Date(time);
        Format format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }

    private void editImage(BufferedImage edited, File file, String resamplePath) {
        String format = FilenameUtils.getExtension(file.getPath());
        try {
            file.createNewFile();
            ImageIO.write(edited, format, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        setSelectedImage(new ImageModel(file.getPath(), resamplePath));
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
                    Iterator<File> iterator = FileUtils.iterateFiles(new File(directoryPath),
                            new String[]{"jpg", "jpeg", "png", "gif"}, false);
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
        if (imageModel == null) {
            return;
        }

        imageModel.setIsFavorite(isFavorite(imageModel));

        selectedImageModelWrapper.set(imageModel);
        status.unbind();
        status.set((getCurrentIndex() + 1) + " / " + imageModels.size() + "  |  Resolution: "
                + imageModel.getResolution() + " @ " + imageModel.getColorDepth() + "-bits" + "  |  Format: "
                + imageModel.getFormat() + "  |  Size: " + imageModel.getFormattedFileSize() + "  |  Created: "
                + formatTime(imageModel.getDateCreated()) + "  |  Modified: "
                + formatTime(imageModel.getDateModified()));
    }

    private void zoomImage(ImageModel imageModel, double percentage) {
        double newWidth = imageModel.getWidth() + imageModel.getWidth() * percentage;
        double newHeight = imageModel.getHeight() + imageModel.getHeight() * percentage;
        resizeImage(imageModel, (int) newWidth, (int) newHeight);
    }

    private void rotateImage(ImageModel imageModel, Scalr.Rotation rotation) {
        var file = new File(Paths.get(tempDirectory(), imageModel.getShortName()).toString());
        var rotated = Scalr.rotate(SwingFXUtils.fromFXImage(imageModel.getImage(), null), rotation);
        editImage(rotated, file, imageModel.getBestPath());
    }

    /**
     * @return The index of the currently displayed image from the list of images
     */
    private int getCurrentIndex() {
        if (imageModels != null && getSelectedImageModel() != null) {

            return imageModels.indexOf(
                    imageModels.stream().filter(item -> getSelectedImageModel().getBestPath().equals(item.getPath()))
                            .findAny().orElse(null));
        }
        return 0;
    }

    private ImageModel findByPath(String path) {
        for (ImageModel imageModel : imageModels) {
            if (imageModel.getPath().equals(path)) {
                return imageModel;
            }
        }

        return null;
    }

    private boolean isFavorite(ImageModel imageModel) {

        return favoritesController.exists(imageModel.getBestPath());
    }
}
