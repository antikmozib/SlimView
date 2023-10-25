/*
 * Copyright (C) 2021-2023 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import javafx.event.ActionEvent;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TestUtil {

    @Test
    public void testAspectRatio(){
        // could not write test for this yikes
    }
    
    @Test //Sadie Forbes
    public void testComplicatedExtension(){
        assertEquals("zip", Util.getFileExt("t.t.t.t.t.t.t.zip"));
    }

    @Test //Justin Woodring
    void testAppServiceConstructor() {
        AppUpdateService service = new AppUpdateService("testName", "testVersion");
        Assertions.assertInstanceOf(AppUpdateService.class, service);
    }

    @Test //Justin Woodring
    public void testImageFileSize()
    {
        ImageModel img = new ImageModel("src/main/resources/io/mozib/slimview/icons/save.png");
        assertEquals(img.getFileSize(), 256);
    }

    @Test
    public void testGetFileExt() {
        assertEquals("jpg", Util.getFileExt("test.bmp.jpg"));
        assertEquals("", Util.getFileExt("Test.This Folder\\test"));
        assertEquals("", Util.getFileExt("test"));
        assertEquals("png", Util.getFileExt("test.photo.png"));
    }

    @Test
    public void testReplaceFileExt() {
        assertEquals("test.bmp.jpg", Util.replaceFileExt("test.bmp.gif", "jpg"));
        assertEquals("test.jpg", Util.replaceFileExt("test", "jpg"));
    }

    @Test
    public void testGetFileName() {
        assertEquals("test.jpg", Util.getFileName("C:\\\\Test Folder\\\\test.jpg"));
        assertEquals("test.jpg", Util.getFileName("C:\\Test Folder\\test.jpg"));
        assertEquals("test.jpg", Util.getFileName("C:/Test Folder/test.jpg"));
        assertEquals("test.jpg", Util.getFileName("test.jpg"));
    }

    @Test
    public void testWriteStringToFile() throws IOException {
        File file = new File("test.txt");
        String content = "The quick brown fox\nJumps over the lazy dog";
        file.delete();

        assertFalse(file.exists());
        Util.writeStringToFile(file.getPath(), content);
        assertEquals(file.length(), content.length());

        file.delete();
    }

    @Test
    public void overwriteStringToFile() throws IOException {
        File file = new File("test.txt");
        String content1 = "Some text here";
        Util.writeStringToFile(file.getPath(), content1);
        assertEquals(file.length(), content1.length());
        String content2 = "New text here instead";
        Util.writeStringToFile(file.getPath(), content2);
        assertEquals(file.length(), content2.length());
    }

    @Test //Nicholas Levergne's test
    public void testDeleteException() throws IOException {
        File file = null;
        assertThrows(NullPointerException.class, ()->{file.delete();});
    }

    @Test
    public void testGetOSType() {
        assertEquals(Util.getOSType(), Util.OSType.MAC);
    }

    @Test
    public void CalvinsCoolTestCase()
    {
        ImageModel img = new ImageModel("src/main/resources/io/mozib/slimview/icons/save.png");
        assertEquals(img.getPath(), "src/main/resources/io/mozib/slimview/icons/save.png");
    }

    @Test
    public void confirmingFavoritesBugFix()
    {
        MainViewModel mainViewModel = new MainViewModel();
        ImageModel imageModel = null;
        assertThrows(NullPointerException.class, ()->{mainViewModel.setAsFavorite(imageModel, true);});
    }

    @Test
    public void zoomWithNoPictureTest() {
        MainViewModel mainViewModel = new MainViewModel();
        MainWindowController mainWindowController = new MainWindowController();

        ActionEvent actionEvent = new ActionEvent();
        assertDoesNotThrow(() -> mainWindowController.buttonZoomIn_onAction(actionEvent));
    }
}
