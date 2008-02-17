echo off
REM ## Script to compile camml under windoes

cd ..

REM Set Classpath
set cpath=Camml;CDMS;jar\NeticaJ.jar;jar\junit.jar;jar\weka.jar;.\;jar\tetrad4.jar

REM Compile all java files.
REM There must be a better way of doing this...

echo Compiling CDMS
javac -source 1.4 -classpath %cpath% CDMS\cdms\core\*.java
javac -source 1.4 -classpath %cpath% CDMS\cdms\plugin\bean\*.java
javac -source 1.4 -classpath %cpath% CDMS\cdms\plugin\c5\*.java
javac -source 1.4 -classpath %cpath% CDMS\cdms\plugin\desktop\*.java
javac -source 1.4 -classpath %cpath% CDMS\cdms\plugin\dialog\*.java
javac -source 1.4 -classpath %cpath% CDMS\cdms\plugin\enview\*.java
javac -source 1.4 -classpath %cpath% CDMS\cdms\plugin\fpli\*.java
javac -source 1.4 -classpath %cpath% CDMS\cdms\plugin\latex\*.java
javac -source 1.4 -classpath %cpath% CDMS\cdms\plugin\ml\*.java
javac -source 1.4 -classpath %cpath% CDMS\cdms\plugin\mml87\*.java
javac -source 1.4 -classpath %cpath% CDMS\cdms\plugin\model\*.java
javac -source 1.4 -classpath %cpath% CDMS\cdms\plugin\search\*.java
javac -source 1.4 -classpath %cpath% CDMS\cdms\plugin\twodplot\*.java
javac -source 1.4 -classpath %cpath% CDMS\java\util\*.java
javac -source 1.4 -classpath %cpath% CDMS\test\cdms\plugin\*.java

echo Compiling Camml
javac -source 1.4 -classpath %cpath% Camml\camml\core\library\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\core\models\bNet\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\core\models\cpt\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\core\models\dTree\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\core\models\dual\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\core\models\mixture\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\core\models\multinomial\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\core\models\normal\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\core\models\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\core\search\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\plugin\augment\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\plugin\friedman\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\plugin\netica\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\plugin\rmi\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\plugin\rodoCamml\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\plugin\scripter\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\plugin\tetrad4\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\plugin\weka\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\plugin\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\test\core\library\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\test\core\models\bNet\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\test\core\models\cpt\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\test\core\models\dTree\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\test\core\models\dual\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\test\core\models\mixture\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\test\core\models\multinomial\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\test\core\models\normal\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\test\core\models\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\test\core\search\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\test\core\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\test\plugin\friedman\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\test\plugin\netica\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\test\plugin\rmi\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\test\plugin\rodoCamml\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\test\plugin\tetrad4\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\test\plugin\weka\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\test\plugin\*.java
javac -source 1.4 -classpath %cpath% Camml\camml\test\*.java

