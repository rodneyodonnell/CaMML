echo Bootstrap = $1
time java -server -Xms128m -Xmx512m -Dnetica.reg=$NETICAREG -Xfuture -Xbootclasspath/p:./cdms/bootclass cdms.core.Cdms cdms.plugin.fpli.Fpli\$Interpreter $1
