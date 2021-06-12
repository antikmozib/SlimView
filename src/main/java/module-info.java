module io.mozib.simview {
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires org.apache.commons.io;
    requires transitive javafx.graphics;
	requires javafx.base;
    requires java.base;
    requires java.desktop;

    opens io.mozib.simview to javafx.fxml;
    exports io.mozib.simview;
}
