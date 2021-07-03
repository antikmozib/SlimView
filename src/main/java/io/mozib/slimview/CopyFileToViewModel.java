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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static io.mozib.slimview.Common.copyToDestinationsCache;
import static io.mozib.slimview.Common.inputStreamToString;

public class CopyFileToViewModel {
    public ObservableList<CopyToDestinations.CopyToDestination> destinations;
    private ImageModel source;

    public enum OnConflict {
        SKIP, OVERWRITE
    }

    public CopyFileToViewModel(ImageModel source) {
        CopyToDestinations copyToDestinations = loadDestinations();
        this.destinations = FXCollections.observableList(copyToDestinations.destinations);
        this.source=source;
    }

    private CopyToDestinations loadDestinations() {
        XmlMapper xmlMapper = new XmlMapper();
        String xml;
        CopyToDestinations copyToDestinations = null;
        try {
            xml = inputStreamToString(new FileInputStream(copyToDestinationsCache()));
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
            xmlMapper.writeValue(new File(copyToDestinationsCache()), copyToDestinations);
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
                    copied.delete();
                } else if (onConflict == OnConflict.SKIP) {
                    continue;
                }
            }

            try {
                FileUtils.copyFile(original, copied);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
