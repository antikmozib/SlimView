/*
 * Copyright (C) Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class ResizeWindowController implements Initializable {

    private ResizeViewModel resizeViewModel;

    @FXML
    public TextField textWidth;
    @FXML
    public TextField textHeight;
    @FXML
    public CheckBox checkBoxPreserveAspectRatio;
    @FXML
    public ComboBox<String> comboBoxQuality;

    public void setViewModel(ResizeViewModel resizeViewModel) {
        this.resizeViewModel = resizeViewModel;
        textWidth.textProperty().bindBidirectional(resizeViewModel.newWidthProperty);
        textHeight.textProperty().bindBidirectional(resizeViewModel.newHeightProperty);
        comboBoxQuality.setItems(resizeViewModel.resizeQualities);
        comboBoxQuality.setOnAction(event
                -> resizeViewModel.setSelectedQuality(comboBoxQuality.getSelectionModel().getSelectedItem()));
        comboBoxQuality.getSelectionModel().select(resizeViewModel.getSelectedQuality());
        checkBoxPreserveAspectRatio.selectedProperty().bindBidirectional(resizeViewModel.preserveAspectRatioProperty);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // force height and width fields to accept numbers only
        ChangeListener<String> changeListener = new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (!newValue.matches("^[0-9]*.?[0-9]$")) {
                    ((TextField) ((StringProperty) observable).getBean()).setText(oldValue);
                }
            }
        };
        textWidth.textProperty().addListener(changeListener);
        textHeight.textProperty().addListener(changeListener);
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
