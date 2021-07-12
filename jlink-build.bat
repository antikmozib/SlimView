call mvn clean package
copy acknowledgements.txt target\acknowledgements.txt
rmdir /s /q jlink-image
jlink --module-path "%PATH_TO_FX_MODS%" --add-modules javafx.controls,javafx.graphics,java.base,java.desktop,javafx.fxml,javafx.base,javafx.swing,java.se,jdk.net,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.crypto.mscapi --output jlink-image