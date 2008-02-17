#
# This script downloads several libraries/executables which really shouldn't be included in CVS
# Ideally they would be downloaded from the appropriate source, but are provided here for
# convenience.  I am unsure of the legality of mirroring these files and will remove them if
# requested to do so.
#
# Original Sources:
# http://www.cs.huji.ac.il/labs/compbio/LibB/
# http://www.norsys.com/
# http://www.junit.org/
# http://www.cs.waikato.ac.nz/ml/weka/
# http://www.phil.cmu.edu/projects/tetrad_download/
#

# Files associated with friedman's program have been removed
# as they are large and rarely used. Uncommend #FRIEDMAN lines
# to replace.

# Grab all necesarry files from rodo's webpage.
echo ---- DOWNLOADING FILES: This may take a while. ----
wget -nv \
    http://www.csse.monash.edu.au/~rodo/personal/files/libnetica.so  \
    http://www.csse.monash.edu.au/~rodo/personal/files/junit.jar     \
    http://www.csse.monash.edu.au/~rodo/personal/files/weka.jar      \
    http://www.csse.monash.edu.au/~rodo/personal/files/tetradcmd-4.3.3-3.jar \
    http://www.csse.monash.edu.au/~rodo/personal/files/libNeticaJ.so \
    http://www.csse.monash.edu.au/~rodo/personal/files/NeticaJ.jar   \
#FRIEDMAN     http://www.csse.monash.edu.au/~rodo/personal/files/GenInstance   \
#FRIEDMAN     http://www.csse.monash.edu.au/~rodo/personal/files/LearnBayes    \
#FRIEDMAN     http://www.csse.monash.edu.au/~rodo/personal/files/ScoreNet      \
#FRIEDMAN     http://www.csse.monash.edu.au/~rodo/personal/files/libstdc++.so.3 \

# Set permissions
#FRIEDMAN chmod 774 GenInstance
#FRIEDMAN chmod 774 LearnBayes
#FRIEDMAN chmod 774 ScoreNet

# Create the required directories.
mkdir lib jar bin

# Move files to appropriate part of directory structure
# Friedman's LearnBayes program
#FRIEDMAN mv GenInstance bin
#FRIEDMAN mv LearnBayes bin
#FRIEDMAN mv ScoreNet bin

# Various netica libraries
mv libNeticaJ.so lib
mv libnetica.so lib
mv NeticaJ.jar jar
mv tetradcmd-4.3.3-3.jar jar/tetrad4.jar

# Friedman's programs don't work without the appropriate version of libstdc 
#FRIEDMAN mv libstdc++.so.3 lib

# Junit testing suits
mv junit.jar jar

# Weka, a machine learning package.
mv weka.jar jar
