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
// Functions to learn BNet's from raw data using Tetrad IV.
//

// File: TetradLearner.java
// Author: rodo@csse.monash.edu.au

package camml.plugin.tetrad4;

import cdms.core.*;
import camml.core.models.*;
import camml.core.models.cpt.CPTLearner;

import camml.core.models.ModelLearner;
import camml.core.models.MakeModelLearner;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import edu.cmu.tetrad.data.Knowledge;


/**
 * Class to learn bNets using calls to Tetrad IV.
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision $ $Date: 2006/11/13 14:01:11 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/plugin/tetrad4/TetradLearner.java,v $
 */
public class TetradLearner extends ModelLearner.DefaultImplementation
{   
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 1072199760280708626L;


    /** List of valid search names. */
    // Note: This must come before definitions of ges, fci & pc or initial calls
    //       to setSearchType fail. (NullPointerException)
    protected static final List validSearch = Arrays.asList( new String[] { "pc", "fci", "ges" } );

    
    /** Static instance of TetradLearnerCreator */
    public static final MakeTetradLearner creator = new MakeTetradLearner();

    /** Static insance of GES search */
    public static final TetradLearner ges = 
        (TetradLearner)creator._apply( new String[]{"search", "mix"}, 
                                       new Value[] {new Value.Str("ges"), Value.FALSE } );

    /** Static insance of mixed GES search */
    public static final TetradLearner gesMix = 
        (TetradLearner)creator._apply( new String[]{"search", "mix"}, 
                                       new Value[] {new Value.Str("ges"), Value.TRUE } );

    /** Static insance of FCI search */
    public static final TetradLearner fci = 
        (TetradLearner)creator._apply( new String[]{"search", "mix"}, 
                                       new Value[] {new Value.Str("fci"), Value.FALSE } );

    /** Static insance of mixed FCI search */
    public static final TetradLearner fciMix = 
        (TetradLearner)creator._apply( new String[]{"search", "mix"}, 
                                       new Value[] {new Value.Str("fci"), Value.TRUE } );

    /** Static insance of PC search */
    public static final TetradLearner pcRepair = 
        (TetradLearner)creator._apply( new String[]{"search", "mix"}, 
                                       new Value[] {new Value.Str("pc"), Value.FALSE } );

    /** Static insance of mixed PC search */
    public static final TetradLearner pcMixRepair = 
        (TetradLearner)creator._apply( new String[]{"search", "mix"}, 
                                       new Value[] {new Value.Str("pc"), Value.TRUE } );

    /** Static insance of PC search */
    public static final TetradLearner pcRerun = 
        (TetradLearner)creator._apply( new String[]{"search", "mix"}, 
                                       new Value[] {new Value.Str("pc"), Value.FALSE } );

    /** Static insance of mixed PC search */
    public static final TetradLearner pcMixRerun = 
        (TetradLearner)creator._apply( new String[]{"search", "mix"}, 
                                       new Value[] {new Value.Str("pc"), Value.TRUE } );

    static {
        pcRepair.repair = true;
        pcMixRepair.repair = true;

        pcRerun.rerun = true;
        pcMixRerun.rerun = true;
    }


    /** return "TetradLearner" */
    public String getName() { return "TetradLearner"; }    
    
    /** ModelLearner used to create local structre */
    protected ModelLearner localLearner;

    /** "pc", "fci" or "ges" */
    String searchType;
    public String getSearchType() { return searchType; }

    /** Set to true to mix all consistent DAGs, if false a single DAG is used. */
    protected boolean mix = false;

    /** If true, parameterize returns a string representation of the tetrad graph. */
    protected static boolean stringParams = false;;
    
    /** Use variable names instead of numbers. */
    protected static boolean useVariableNames = true;
    
    /** Set search to specified type. */
    public void setSearchType( String searchType) {
        if ( !validSearch.contains(searchType) ) {
            throw new IllegalArgumentException("Invalid Search type specified.");
        }
        this.searchType = searchType;
    }
    

    /** Significance level used by tetrad */
    double significance = 0.05;

    /** Search depth used by tetrad (-1 = infinite). */
    int depth = -1;

    /** Should invalid models be repaired? (default = false) */
    boolean repair = false;

    /** Should searches yeilding invalid models be rerun? (default = false) */
    boolean rerun = false;
    
    /** Prior constraints (eg. Tiers). Null implies default prior. */
    Knowledge prior = null;

