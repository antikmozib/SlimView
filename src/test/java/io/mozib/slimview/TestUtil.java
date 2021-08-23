package io.mozib.slimview;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUtil {
    @Test
    public void testGetFileExt() {
        assertEquals("jpg", Util.getFileExt("test.bmp.jpg"));
        assertEquals("", Util.getFileExt("test"));
    }
}
