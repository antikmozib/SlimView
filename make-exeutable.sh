MODULES=javafx.controls,javafx.graphics,javafx.fxml,javafx.base,javafx.swing,\
com.fasterxml.jackson.core,com.fasterxml.jackson.dataformat.xml,\
org.apache.commons.io,org.apache.commons.lang3,\
org.apache.httpcomponents.httpcore,org.apache.httpcomponents.httpclient,\
com.twelvemonkeys.imageio.core,com.twelvemonkeys.imageio.metadata,\
com.twelvemonkeys.imageio.jpeg,com.twelvemonkeys.imageio.bmp,com.twelvemonkeys.imageio.tiff,com.twelvemonkeys.imageio.psd,\
imgscalr.lib,metadata.extractor

./build.sh

if [[ "$OSTYPE" == "linux-gnu"* ]]; then
	
	printf "\nMaking *nix executable\n"
	cp target/slimview-1.0.7.jar target/bin/
	echo "#!/bin/bash" > target/bin/slimview.sh
	echo "runtime/bin/java --module-path lib --add-modules $MODULES -jar slimview-1.0.7.jar $1" >> target/bin/slimview.sh
	chmod +x target/bin/slimview.sh
	
elif [[ "$OSTYPE" == "cygwin" ]]; then

	printf "\nMaking Windows executable\n"
	launch4jc launch4j-config.xml
	
fi
