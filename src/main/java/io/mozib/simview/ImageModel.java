package io.mozib.simview;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;
import java.io.File;
import java.nio.file.Path;

public class ImageModel {
    private final StringProperty fullPath = new SimpleStringProperty();
    private final StringProperty shortName = new SimpleStringProperty();

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
    	return new Image(new File(getPath()).toURI().toString());
    }
}
