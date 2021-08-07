/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ImageInfoWindowController implements Initializable {

    @FXML
    public TextField textPath;
    @FXML
    public TabPane tabPaneMain;

    private StringBuilder imageInfo; // used to copy info to clipboard
    private final Clipboard clipboard = Clipboard.getSystemClipboard();
    private final ClipboardContent clipboardContent = new ClipboardContent();

    public void loadInfo(ImageModel imageModel) {
        String path = imageModel.getBestPath();
        imageInfo = new StringBuilder();
        textPath.setText(path);
        tabPaneMain.getTabs().clear();
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new File(path));
            for (Directory directory : metadata.getDirectories()) {
                if (directory.getTags().size() < 1) continue;

                imageInfo.append(directory.getName().toUpperCase()).append("\n");

                // Structure: Tab > AnchorPane > ScrollPane > GridPane > [Label + TextField]
                Tab tab = new Tab(directory.getName());
                AnchorPane anchorPane = new AnchorPane();
                ScrollPane scrollPane = new ScrollPane();
                GridPane gridPane = new GridPane();

                AnchorPane.setBottomAnchor(scrollPane, 0.0);
                AnchorPane.setLeftAnchor(scrollPane, 0.0);
                AnchorPane.setRightAnchor(scrollPane, 0.0);
                AnchorPane.setTopAnchor(scrollPane, 0.0);

                int i = 0;
                
                for (Tag tag : directory.getTags()) {
                    imageInfo.append(tag.getTagName()).append(": ").append(tag.getDescription()).append("\n");

                    Label label = new Label(tag.getTagName() + ":");
                    TextField textField = new TextField(tag.getDescription());
                    textField.setEditable(false);
                    textField.setMinWidth(100);
                    label.setMinWidth(50);

                    gridPane.add(label, 0, i);
                    gridPane.add(textField, 1, i++);
                }

                imageInfo.append("\n");

                scrollPane.setFitToWidth(true);
                scrollPane.setPadding(new Insets(8, 8, 8, 8));
                ColumnConstraints columnConstraints = new ColumnConstraints();
                columnConstraints.setHgrow(Priority.ALWAYS);
                columnConstraints.setFillWidth(true);
                gridPane.getColumnConstraints().addAll(new ColumnConstraints(), columnConstraints);
                gridPane.setHgap(4);
                gridPane.setVgap(4);

                scrollPane.setContent(gridPane);
                anchorPane.getChildren().add(scrollPane);
                tab.setContent(anchorPane);
                tabPaneMain.getTabs().add(tab);
            }
        } catch (ImageProcessingException | IOException e) {
            e.printStackTrace();
        }
        tabPaneMain.requestFocus();
    }

    @FXML
    public void buttonOK_onAction(ActionEvent actionEvent) {
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
    }

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @FXML
    public void buttonCopyToClipboard_onAction(ActionEvent actionEvent) {
        if (imageInfo == null) return;

        clipboardContent.clear();
        clipboardContent.putString(imageInfo.toString());
        clipboard.setContent(clipboardContent);
    }
}
