/*
 * Copyright (C) 2021-2023 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class FavoritesWindowController implements Initializable {

    @FXML
    public ListView<FavoritesModel.FavoriteModel> listViewFavorites;
    @FXML
    public ImageView imageViewPreview;
    @FXML
    public StackPane stackPanePreview;
    @FXML
    public TextField textFieldSearch;
    @FXML
    public Label labelFilename;
    @FXML
    public AnchorPane anchorPanePreview;

    private FilteredList<FavoritesModel.FavoriteModel> filteredList;
    private FavoritesController favoritesController;
    private final ReadOnlyObjectWrapper<FavoritesModel.FavoriteModel> selectedFavorite
            = new ReadOnlyObjectWrapper<>(null);

    public void setFavoritesController(FavoritesController favoritesController) {
        this.favoritesController = favoritesController;
        this.filteredList = new FilteredList<>(favoritesController.getFavorites(), favoriteModel -> true);
        listViewFavorites.setItems(filteredList);

        if (listViewFavorites.getItems().size() > 0) {
            listViewFavorites.getSelectionModel().select(0);
        }
    }

    public ReadOnlyObjectProperty<FavoritesModel.FavoriteModel> getSelectedFavorite() {
        return selectedFavorite.getReadOnlyProperty();
    }

    @FXML
    public void buttonOpen_onAction(ActionEvent actionEvent) {
        close();
    }

    @FXML
    public void btnCancel_onAction(ActionEvent actionEvent) {
        close();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        anchorPanePreview.getChildren().get(anchorPanePreview.getChildren().indexOf(stackPanePreview)).toBack();
        listViewFavorites.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            imageViewPreview.setImage(null);
            labelFilename.setText("");
            imageViewPreview.fitHeightProperty().unbind();
            imageViewPreview.fitWidthProperty().unbind();

//            if (newValue == null) {
//                return;
//            }

            var preview = new Image(new File(newValue.toString()).toURI().toString());
            imageViewPreview.setImage(preview);
            labelFilename.setText(newValue.getPath());
            imageViewPreview.fitHeightProperty().bind(anchorPanePreview.heightProperty());
            imageViewPreview.fitWidthProperty().bind(anchorPanePreview.widthProperty());
        }));

        listViewFavorites.setCellFactory(param -> new ImagePreviewCell());

        textFieldSearch.textProperty().addListener(((observable, oldValue, newValue) -> {
            filteredList.setPredicate(favoriteModel -> {
//                if (newValue == null || newValue.isEmpty()) {
//                    return true;
//                }
                return favoriteModel.getPath().toLowerCase().contains(newValue.toLowerCase());
            });
        }));
    }

    private static class ImagePreviewCell extends ListCell<FavoritesModel.FavoriteModel> {

        @Override
        protected void updateItem(FavoritesModel.FavoriteModel item, boolean empty) {
            super.updateItem(item, empty);
            if (item != null) {
                HBox hBox = new HBox();
                ImageView imageView = new ImageView();

                File file = new File(item.getPath());
                if (!file.isDirectory() && file.exists()) {
                    imageView.setImage(new Image(file.toURI().toString()));
                }

                imageView.preserveRatioProperty().set(true);
                imageView.setFitHeight(400);
                Label label = new Label(new File(item.getPath()).getName());
                label.setWrapText(true);
                hBox.getChildren().add(imageView);
                hBox.getChildren().add(label);
                hBox.setSpacing(8);
                hBox.setAlignment(Pos.CENTER_LEFT);
                setGraphic(hBox);
            } else {
                setGraphic(null);
            }
        }
    }

    private void open() {
        if (listViewFavorites.getSelectionModel().getSelectedItem() == null) {
            return;
        }

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
            default:
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
        favoritesController.getFavorites().remove(filteredList.getSourceIndexFor(
                favoritesController.getFavorites(), listViewFavorites.getSelectionModel().getSelectedIndex()));
        favoritesController.saveFavorites();
    }

    @FXML
    public void menuRemoveAll_onAction(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(imageViewPreview.getScene().getWindow());
        alert.setTitle("Remove All");
        alert.setHeaderText("Are you sure you want to remove all favorites?");
        alert.setContentText("This action is irreversible.");

        Optional<ButtonType> result = alert.showAndWait();

        if (result.get() != ButtonType.OK) {
            return;
        }

        favoritesController.getFavorites().clear();
        favoritesController.saveFavorites();
    }

    @FXML
    public void menuRemoveMissing_onAction(ActionEvent actionEvent) {
        List<FavoritesModel.FavoriteModel> removeThese = new ArrayList<>();
        listViewFavorites.getItems().forEach(favoriteModel -> {
            File file = new File(favoriteModel.getPath());
            if (!file.isDirectory() && !file.exists()) {
                removeThese.add(favoriteModel);
            }
        });

        favoritesController.getFavorites().removeAll(removeThese);
        favoritesController.saveFavorites();
    }

    @FXML
    public void textFieldSearch_onAction(ActionEvent actionEvent) {
    }

    @FXML
    public void textFieldSearch_onKeyPress(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.DOWN) {
            listViewFavorites.requestFocus();
        }
    }
}
