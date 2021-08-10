MODULES=javafx.controls,javafx.graphics,javafx.fxml,javafx.base,javafx.swing,imgscalr.lib,org.apache.commons.io,com.fasterxml.jackson.core,com.fasterxml.jackson.dataformat.xml,metadata.extractor,org.apache.httpcomponents.httpclient,org.apache.httpcomponents.httpcore
target/linux/runtime/bin/java --module-path target/linux/lib --add-modules $MODULES -jar target/slimview-1.0.4.jar
