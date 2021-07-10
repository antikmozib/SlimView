/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import java.text.DecimalFormat;

public class ResizeViewModel {
    public SimpleStringProperty newHeightProperty = new SimpleStringProperty();
    public SimpleStringProperty newWidthProperty = new SimpleStringProperty();
    public SimpleBooleanProperty useNewValues = new SimpleBooleanProperty(false);
    public SimpleBooleanProperty preserveAspectRatioProperty = new SimpleBooleanProperty(true);
    private final double originalAspectRatio;

    public ResizeViewModel(double currentWidth, double currentHeight) {
        originalAspectRatio = currentWidth / currentHeight;
        newWidthProperty.set(String.valueOf((int) currentWidth));
        newHeightProperty.set(String.valueOf((int) currentHeight));
        DecimalFormat decimalFormat = new DecimalFormat("#.##");

        newWidthProperty.addListener(
                (ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
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
}
