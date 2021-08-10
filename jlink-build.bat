SET modules=java.base,java.security.jgss,javafx.controls,javafx.graphics,javafx.fxml,javafx.base,javafx.swing,jdk.net,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.crypto.mscapi

call mvn clean package

echo.
echo Making directories
echo.
mkdir target\win
mkdir target\win\lib

echo.
echo Copying files
echo.
copy notice.txt target\win\notice.txt

echo.
echo Making new jlink image
echo.
jlink --module-path target\modules --add-modules %modules% --output target\win\runtime

echo.
echo Copy dependencies
echo.
xcopy /E target\modules target\win\lib

echo.
echo Done.