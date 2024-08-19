/*
 * Copyright (C) 2021-2023 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.lang3.StringUtils;
import org.imgscalr.Scalr;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class ResizeViewModel {

    private final Preferences preferences = Preferences.userNodeForPackage(this.getClass());
    private final double originalAspectRatio;
    private final List<String> qualities = new ArrayList<>();
    private String selectedQuality
            = preferences.get("SelectedResizeQuality", methodToString(Scalr.Method.ULTRA_QUALITY));

    public SimpleStringProperty newHeightProperty = new SimpleStringProperty();
    public SimpleStringProperty newWidthProperty = new SimpleStringProperty();
    public SimpleBooleanProperty preserveAspectRatioProperty = new SimpleBooleanProperty(true);
    public ObservableList<String> resizeQualities = FXCollections.observableList(qualities);

    // indicates whether we should use the new values
    public SimpleBooleanProperty useNewValues = new SimpleBooleanProperty(false);

    public ResizeViewModel(double currentWidth, double currentHeight) {
        originalAspectRatio = currentWidth / currentHeight;
        newWidthProperty.set(String.valueOf((int) currentWidth));
        newHeightProperty.set(String.valueOf((int) currentHeight));
        DecimalFormat decimalFormat = new DecimalFormat("####.##");

        for (var method : Scalr.Method.values()) {
            qualities.add(methodToString(method));
        }

        newWidthProperty.addListener((observable, oldValue, newValue) -> {
            if (preserveAspectRatioProperty.get()) {
                double currentAspectRatio = getWidth() / getHeight();
                if (decimalFormat.format(currentAspectRatio).equals(
                        decimalFormat.format(originalAspectRatio))) {
                    return;
                }

                double width = Double.parseDouble(newWidthProperty.get());
                double newHeight = width / originalAspectRatio;
                newHeightProperty.set(String.valueOf((int) newHeight));
            }
        });

        newHeightProperty.addListener((observable, oldValue, newValue) -> {
            if (preserveAspectRatioProperty.get()) {
                double currentAspectRatio = getWidth() / getHeight();
                if (decimalFormat.format(currentAspectRatio).equals(decimalFormat.format(originalAspectRatio))) {
                    return;
                }

                double height = Double.parseDouble(newHeightProperty.get());
                double newWidth = height * originalAspectRatio;
                newWidthProperty.set(String.valueOf((int) newWidth));
            }
        });
    }

    public double getWidth() {
        return Double.parseDouble(newWidthProperty.get());
    }

    public double getHeight() {
        return Double.parseDouble(newHeightProperty.get());
    }

    public String getSelectedQuality() {
        return selectedQuality;
    }

    public void setSelectedQuality(String selectedQuality) {
        this.selectedQuality = selectedQuality;
        preferences.put("SelectedResizeQuality", selectedQuality);
    }

    public Scalr.Method stringToMethod(String value) {
        return Scalr.Method.valueOf(value.toUpperCase().replace(" ", "_"));
    }

    public String methodToString(Scalr.Method method) {
        return StringUtils.capitalize(method.name().replace("_", " ").toLowerCase());
    }
}
