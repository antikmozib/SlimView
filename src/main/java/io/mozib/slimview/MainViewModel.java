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
import java.nio.charset.Charset;
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
    public void loadImage(ImageModel imageModel) throws IOException {
        File file = new File(imageModel.getPath());
        if (!file.exists()) throw new IOException();

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
                showFirstImage();
            } else {
                setSelectedImage(imageModels.get(getCurrentIndex() + 1));
            }
        }
    }

    public void showPreviousImage() {
        if (directoryScanComplete() && imageModels.size() > 0) {
            if (getCurrentIndex() - 1 < 0) {
                showLastImage();
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

    public void resetZoom() {
        if (!getSelectedImageModel().hasOriginal()) {
            return;
        }
        setSelectedImage(
                imageModels.stream()
                        .filter(item -> item.getPath().equals(getSelectedImageModel().getOriginalImageModel().getPath()))
                        .findFirst()
                        .orElse(null));
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
        String path = imageModel.getPath();
        boolean success = false;

        switch (getOSType()) {
            case WINDOWS:
            case MAC:
                if (Desktop.isDesktopSupported()) {
                    if (Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
                        try {
                            Desktop.getDesktop().moveToTrash(new File(path));
                            success = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case LINUX:
                // in linux we've to manually trash the file by creating a .trashinfo file first

                String trashInfo =
                        "[Trash Info]\n" +
                                "Path=" + path + "\n" +
                                "DeletionDate=" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());

                String pathToTrash =
                        Paths.get(System.getProperty("user.home"), ".local", "share", "Trash").toString();

                try {
                    // write .trashinfo file
                    FileUtils.writeStringToFile(new File(Paths.get(
                                    pathToTrash, "info", FilenameUtils.getName(path) + ".trashinfo").toString()),
                            trashInfo, Charset.defaultCharset());

                    // move file to actual trash folder
                    FileUtils.moveFile(new File(path),
                            new File(Paths.get(pathToTrash, "files", FilenameUtils.getName(path)).toString()));

                    success = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }

        if (success) {
            // remove from list
            ImageModel remove;

            if (imageModel.hasOriginal()) {
                remove = findByPath(imageModel.getOriginalImageModel().getPath());
            } else {
                remove = imageModel;
            }

            // item must be removed after showing next image to preserve index integrity
            showNextImage();
            imageModels.remove(remove);
        }
    }

    public void openInEditor(ImageModel imageModel) {
        if (getOSType() == OSType.WINDOWS) {
            try {
                Runtime.getRuntime().exec("mspaint \"" + imageModel.getBestPath() + "\"");
            } catch (IOException ignored) {

            }
        } else {
            Util.browseUrl(imageModel.getBestPath());
        }
    }

    public void openContainingFolder(ImageModel imageModel) {
        if (getOSType() == OSType.WINDOWS) {
            try {
                Runtime.getRuntime().exec("explorer.exe /select, \"\"" + imageModel.getBestPath() + "\"\"");
            } catch (IOException ignored) {

            }
        } else {
            Util.browseUrl(imageModel.getContainingFolder().getPath());
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
                    imageModels.sort(Comparator.comparing(ImageModel::getName));
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
                    String[] extensions = new String[]{"jpg", "jpeg", "png", "gif"};
                    File dir = new File(directoryPath);
                    File[] files = dir.listFiles();

                    if (files != null) {

                        for (File file : files) {
                            if (file.isDirectory()) continue;

                            String ext = FilenameUtils.getExtension(file.getName());
                            if (Arrays.stream(extensions).anyMatch(s -> s.equalsIgnoreCase(ext))) {

                                ImageModel image = new ImageModel(file.getPath());
                                images.add(image);
                                fileCount.addAndGet(1);
                                updateMessage("Scanning " + directoryPath + "... " + image.getName());
                            }
                        }
                    }
                    updateMessage("Found " + getFileCount() + " files.");
                    return images;
                }
            };
        }
    }

    private void setSelectedImage(ImageModel imageModel) {
        status.unbind();

        if (imageModel == null) {
            selectedImageModelWrapper.set(null);
            status.set("Ready.");
        } else {
            imageModel.setIsFavorite(isFavorite(imageModel));

            selectedImageModelWrapper.set(imageModel);
            status.set((getCurrentIndex() + 1) + "/" + imageModels.size()
                    + "  |  " + imageModel.getFormat()
                    + "  |  " + imageModel.getColorDepth() + "-bits"
                    + "  |  " + imageModel.getFormattedFileSize()
                    + "  |  Created: " + formatTime(imageModel.getDateCreated())
                    + "  |  Modified: " + formatTime(imageModel.getDateModified()));
        }
    }

    public void resizeImage(ImageModel imageModel, int newWidth, int newHeight) {
        var file = new File(Paths.get(tempDirectory(), imageModel.getName()).toString());
        BufferedImage image;

        // resample image to ensure best resizing quality
        if (!imageModel.hasOriginal()) {
            imageModel.setOriginalImageModel(new ImageModel(imageModel.getPath()));
            image = SwingFXUtils.fromFXImage(imageModel.getImage(), null);
        } else {
            image = SwingFXUtils.fromFXImage(imageModel.getOriginalImageModel().getImage(), null);
        }

        var resized = Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_EXACT, newWidth, newHeight);
        createTempImage(resized, file, imageModel.getOriginalImageModel().getPath());
    }

    private void rotateImage(ImageModel imageModel, Scalr.Rotation rotation) {
        var file = new File(Paths.get(tempDirectory(), imageModel.getName()).toString());
        var rotated = Scalr.rotate(SwingFXUtils.fromFXImage(imageModel.getImage(), null), rotation);
        createTempImage(rotated, file, imageModel.getBestPath());
    }

    /**
     * Creates a temporary, edited image and sets it as the currently displayed one
     */
    private void createTempImage(BufferedImage image, File file, String originalPath) {
        String format = FilenameUtils.getExtension(file.getPath());
        try {
            file.createNewFile();
            ImageIO.write(image, format, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        setSelectedImage(new ImageModel(file.getPath(), originalPath));
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
