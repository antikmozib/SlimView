package io.mozib.slimview;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class ImageInfoWindowController {

    @FXML
    public TextArea textInfo;

    public void loadInfo(ImageModel imageModel) {
        StringBuilder info = new StringBuilder();
        String path;

        if (imageModel.hasOriginal()) {
            path = imageModel.getResamplePath();
        } else {
            path = imageModel.getPath();
        }

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new File(path));
            for (Directory directory : metadata.getDirectories()) {
                directory.getTags().forEach(tag -> info.append(tag).append("\n"));
            }
            textInfo.setText(info.toString());
        } catch (ImageProcessingException | IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void buttonOK_onAction(ActionEvent actionEvent) {
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
    }
}
