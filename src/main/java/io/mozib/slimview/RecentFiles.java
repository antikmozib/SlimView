/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RecentFiles implements Serializable {
    public List<RecentFile> recentFileList;

    public RecentFiles() {
        recentFileList = new ArrayList<>();
    }

    public static class RecentFile implements Serializable {
        private String path;
        private long lastSeen;

        // required for serialization
        public RecentFile() {
        }

        public long getLastSeen() {
            return lastSeen;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
            this.lastSeen = System.currentTimeMillis();
        }
    }
}
