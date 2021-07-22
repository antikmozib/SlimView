/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
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
        FavoritesModel favoritesModel = Util.readDataFile(FavoritesModel.class, Util.DataFileType.FAVORITES);
        if (favoritesModel == null || favoritesModel.favoritesList == null) {
            return;
        }

        favorites.addAll(favoritesModel.favoritesList);
    }

    public void saveFavorites() {
        FavoritesModel favoritesModel = new FavoritesModel();
        favoritesModel.favoritesList.addAll(favorites);
        Util.writeDataFile(favoritesModel, Util.DataFileType.FAVORITES);
    }
}
