@echo off
SET modules=javafx.controls,javafx.graphics,javafx.fxml,javafx.base,javafx.swing,imgscalr.lib,org.apache.commons.io,com.fasterxml.jackson.core,com.fasterxml.jackson.dataformat.xml,metadata.extractor,org.apache.httpcomponents.httpclient,org.apache.httpcomponents.httpcore

SET param=target\win\runtime\bin\javaw --module-path target\modules --add-modules %modules% -jar target\slimview-1.0.5.jar %*

REM %command%
start %param%