/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Fake main() for JavaFX support. True main() is in SlimView.java
 */
public class App {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("--uninst")) {
            try {
                // clear Java prefs
                Preferences.userNodeForPackage(App.class).clear();
                // delete settings files
                Util.deleteDirectoryRecursively(new File(Util.getTempDirectory()).getParent());
                //FileUtils.deleteDirectory(new File(Util.getTempDirectory()).getParentFile());
            } catch (BackingStoreException | IOException e) {
                e.printStackTrace();
            }
            return;
        }
        SlimView.main(args);
    }
}