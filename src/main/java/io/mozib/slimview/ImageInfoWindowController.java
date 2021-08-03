/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class ImageInfoWindowController {

    @FXML
    public TextArea textInfo;
    @FXML
    public TextField textPath;

    public void loadInfo(ImageModel imageModel) {
        StringBuilder info = new StringBuilder();
        String path = imageModel.getBestPath();

        textPath.setText(path);
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new File(path));
            for (Directory directory : metadata.getDirectories()) {
                directory.getTags().forEach(tag -> info.append(tag).append("\n"));
            }
            textInfo.setText(info.toString());
            textInfo.requestFocus();
        } catch (ImageProcessingException | IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void buttonOK_onAction(ActionEvent actionEvent) {
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
    }
}
