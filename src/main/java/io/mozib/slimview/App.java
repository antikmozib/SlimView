/*
 * Copyright (C) 2021-2023 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import java.io.File;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static io.mozib.slimview.Util.getDataFile;

/**
 * Fake main() for JavaFX support. True main() is in SlimView.java
 */
public class App {

    public static void main(String[] args) {
        // check if we're in uninstallation mode
        if (args.length > 0 && args[0].equalsIgnoreCase("--uninst")) {
            try {
                // clear Java prefs
                Preferences.userNodeForPackage(App.class).clear();
                // delete settings files
                Util.deleteDirectoryRecursively(new File(getDataFile(Util.DataFileLocation.SETTINGS_DIR)).getPath());
                System.out.println("Execution of auxiliary uninstallation tasks complete.");
            } catch (BackingStoreException | IOException e) {
                e.printStackTrace();
            }
            return;
        }
        SlimView.main(args);
    }
}
