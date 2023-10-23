/*
 * Copyright (C) 2021-2023 Antik Mozib. All rights reserved.
 */

package io.mozib.slimview;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class TestUtil {

    @Test
    public void testComplicatedExtension(){
        assertEquals("zip", Util.getFileExt("t.t.t.t.t.t.t.zip"));
    }

    @Test
    public void testGetFileExt() {
        assertEquals("jpg", Util.getFileExt("test.bmp.jpg"));
        assertEquals("", Util.getFileExt("Test.This Folder\\test"));
        assertEquals("", Util.getFileExt("test"));
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

    @Test //Nicholas Levergne's test
    public void testDeleteException() throws IOException {
        File file = null;
        assertThrows(NullPointerException.class, ()->{file.delete();});
    }
}
