module io.mozib.simview {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.commons.io;
    requires transitive javafx.graphics;
	requires javafx.base;

    opens io.mozib.simview to javafx.fxml;
    exports io.mozib.simview;
}
