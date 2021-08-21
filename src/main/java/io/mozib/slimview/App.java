/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Fake main() for JavaFX support. True main() is in SlimView.java
 */
public class App {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("--uninst")) {
            try {
                Preferences.userNodeForPackage(App.class).clear();
            } catch (BackingStoreException e) {
                e.printStackTrace();
            }

            return;
        }

        SlimView.main(args);
    }
}