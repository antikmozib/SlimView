/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

import static io.mozib.slimview.Common.*;

public class CopyFileToViewModel {
    public ObservableList<CopyToDestinations.CopyToDestination> destinations;
    private final ImageModel source;

    public enum OnConflict {
        SKIP {
            @Override
            public String toString() {
                return "Skip";
            }
        },
        OVERWRITE {
            @Override
            public String toString() {
                return "Overwrite";
            }
        }
    }

    public CopyFileToViewModel(ImageModel source) {
        //CopyToDestinations copyToDestinations = loadDestinations();
        CopyToDestinations copyToDestinations = Common.loadDataFile(
                CopyToDestinations.class,
                SettingFileType.COPY_TO_DESTINATIONS);

        if (copyToDestinations.destinations == null) {
            copyToDestinations.destinations = new ArrayList<>();
        }

        this.destinations = FXCollections.observableList(copyToDestinations.destinations);
        this.source = source;
    }

    private CopyToDestinations loadDestinations() {
        XmlMapper xmlMapper = new XmlMapper();
        String xml;
        CopyToDestinations copyToDestinations = null;
        try {
            xml = inputStreamToString(new FileInputStream(getSettingsFile(SettingFileType.COPY_TO_DESTINATIONS)));
            copyToDestinations = xmlMapper.readValue(xml, CopyToDestinations.class);
        } catch (JsonProcessingException | FileNotFoundException e) {
            e.printStackTrace();
        }
        if (copyToDestinations == null || copyToDestinations.destinations == null) {
            copyToDestinations = new CopyToDestinations();
        }
        return copyToDestinations;
    }

    public void saveDestinations() {
        XmlMapper xmlMapper = new XmlMapper();
        CopyToDestinations copyToDestinations = new CopyToDestinations();
        copyToDestinations.destinations = destinations;
        try {
            xmlMapper.writeValue(new File(getSettingsFile(SettingFileType.COPY_TO_DESTINATIONS)), copyToDestinations);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void copy(OnConflict onConflict, ObservableList<CopyToDestinations.CopyToDestination> destinations) {
        File original = new File(source.getPath());
        for (CopyToDestinations.CopyToDestination destination : destinations) {
            File copied = Paths.get(destination.getDestination(), source.getShortName()).toFile();

            if (copied.exists() && !copied.isDirectory()) {
                if (onConflict == OnConflict.OVERWRITE) {
                    if (!copied.delete()) continue;
                } else if (onConflict == OnConflict.SKIP) {
                    continue;
                }
            }

            try {
                FileUtils.copyFile(original, copied);
            } catch (IOException ignored) {
            }
        }
    }


}
