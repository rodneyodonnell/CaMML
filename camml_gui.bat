REM Batch file to run CaMML GUI under windows
path=%path%;jar
java -Xmx512m -Djava.library.path=jar -classpath Camml;jar\cdms.jar;jar\NeticaJ.jar;jar\junit.jar;jar\weka.jar;.\;jar\tetrad4.jar camml.core.newgui.RunGUI