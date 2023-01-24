/*
 * Copyright (C) 2021-2023 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RecentFiles implements Serializable {

    private List<RecentFile> recentFiles = new ArrayList<>();

    // required for serialization
    public RecentFiles() {
    }

    public List<RecentFile> getRecentFiles() {
        return recentFiles;
    }

    public void setRecentFiles(List<RecentFile> recentFiles) {
        this.recentFiles = recentFiles;
    }

    public static class RecentFile implements Serializable {

        private String path;
        private long lastSeen;

        // required for serialization
        public RecentFile() {
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
            this.lastSeen = System.currentTimeMillis();
        }

        /**
         * @return The last time this file was seen. Value will be bigger for newer files and smaller for older files.
         */
        public long getLastSeen() {
            return lastSeen;
        }

        public void setLastSeen(long lastSeen) {
            this.lastSeen = lastSeen;
        }

        public void refresh() {
            setPath(path);
        }
    }
}
