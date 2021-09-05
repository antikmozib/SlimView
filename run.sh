#!/bin/bash
MODULES=$(cat all-mods.txt | perl -p -e 's/\n/,/')

# Excluded: com.twelvemonkeys.imageio.batik,xmlgraphics.commons

./target/bin/runtime/bin/java --module-path target/bin/lib \
	--add-modules $MODULES \
	-jar target/slimview-1.0.7.jar $1
