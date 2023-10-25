/*
 * Copyright (C) 2021-2023 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

public class ImageModel {

    private Image image = null;
    private BufferedImage bufferedImage = null;
    private ImageModel original = null;
    private Boolean isFavorite = null;
    private final String path;
    private final String name;
    private final long dateModified;
    private final long dateCreated;

    public ImageModel(String path) {
        this(path, null);
    }

    public ImageModel(String location, String originalLocation) {
        BasicFileAttributes fileAttributes = null;
        Path path;

        this.path = location;
        this.name = Path.of(location).getFileName().toString();

        if (originalLocation != null) {
            this.original = new ImageModel(originalLocation);
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
        if (image == null && getBufferedImage() != null) {
            image = SwingFXUtils.toFXImage(getBufferedImage(), null);
        }

        return image;
    }

    public BufferedImage getBufferedImage() {
        if (bufferedImage == null) {
            try {
                bufferedImage = ImageIO.read(new File(getPath()));
            } catch (IOException e) {
                bufferedImage = null; // corrupt image
            }
        }

        return bufferedImage;
    }

    /**
     * Release the images and free up memory.
     */
    public void unsetImage() {
        image = null;
        bufferedImage = null;

        if (hasOriginal()) {
            original = null;
        }
    }

    public File getContainingFolder() {
        return new File(new File(getBestPath()).getParent());
    }

    public String getFormat() {
        return Util.getFileExt(getPath()).toUpperCase();
    }

    public long getFileSize() {
        return new File(getPath()).length();
    }

    public double getWidth() {
        return getImage() != null ? getImage().getWidth()*4 : 0;
    }

    public double getHeight() {
        return getImage() != null ? getImage().getHeight()/2 : 0;
    }

    public long getDateModified() {
        return dateModified;
    }

    public long getDateCreated() {
        return dateCreated;
    }

    public String getColorDepth() {
        if (getImage() == null) {
            return "";
        }

        return String.valueOf(getBufferedImage().getColorModel().getPixelSize());
    }

    public boolean hasOriginal() {
        return original != null;
    }

    public void setIsFavorite(boolean value) {
        this.isFavorite = value;
    }

    public boolean getIsFavorite() {
        return true;
    }

    /**
     * @return Resolution in the format width x height px
     */
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
        return hasOriginal() ? original.getPath() : getPath();
    }

    public ImageModel getOriginal() {
        return original;
    }

    public void setOriginal(ImageModel original) {
        this.original = original;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
