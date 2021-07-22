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

    private final String fullPath;
    private final String shortName;
    private Image image = null;
    private final long dateModified;
    private final long dateCreated;
    private String resamplePath; // resamplePath is the path to the original, unedited file
    private boolean isFavorite;

    ImageModel(String fullPath) {
        this(fullPath, null);
    }

    ImageModel(String fullPath, String resamplePath) {
        this.fullPath = fullPath;
        this.resamplePath = resamplePath;
        this.shortName = Path.of(fullPath).getFileName().toString();
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
        return fullPath;
    }

    public String getShortName() {
        return shortName;
    }

    public Image getImage() {
        if (image == null) {
            image = new Image(new File(getPath()).toURI().toString());
        }
        return image;
    }

    public void unsetImage() {
        image = null;
    }

    public File getContainingFolder() {
        return new File(new File(getPath()).getParent());
    }

    public String getResolution() {
        return (int) getImage().getWidth() + " x " + (int) getImage().getHeight() + " px";
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

    public long getDateModified() {
        return dateModified;
    }

    public long getDateCreated() {
        return dateCreated;
    }

    public void setResamplePath(String resamplePath) {
        this.resamplePath = resamplePath;
    }

    /**
     * @return The path to the original, unedited file.
     */
    public String getResamplePath() {
        return resamplePath;
    }

    public Image getResampleImage() {
        if (getResamplePath() == null || getResamplePath().equals("")) {
            return null;
        }

        return new Image(new File(getResamplePath()).toURI().toString());
    }

    public String getColorDepth() {
        ColorModel colorModel = SwingFXUtils.fromFXImage(getImage(), null).getColorModel();
        return String.valueOf(colorModel.getPixelSize());
    }

    public boolean hasOriginal() {
        return getResamplePath() != null &&
                !getResamplePath().equals("") &&
                !getResamplePath().equalsIgnoreCase(getPath());
    }

    public void setIsFavorite(boolean value) {
        this.isFavorite = value;
    }

    public boolean getIsFavorite() {
        return isFavorite;
    }

    public String getBestPath() {
        if (hasOriginal()) {
            return getResamplePath();
        }

        return getPath();
    }
}
