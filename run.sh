MODULES=javafx.controls,javafx.graphics,javafx.fxml,javafx.base,javafx.swing,\
com.fasterxml.jackson.core,com.fasterxml.jackson.dataformat.xml,\
org.apache.httpcomponents.httpclient,org.apache.httpcomponents.httpcore,org.apache.commons.io,\
imgscalr.lib,metadata.extractor

java 	--module-path target/modules \
		--add-modules $MODULES \
		-jar target/slimview-1.0.5.jar
