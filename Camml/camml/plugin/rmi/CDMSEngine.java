/*
 *  [The "BSD license"]
 *  Copyright (c) 2002-2011, Rodney O'Donnell, Lloyd Allison, Kevin Korb
 *  Copyright (c) 2002-2011, Monash University
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products
 *       derived from this software without specific prior written permission.*
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

//
// RMI interface to CDMS
//

// File: CDMSEngine.java
// Author: rodo@dgs.monash.edu.au

package camml.plugin.rmi;

import java.rmi.RemoteException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Naming;

import java.io.*;
import java.util.ArrayList;

import cdms.core.*;

/**
 * RMI Interface to CDMS.
 * Client and server code is jumbled together and should probably be fixed.
 * 
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.10 $ $Date: 2006/08/22 03:13:34 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/plugin/rmi/CDMSEngine.java,v $
 */
public class CDMSEngine {

    public static class EngineModule extends Module {
        public static java.net.URL helpURL = Module
            .createStandardURL(EngineModule.class);

        public String getModuleName() {
            return "RMI";
        }

        public java.net.URL getHelp() {
            return helpURL;
        }

        public void install(Value params) throws Exception {
            add("fib", CDMSEngine.fib, "Calculate fibonacci naively.");
            add("rmap", CDMSEngine.rmap, "Run map on a remote server");
        }
    }

    /**
     * Should be run as java CDMSEngin [client|server] <br>
     * The appropriate object type is created.
     */
    public static void main(String[] args) throws java.rmi.RemoteException {

        // Set name of server.
        String server = (args.length < 2) ? "localhost:1099" : args[1];
        String name = "rmi://" + server + "/CalculatorService";

        if (args.length >= 1 && args[0].equals("client")) {
            new Client(name).runTest();
        } else if (args.length >= 1 && args[0].equals("server")) {
            new Server(name);
        } else {
            System.out.println("Invalid options.");
            System.out
                .println("try [client|server] rmi://localhost:1099/CalculatorService");
            System.out.println(args[1]);
        }
    }

    /** Create a permanently running server object. */
    public static class Server {

        /**
         * Create a new Calculator object and bind it to
         * "rmi://localhost:1099/CalculatorService"
         */
        public Server(String name) {
            try {
                // // Create a registry for clients to connect to.
                java.rmi.registry.LocateRegistry.createRegistry(1099);

                // System.out.println("Making FunctionRunner");
                FunctionRunner f = new FunctionRunner();
                // System.out.println("Binding");
                System.out.println("Server Running...");
                Naming.rebind(name, f);
            } catch (Exception e) {
                System.out.println("Trouble: " + e);
            }
            System.out.println("Exiting server constructor.");
        }
    }

    /** Class to run CDMS functions and return the results. */
    public static class FunctionRunner extends
                                           java.rmi.server.UnicastRemoteObject implements
                                                                                   FunctionRunnerInterface, EnvironmentUpdateInterface {

        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 8700867388719564298L;

        /** Explicit constructor required due to throws clause. */
        public FunctionRunner() throws java.rmi.RemoteException {
            super();
        }

        public Value result;

        /** Returns true if string is defined in remote environment */
        public boolean isDefined(String name, String module)
            throws java.rmi.RemoteException {
            return Environment.env.isDefined(name, module);
        }

        /** Add an object to the remote environment. */
        public void addToEnvironment(String name, String plugin, Object o,
                                     String desc) throws java.rmi.RemoteException {
            System.out.println("Adding : " + plugin + " -> " + name);

            Environment.env.add(name, plugin, o, desc);
        }

        /**
         * result = fn.apply(v); return result;
         */
        public Value apply(Value.Function fn, Value v) {
            // Calculate the result
            try {
                result = fn.apply(v);

                // We need to make sure the result is not lazily calculated,
                // running toString()
                // usually does a good job of this.
                System.out.println("Result = " + result);

                return result;
            } catch (RuntimeException e) {
                // System.err.println( e );
                throw e;
            }
        }

        /**
         * result = ((Value.Function)result).apply(v); return result;
         */
        public Value apply(Value v) throws java.rmi.RemoteException,
                                           ClassCastException {
            result = ((Value.Function) result).apply(v);
            return result;
        }

    }

