/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import static io.mozib.slimview.CopyFileToViewModel.OnConflict;

public class CopyFileToWindowController implements Initializable {
    private CopyFileToViewModel copyFileToViewModel = null;
    private final Preferences preferences = Preferences.userNodeForPackage(this.getClass());

    public void setViewModel(CopyFileToViewModel copyFileToViewModel) {
        this.copyFileToViewModel = copyFileToViewModel;
        listViewMain.setItems(copyFileToViewModel.destinations);

        // add the user's Pictures directory if destinations is empty
        if (listViewMain.getItems().size() == 0) {
            listViewMain.getItems().add(new CopyToDestinations.CopyToDestination(
                    Paths.get(System.getProperty("user.home"), "Pictures").toString()));
        }

        // select previously chosen locations
        String[] selectedDestinations = preferences.get("SelectedDestinations", "").split(";");
        for (String selectedDestination : selectedDestinations) {
            selectItemByName(selectedDestination);
        }
    }

    @FXML
    public ListView<CopyToDestinations.CopyToDestination> listViewMain;

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
    public void comboBoxOnConflict_onAction(ActionEvent actionEvent) {
        preferences.put("CopyFileToOnConflict", comboBoxOnConflict.getSelectionModel().getSelectedItem().toString());
    }

    @FXML
    public void listViewMain_onKeyPress(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            copy();
        } else if (keyEvent.getCode() == KeyCode.ESCAPE) {
            close();
        }
    }

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        listViewMain.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        comboBoxOnConflict.getItems().setAll(CopyFileToViewModel.OnConflict.values());
        comboBoxOnConflict.getSelectionModel().select(OnConflict.valueOf(
                preferences.get("CopyFileToOnConflict", OnConflict.SKIP.toString()).toUpperCase()));
    }

    private void add() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(listViewMain.getScene().getWindow());

        if (selectedDirectory != null) {
            CopyToDestinations.CopyToDestination newItem =
                    new CopyToDestinations.CopyToDestination((selectedDirectory.getPath()));
            listViewMain.getItems().add(newItem);
            copyFileToViewModel.saveDestinations();
            listViewMain.getSelectionModel().clearAndSelect(listViewMain.getItems().indexOf(newItem));
            listViewMain.requestFocus();
        }
    }

    private void copy() {
        if (listViewMain.getSelectionModel().getSelectedItem() == null) {
            return;
        }

        // save selected locations
        String selectedDestinations = "";
        for (CopyToDestinations.CopyToDestination cd : listViewMain.getSelectionModel().getSelectedItems()) {
            selectedDestinations = selectedDestinations + cd.getDestination() + ";";
        }
        // remove last semicolon
        if (selectedDestinations.length() > 1) {
            selectedDestinations = selectedDestinations.substring(0, selectedDestinations.length() - 1);
        }
        preferences.put("SelectedDestinations", selectedDestinations);

        copyFileToViewModel.copy(comboBoxOnConflict.getSelectionModel().getSelectedItem(),
                listViewMain.getSelectionModel().getSelectedItems());
        close();
    }

    private void close() {
        ((Stage) listViewMain.getScene().getWindow()).close();
    }

    private void remove() {
        ObservableList<Integer> selectedIndices = listViewMain.getSelectionModel().getSelectedIndices();
        List<Integer> sortableItems = new ArrayList<>(selectedIndices);

        // remove items starting from the bottom of the list to preserve index integrity
        Collections.sort(sortableItems);
        Collections.reverse(sortableItems);
        for (Integer i : sortableItems) {
            listViewMain.getItems().remove(i.intValue());
        }

        listViewMain.refresh();
        copyFileToViewModel.saveDestinations();
    }

    private void selectItemByName(String name) {
        for (CopyToDestinations.CopyToDestination cp : listViewMain.getItems()) {
            if (cp.getDestination().equals(name)) {
                listViewMain.getSelectionModel().select(cp);
                return;
            }
        }
    }
}
