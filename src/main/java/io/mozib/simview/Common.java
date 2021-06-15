package io.mozib.simview;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    public static String cacheDirectory() {
        Path path = Paths.get(System.getProperty("user.home"), ".simview", "cache");
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Paths.get(System.getProperty("user.home"), ".simview", "cache").toString();
    }

    public static String recentFilesCache() {
        Path path = Paths.get(System.getProperty("user.home"), ".simview", "recent.xml");
        // create file if not exists
        try {
            new File(path.toString()).createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path.toString();
    }

    public static String inputStreamToString(InputStream is) {
        StringBuilder sb = new StringBuilder();
        String line = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        try {
            while (true) {

                if (!((line = br.readLine()) != null)) break;

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
        RecentFiles recentFiles = loadRecentFiles();

        assert false;
        if (recentFiles.recentFileList.size() > 5) {
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
        newRecent.setNew(path);
        recentFiles.recentFileList.add(newRecent);
        try {
            xmlMapper.writeValue(new File(recentFilesCache()), recentFiles);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static RecentFiles loadRecentFiles() {
        // load recent files
        XmlMapper xmlMapper = new XmlMapper();
        String xml = null;
        try {
            xml = inputStreamToString(new FileInputStream(recentFilesCache()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        RecentFiles recentFiles = null;

        try {
            recentFiles = xmlMapper.readValue(xml, RecentFiles.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        if (recentFiles == null || recentFiles.recentFileList == null) {
            recentFiles = new RecentFiles();
        }

        return recentFiles;
    }
}
