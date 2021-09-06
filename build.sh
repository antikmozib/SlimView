#!/bin/bash

MODULES=java.base,java.security.jgss,\
javafx.controls,javafx.graphics,javafx.fxml,javafx.base,javafx.swing,\
jdk.net,jdk.crypto.cryptoki,jdk.crypto.ec,\
com.fasterxml.jackson.core,com.fasterxml.jackson.dataformat.xml

printf "\nExecuting Maven goals\n"
mvn -B clean package

printf "\nMaking directories\n"
mkdir target/bin
mkdir target/bin/lib

printf "\nCopying files\n"
cp notice.txt target/bin

printf "\nMaking new jlink image\n"
jlink --module-path target/modules \
	--add-modules $MODULES \
	--strip-debug \
	--no-header-files \
	--no-man-pages \
	--compress 2 \
	--output target/bin/runtime

printf "\nCopying dependencies\n"
# for file in target/modules/batik-*; 			do cp "$file" "target/bin/lib"; done
for file in target/modules/common-*; 			do cp "$file" "target/bin/lib"; done
for file in target/modules/commons-*; 			do cp "$file" "target/bin/lib"; done
for file in target/modules/imageio-*; 			do cp "$file" "target/bin/lib"; done
cp target/modules/httpclient-4.5.13.jar 		target/bin/lib
cp target/modules/httpcore-4.4.13.jar 			target/bin/lib
cp target/modules/imgscalr-lib-4.2.jar 			target/bin/lib
cp target/modules/metadata-extractor-2.16.0.jar	target/bin/lib
cp target/modules/xmpcore-6.1.11.jar 			target/bin/lib

printf "\nDone.\n"
