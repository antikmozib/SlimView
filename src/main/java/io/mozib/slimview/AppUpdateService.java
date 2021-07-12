/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class AppUpdateService extends Service<Boolean> {
    private final ReadOnlyStringWrapper updateUrlWrapper = new ReadOnlyStringWrapper("");
    private final String apiUrl = "https://mozib.io/downloads/update.php";

    public ReadOnlyStringProperty updateUrlProperty() {
        return updateUrlWrapper.getReadOnlyProperty();
    }

    public String getUpdateUrl() {
        return updateUrlWrapper.get();
    }

    @Override
    protected Task<Boolean> createTask() {
        return new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                updateUrlWrapper.set("");

                URL url = new URL("https://mozib.io/downloads/update.php?appname=slimview&version=" + getAppVer());
                URLConnection urlConnection = url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String inputLine;
                String response = "";

                while ((inputLine = in.readLine()) != null) {
                    response += inputLine;
                }
                in.close();

                updateUrlWrapper.set(response);
                return !response.equals("");
            }
        };
    }

    private String getAppVer() {
        return this.getClass().getPackage().getImplementationVersion();
    }
}
