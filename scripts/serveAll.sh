# Firstly define which nodes are to be used and the root of the output directory.
NODES=`cat $CAMML_ROOT/scripts/rmi.nodes | egrep -v \#`
SERVER_ROOT=`pwd`/server

# Firstly make sure any old servers are killed.  This is VERY ugly but I'm not sure of a better way.
echo -- Ensuring old servers are dead
for X in $NODES
  do
  echo $X : ; ssh $X "killall -9 -q java rmiregistry; "
done



# Make sure each node has a working directory.
# This allows programes (LearneBayes,rodoCamml,etc) to dump temporary files without concern of
# a process on a different machine reading them (though we still have to be careful if allowing
# multiple processes to run on the same machine.)
echo -- Creating working directories
rm -rf $SERVER_ROOT; mkdir $SERVER_ROOT
for X in $NODES
  do
  rm -rf $SERVER_ROOT/$X
  mkdir $SERVER_ROOT/$X
done


# Now we create new processes.  Each runs in it's working directory and outputs 
# all messages to stdout and stderr in it's own directory.
echo -- Creating new servers. Please wait 10 seconds before using
for X in $NODES
  do
#  ssh $X "cd CAMML; source bashrc;  cd \$SERVER_ROOT/\$X; java -server -Xmx200m camml.plugin.rmi.CDMSEngine server localhost" > $SERVER_ROOT/$X/stdout 2> $SERVER_ROOT/$X/stderr&
  ssh $X "cd CAMML; source bashrc;  echo Writing files to : $SERVER_ROOT/$X; cd $SERVER_ROOT/$X; java -Xmx1000m camml.plugin.rmi.CDMSEngine server localhost" > $SERVER_ROOT/$X/stdout 2> $SERVER_ROOT/$X/stderr&
done

# tail -f $SERVER_ROOT/*
