#!/bin/bash
./make-executable.sh

if [ "$OSTYPE" == "cygwin" ]; then

	printf "\nBuilding installer\n"
	iscc "installer/slimview.iss"
	
fi
