java -Xrunhprof:cpu=samples,depth=20 -classpath Camml:CDMS:$CLASSPATH -Xmx512m cdms.core.Cdms cdms.plugin.fpli.Fpli\$Interpreter Camml/script/cammlBootstrap.fp
