mkdir -p jar
mkdir -p lib
mkdir -p get_libs
cd get_libs

if [ ! -e NeticaJ_Linux.zip ]; then
    wget http://norsys.com/downloads/NeticaJ_Linux.zip
    unzip NeticaJ_Linux.zip
fi

echo Copying Netica libs
cp NeticaJ_*/bin/NeticaJ.jar ../jar
cp NeticaJ_*/bin/lib* ../lib

echo Copying Tetrad
