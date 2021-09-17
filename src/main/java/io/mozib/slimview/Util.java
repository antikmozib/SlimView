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
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        RECENT_FILES, COPY_TO_DESTINATIONS, FAVORITES, CACHE_DIR, SETTINGS_DIR
    }

    /**
     * @param dataFileLocation Type of data file or folder being requested
     * @return Path to the data file or folder
     */
    public static String getDataFile(DataFileLocation dataFileLocation) {
        Path path = null;

        switch (dataFileLocation) {
            case SETTINGS_DIR:
                path = Paths.get(System.getProperty("user.home"), ".slimview");
                break;

            case CACHE_DIR:
                path = Paths.get(getDataFile(DataFileLocation.SETTINGS_DIR), "cache");
                break;

            case RECENT_FILES:
                path = Paths.get(getDataFile(DataFileLocation.SETTINGS_DIR), "recent.xml");
                break;

            case COPY_TO_DESTINATIONS:
                path = Paths.get(getDataFile(DataFileLocation.SETTINGS_DIR), "copy-to-destinations.xml");
                break;

            case FAVORITES:
                path = Paths.get(getDataFile(DataFileLocation.SETTINGS_DIR), "favorites.xml");
                break;
        }

        // create the file if it doesn't exist
        try {
            if (dataFileLocation == DataFileLocation.SETTINGS_DIR || dataFileLocation == DataFileLocation.CACHE_DIR) {
                Files.createDirectory(path);
            } else {
                Files.createFile(path);
            }
        } catch (FileAlreadyExistsException ignored) {

        } catch (IOException e) {
            e.printStackTrace();
        }

        return path.toString();
    }

    public static String inputStreamToString(InputStream inputStream) {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
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

                RecentFiles.RecentFile removeThis = null;
                for (RecentFiles.RecentFile r : recentFiles.getRecentFiles()) {
                    if (removeThis == null || removeThis.getLastSeen() > r.getLastSeen()) {
                        removeThis = r;
                    }
                }

                if (removeThis != null) {
                    recentFiles.getRecentFiles().remove(removeThis);
                }
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
     * @param <T>              Generic type definition
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
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                    | NoSuchMethodException e) {
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
                    if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                        try {
                            Desktop.getDesktop().browse(new URL(url).toURI());
                        } catch (IOException | URISyntaxException e) {
                            e.printStackTrace();
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

    public static void deleteDirectoryRecursively(String path) throws IOException {
        File root = new File(path);

        if (root.isDirectory()) {
            for (File file : root.listFiles()) {
                if (!file.isDirectory()) {
                    if (!file.delete()) {
                        throw new IOException();
                    }
                } else {
                    deleteDirectoryRecursively(file.getPath());
                }
            }
        }

        if (!root.delete()) {
            throw new IOException();
        }
    }

    public static String replaceFileExt(String filename, String newExt) {
        if (!filename.contains(".")) {
            return filename + "." + newExt;
        }

        return filename.substring(0, filename.length() - getFileExt(filename).length()) + newExt;
    }

    /**
     * Extracts and returns the extension of a given file.
     *
     * @param filename The name of or the path to the file.
     * @return The extension of the provided filename, without the period.
     */
    public static String getFileExt(String filename) {
        if (filename.contains(".")) {
            Pattern pattern = Pattern.compile("\\.([^.\\\\/]+$)");
            Matcher matcher = pattern.matcher(filename);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return "";
    }

    /**
     * Extracts the filename from a given path.
     *
     * @param filePath Full path to the file, e.g. /home/test/test.jpg
     * @return The name of the file including the extension, e.g. test.jpg
     */
    public static String getFileName(String filePath) {
        Pattern pattern = Pattern.compile("[\\\\{1,2}/]([^\\\\/]+$)");
        Matcher matcher = pattern.matcher(filePath);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return filePath;
    }

    /**
     * Writes a given string to a specified file. A new file will be created, replacing any existing one.
     *
     * @param out     The full path to the output file
     * @param content The content to write to the file
     * @throws IOException if a new file cannot be created, or it cannot be written to
     */
    public static void writeStringToFile(String out, String content) throws IOException {
        File file = new File(out);

        if (file.exists() && !file.isDirectory()) {
            if (!file.delete()) {
                throw new IOException("The existing file couldn't be deleted");
            }
        }

        if (!file.createNewFile()) {
            throw new IOException("The requested file couldn't be created");
        }

        try (PrintWriter printWriter = new PrintWriter(out)) {
            printWriter.print(content);
        }
    }

    public static String getAppVersion() {
        return Util.class.getModule().getDescriptor().version().get().toString();
    }
}
