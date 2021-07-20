/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import java.util.ArrayList;
import java.util.List;

public class FavoritesController {
    private final List<String> favorites = new ArrayList<>();

    public void add(String path) {
        favorites.add(path);
        saveFavorites();
    }

    public void remove(String path) {
        for (String favorite : favorites) {
            if (favorite.equals(path)) {
                favorites.remove(favorite);
                break;
            }
        }
        saveFavorites();
    }

    public boolean exists(String path) {
        for (String favorite : favorites) {
            if (favorite.equals(path)) {
                return true;
            }
        }
        return false;
    }

    public FavoritesController() {
        loadFavorites();
    }

    private void loadFavorites() {
        FavoritesModel favoritesModel = Common.readDataFile(FavoritesModel.class, Common.DataFileType.FAVORITES);
        if (favoritesModel == null || favoritesModel.favoritesList == null) {
            return;
        }

        for (FavoritesModel.FavoriteModel favoriteModel : favoritesModel.favoritesList) {
            favorites.add(favoriteModel.path);
        }
    }

    private void saveFavorites() {
        FavoritesModel favoritesModel = new FavoritesModel();
        for (String favorite : favorites) {
            favoritesModel.favoritesList.add(new FavoritesModel.FavoriteModel(favorite));
        }
        Common.writeDataFile(favoritesModel, Common.DataFileType.FAVORITES);
    }
}
