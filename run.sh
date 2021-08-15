MODULES=javafx.controls,javafx.graphics,javafx.fxml,javafx.base,javafx.swing,\
com.fasterxml.jackson.core,com.fasterxml.jackson.dataformat.xml,\
org.apache.httpcomponents.httpcore,org.apache.httpcomponents.httpclient,org.apache.commons.io,\
imgscalr.lib,metadata.extractor

target/bin/runtime/bin/java --module-path target/bin/lib \
	--add-modules $MODULES \
	-jar target/slimview-1.0.5.jar