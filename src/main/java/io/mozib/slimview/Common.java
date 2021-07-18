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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Common {
    public enum OSType {
        Windows, Mac, Linux
    }

    public static OSType getOSType() {
        String platform = System.getProperty("os.name").toLowerCase();
        if (platform.contains("win")) {
            return OSType.Windows;
        } else if (platform.contains("mac")) {
            return OSType.Mac;
        } else if (platform.contains("nux")) {
            return OSType.Linux;
        }
        return null;
    }

    public enum SettingFileType {
        RECENT_FILES, COPY_TO_DESTINATIONS
    }

    /**
     * @return Path to the xml settings file
     */
    public static String getSettingsFile(SettingFileType settingFileType) {
        createSettingsDir();
        Path path = Paths.get(System.getProperty("user.home"), ".slimview");

        switch (settingFileType) {
            case RECENT_FILES:
                path = Paths.get(path.toString(), "recent.xml");
                break;
            case COPY_TO_DESTINATIONS:
                path = Paths.get(path.toString(), "copytodestinations.xml");
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
            } catch (Exception e) {
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
        XmlMapper xmlMapper = new XmlMapper();
        RecentFiles recentFiles = Common.loadDataFile(RecentFiles.class, SettingFileType.RECENT_FILES);

        if (recentFiles.recentFileList == null) {
            recentFiles.recentFileList = new ArrayList<>();
        }

        // if file already exists in recent, don't add again
        for (RecentFiles.RecentFile rf : recentFiles.recentFileList) {
            if (rf.getPath().equals(path)) {
                return;
            }
        }

        if (recentFiles.recentFileList.size() >= 5) {
            long oldestSeen = System.currentTimeMillis();
            for (RecentFiles.RecentFile rf : recentFiles.recentFileList) {
                if (rf.getLastSeen() < oldestSeen) {
                    oldestSeen = rf.getLastSeen();
                }
            }
            RecentFiles.RecentFile remove = null;
            for (RecentFiles.RecentFile rf : recentFiles.recentFileList) {
                if (rf.getLastSeen() == oldestSeen) {
                    remove = rf;
                    break;
                }
            }
            recentFiles.recentFileList.remove(remove);
        }
        RecentFiles.RecentFile newRecent = new RecentFiles.RecentFile();
        newRecent.setPath(path);
        recentFiles.recentFileList.add(newRecent);
        try {
            xmlMapper.writeValue(new File(getSettingsFile(SettingFileType.RECENT_FILES)), recentFiles);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Required class to enable copying image to the system clipboard
     */
    public static class ImageTransferable implements Transferable {
        private final Image image;

        public ImageTransferable(Image image) {
            this.image = image;
        }

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (isDataFlavorSupported(flavor)) {
                return image;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor == DataFlavor.imageFlavor;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }
    }

    /**
     * @param classType The type of object the xml mapper will map to
     * @param settingFileType The location of the setting file, determined through its enum
     * @param <T> Generic type
     * @return Data read from xml file and mapped to a JavaBean
     */
    public static <T> T loadDataFile(Class<T> classType, SettingFileType settingFileType) {
        XmlMapper xmlMapper = new XmlMapper();
        String xml;
        T data = null;

        try {
            xml = inputStreamToString(new FileInputStream(getSettingsFile(settingFileType)));
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
}
