#!/bin/bash

## Script initialised the local environment with CaMML environemtne vars
##  then calls remaining args.
## Useful for running camml in a sctiped environment, etc.
## e.g., 
##   ./with_camml.sh jython

JVM_OPTS="-Xmx1024m -Dnetica.reg=$NETICAREG"
CAMML_HOME=`pwd`
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$CAMML_HOME/lib
export CLASSPATH=$CAMML_HOME/Camml:$CAMML_HOME/CDMS:jar/*
export PYTHONPATH=$PYTHONPATH:$CAMML_HOME/jython/lib/
$*
