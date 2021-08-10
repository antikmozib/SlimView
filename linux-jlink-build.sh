MODULES=java.base,java.security.jgss,javafx.controls,javafx.graphics,javafx.fxml,javafx.base,javafx.swing,jdk.net,jdk.crypto.cryptoki,jdk.crypto.ec
mvn clean package
mkdir target/linux
mkdir target/linux/lib
cp notice.txt target/linux
jlink --module-path target/modules --add-modules $MODULES --output target/linux/runtime
cp -R target/modules/. target/linux/lib

