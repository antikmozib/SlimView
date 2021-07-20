/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class FavoritesWindowController implements Initializable {
    @FXML
    public ListView<FavoritesModel.FavoriteModel> listViewFavorites;
    @FXML
    public ImageView imageViewPreview;
    @FXML
    public StackPane stackPanePreview;

    private FavoritesController favoritesController;
    private final ReadOnlyObjectWrapper<FavoritesModel.FavoriteModel> selectedFavorite =
            new ReadOnlyObjectWrapper<>(null);

    public void setFavoritesController(FavoritesController favoritesController) {
        this.favoritesController = favoritesController;
        listViewFavorites.setItems(favoritesController.getFavorites());
    }

    public ReadOnlyObjectProperty<FavoritesModel.FavoriteModel> getSelectedFavorite() {
        return selectedFavorite.getReadOnlyProperty();
    }

    @FXML
    public void buttonOpen_onAction(ActionEvent actionEvent) {
        open();
    }

    @FXML
    public void btnCancel_onAction(ActionEvent actionEvent) {
        close();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        listViewFavorites.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            var preview = new Image(new File(newValue.toString()).toURI().toString());
            imageViewPreview.fitHeightProperty().unbind();
            imageViewPreview.fitWidthProperty().unbind();
            imageViewPreview.setImage(preview);
            imageViewPreview.fitHeightProperty().bind(stackPanePreview.heightProperty());
            imageViewPreview.fitWidthProperty().bind(stackPanePreview.widthProperty());
        }));
    }

    private void open() {
        selectedFavorite.set(listViewFavorites.getSelectionModel().getSelectedItem());
        close();
    }

    private void close() {
        ((Stage) listViewFavorites.getScene().getWindow()).close();
    }

    @FXML
    public void listViewFavorites_onKeyPress(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case ENTER:
                open();
                break;
            case ESCAPE:
                close();
                break;
        }
    }

    @FXML
    public void listViewFavorites_onClick(MouseEvent mouseEvent) {
        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            if (mouseEvent.getClickCount() == 2) {
                open();
            }
        }
    }

    @FXML
    public void buttonRemove_onAction(ActionEvent actionEvent) {
        listViewFavorites.getItems().remove(listViewFavorites.getSelectionModel().getSelectedIndex());
        favoritesController.saveFavorites();
    }

    @FXML
    public void menuRemoveAll_onAction(ActionEvent actionEvent) {
        listViewFavorites.getItems().clear();
        favoritesController.saveFavorites();
    }
}
