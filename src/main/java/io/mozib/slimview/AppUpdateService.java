/*
 * Copyright (C) 2021-2023 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class AppUpdateService extends Service<Boolean> {

    private final ReadOnlyStringWrapper updateUrlWrapper = new ReadOnlyStringWrapper("");
    private final String apiUrl = "https://mozib.io/downloads/update.php";
    private final int connectionTimeout = 5000;
    private final String appName;
    private final String appVersion;

    public AppUpdateService(String appName, String appVersion) {
        this.appName = appName;
        this.appVersion = appVersion;
    }

    public ReadOnlyStringProperty updateUrlProperty() {
        return updateUrlWrapper.getReadOnlyProperty();
    }

    /**
     * @return The url to the file to be downloaded.
     */
    public String getUpdateUrl() {
        return updateUrlWrapper.get();
    }

    @Override
    protected Task<Boolean> createTask() {
        return new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                updateUrlWrapper.set("");

                String response = "";
                RequestConfig requestConfig = RequestConfig
                        .custom()
                        .setConnectTimeout(connectionTimeout)
                        .setSocketTimeout(connectionTimeout)
                        .setConnectionRequestTimeout(connectionTimeout)
                        .build();
                HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
                HttpGet httpGet = new HttpGet(apiUrl);

                // Add appname and version parameters to request.
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("appname", appName));
                params.add(new BasicNameValuePair("version", appVersion));
                URI uri = new URIBuilder(httpGet.getURI()).addParameters(params).build();
                httpGet.setURI(uri);

                HttpResponse httpResponse = httpClient.execute(httpGet);
                HttpEntity httpEntity = httpResponse.getEntity();

                if (httpEntity != null) {
                    InputStream inputStream = httpEntity.getContent();
                    BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        response += inputLine;
                    }
                    in.close();
                }

                updateUrlWrapper.set(response);
                return !response.equals("");
            }
        };
    }
}
