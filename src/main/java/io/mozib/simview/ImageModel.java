package io.mozib.simview;

import javafx.beans.property.*;
import javafx.scene.image.Image;

import java.io.File;
import java.nio.file.Path;

public class ImageModel {
    private final StringProperty fullPath = new SimpleStringProperty();
    private final StringProperty shortName = new SimpleStringProperty();
    private Image image = null;

    ImageModel(String fullPath) {
        this.fullPath.set(fullPath);
        this.shortName.set(Path.of(fullPath).getFileName().toString());
    }

    public String getPath() {
        return fullPath.get();
    }

    public String getShortName() {
        return shortName.get();
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
}
