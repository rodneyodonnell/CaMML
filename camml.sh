for JAR in jar/*.jar; do JARS=$JARS:$JAR; done
java -Dnetica.reg=$NETICAREG -classpath Camml:CDMS:$JARS -Xmx512m cdms.core.Cdms cdms.plugin.fpli.Fpli\$Interpreter Camml/script/cammlBootstrap.fp
