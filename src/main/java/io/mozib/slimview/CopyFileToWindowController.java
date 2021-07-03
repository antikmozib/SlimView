package io.mozib.slimview;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SingleSelectionModel;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.jar.JarFile;
import java.util.prefs.Preferences;

import static io.mozib.slimview.CopyFileToViewModel.*;

public class CopyFileToWindowController implements Initializable {
    private CopyFileToViewModel copyFileToViewModel = null;
    private final Preferences preferences = Preferences.userNodeForPackage(this.getClass());

    public void setViewModel(CopyFileToViewModel copyFileToViewModel) {
        this.copyFileToViewModel = copyFileToViewModel;
        listViewMain.setItems(copyFileToViewModel.destinations);
    }

    @FXML
    public ListView listViewMain;

    @FXML
    public ComboBox<CopyFileToViewModel.OnConflict> comboBoxOnConflict;

    @FXML
    public void buttonAdd_onAction(ActionEvent actionEvent) {
        add();
    }

    @FXML
    public void buttonRemove_onAction(ActionEvent actionEvent) {
        remove();
    }

    @FXML
    public void buttonCopy_onAction(ActionEvent actionEvent) {
        copy();
    }

    @FXML
    public void buttonClose_onAction(ActionEvent actionEvent) {
        close();
    }

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        listViewMain.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        comboBoxOnConflict.getItems().setAll(CopyFileToViewModel.OnConflict.values());
        comboBoxOnConflict.getSelectionModel().select(OnConflict.valueOf(
                preferences.get("CopyFileToOnConflict", OnConflict.SKIP.toString())));
    }

    private void add() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(listViewMain.getScene().getWindow());

        if (selectedDirectory != null) {
            listViewMain.getItems().add(selectedDirectory.getPath());
            copyFileToViewModel.saveDestinations();
        }
    }

    private void copy() {
        copyFileToViewModel.copy(comboBoxOnConflict.getSelectionModel().getSelectedItem(),
                listViewMain.getSelectionModel().getSelectedItems());
        close();

    }

    private void close() {
        ((Stage) listViewMain.getScene().getWindow()).close();
    }

    @FXML
    private void remove() {
        ObservableList<Integer> selectedIndices = listViewMain.getSelectionModel().getSelectedIndices();
        List<Integer> sortableItems = new ArrayList<>(selectedIndices);

        Collections.sort(sortableItems);
        Collections.reverse(sortableItems);

        for (Integer i : sortableItems) {
            listViewMain.getItems().remove(i.intValue());
        }

        listViewMain.refresh();
        copyFileToViewModel.saveDestinations();
    }

    @FXML
    public void comboBoxOnConflict_onAction(ActionEvent actionEvent) {
        preferences.put("CopyFileToOnConflict", comboBoxOnConflict.getSelectionModel().getSelectedItem().toString());
    }
}
