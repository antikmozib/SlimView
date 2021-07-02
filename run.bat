call mvn clean package
copy acknowledgements.txt target\acknowledgements.txt
REM start javaw -jar "target\slimview-1.0.jar"
start javaw -jar "target\slimview-1.0.jar"