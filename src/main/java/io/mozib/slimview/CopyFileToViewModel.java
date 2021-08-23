/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static io.mozib.slimview.Util.DataFileLocation;

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
        },
        RENAME {
            @Override
            public String toString() {
                return "Rename";
            }
        }
    }

    public CopyFileToViewModel(ImageModel source) {
        CopyToDestinations copyToDestinations = Util.readDataFile(
                CopyToDestinations.class,
                DataFileLocation.COPY_TO_DESTINATIONS);

        if (copyToDestinations == null) {
            copyToDestinations = new CopyToDestinations();
        }

        if (copyToDestinations.getDestinations() == null) {
            copyToDestinations.setDestinations(new ArrayList<>());
        }

        this.destinations = FXCollections.observableList(copyToDestinations.getDestinations());
        this.source = source;
    }

    public void saveDestinations() {
        CopyToDestinations copyToDestinations = new CopyToDestinations();
        copyToDestinations.setDestinations(destinations);
        Util.writeDataFile(copyToDestinations, DataFileLocation.COPY_TO_DESTINATIONS);
    }

    /**
     * @param onConflict   What to do when a conflict arises
     * @param destinations Where to copy the file to
     * @return A list of exceptions (if any) that occurred during the copying process
     */
    public List<IOException> copy(OnConflict onConflict,
                                  ObservableList<CopyToDestinations.CopyToDestination> destinations) {
        List<IOException> exceptions = new ArrayList<>();
        File original = new File(source.getPath());

        for (CopyToDestinations.CopyToDestination destination : destinations) {
            File copied = Paths.get(destination.getDestination(), source.getName()).toFile();

            if (copied.exists() && !copied.isDirectory()) {
                switch (onConflict) {
                    case SKIP:
                        continue;
                    case RENAME:
                        copied = new File(getNewFilePath(copied.getPath()));
                        break;
                    case OVERWRITE:
                        if (!copied.delete()) continue;
                        break;
                }
            }

            try {
                FileUtils.copyFile(original, copied);
            } catch (IOException e) {
                exceptions.add(e);
            }
        }

        return exceptions;
    }

    public String getNewFilePath(String originalFilePath) {
        String dirPath = new File(originalFilePath).getParent();
        String fileExt = FilenameUtils.getExtension(originalFilePath);
        String fileNameWithExt = new File(originalFilePath).getName();
        String fileNameWithoutExt = fileNameWithExt.substring(0, fileNameWithExt.length() - fileExt.length() - 1);
        int i = 1;

        while (true) {
            String newFilename = Paths.get(dirPath, fileNameWithoutExt + " (" + i + ")." + fileExt).toString();
            File newFile = new File(newFilename);
            if (!newFile.isDirectory() && !newFile.exists()) {
                return newFilename;
            }
            i++;
        }
    }
}
