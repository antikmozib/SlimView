module io.mozib.slimview {
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    requires java.base;
    requires java.desktop;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.swing;
    requires imgscalr.lib;
    requires org.apache.commons.io;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.xml;
    requires java.prefs;
    requires metadata.extractor;

    opens io.mozib.slimview to javafx.fxml, com.fasterxml.jackson.databind;
    exports io.mozib.slimview;
}
