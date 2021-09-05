#!/bin/bash
MODULES=$(cat all-mods.txt | perl -p -e 's/\n/,/g/s/\s//g')

./build.sh

if [[ "$OSTYPE" == "linux-gnu"* ]] || [[ "$OSTYPE" == "darwin"* ]]; then
	
	# macOS and Linux
	
	printf "\nMaking *nix executable\n"
	cp target/slimview-1.0.7.jar target/bin/
	echo "#!/bin/bash" > target/bin/slimview.sh
	echo "./runtime/bin/java --module-path lib --add-modules $MODULES -jar slimview-1.0.7.jar $1" >> target/bin/slimview.sh
	chmod +x target/bin/slimview.sh
	
elif [[ "$OSTYPE" == "cygwin"* ]]; then

	# Windows

	printf "\nMaking Windows executable\n"
	launch4jc launch4j-config.xml
	
fi
