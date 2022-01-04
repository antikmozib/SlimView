/*
 * Copyright (C) Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class AboutWindowController implements Initializable {

    @FXML
    public Hyperlink hyperlinkWebsite;
    @FXML
    public Label labelVersion;
    @FXML
    public Button buttonOK;

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        labelVersion.setText("Version: " + (Util.getAppVersion().equals("") ? "Unknown" : Util.getAppVersion()));
        buttonOK.requestFocus();
    }

    public void buttonOK_onAction(ActionEvent actionEvent) {
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
    }

    public void hyperlinkWebsite_onAction(ActionEvent actionEvent) {
        Util.browseUrl(hyperlinkWebsite.getText());
    }

    @FXML
    public void buttonAcknowledgements_onAction(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Acknowledgements");
        alert.setHeaderText("");
        alert.initModality(Modality.WINDOW_MODAL);
        alert.initOwner(((Button) actionEvent.getSource()).getScene().getWindow());

        TextArea textArea = new TextArea();
        GridPane gridPane = new GridPane();

        try {
            textArea.setText(Files.readString(Paths.get("notice.txt")));
        } catch (IOException e) {
            textArea.setText("Failed to load the acknowledgements file.");
        }

        textArea.setMaxHeight(240);
        textArea.setMaxWidth(320);
        textArea.setEditable(false);
        gridPane.add(textArea, 0, 0);
        alert.getDialogPane().setContent(gridPane);
        alert.show();
    }
}
