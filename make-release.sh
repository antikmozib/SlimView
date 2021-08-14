./build.sh
rm target/bin/slimview.exe
launch4jc launch4j-config.xml
iscc "installer/slimview.iss"
