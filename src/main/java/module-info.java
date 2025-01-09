module io.mozib.slimview {
    requires java.prefs;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.swing;
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.httpcomponents.httpclient;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.dataformat.xml;
    requires com.twelvemonkeys.imageio.core;
    requires com.twelvemonkeys.imageio.metadata;
    requires com.twelvemonkeys.imageio.bmp;
    requires com.twelvemonkeys.imageio.jpeg;
    requires com.twelvemonkeys.imageio.tiff;
    requires com.twelvemonkeys.imageio.psd;
    requires imgscalr.lib;
    requires metadata.extractor;

    opens io.mozib.slimview to javafx.fxml;
    exports io.mozib.slimview;
}