    /** Pass in extra options to be passed to BNetSearch */
    public void setOptions( String[] options, Value[] optionVal ) {    
        if ( options.length != optionVal.length ) {
            throw new RuntimeException("Option length mismatch in TetradLearner.setOptions()");
        }

        for ( int i = 0; i < options.length; i++ ) {
            if ( "significance".equals(options[i]) ) { 
                significance = ((Value.Continuous)optionVal[i]).getContinuous();
            }
            else if ( "depth".equals(options[i]) ) {
                depth = ((Value.Discrete)optionVal[i]).getDiscrete();
            }
            else if ( "search".equals(options[i]) ) {
                setSearchType( ((Value.Str)optionVal[i]).getString() );
            }
            else if ( "localLearner".equals(options[i]) ) {
                localLearner = ((FunctionStruct)optionVal[i]).getLearner();
            }
            else if ( "mix".equals(options[i]) ) {
                mix = optionVal[i].equals(Value.TRUE);
            }
            else if ( "prior".equals(options[i]) ) {
                prior = (Knowledge)((Value.Obj)optionVal[i]).getObj();
            }
            else {
                throw new IllegalArgumentException("Unrecognised option: " + options[i]);
            }
        }
    }
    
    /** Constuctor currently only specifies Type.MODEL, this needs to be fixed. */
    public TetradLearner( ModelLearner localLearner, String searchType )
    {
        super( Type.MODEL, Type.TRIV );
        
        // Set default values.
        this.localLearner = localLearner;
        
        // Set search type.
        setSearchType( searchType ); 
    }
    
    /** RNG used to choose DAG from SEC in parameterize */
    Random rand = new Random();
    
    /** Parameterize and return (m,s,y) */
    public Value.Structured parameterize( Value initialInfo, Value.Vector x, Value.Vector z )
        throws LearnerException
    {
        
        if ( x.length() != z.length() ) {
            throw new IllegalArgumentException("Length mismatch in parameterize()");
        }
        
        // Run search.
        final Value.Structured my;
        final Value.Model bNet;
        if (mix == true) {
            my = Tetrad4FN.mixTetrad(x, searchType, significance, depth, localLearner, 
                                     repair, rerun, prior);
            Value.Vector paramVec = (Value.Vector)my.cmpnt(1);            
            bNet = (Value.Model)((Value.Structured)((Value.Vector)paramVec).elt(0)).cmpnt(1);
        }
        else {
            my = Tetrad4FN.singleTetrad(rand, x, searchType, significance, 
                                        depth, localLearner, repair, rerun, prior, 
                                        stringParams);
            bNet = (Value.Model)my.cmpnt(0);
        }

        // We need to return (model,stats,params), so create sufficient statistics.
        Value stats = bNet.getSufficient(x,z);

        // return (mixModel,stats,params)
        return new Value.DefStructured( new Value[] {my.cmpnt(0),stats,my.cmpnt(1)} );
    }
    
    
    /** Parameterize and return (m,s,y) */
    public double parameterizeAndCost( Value initialInfo, Value.Vector x, Value.Vector z )
    {
        throw new RuntimeException("Not implemented");
    }
    
    /** Parameterize and return (m,s,y) */
    public Value.Structured sParameterize( Value.Model model, Value s )
        throws LearnerException
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
    
    
    public String toString() { return "TetradLearner: " + searchType; }    
    
    
    
    /** Default implementation of makeTetradLearner */
    public static final MakeTetradLearner makeTetradLearner = new MakeTetradLearner();
    
    /** MakeTetradLearner returns a TetradLearner given a "leafLearner" in its options. */
    public static class MakeTetradLearner extends MakeModelLearner
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -3630456969977587469L;

        public MakeTetradLearner( ) { }
        
        /** Shortcut apply method */
        public TetradLearner _apply( String[] option, Value[] optionVal ) {  

            TetradLearner tl = new TetradLearner( CPTLearner.mmlAdaptiveCPTLearner, "pc" );
            tl.setOptions( option, optionVal );
            
            return tl;
            
        }
        
        public String[] getOptions() { return new String[] {
                "significance -- Significance level used by tetrad (Continuous)",
                "depth        -- Search depth used by tetrad (discrete, -1 = infinite)", 
                "search       -- Search type: \"pc\", \"fci\" or \"ges\"",
                "localLearner -- Learner used for each subModel.  Default is AdaptiveCode"
            };};       
    }
    
}
