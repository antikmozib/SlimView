/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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
    public TextArea textAcknowledgements;

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        labelVersion.setText("Version: " + this.getClass().getPackage().getImplementationVersion());
        try {
            textAcknowledgements.setText(Files.readString(Paths.get("notice.txt")));
        } catch (IOException e) {
            textAcknowledgements.setText("Failed to load the acknowledgements file.");
        }
    }

    public void buttonOK_onAction(ActionEvent actionEvent) {
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
    }

    public void hyperlinkWebsite_onAction(ActionEvent actionEvent) {
        Util.browseUrl(hyperlinkWebsite.getText());
    }
}