    public static interface FunctionRunnerInterface extends java.rmi.Remote {
        public Value apply(Value.Function fn, Value v)
            throws java.rmi.RemoteException;

        public Value apply(Value v) throws java.rmi.RemoteException,
                                           ClassCastException;
    }

    public static interface EnvironmentUpdateInterface extends java.rmi.Remote {
        /** Returns true if string is defined in remote environment */
        public boolean isDefined(String name, String module)
            throws java.rmi.RemoteException;

        /** Add an object to the remote environment. */
        public void addToEnvironment(String name, String plugin, Object o,
                                     String desc) throws java.rmi.RemoteException;
    }

    /** Client loads remote object and performs some simple operations. */
    public static class Client {

        public FunctionRunnerInterface f;

        public final String name;

        public Client(String name) {
            this.name = name;
            try {
                this.f = (FunctionRunnerInterface) Naming.lookup(name);
            } catch (MalformedURLException murle) {
                System.err.println("MalformedURLException");
                System.err.println(murle);
            } catch (RemoteException re) {
                System.err.println("RemoteException");
                System.err.println(re);
            } catch (NotBoundException nbe) {
                System.err.println("NotBoundException");
                System.err.println(nbe);
            }

        }

        public void runTest() throws java.rmi.RemoteException {
            System.out.println("abs(7) = "
                               + f.apply(FN.abs, new Value.Continuous(7)));
            System.out.println("abs(-10) = "
                               + f.apply(FN.abs, new Value.Continuous(-10)));
            System.out.println("abs(20) = "
                               + f.apply(FN.abs, new Value.Continuous(20)));
            System.out.println("abs(-999) = "
                               + f.apply(FN.abs, new Value.Continuous(-999)));

            Value.Str nameVal = new Value.Str(this.name);
            Value.Function rmap = (Value.Function) CDMSEngine.rmap
                .apply(new VectorFN.FatVector(new Value[] { nameVal }));
            int[] intArray = new int[] { 20, 22, 24, 26, 28, 30, 32, 34, 36,
                                         38, 40, 42, 44 };
            Value.Vector iota = new VectorFN.FastDiscreteVector(intArray);
            System.out.println("iota = " + iota);
            Value.Function temp = (Value.Function) rmap.apply(fib);
            System.out.println("map fibonacci = " + temp);
            System.out.println("map fibonacci (iota 10)= " + temp.apply(iota));

        }
    }

    public static Fibonacci fib = new Fibonacci();

    public static class Fibonacci extends Value.Function {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 204981564097910198L;

        public Fibonacci() {
            super(new Type.Function(Type.DISCRETE, Type.DISCRETE));
        }

        public Value apply(Value v) {
            return new Value.Discrete(fibonacci(((Value.Discrete) v)
                                                .getDiscrete()));
        }

        public int fibonacci(int x) {
            if (x == 0 || x == 1) {
                return 1;
            } else {
                return fibonacci(x - 1) + fibonacci(x - 2);
            }
        }

    }

    /** ["hostname"] -> fn -> [input] -> [fn input(i)] */
    public static RemoteMap rmap = new RemoteMap();
    
    /** Path to file containing list of rmi servers 
     *  Currently defaults to "/u/cluster2/postg/rodo/CAMML/scripts/rmi.nodes" */
    public static String defaultServerPath = 
        "/u/cluster2/postg/rodo/CAMML/scripts/rmi.nodes";
    
