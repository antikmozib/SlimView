module io.mozib.simview {
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires org.apache.commons.io;
    requires transitive javafx.graphics;
    requires javafx.base;
    requires java.base;
    requires java.desktop;
    requires imgscalr.lib;
    requires javafx.swing;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.xml;

    opens io.mozib.simview to javafx.fxml, com.fasterxml.jackson.databind;
    exports io.mozib.simview;
}
