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
import org.apache.commons.io.FileUtils;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static io.mozib.slimview.Util.*;

public class MainViewModel {

    private final FavoritesController favoritesController = new FavoritesController();
    private List<ImageModel> imageModels = new ArrayList<>();
    private LoadDirectory loadDirectory = null;
    private final ReadOnlyStringWrapper status = new ReadOnlyStringWrapper("Ready.");
    private final ReadOnlyObjectWrapper<ImageModel> selectedImageModelWrapper = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<SortStyle> selectedSortStyleWrapper = new ReadOnlyObjectWrapper<>();
    private final String[] supportedReadExtensions
            = new String[]{"bmp", "png", "gif", "jpeg", "jpg", "tiff", "ico", "cur", "psd", "psb" /*, "svg", "wmf"*/};
    private final String[] supportedWriteExtensions
            = new String[]{"bmp", "png", "gif", "jpeg", "jpg", "tiff", "ico"};

    public MainViewModel() {
        selectedImageModelProperty().addListener(((observable, oldValue, newValue) -> {
            if (oldValue != null && !newValue.hasOriginal()) {
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
    public void loadImage(String path) throws IOException {
        File file = new File(path);
        ImageModel imageModel = new ImageModel(path);

        // ensure the file exists and is a valid image file
        if (!file.exists() || !Files.isReadable(file.toPath()) || imageModel.getImage() == null) {
            throw new IOException("The requested file either doesn't exist or isn't a valid image file");
        }

        // first, show the image requested while the directory is being scanned
        setSelectedImage(imageModel);

        // now scan the rest of the directory
        loadDirectory = new LoadDirectory(new File(path).getParent(), supportedReadExtensions);
        loadDirectory.setOnSucceeded((WorkerStateEvent event) -> {

            imageModels = (List<ImageModel>) event.getSource().getValue();
            sortImages(selectedSortStyleProperty().get());

            // swap out the new item in the list with the one being displayed
            var newItem = imageModels.stream().filter(image -> image.getPath().equals(path))
                    .findFirst().orElse(null);
            imageModels.add(imageModels.indexOf(newItem), imageModel);
            imageModels.remove(newItem);
        });
        status.bind(loadDirectory.messageProperty());
        loadDirectory.start();
        addToRecent(path);
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

    public void saveImage(ImageModel imageModel, String destination) throws IOException {
        File file = new File(destination);
        file.createNewFile();
        ImageIO.write(imageModel.getBufferedImage(), Util.getFileExt(destination), file);
    }

    public void trashImage(ImageModel imageModel) throws Exception {
        String path = imageModel.getPath();
        boolean success = false;

        switch (getOSType()) {
            case WINDOWS:
            case MAC:
            default:
                if (Desktop.isDesktopSupported()) {
                    if (Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
                        Desktop.getDesktop().moveToTrash(new File(path));
                        success = true;
                    }
                }
                break;
            case LINUX:
                // in linux we've to manually trash the file by creating a .trashinfo file first

                String trashInfo = "[Trash Info]\n"
                        + "Path=" + path + "\n"
                        + "DeletionDate=" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());

                String pathToTrash
                        = Paths.get(System.getProperty("user.home"), ".local", "share", "Trash").toString();

                // write .trashinfo file
                Util.writeStringToFile(
                        Paths.get(pathToTrash, "info", Util.getFileName(path) + ".trashinfo").toString(),
                        trashInfo);

                // move file to actual trash folder
                FileUtils.moveFile(new File(path),
                        new File(Paths.get(pathToTrash, "files", Util.getFileName(path)).toString()));

                success = true;
                break;
        }

        if (success) {
            // remove from list
            ImageModel remove;

            if (imageModel.hasOriginal()) {
                remove = findByPath(imageModel.getOriginal().getPath());
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

    public void copyToClipboard(BufferedImage bufferedImage) {
        var transferableImage = new ImageTransferable(bufferedImage);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferableImage, null);
    }

    public enum SortStyle {
        NAME, DATE_CREATED, DATE_MODIFIED
    }

    public void sortImages(SortStyle sortStyle) {
        if (sortStyle == null) {
            return;
        }

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
            }
            setSelectedImage(getSelectedImageModel()); // refresh status and index
        }
        selectedSortStyleWrapper.set(sortStyle);
    }

    public FavoritesController getFavoritesController() {
        return favoritesController;
    }

    public String[] getSupportedReadExtensions() {
        return supportedReadExtensions;
    }

    public String[] getSupportedWriteExtensions() {
        return supportedWriteExtensions;
    }

    private String formatTime(long time) {
        Date date = new Date(time);
        Format format = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        return format.format(date);
    }

    private String formatFileSize(long bytes) {
        long fileSize = bytes;
        DecimalFormat decimalFormat = new DecimalFormat("#.##");

        if (fileSize > 1e3 && fileSize < 1e6) {
            return Math.round(fileSize / 1e3) + " KB";
        }
        if (fileSize >= 1e6) {
            return decimalFormat.format(fileSize / 1e6) + " MB";
        }
        return fileSize + " B";
    }

    /**
     * Loads a directory in the background
     */
    private static class LoadDirectory extends Service<List<ImageModel>> {

        private final String directoryPath;
        private final String[] supportedExtensions;
        private final AtomicInteger fileCount = new AtomicInteger(0);
        private final List<ImageModel> images = new ArrayList<>();

        public LoadDirectory(String directoryPath, String[] supportedExtensions) {
            this.directoryPath = directoryPath;
            this.supportedExtensions = supportedExtensions;
        }

        public Integer getFileCount() {
            return fileCount.get();
        }

        @Override
        protected Task<List<ImageModel>> createTask() {
            return new Task<>() {
                @Override
                protected List<ImageModel> call() {
                    File dir = new File(directoryPath);
                    File[] files = dir.listFiles();

                    if (files != null) {

                        for (File file : files) {
                            if (file.isDirectory()) {
                                continue;
                            }

                            String ext = Util.getFileExt(file.getName());
                            if (Arrays.stream(supportedExtensions).anyMatch(s -> s.equalsIgnoreCase(ext))) {

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

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getCurrentIndex() + 1).append("/").append(getFileCount());
            if (imageModel.getImage() != null) {
                stringBuilder
                        .append("  |  ").append(imageModel.getFormat())
                        .append("  |  ").append(getMegapixelCount(imageModel))
                        .append("  |  ").append(imageModel.getColorDepth()).append("-bits")
                        .append("  |  ").append(formatFileSize(imageModel.getFileSize()))
                        .append(imageModel.hasOriginal()
                                ? " (" + formatFileSize(imageModel.getOriginal().getFileSize()) + ")"
                                : "")
                        .append("  |  Created: ").append(formatTime(imageModel.getDateCreated()))
                        .append("  |  Modified: ").append(formatTime(imageModel.getDateModified()));
            } else {
                stringBuilder.append("  |  Error");
            }
            status.set(stringBuilder.toString());
        }
    }

    public void resizeImage(ImageModel imageModel, int newWidth, int newHeight, Scalr.Method method) {

        // resample image to ensure best resizing quality
        BufferedImage image;

        if (!imageModel.hasOriginal()) {
            imageModel.setOriginal(new ImageModel(imageModel.getPath()));
            image = imageModel.getBufferedImage();
        } else {
            image = imageModel.getOriginal().getBufferedImage();
        }

        var resized = Scalr.resize(image, method, Scalr.Mode.FIT_EXACT, newWidth, newHeight);
        var file = new File(Paths.get(getDataFile(DataFileLocation.CACHE_DIR), imageModel.getName()).toString());
        setSelectedImage(createTempImage(resized, file, imageModel.getBestPath()));
    }

    private void rotateImage(ImageModel imageModel, Scalr.Rotation rotation) {
        var rotated = Scalr.rotate(imageModel.getBufferedImage(), rotation);
        var file = new File(Paths.get(getDataFile(DataFileLocation.CACHE_DIR), imageModel.getName()).toString());
        setSelectedImage(createTempImage(rotated, file, imageModel.getBestPath()));
    }

    /**
     * Creates a temporary, edited image
     */
    private ImageModel createTempImage(BufferedImage image, File tempFile, String originalPath) {
        String suppliedFormat = Util.getFileExt(tempFile.getPath());
        String targetFormat;

        // if we're working with one of the read-only image types (e.g. psd), convert and save it as a bmp instead
        if (Arrays.stream(getSupportedWriteExtensions()).noneMatch(s -> s.equalsIgnoreCase(suppliedFormat))) {
            targetFormat = "bmp";
            tempFile = new File(tempFile.getPath().replaceFirst("\\.[^.]+$", "." + targetFormat));
        } else {
            targetFormat = suppliedFormat;
        }

        try {
            tempFile.createNewFile();
            ImageIO.write(image, targetFormat, tempFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ImageModel(tempFile.getPath(), originalPath);
    }

    /**
     * @param imageModel  The image to crop
     * @param x           Starting x position
     * @param y           Starting y position
     * @param width       Width of the crop
     * @param height      Height of the crop
     * @param scaleFactor The width of the original image as a ratio of the displayed width
     * @return A cropped BufferedImage
     */
    public BufferedImage cropImage(ImageModel imageModel,
                                   double x, double y,
                                   double width, double height,
                                   double scaleFactor) {

        double targetX = x * (1 / scaleFactor);
        double targetY = y * (1 / scaleFactor);
        double targetWidth = width * (1 / scaleFactor);
        double targetHeight = height * (1 / scaleFactor);

        return Scalr.crop(
                imageModel.getBufferedImage(),
                (int) Math.round(targetX),
                (int) Math.round(targetY),
                (int) Math.round(targetWidth),
                (int) Math.round(targetHeight));
    }

    /**
     * @return The index of the currently displayed image from the list of images
     */
    private int getCurrentIndex() {
        if (getSelectedImageModel() != null) {
            return getIndex(getSelectedImageModel());
        }

        return 0;
    }

    public int getIndex(ImageModel imageModel) {
        if (imageModel != null && imageModels != null) {
            return imageModels.indexOf(
                    imageModels.stream().filter(item -> imageModel.getBestPath().equals(item.getBestPath()))
                            .findAny().orElse(null));
        }
        return 0;
    }

    public int getFileCount() {
        if (imageModels != null) {
            return imageModels.size();
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

    private String getMegapixelCount(ImageModel imageModel) {
        double megapixel
                = (imageModel.hasOriginal() ? imageModel.getOriginal().getWidth() : imageModel.getWidth())
                * (imageModel.hasOriginal() ? imageModel.getOriginal().getHeight() : imageModel.getHeight())
                / 1e6;

        return new DecimalFormat("#.##").format(megapixel) + "MP";
    }
}
