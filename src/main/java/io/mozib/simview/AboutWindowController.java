package io.mozib.simview;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.awt.Desktop;

public class AboutWindowController implements Initializable {
    @FXML
    Hyperlink hyperlinkWebsite;

    @FXML
    Label labelVersion;

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        labelVersion.setText("Version: " + this.getClass().getPackage().getImplementationVersion());
    }

    public void buttonOK_onAction(ActionEvent actionEvent) {
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
    }

    public void hyperlinkWebsite_onAction(ActionEvent actionEvent) {
        try {
            Desktop.getDesktop().browse(new URL(hyperlinkWebsite.getText()).toURI());
        } catch (Exception e) {

        }
    }
}
