REM Batch file to run CaMML under windows
cd ..
path=%path%;lib
echo %path%
java -Xmx512m -Djava.library.path=lib -classpath Camml;CDMS;jar\NeticaJ.jar;jar\junit.jar;jar\weka.jar;.\;jar\tetrad4.jar cdms.core.Cdms cdms.plugin.fpli.Fpli$Interpreter Camml\script\cammlBootstrap.fp
