/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to serialize favorites to an xml file
 */
public class FavoritesModel implements Serializable {

    private List<FavoriteModel> favoritesList = new ArrayList<>();

    public FavoritesModel() {
    }

    public void setFavoritesList(List<FavoriteModel> favoritesList) {
        this.favoritesList = favoritesList;
    }

    public List<FavoriteModel> getFavoritesList() {
        return favoritesList;
    }

    public static class FavoriteModel implements Serializable {

        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public FavoriteModel() {
        }

        public FavoriteModel(String path) {
            this.path = path;
        }

        @Override
        public String toString() {
            return path;
        }
    }
}
