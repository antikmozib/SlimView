package io.mozib.simview;

import javafx.beans.property.*;
import javafx.scene.image.Image;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static io.mozib.simview.SimView.*;

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

    public void OpenInEditor() {
        if (getOSType() == OSType.Windows) {
            try {
                Runtime.getRuntime().exec("mspaint \"" + getPath() + "\"");
            } catch (Exception ignored) {

            }
        }
    }

    public void OpenContainingFolder() {
        if (getOSType() == OSType.Windows) {
            try {
                Runtime.getRuntime().exec("explorer.exe /select, \"\"" + getPath() + "\"\"");
            } catch (Exception ignored) {

            }
        } else {
            try {
                Desktop.getDesktop().open(getContainingFolder());
            } catch (Exception ignored) {

            }
        }
    }

    public File getContainingFolder() {
        return new File(new File(getPath()).getParent());
    }
}
