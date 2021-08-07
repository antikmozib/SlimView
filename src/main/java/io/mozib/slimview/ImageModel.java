/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.commons.io.FilenameUtils;

import java.awt.image.ColorModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;

public class ImageModel {

    private Image image = null;
    private ImageModel originalImageModel = null;
    private Boolean isFavorite = null;
    private final String path;
    private final String name;
    private final long dateModified;
    private final long dateCreated;

    ImageModel(String path) {
        this(path, null);
    }

    ImageModel(String location, String originalLocation) {
        BasicFileAttributes fileAttributes = null;
        Path path;

        this.path = location;
        this.name = Path.of(location).getFileName().toString();

        if (originalLocation != null) {
            this.originalImageModel = new ImageModel(originalLocation);
            path = Paths.get(originalLocation);
        } else {
            path = Paths.get(location);
        }

        try {
            fileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (fileAttributes != null) {
            this.dateModified = fileAttributes.lastModifiedTime().toMillis();
            this.dateCreated = fileAttributes.creationTime().toMillis();
        } else {
            this.dateModified = 0;
            this.dateCreated = 0;
        }
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public Image getImage() {
        if (image == null) {
            image = new Image(new File(getPath()).toURI().toString());
        }
        return image;
    }

    /**
     * Release the images and free up memory.
     */
    public void unsetImage() {
        image = null;

        if (hasOriginal()) {
            originalImageModel = null;
        }
    }

    public File getContainingFolder() {
        return new File(new File(getBestPath()).getParent());
    }

    public String getFormat() {
        return FilenameUtils.getExtension(getPath()).toUpperCase();
    }

    /**
     * @return Converts bytes to KB/MB
     */
    public String getFormattedFileSize() {
        long fileSize = new File(getPath()).length();
        DecimalFormat decimalFormat = new DecimalFormat("#.##");

        if (fileSize > 1e3 && fileSize < 1e6) return Math.round(fileSize / 1e3) + " KB";
        if (fileSize >= 1e6) return decimalFormat.format(fileSize / 1e6) + " MB";
        return fileSize + " B";

    }

    public double getWidth() {
        return getImage().getWidth();
    }

    public double getHeight() {
        return getImage().getHeight();
    }

    public long getDateModified() {
        return dateModified;
    }

    public long getDateCreated() {
        return dateCreated;
    }

    public String getColorDepth() {
        ColorModel colorModel = SwingFXUtils.fromFXImage(getImage(), null).getColorModel();
        return String.valueOf(colorModel.getPixelSize());
    }

    public boolean hasOriginal() {
        return originalImageModel != null;
    }

    public void setIsFavorite(boolean value) {
        this.isFavorite = value;
    }

    public boolean getIsFavorite() {
        return isFavorite;
    }

    public String getResolution() {
        return Math.round(getWidth()) + " x " + Math.round(getHeight()) + " px";
    }

    public double getAspectRatio() {
        return getWidth() / getHeight();
    }

    /**
     * @return If the image has an original, then returns the path to the original. Otherwise, returns the current path.
     */
    public String getBestPath() {
        return hasOriginal() ? originalImageModel.getPath() : getPath();
    }

    public ImageModel getOriginalImageModel() {
        return originalImageModel;
    }

    public void setOriginalImageModel(ImageModel originalImageModel) {
        this.originalImageModel = originalImageModel;
    }
}