#!/bin/bash

# modules which have a module descriptor and can be embedded into the custom jre
JLINK_MODULES=java.base,java.security.jgss,\
javafx.controls,javafx.graphics,javafx.fxml,javafx.base,javafx.swing,\
jdk.net,jdk.crypto.cryptoki,jdk.crypto.ec,\
com.fasterxml.jackson.core,com.fasterxml.jackson.dataformat.xml

# all modules required for running the app
ALL_MODULES=javafx.controls,javafx.graphics,javafx.fxml,javafx.base,javafx.swing,\
org.apache.commons.io,org.apache.commons.lang3,\
org.apache.httpcomponents.httpcore,org.apache.httpcomponents.httpclient,\
com.fasterxml.jackson.core,com.fasterxml.jackson.dataformat.xml,\
com.twelvemonkeys.imageio.core,com.twelvemonkeys.imageio.metadata,\
com.twelvemonkeys.imageio.jpeg,com.twelvemonkeys.imageio.bmp,\
com.twelvemonkeys.imageio.tiff,com.twelvemonkeys.imageio.psd,\
imgscalr.lib,metadata.extractor

printf "\nExecuting Maven goals\n"
mvn -B clean package

printf "\nMaking directories\n"
mkdir target/bin
mkdir target/bin/lib

printf "\nCopying files\n"
cp notice.txt target/bin

printf "\nMaking new jlink image\n"
jlink --module-path target/modules \
	--add-modules $JLINK_MODULES \
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

while getopts "erl" opt; do
	case $opt in
		e) # make executable

			if [[ "$OSTYPE" == "linux-gnu"* ]] || [[ "$OSTYPE" == "darwin"* ]]; then

				# macOS and Linux

				printf "\nMaking *nix executable\n"
				cp target/slimview-1.0.7.jar target/bin/
				echo "#!/bin/bash" > target/bin/slimview.sh
				echo "./runtime/bin/java --module-path lib --add-modules $ALL_MODULES -jar slimview-1.0.7.jar" '$1' >> target/bin/slimview.sh
				chmod +x target/bin/slimview.sh

			elif [[ "$OSTYPE" == "cygwin"* ]]; then

				# Windows

				printf "\nMaking Windows executable\n"
				launch4jc launch4j-config.xml

			fi
			;;

		r) # make release

			if [[ "$OSTYPE" == "cygwin"* ]]; then
				printf "\nBuilding installer\n"
				iscc "installer/slimview.iss"	
			fi
			;;

		l) # launch

			./target/bin/runtime/bin/java --module-path target/bin/lib \
				--add-modules $ALL_MODULES \
				-jar target/slimview-1.0.7.jar
			;;
	esac
done

printf "\nDone.\n"