#!/bin/bash

export DEFAULT_WS="./tempWS"

if [ $# == 1 ]
then
    OPT=""
    DIR_PROJ=${1}
else
    OPT=""
    while [ $# -gt 1 ]
    do
		OPT="$OPT ${1}"
	shift
    done
    DIR_PROJ=${1}
fi

rm -rf ${DEFAULT_WS}

java -cp ../plugins/org.eclipse.equinox.launcher_1.3.100.v20150511-1540.jar  -Xmx4181721088 -Xss1024m  org.eclipse.equinox.launcher.Main -consoleLog -application verveine.extractor.Cpp.Main ${OPT} -includeconf includes.vconf -o default.mse ${DIR_PROJ} | tee verveineC.log 2>&1
