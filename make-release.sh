MODULES=java.base,java.security.jgss,\
javafx.controls,javafx.graphics,javafx.fxml,javafx.base,javafx.swing,\
jdk.net,jdk.crypto.cryptoki,jdk.crypto.ec,\
com.fasterxml.jackson.core,com.fasterxml.jackson.dataformat.xml,\
imgscalr.lib,org.apache.httpcomponents.httpcore,xmpcore

printf "\nExecuting Maven goals\n"
mvn -B clean package

printf "\nMaking directories\n"
mkdir target/bin
mkdir target/bin/lib

printf "\nCopying files\n"
cp notice.txt target/bin
cp -r patched/. target/modules
rm target/modules/httpcore-4.4.13.jar
rm target/modules/imgscalr-lib-4.2.jar
rm target/modules/xmpcore-6.1.11.jar

printf "\nMaking new jlink image\n"
jlink --module-path target/modules \
	--add-modules $MODULES \
	--strip-debug \
	--no-header-files \
	--no-man-pages \
	--compress 2 \
	--output target/bin/runtime

printf "\nCopying dependencies\n"
cp -R target/modules/. target/bin/lib

printf "\nDone.\n"

./make-exe.sh