call mvn clean package
mkdir target\win
copy notice.txt target\win\notice.txt
rmdir /s /q target\win\jlink-image
jlink --module-path "%PATH_TO_FX_MODS%" --add-modules javafx.controls,javafx.graphics,java.base,java.desktop,javafx.fxml,javafx.base,javafx.swing,java.se,jdk.net,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.crypto.mscapi --output target\win\jlink-image