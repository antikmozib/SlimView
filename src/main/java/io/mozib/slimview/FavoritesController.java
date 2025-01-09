/*
 * Copyright (C) 2021-2023 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class FavoritesController {

    private final ObservableList<FavoritesModel.FavoriteModel> favorites = FXCollections.observableArrayList();

    public void add(String path) {
        favorites.add(new FavoritesModel.FavoriteModel(path));
        saveFavorites();
    }

    public void remove(String path) {
        for (FavoritesModel.FavoriteModel favorite : favorites) {
            if (favorite.getPath().equals(path)) {
                favorites.remove(favorite);
                break;
            }
        }
        saveFavorites();
    }

    public boolean exists(String path) {
        for (FavoritesModel.FavoriteModel favorite : favorites) {
            if (favorite.getPath().equals(path)) {
                return true;
            }
        }
        return false;
    }

    public ObservableList<FavoritesModel.FavoriteModel> getFavorites() {
        return favorites;
    }

    public FavoritesController() {
        loadFavorites();
    }

    private void loadFavorites() {
        FavoritesModel favoritesModel = Util.readDataFile(FavoritesModel.class, Util.DataFileLocation.FAVORITES);
        if (favoritesModel == null || favoritesModel.getFavoritesList() == null) {
            return;
        }

        favorites.addAll(favoritesModel.getFavoritesList());
    }

    public void saveFavorites() {
        FavoritesModel favoritesModel = new FavoritesModel();
        favoritesModel.getFavoritesList().addAll(favorites);
        Util.writeDataFile(favoritesModel, Util.DataFileLocation.FAVORITES);
    }
}
