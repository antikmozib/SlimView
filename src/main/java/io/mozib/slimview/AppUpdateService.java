/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
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
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class AppUpdateService extends Service<Boolean> {
    private final ReadOnlyStringWrapper updateUrlWrapper = new ReadOnlyStringWrapper("");
    private final String apiUrl = "https://mozib.io/downloads/update.php";
    private final int connectionTimeout = 5000;

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

                BufferedReader in;
                String response = "";
                RequestConfig requestConfig = RequestConfig
                        .custom()
                        .setConnectTimeout(connectionTimeout)
                        .setSocketTimeout(connectionTimeout)
                        .setConnectionRequestTimeout(connectionTimeout)
                        .build();
                HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
                HttpPost httpPost = new HttpPost(apiUrl);
                List<NameValuePair> params = new ArrayList<>();

                params.add(new BasicNameValuePair("appname", "slimview"));
                params.add(new BasicNameValuePair("version", getAppVer()));
                httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
                HttpResponse httpResponse = httpClient.execute(httpPost);
                HttpEntity httpEntity = httpResponse.getEntity();

                if (httpEntity != null) {
                    InputStream inputStream = httpEntity.getContent();
                    in = new BufferedReader(new InputStreamReader(inputStream));
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

    private String getAppVer() {
        return this.getClass().getPackage().getImplementationVersion();
    }
}