    /** 
     *  RemoteMap functions similarly to map but uses java's RMI functions. 
     *  This allows us to execute several tasks in parallel on a cluster or
     *  other group of machines.
     *  
     *  The first argument to rmap can be either a list of hosts names
     *  or () to default to the list of servers specified by defaultServerPath.
     *  
     *  Once this is done, the rmap value may be treated similarly to map with a 
     *  few exceptions.
     *  1. Care must be taken to ensure all functions are run remotely, 
     *     It is easy to accidentily pass rmap a function which can be lazily
     *     evaluated.  When this happens, the function will generally be evaluated
     *     locally instead of remotely defeating the purpose of rmap.
     *  2. All values returned bust be serialisable.  All CDMS Values implement
     *     the java.io.Serializable interface, but unfortunately not all of them
     *     keep the contract specified by the interface.
     *  
     *  ["hostname"] -> fn -> [input] -> [fn input(i)] 
     *  @see cdms.core.VectorFN.Map
     */
    public static class RemoteMap extends Value.Function {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 1593930013269574986L;

        public RemoteMap() {
            super(
                  new Type.Function(new Type.Vector(Type.STRING),
                                    Type.FUNCTION));
        }

        public Value apply(Value v) {

            System.out.println("rmap.apply.v = " + v);

            String[] name;

            if (v instanceof Value.Vector) {
                Value.Vector nameVec = (Value.Vector) v;
                name = new String[nameVec.length()];
                for (int i = 0; i < name.length; i++) {
                    name[i] = ((Value.Str) nameVec.elt(i)).getString();
                }
            } else {
                String fileName = defaultServerPath;
                if (v instanceof Value.Str) {
                    fileName = ((Value.Str) v).getString();
                }

                // Read in all names from a file and store them as name[]
                try {
                    System.out.println("Opening File");
                    StreamTokenizer tok = new StreamTokenizer(new FileReader(
                                                                             fileName));
                    tok.commentChar('#');
                    ArrayList<String> nameList = new ArrayList<String>();

                    System.out.println("Tokenizing");
                    while (tok.nextToken() == StreamTokenizer.TT_WORD) {
                        nameList.add("rmi://" + tok.sval
                                     + ":1099/CalculatorService");
                        System.out.println("Adding : " + tok.sval);
                    }

                    name = new String[nameList.size()];
                    nameList.toArray(name);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            System.out.println("Initialising " + name.length
                               + " remote servers");
            FunctionRunnerInterface[] f = new FunctionRunnerInterface[name.length];
            for (int i = 0; i < f.length; i++) {
                f[i] = makeFunctionRunner(name[i]);
            }

            return new RemoteMap2(f, name);

        }

        /**
         * Create a function runner from the given name. If there is a problem,
         * null is retunred
         */
        public static FunctionRunnerInterface makeFunctionRunner(String name) {
            FunctionRunnerInterface f = null;
            try {
                f = (FunctionRunnerInterface) Naming.lookup(name);
            } catch (MalformedURLException murle) {
                // System.err.println( "MalformedURLException" );
                // System.err.println( murle );
            } catch (RemoteException re) {
                // System.err.println( "RemoteException" );
                // System.err.println( re );
            } catch (NotBoundException nbe) {
                // System.err.println( "NotBoundException" );
                // System.err.println( nbe );
            }
            // if ( f == null ) { System.err.println("server["+name+"] not
            // initialised");}
            return f;

        }
    }

    public static class RemoteMap2 extends Value.Function {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -4534074877504793663L;

        public FunctionRunnerInterface f[];

        public String[] name;

        public RemoteMap2(FunctionRunnerInterface f[], String[] name) {
            super(new Type.Function(Type.FUNCTION, Type.FUNCTION));
            this.f = f;
            this.name = name;
        }

        public Value apply(Value v) {
            // System.out.println("Runnint rmap");
            return new RemoteMap3(f, name, (Value.Function) v);
        }
    }

    public static class RemoteMap3 extends Value.Function {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -3863692766076777435L;

        FunctionRunnerInterface f[];

        boolean[] broken;

        String[] name;

        Value.Function fn;

        final static int EMPTY = -1;

        final static int DONE = -2;

        final static int ERROR = -2;

        public RemoteMap3(FunctionRunnerInterface f[], String[] name,
                          Value.Function fn) {
            super(new Type.Function(Type.TYPE, Type.TYPE));
            this.f = f;
            this.fn = fn;
            this.name = name;
            broken = new boolean[f.length];
        }

        public Value apply(Value v) {
            Value.Vector vec = (Value.Vector) v;
            Value[] resultArray = new Value[vec.length()];
            RunThread[] threadArray = new RunThread[resultArray.length];

            // Keep track of which process in running on which node.
            RunThread[] runningArray = new RunThread[f.length];

            // processStatus[i] contains -1 if empty, or X where X is the
            // element being worked on
            // ThreadStatus[i] = -2 if thread is broken.
            int[] threadStatus = new int[runningArray.length];

            // Has there been an attempt to recover this thread? (only needed if
            // thread dies.)
            boolean[] restoreAttempted = new boolean[threadStatus.length];

            // resultStatus contains -2 if done, -1 is not staterd, or the
            // processor number X
            int[] resultStatus = new int[resultArray.length];
            for (int i = 0; i < threadStatus.length; i++) {
                threadStatus[i] = EMPTY;
            }
            for (int i = 0; i < resultStatus.length; i++) {
                resultStatus[i] = EMPTY;
            }

            System.err.print("-- Starting Threads[" + resultArray.length + "]");
            while (contains(resultArray, null)) { // While resultArray is
                // incomplete

                // //////////////////////////////////////////////
                // Wait for a processor to become available //
                // //////////////////////////////////////////////
                int nextAvailableProcessor = -1;
                while (nextAvailableProcessor == -1) {

                    for (int i = 0; i < runningArray.length; i++) {
                        // If current node is not flagged as broken
                        if (!broken[i]) {
                            // check if f was not initialised properly
                            if (f[i] == null) {
                                broken[i] = true;

                                if (threadStatus[i] != EMPTY) {
                                    resultStatus[threadStatus[i]] = EMPTY;
                                }
                                threadStatus[i] = ERROR;
                                System.err.println(name[i]
                                                   + " not initialised?");
                            }
                            // or if a ConnectException has been thrown
                            else if (runningArray[i] != null
                                     && runningArray[i].exception != null /*
                                                                           * &&
                                                                           * runningArray[i].exception
                                                                           * instanceof
                                                                           * ConnectException
                                                                           */) {
                                broken[i] = true;
                                System.err.println(name[i] + " error. \t : "
                                                   + runningArray[i].exception);
                                runningArray[i].exception.printStackTrace();

                                if (threadStatus[i] != EMPTY) {
                                    resultStatus[threadStatus[i]] = EMPTY;
                                }
                                threadStatus[i] = ERROR;
                            }
                            // If the current processor has not been
                            // initialised, use if
                            else if (runningArray[i] == null) {
                                nextAvailableProcessor = i;
                            }
                            // If current processor has finished it's job, use
                            // it.
                            else if (!runningArray[i].isAlive()) {
                                if (runningArray[i].exception != null
                                    && runningArray[i].exception instanceof java.rmi.UnmarshalException) {
                                    broken[i] = true;
                                    if (threadStatus[i] != EMPTY) {
                                        resultStatus[threadStatus[i]] = EMPTY;
                                    }
                                    threadStatus[i] = ERROR;

                                    // System.out.println(
                                    // runningArray[i].exception );
                                } else {
                                    nextAvailableProcessor = i;
                                    runningArray[i] = null;
                                    resultStatus[threadStatus[i]] = DONE;
                                    threadStatus[i] = EMPTY;
                                }
                            }
                        }
                    }
                    // if no processors are available, go to sleep for 1 second
                    // then try again.
                    if (nextAvailableProcessor == -1) {
                        for (int i = 0; i < threadStatus.length; i++) {
                            if (threadStatus[i] == ERROR) {

                                f[i] = RemoteMap.makeFunctionRunner(name[i]);
                                if (f[i] != null) {
                                    System.out.println("restoration of "
                                                       + name[i] + " succeeded.");
                                    restoreAttempted[i] = false;
                                    threadStatus[i] = EMPTY;
                                    broken[i] = false;
                                    runningArray[i] = null;
                                }
                                // We only need to print out restoration failed
                                // once, even
                                // if we try multiple times.
                                else if (restoreAttempted[i] == false) {
                                    System.out.println("restoration of "
                                                       + name[i] + " failed.");
                                    restoreAttempted[i] = true;
                                }
                            }
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } // end while(nextAvailableProcessor == -1)

                // Find the next available value to run on
                int nextValueToMap = -1;
                for (int i = 0; i < resultStatus.length; i++) {
                    if (resultStatus[i] == EMPTY) {
                        nextValueToMap = i;
                        break;
                    }
                }

                // If a valid value exists, start running on it.
                if (nextValueToMap != -1) {
                    // Once we have found an available processor, run a thread
                    // on it.
                    threadStatus[nextAvailableProcessor] = nextValueToMap;
                    resultStatus[nextValueToMap] = nextAvailableProcessor;

                    threadArray[nextValueToMap] = new RunThread(
                                                                f[nextAvailableProcessor], fn, vec
                                                                .elt(nextValueToMap));
                    threadArray[nextValueToMap].start();
                    runningArray[nextAvailableProcessor] = threadArray[nextValueToMap];
                    System.err.print('.');
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (java.lang.InterruptedException e) {
                    } // ignore exception
                }

                // Copy all results from threadArray into resultArray
                for (int i = 0; i < resultArray.length; i++) {
                    if (resultStatus[i] == DONE) {
                        resultArray[i] = threadArray[i].result;
                    }
                }


            } // end while( contains( resultArray, null ) )
            System.out.println();

            return new VectorFN.FatVector(resultArray);

        }
    }


    /** return true if any of the threads in array are still alive */
    public static boolean isAlive(Thread[] thread) /*
                                                    * throws
                                                    * java.lang.InterruptedException
                                                    */{
        for (int i = 0; i < thread.length; i++) {
            if (thread[i].isAlive())
                return true;
        }
        return false;
    }

    /** Return true if array[i] == obj */
    public static boolean contains(Object[] array, Object obj) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == obj)
                return true;
        }
        return false;
    }

