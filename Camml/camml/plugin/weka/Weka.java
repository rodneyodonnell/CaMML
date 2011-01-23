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
// Weka plugin.
//

// File: Weka.java
// Author: rodo@csse.monash.edu.au

package camml.plugin.weka;

import cdms.core.*;
import weka.core.*;
import weka.filters.supervised.attribute.Discretize;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

/**
 *   Module to load Weka functions
 *   
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.8 $ $Date: 2006/08/22 03:13:36 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/plugin/weka/Weka.java,v $
 *
 */
public class Weka extends Module 
{
    public static java.net.URL helpURL = Module.createStandardURL(Weka.class);
    
    public String getModuleName() { return "Weka"; }
    public java.net.URL getHelp() { return helpURL; }
    
    public void install(Value params) throws Exception
    {
        add("load", new Load(false,false), "Load a .arff file" );
        add("loadDiscretize", new Load(false,true), "Load and discretize continuous vars." );
        add("loadMissing", new Load(true,false),"Load and replace missing values");
        add("loadMissingDiscretize", new Load(true,true), "Load, replace missing and discretize." );
        add("wekaLogit", WekaLearner.wekaBNetLogitLearner.getFunctionStruct(),"Weka logit Bnet");
    }
    

    /** Load a .arff file, if replaceMissing missing values are replaced.  if discretize continuous
     * values are discretized.
     */
    public static Value.Vector load( String filename, boolean replaceMissing, boolean discretize )
        throws java.io.FileNotFoundException, java.io.IOException, Exception
    {
        Instances instances = new Instances( new java.io.FileReader(filename) );

        instances.setClassIndex(instances.numAttributes() - 1);
        // filter instances if required.
        if ( discretize ) {
            Discretize df = new Discretize();
            //DiscretizeFilter df = new DiscretizeFilter();
            df.setUseBetterEncoding( true );
            df.setInputFormat( instances );
            instances = weka.filters.Filter.useFilter( instances, df );
        }
        if ( replaceMissing ) {
            ReplaceMissingValues mf = new ReplaceMissingValues();
            //ReplaceMissingValuesFilter mf = new ReplaceMissingValuesFilter();
            mf.setInputFormat( instances );
            instances = weka.filters.Filter.useFilter( instances, mf );
        }

        return Converter.instancesToVector( instances );
    }

    /**
     *  Load is a Value.Function of type STR -> [(...)] <br>
     *  It takes as a parameter a filename string, and returns a vector of structured values. <br>
     *  It loads files from the oldCamml format.
     */
    protected static class Load extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 1931636431700914980L;
        public final boolean discretize;
        public final boolean replaceMissing;

        public static final Type.Function tt = 
            new Type.Function( Type.STRING, new Type.Vector(Type.STRUCTURED) );
        public Load( boolean replaceMissing, boolean discretize ) { 
            super(tt); 
            this.replaceMissing = replaceMissing;
            this.discretize = discretize;
        }

        public Value apply( Value v )
        {
            Value.Vector vec;
            try {
                vec = load( ((Value.Str)v).getString(), replaceMissing, discretize );
            }
            catch ( Exception e ) {
                e.printStackTrace();
                vec =  new VectorFN.EmptyVector();
            }
        
            return vec;
        }
    }

}

