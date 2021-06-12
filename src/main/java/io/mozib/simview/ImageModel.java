package io.mozib.simview;

import javafx.scene.image.Image;
import org.apache.commons.io.FilenameUtils;

import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.text.DecimalFormat;

import static io.mozib.simview.SimView.OSType;
import static io.mozib.simview.SimView.getOSType;

public class ImageModel {

    private final String fullPath;
    private final String shortName;
    private Image image = null;

    ImageModel(String fullPath) {
        this.fullPath = fullPath;
        this.shortName = Path.of(fullPath).getFileName().toString();
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

    public void openInEditor() {
        if (getOSType() == OSType.Windows) {
            try {
                Runtime.getRuntime().exec("mspaint \"" + getPath() + "\"");
            } catch (Exception ignored) {

            }
        }
    }

    public void openContainingFolder() {
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

    public String getResolution() {
        return (int) getImage().getWidth() + " x " + (int) getImage().getHeight() + " px";
    }

    public String getFormat() {
        return FilenameUtils.getExtension(getPath()).toUpperCase();
    }

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

    public double getOriginalWidth() {
        return getImage().getWidth();
    }

    public double getOriginalHeight() {
        return getImage().getHeight();
    }
}
