MODULES=java.base,java.security.jgss,javafx.controls,javafx.graphics,javafx.fxml,javafx.base,javafx.swing,jdk.net,jdk.crypto.cryptoki,jdk.crypto.ec

printf "\nExecuting Maven goals\n"
mvn -B clean package

printf "\nMaking directories\n"
mkdir target/linux
mkdir target/linux/lib

printf "\nCopying files\n"
cp notice.txt target/linux

printf "\nMaking new jlink image\n"
jlink --module-path target/modules --add-modules $MODULES --output target/linux/runtime

printf "\nCopying dependencies\n"
cp -R target/modules/. target/linux/lib

printf "\nDone.\n"
