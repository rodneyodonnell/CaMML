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
// rodoCamml wrapper plugin.
//

// File: RodoCammlModelLearner.java
// Author: rodo@csse.monash.edu.au

package camml.plugin.rodoCamml;

import cdms.core.*;
import camml.core.models.ModelLearner;
import camml.core.models.MakeModelLearner;
import camml.core.models.cpt.CPT;

import java.io.*;

import camml.plugin.netica.NeticaFn;

/**
 * Module to interface with the "rodo" version of camml. (this mainly exists for regression testing
 *  but may also be used if faster execution times are required.)
 *  Note: This plugin requires a compiled version of Camml to be present.
 *        The Netica module is also required as Camml spits out a netica network.
 *        This means that both libnetica.so and libNeticaJ.jar must be in $LD_LIBRARY_PATH
 */
public class RodoCammlLearner extends ModelLearner.DefaultImplementation
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -8745445489055511861L;

    public static RodoCammlLearner modelLearner = new RodoCammlLearner( );
        
    public static RodoCammlLearner julesCPTLearner = new JulesLearner("of");
    public static RodoCammlLearner julesLogitLearner = new JulesLearner("oe");
    public static RodoCammlLearner julesDualLearner = new JulesLearner("ob");
    
    
    public String getName() { return "RodoCammlLearner"; }    

    /** Constuctor currently only specifies Type.MODEL, this needs to be fixed. */
    public RodoCammlLearner( )
    {
        super( Type.MODEL, Type.TRIV );
    }


    String fullOption = null;
    boolean crippleClean = false;
    boolean crippleJoin = false;
    boolean crippleArcProb = false;
    int seed;
    boolean setSeed = false;
    String runName = "camml";

    /** Parameterize and return (m,s,y) */
    public Value.Structured parameterize( Value initialInfo, Value.Vector x, Value.Vector z )
    {
        if ( z.length() != z.length() ) {
            throw new RuntimeException("Length mismatch in RodoCammlLearner.parameterize");
        }

        try {

            // Delete previous versions of temp.cas and temp.dnet if they exist.
            // this removes the problem of the oldCamml process failing and
            // this function reloading an old temp.dnet by mistake.
            new File("temp.cas").delete();
            new File("temp.dnet").delete();

            // Save cases to a file called temp.cas
            Value.Function store = 
                (Value.Function)RodoCammlIO.store.apply( new Value.Str("temp.cas") );
            store.apply(x);

            // set up camml process
            String cmd = runName + " temp.cas";
            Process p = Runtime.getRuntime().exec( cmd );
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            PrintStream outputStream = new PrintStream( p.getOutputStream() );


            //         learner.crippleClean = crippleClean;
            //         learner.crippleArcProb = crippleArcProb;
            //         learner.fullOption = fullOption;

            if ( fullOption == null ) {
                if ( crippleClean || crippleArcProb || crippleJoin ) {
                    outputStream.println("r");
                    int val = 0;
                    if ( crippleClean )   { val += 1; }
                    if ( crippleArcProb ) { val += 2; }
                    if ( crippleJoin )    { val += 4; } 
                    outputStream.println( "k " + val );
                    outputStream.println("q");
                }
                if ( setSeed ) {
                    outputStream.println("z");
                    outputStream.println( seed );
                }
                // issue command line inputs to camml.        
                outputStream.println("g");
                outputStream.println("x");
                outputStream.println("temp.dnet");
                outputStream.println("pf");
                outputStream.println("q");
            }
            else {
                outputStream.println( fullOption );
            }
            outputStream.flush();

            // monitor output.
            String line;
            while((line = in.readLine())!=null) {
                System.out.println(line);
            }
            while((line = err.readLine())!=null) {
                System.out.println(line);
            }
            p.waitFor(); // make sure process is done.

            // load netica network from file. returns (model,params)
            Value.Structured my =
                (Value.Structured)NeticaFn.loadNet.apply( new Value.Str("temp.dnet") );

            // turn results into a MSY struct.
            Value.Model m = (Value.Model)my.cmpnt(0);
            Value s = m.getSufficient( x, z );
            Value y = my.cmpnt(1);


            Value.Vector data = RodoCammlIO.load( "temp.cas" );
            Type.Structured dataType = (Type.Structured)((Type.Vector)data.t).elt;
            String[] names = camml.plugin.netica.NeticaFn.makeValidNeticaNames( dataType.labels, false );
            Value.Structured reorderedMYStruct = 
                NeticaFn.ReorderNet._apply( names, new Value.DefStructured( new Value[] {m,y} ) );
                
            return new Value.DefStructured( new Value[] {reorderedMYStruct.cmpnt(0),
                                                         s,
                                                         reorderedMYStruct.cmpnt(1),} );
        

        }
        catch ( Exception e ) {
            System.out.println("Exception " + e + " caught.  Rethrowing exception." );
            throw new RuntimeException(e);
        }

    }


    /** Parameterize and return (m,s,y) */
    public double parameterizeAndCost( Value initialInfo, Value.Vector x, Value.Vector z )
    {
        throw new RuntimeException("Not implemented");
    }

    /** Parameterize and return (m,s,y) */
    public Value.Structured sParameterize( Value.Model model, Value s )
    {    
        return parameterize( Value.TRIV, 
                             (Value.Vector)((Value.Structured)s).cmpnt(0), 
                             (Value.Vector)((Value.Structured)s).cmpnt(1) );
    }



    /**
     * return cost.  This is read directly out of parameters.  Ideally it should be calculated
     * using parameters and data as currently it entirely ignores data.
     */
    public double sCost(Value.Model m, Value s, Value y)
    {    
        throw new RuntimeException("Not implemented");
    } 


    public String toString() { return getName(); }    



    /** Default implementation of makeBNetLearner */
    public static final MakeRodoCammlLearner makeRodoCammlLearner = new MakeRodoCammlLearner();

    /** MakeBNetLearner returns a BNetLearner given a "leafLearner" in its options. */
    public static class MakeRodoCammlLearner extends MakeModelLearner
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -3275048826111050044L;

        public MakeRodoCammlLearner( ) { }

        /** Shortcut apply method */
        public ModelLearner _apply( String[] option, Value[] optionVal ) {  

            // Set default values.
            boolean crippleClean = false;
            boolean crippleArcProb = false;
            boolean crippleJoin = false;
            String fullOption = null;
            int seed = 0;
            boolean setSeed = false;
            String runName = null;

            // Search options for overrides.
            for ( int i = 0; i < option.length; i++ ) {
                // If none of the options listed are valid, validOption = false.
                boolean validOption = true;
                if ( option[i].equals("crippleClean") ) {
                    crippleClean = (((Value.Discrete)optionVal[i]).getDiscrete() == 0);
                }
                else if ( option[i].equals("crippleJoin") ) {
                    crippleJoin = (((Value.Discrete)optionVal[i]).getDiscrete() == 0);
                }
                else if ( option[i].equals("crippleArcProb") ) {
                    crippleArcProb = (((Value.Discrete)optionVal[i]).getDiscrete() == 0);
                }
                else if ( option[i].equals("fullOption") ) {
                    fullOption = ((Value.Str)optionVal[i]).getString();
                }
                else if ( option[i].equals("seed") ) {
                    seed = ((Value.Discrete)optionVal[i]).getDiscrete();
                    setSeed = true;
                }
                else if ( option[i].equals("runName") ) { // name of executable
                    runName = ((Value.Str)optionVal[i]).getString();
                }
                else { validOption = false; }

                // Remove the current option from the list of options.
                // All remaining options are passed to the search object later.
                if ( validOption == true ) {
                    option = remove(option,i);
                    optionVal = remove(optionVal,i);
                    i--;
                }
            }
        
            if ( option.length != 0 ) { throw new RuntimeException("Unknown option: " + option[0]);}

            // Initialise a BNetLearner with all options specified.
            RodoCammlLearner learner = new RodoCammlLearner( );
            learner.crippleClean = crippleClean;
            learner.crippleArcProb = crippleArcProb;
            learner.crippleJoin = crippleJoin;
            learner.fullOption = fullOption;        
            learner.setSeed = setSeed;
            learner.seed = seed;
            if (runName != null) learner.runName = runName;
            return learner;
        }

        public String[] getOptions() { return new String[] {
                "crippleClean   -- Cripple ability to clean models",
                "crippleJoin    -- Cripple ability to join models",
                "crippleArcProb -- ArcProb cannot be changed from 0.5",
                "seed           -- Set random seed",
                "fullOption     -- Input string to run camml. standard is \"g\\nx\\ntemp.dnet\\npf\\n\\nq" // g x temp.dnet pf q
            }; }
    }


    public static class JulesLearner extends RodoCammlLearner {
        
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -3883607963400918021L;

        public String getName() { return "JulesLearner"; }    

        String option;
        
        /** Constuctor currently only specifies Type.MODEL, this needs to be fixed. */
        public JulesLearner( String option) { 
            super( ); 
            this.option = option;
            runName = "en7";
        }


        /** Parameterize and return (m,s,y) */
        public Value.Structured parameterize( Value initialInfo, Value.Vector x, Value.Vector z )
        {
            if ( z.length() != z.length() ) {
                throw new RuntimeException("Length mismatch in RodoCammlLearner.parameterize");
            }

            try {
                // Delete previous versions of temp.cas and temp.dnet if they exist.
                // this removes the problem of the oldCamml process failing and
                // this function reloading an old temp.dnet by mistake.
                new File("temp.cas").delete();
                new File("temp.dnet").delete();
                new File("temp.dnet.tmp").delete();
            
                // Save cases to a file called temp.cas
                Value.Function store = 
                    (Value.Function)RodoCammlIO.storeInOldFormat.apply( new Value.Str("temp.cas") );
                store.apply(x);

                // set up camml process
                String cmd = runName + " temp.cas";
                Process p = Runtime.getRuntime().exec( cmd );
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                PrintStream outputStream = new PrintStream( p.getOutputStream() );


                if ( fullOption == null ) {
                    if ( crippleClean || crippleArcProb || crippleJoin ) {                
                        int val = 0;
                        if ( crippleClean )   { val += 1; }
                        if ( crippleArcProb ) { val += 2; }
                        if ( crippleJoin )    { val += 4; } 
                        outputStream.println( "k " + val );
                    }
                    if ( setSeed ) {
                        System.out.println("SetSeed option not available for JulesLearner");
                    }
                    // issue command line inputs to camml.        
                    outputStream.println(option);  // Option to tell camml which local learner to use.
                    outputStream.println("g");     // gibbs sample
                    outputStream.println("N");     // export Netica model
                    outputStream.println("temp.dnet");
                    outputStream.println("pf");
                    outputStream.println("q");     // quit.
                }
                else {
                    outputStream.println( fullOption );
                }
                outputStream.flush();

                // monitor output.
                String line;
                while((line = in.readLine())!=null) {
                    System.out.println(line);
                }
                while((line = err.readLine())!=null) {
                    System.out.println(line);
                }
                p.waitFor(); // make sure process is done.

                // load netica network from file. returns (model,params)
                Value.Structured my =
                    (Value.Structured)NeticaFn.loadNet.apply( new Value.Str("temp.dnet") );

                // turn results into a MSY struct.
                Value.Model m = (Value.Model)my.cmpnt(0);
                Value s = m.getSufficient( x, z );
                Value y = my.cmpnt(1);


                Value.Vector data = RodoCammlIO.load( "temp.cas" );
                Type.Structured dataType = (Type.Structured)((Type.Vector)data.t).elt;
                String[] names = camml.plugin.netica.NeticaFn.makeValidNeticaNames( dataType.labels, false );
                Value.Structured reorderedMYStruct = 
                    NeticaFn.ReorderNet._apply( names, new Value.DefStructured( new Value[] {m,y} ) );

                int[] numParams = readInts(names.length, new FileReader("temp.dnet.tmp"));
                Value.Vector tempY = (Value.Vector)reorderedMYStruct.cmpnt(1);
                for (int i = 0; i < tempY.length(); i++) {
                    Value.Structured subParams = (Value.Structured)((Value.Structured)tempY.elt(i)).cmpnt(2);
                    CPT cpt = (CPT)subParams.cmpnt(0);
                    cpt.setNumParams(numParams[i]);
                }
            
                return new Value.DefStructured( new Value[] {reorderedMYStruct.cmpnt(0),
                                                             s,
                                                             reorderedMYStruct.cmpnt(1),} );
            

            }
            catch ( Exception e ) {
                System.out.println("Exception " + e + " caught.  Rethrowing exception." );
                throw new RuntimeException(e);
            }

        }

        /** Read in n numbers from the stream r. */
        protected static int[] readInts(int n, Reader r) throws IOException {
            StreamTokenizer st = new StreamTokenizer(r);
            int buffer[] = new int[n];
            for (int i = 0; i < buffer.length; i++) {
                if (st.nextToken() != StreamTokenizer.TT_NUMBER) {throw new RuntimeException("Invalid char found : " + st.sval);}
                else { buffer[i] = (int)st.nval;}
            }
            return buffer;
        }

    }
}

