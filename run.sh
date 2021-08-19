MODULES=javafx.controls,javafx.graphics,javafx.fxml,javafx.base,javafx.swing,\
com.fasterxml.jackson.core,com.fasterxml.jackson.dataformat.xml,\
org.apache.httpcomponents.httpcore,org.apache.httpcomponents.httpclient,org.apache.commons.io,\
com.twelvemonkeys.imageio.core,com.twelvemonkeys.imageio.metadata,\
com.twelvemonkeys.imageio.jpeg,com.twelvemonkeys.imageio.bmp,com.twelvemonkeys.imageio.tiff,com.twelvemonkeys.imageio.psd\
imgscalr.lib,metadata.extractor

# Excluded: com.twelvemonkeys.imageio.batik,xmlgraphics.commons

target/bin/runtime/bin/java --module-path target/bin/lib \
	--add-modules $MODULES \
	-jar target/slimview-1.0.6.jar
