/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import javafx.scene.control.Alert;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * A collection of various static utility methods
 */
public class Util {

    public enum OSType {
        WINDOWS, MAC, LINUX, OTHER
    }

    public static OSType getOSType() {
        String platform = System.getProperty("os.name").toLowerCase();

        if (platform.contains("win")) {
            return OSType.WINDOWS;
        }
        if (platform.contains("mac")) {
            return OSType.MAC;
        }
        if (platform.contains("nux")) {
            return OSType.LINUX;
        }
        return OSType.OTHER;
    }

    public enum DataFileLocation {
        RECENT_FILES, COPY_TO_DESTINATIONS, FAVORITES
    }

    /**
     * @param dataFileLocation Path to the data file
     * @return Path to the XML settings file
     */
    public static String getDataFile(DataFileLocation dataFileLocation) {
        createSettingsDir();
        Path path = Paths.get(System.getProperty("user.home"), ".slimview");

        switch (dataFileLocation) {
            case RECENT_FILES:
                path = Paths.get(path.toString(), "recent.xml");
                break;
            case COPY_TO_DESTINATIONS:
                path = Paths.get(path.toString(), "copy-to-destinations.xml");
                break;
            case FAVORITES:
                path = Paths.get(path.toString(), "favorites.xml");
                break;
        }

        // create file if not exists
        try {
            new File(path.toString()).createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return path.toString();
    }

    /**
     * @return Path to the directory where to save temp, edited images
     */
    public static String tempDirectory() {
        createSettingsDir(); // ensure settings directory exists
        Path path = Paths.get(System.getProperty("user.home"), ".slimview", "cache");
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Paths.get(System.getProperty("user.home"), ".slimview", "cache").toString();
    }

    private static void createSettingsDir() {
        Path path = Paths.get(System.getProperty("user.home"), ".slimview");
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String inputStreamToString(InputStream inputStream) {
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    public static void addToRecent(String path) {
        final int maxRecent = 10; // max number of recent files to save
        RecentFiles.RecentFile alreadyExists = null;
        RecentFiles recentFiles = Util.readDataFile(RecentFiles.class, DataFileLocation.RECENT_FILES);

        if (recentFiles == null) {
            recentFiles = new RecentFiles();
        }

        if (recentFiles.getRecentFiles() == null) {
            recentFiles.setRecentFiles(new ArrayList<>());
        }

        // if file already exists in recent, don't add again
        for (RecentFiles.RecentFile rf : recentFiles.getRecentFiles()) {
            if (rf.getPath().equals(path)) {
                alreadyExists = rf;
                break;
            }
        }

        if (alreadyExists != null) {
            alreadyExists.refresh();
        } else {
            if (recentFiles.getRecentFiles().size() >= maxRecent) {
                long oldestSeen = System.currentTimeMillis();
                for (RecentFiles.RecentFile rf : recentFiles.getRecentFiles()) {
                    if (rf.getLastSeen() < oldestSeen) {
                        oldestSeen = rf.getLastSeen();
                    }
                }
                RecentFiles.RecentFile remove = null;
                for (RecentFiles.RecentFile rf : recentFiles.getRecentFiles()) {
                    if (rf.getLastSeen() == oldestSeen) {
                        remove = rf;
                        break;
                    }
                }
                recentFiles.getRecentFiles().remove(remove);
            }
            RecentFiles.RecentFile newRecent = new RecentFiles.RecentFile();
            newRecent.setPath(path);
            recentFiles.getRecentFiles().add(newRecent);
        }
        writeDataFile(recentFiles, DataFileLocation.RECENT_FILES);
    }

    /**
     * Required class to enable copying image to the system clipboard
     */
    public static class ImageTransferable implements Transferable {

        private final Image image;

        public ImageTransferable(Image image) {
            this.image = image;
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (isDataFlavorSupported(flavor)) {
                return image;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor == DataFlavor.imageFlavor;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }
    }

    /**
     * @param classType        The type of object the XML mapper will map to
     * @param dataFileLocation The location of the setting file, determined through its enum
     * @param <T>              Generic type
     * @return Data read from XML file and mapped to a JavaBean
     */
    public static <T> T readDataFile(Class<T> classType, DataFileLocation dataFileLocation) {
        XmlMapper xmlMapper = new XmlMapper();
        String xmlContents;
        T data = null;

        try {
            xmlContents = inputStreamToString(new FileInputStream(getDataFile(dataFileLocation)));
            data = xmlMapper.readValue(xmlContents, classType);
        } catch (FileNotFoundException | JsonProcessingException ignored) {
            // ignore this error; this means we're running the app for the first time
        }

        if (data == null) {
            try {
                data = classType.getDeclaredConstructor().newInstance();
            } catch (InstantiationException |
                    IllegalAccessException |
                    InvocationTargetException |
                    NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        return data;
    }

    /**
     * Serializes an object to a XML file
     *
     * @param data             Object to write to a XML file
     * @param dataFileLocation Location of the XML file
     */
    public static void writeDataFile(Object data, DataFileLocation dataFileLocation) {
        XmlMapper xmlMapper = new XmlMapper();
        try {
            xmlMapper.writeValue(new File(getDataFile(dataFileLocation)), data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens an url in the system's default browser application
     */
    public static void browseUrl(String url) {
        switch (getOSType()) {
            case WINDOWS:
            case MAC:
                if (Desktop.isDesktopSupported()) {
                    if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        try {
                            Desktop.getDesktop().browse(new URL(url).toURI());
                        } catch (IOException | URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case LINUX:
                try {
                    Runtime.getRuntime().exec(new String[]{"xdg-open", url});
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }

    public static Alert showCustomErrorDialog(String header, String content, Window owner, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.initOwner(owner);
        alert.setTitle("Error");

        if (e != null) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            String stackTrace = stringWriter.toString(); // stack trace as a string

            javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea(stackTrace);
            textArea.setEditable(false);
            textArea.setWrapText(false);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);
            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(textArea, 0, 0);

            alert.getDialogPane().setExpandableContent(expContent);
        }
        return alert;
    }
}
