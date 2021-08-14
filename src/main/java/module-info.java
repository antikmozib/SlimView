module io.mozib.slimview {
    requires java.base;
    requires java.prefs;
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
    requires javafx.fxml;
    requires javafx.base;
    requires javafx.swing;
    requires org.apache.commons.io;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.httpcomponents.httpclient;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.dataformat.xml;
    requires imgscalr.lib;
    requires metadata.extractor;

    opens io.mozib.slimview to javafx.fxml;
    exports io.mozib.slimview;
}
