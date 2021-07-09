call mvn clean package
copy acknowledgements.txt target\acknowledgements.txt
rmdir /s /q jlink-image
jlink --module-path "%PATH_TO_FX_MODS%" --add-modules javafx.controls,javafx.graphics,java.base,java.desktop,javafx.fxml,javafx.base,javafx.swing,java.se --output jlink-image
REM start jlink-image\bin\javaw -jar "target\slimview-1.0.jar"
start jlink-image\bin\javaw -jar "target\slimview-1.0.jar"