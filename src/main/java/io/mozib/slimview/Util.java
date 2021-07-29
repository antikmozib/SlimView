/*
 * Copyright (C) 2021 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

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
        } else if (platform.contains("mac")) {
            return OSType.MAC;
        } else if (platform.contains("nux")) {
            return OSType.LINUX;
        } else {
            return OSType.OTHER;
        }
    }

    public enum DataFileLocation {
        RECENT_FILES, COPY_TO_DESTINATIONS, FAVORITES
    }

    /**
     * @param dataFileLocation Type of data file
     * @return Path to the xml settings file
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

    public static String inputStreamToString(InputStream is) {
        StringBuilder sb = new StringBuilder();
        String line = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        try {
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static void addToRecent(String path) {
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
                return;
            }
        }

        if (recentFiles.getRecentFiles().size() >= 5) {
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
     * @param classType        The type of object the xml mapper will map to
     * @param dataFileLocation The location of the setting file, determined through its enum
     * @param <T>              Generic type
     * @return Data read from xml file and mapped to a JavaBean
     */
    public static <T> T readDataFile(Class<T> classType, DataFileLocation dataFileLocation) {
        XmlMapper xmlMapper = new XmlMapper();
        String xml;
        T data = null;

        try {
            xml = inputStreamToString(new FileInputStream(getDataFile(dataFileLocation)));
            data = xmlMapper.readValue(xml, classType);
            if (data == null) {
                data = classType.getDeclaredConstructor().newInstance();
            }
        } catch (InstantiationException |
                IllegalAccessException |
                NoSuchMethodException |
                InvocationTargetException |
                FileNotFoundException |
                JsonProcessingException e) {

            e.printStackTrace();
        }

        return data;
    }

    /**
     * Serializes an object to a xml file
     *
     * @param data             Object to write to a xml file
     * @param dataFileLocation Location of the xml file
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
        }
    }
}
