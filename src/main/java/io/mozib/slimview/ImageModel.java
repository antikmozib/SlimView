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

    private String path;
    private final String name;
    private Image image = null;
    private Image originalImage = null;
    private final long dateModified;
    private final long dateCreated;
    private String originalPath;
    private boolean isFavorite;

    ImageModel(String path) {
        this(path, null);
    }

    ImageModel(String fullPath, String originalPath) {
        this.path = fullPath;
        this.originalPath = originalPath;
        this.name = Path.of(fullPath).getFileName().toString();
        Path path = Paths.get(fullPath);
        BasicFileAttributes fileAttributes = null;
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
            path = originalPath;
            originalPath = null;
            originalImage = null;
        }
    }

    public File getContainingFolder() {
        return new File(new File(getBestPath()).getParent());
    }

    public String getOriginalResolution() {
        return (int) getOriginalWidth() + " x " + (int) getOriginalHeight() + " px";
    }

    public String getFormat() {
        return FilenameUtils.getExtension(getPath()).toUpperCase();
    }

    /**
     * @return Converts bytes to context-sensitive KB/MB
     */
    public String getFormattedFileSize() {
        long fileSize = new File(getPath()).length();
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        if (fileSize > 1000 && fileSize < 1000000) {
            return fileSize / 1000 + " KB";
        } else if (fileSize >= 1000000) {
            return decimalFormat.format(fileSize / 1000000.0) + " MB";
        } else {
            return fileSize + " B";
        }
    }

    public double getWidth() {
        return getImage().getWidth();
    }

    public double getHeight() {
        return getImage().getHeight();
    }

    public double getOriginalWidth() {
        if (getOriginalImage() == null) {
            return getWidth();
        } else {
            return getOriginalImage().getWidth();
        }
    }

    public double getOriginalHeight() {
        if (getOriginalImage() == null) {
            return getHeight();
        } else {
            return getOriginalImage().getHeight();
        }
    }

    public long getDateModified() {
        return dateModified;
    }

    public long getDateCreated() {
        return dateCreated;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    /**
     * @return The path to the original, unedited file.
     */
    public String getOriginalPath() {
        return originalPath;
    }

    public Image getOriginalImage() {
        if (getOriginalPath() == null || getOriginalPath().equals("")) {
            return null;
        }

        if (originalImage == null) {
            originalImage = new Image(new File(getOriginalPath()).toURI().toString());
        }

        return originalImage;
    }

    public String getColorDepth() {
        ColorModel colorModel = SwingFXUtils.fromFXImage(getImage(), null).getColorModel();
        return String.valueOf(colorModel.getPixelSize());
    }

    public boolean hasOriginal() {
        return getOriginalPath() != null &&
                !getOriginalPath().equals("") &&
                !getOriginalPath().equalsIgnoreCase(getPath());
    }

    public void setIsFavorite(boolean value) {
        this.isFavorite = value;
    }

    public boolean getIsFavorite() {
        return isFavorite;
    }

    public String getBestPath() {
        if (hasOriginal()) {
            return getOriginalPath();
        }

        return getPath();
    }

    public double getOriginalAspectRatio() {
        if (hasOriginal()) {
            return (getOriginalWidth() / getOriginalHeight());
        } else {
            return (getWidth() / getHeight());
        }
    }
}