    public static class RunThread extends Thread {
        FunctionRunnerInterface f;

        Value.Function fn;

        Value v;

        Value result;

        /**
         * run() cannot throw exception so storing it here may be helpful for
         * debug purposes.
         */
        Exception exception = null;

        public RunThread(FunctionRunnerInterface f, Value.Function fn, Value v) {
            this.f = f;
            this.fn = fn;
            this.v = v;
        }

        /** run updates the foreign environment, then runs the function. */
        public void run() {
            try {
                // System.out.println("-- Updating Environment --");
                // FunctionRunner functionRunner = (FunctionRunner)f;
                updateEnvironment((EnvironmentUpdateInterface) f);
                if (f == null || fn == null || v == null) {
                    System.err.println("f  = " + f + "\n" + "fn = " + fn + "\n"
                                       + "(v==null)  = " + (v == null));
                }
                // System.out.println("-- f.apply() --");
                this.result = f.apply(fn, v);
                // System.out.println("threadResult = " + result);
            } catch (java.rmi.RemoteException e) {
                // System.err.println( e );
                // this.result = new Value.Obj( e );
                this.exception = e;
            } catch (RuntimeException e) {
                // System.err.println( e );
                this.exception = e;
                // this.result = new Value.Obj( e );
            }
        }
    }

    public static void updateEnvironment(EnvironmentUpdateInterface host)
        throws java.rmi.RemoteException {
        boolean doUpdate = true;
        if (doUpdate) {
            Object[] localObj = Environment.env.getAllEntries();
            // System.out.println("Checking " + localObj.length + " values");

            for (int i = 0; i < localObj.length; i++) {
                Environment.RegEntry entry = (Environment.RegEntry) localObj[i];
                // if current variable not defined on remote host
                if (!host.isDefined(entry.name, entry.module)) {
                    host.addToEnvironment(entry.name, entry.module, entry.o,
                                          entry.description);
                }
            }
        }
    }

}
