package io.mozib.simview;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class ResizeWindowController implements Initializable {
    @FXML
    public TextField textWidth;
    @FXML
    public TextField textHeight;
    @FXML
    public CheckBox checkBoxPreserveAspectRatio;

    private ResizeViewModel resizeViewModel;

    public void setViewModel(ResizeViewModel resizeViewModel) {
        this.resizeViewModel = resizeViewModel;
        textWidth.textProperty().bindBidirectional(resizeViewModel.newWidthProperty);
        textHeight.textProperty().bindBidirectional(resizeViewModel.newHeightProperty);
        checkBoxPreserveAspectRatio.selectedProperty().bindBidirectional(resizeViewModel.preserveAspectRatioProperty);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @FXML
    public void buttonOK_onAction(ActionEvent actionEvent) {
        resizeViewModel.useNewValues.set(true);
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
    }

    @FXML
    public void buttonCancel_onAction(ActionEvent actionEvent) {
        ((Stage) ((Node) actionEvent.getSource()).getScene().getWindow()).close();
    }
}
