#!/bin/bash

./build.sh

MODULES=javafx.controls,javafx.graphics,javafx.fxml,javafx.base,javafx.swing,\
com.fasterxml.jackson.core,com.fasterxml.jackson.dataformat.xml,\
org.apache.commons.io,org.apache.commons.lang3,\
org.apache.httpcomponents.httpcore,org.apache.httpcomponents.httpclient,\
com.twelvemonkeys.imageio.core,com.twelvemonkeys.imageio.metadata,\
com.twelvemonkeys.imageio.jpeg,com.twelvemonkeys.imageio.bmp,com.twelvemonkeys.imageio.tiff,com.twelvemonkeys.imageio.psd,\
imgscalr.lib,metadata.extractor

# MODULES=$(cat all-mods.txt | perl -p -e 's/\n/,/g/s/\s//g')

if [[ "$OSTYPE" == "linux-gnu"* ]] || [[ "$OSTYPE" == "darwin"* ]]; then
	
	# macOS and Linux
	
	printf "\nMaking *nix executable\n"
	cp target/slimview-1.0.7.jar target/bin/
	echo "#!/bin/bash" > target/bin/slimview.sh
	echo "./runtime/bin/java --module-path lib --add-modules $MODULES -jar slimview-1.0.7.jar" '$1' >> target/bin/slimview.sh
	chmod +x target/bin/slimview.sh
	
elif [[ "$OSTYPE" == "cygwin"* ]]; then

	# Windows

	printf "\nMaking Windows executable\n"
	launch4jc launch4j-config.xml
	
fi
