JVM_OPTS="-Xmx512m -Dnetica.reg=$NETICAREG"
CAMML_HOME=`pwd`
for JAR in $CAMML_HOME/jar/*.jar; do JARS=$JARS:$JAR; done
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$CAMML_HOME/lib

java $JVM_OPTS -classpath Camml:CDMS:$JARS camml.core.newgui.RunGUI