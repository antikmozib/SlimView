package io.mozib.slimview;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.awt.Desktop;

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
            textAcknowledgements.setText(Files.readString(Paths.get("acknowledgements.txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void buttonOK_onAction(ActionEvent actionEvent) {
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
    }

    public void hyperlinkWebsite_onAction(ActionEvent actionEvent) {
        try {
            Desktop.getDesktop().browse(new URL(hyperlinkWebsite.getText()).toURI());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
