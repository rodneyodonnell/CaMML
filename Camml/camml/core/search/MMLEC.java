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
// MML Equivelence Class for CaMML
//

// File: MMLEC.java
// Author: rodo@dgs.monash.edu.au


package camml.core.search;

import camml.core.models.bNet.BNet;
import cdms.core.*;
import cdms.core.Value.Function;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;

/**
 *  SECList contains a list of SECs forming a MMLEC.  <br>
 */
public class MMLEC implements Serializable
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -1575632873872187413L;

    /** Return number of SECs in secList */
    public int length() { return secList.size(); }
    
    /** Return posterior of MMLEC */
    public double getPosterior() {return posterior;}

    /** An ArrayList of MML equivelent SECs */
    protected final ArrayList<SEC> secList;
    
    /** total weight of all TOMs in this MMLEC */
    protected double weight = 0;
    
    /** total posterior of all TOMs in this MMLEC*/
    protected double posterior;
    
    /** sum of SEC[i].relativePrior */
    protected double relativePrior = -1;
    
    /** best MML cost in this MMLEC */
    public double bestMML = Double.POSITIVE_INFINITY;;
    
    /** CaseInfo contains various useful values. */
    protected final CaseInfo caseInfo;
    
    /** Create a new MMLEC based on sec */
    public MMLEC( SEC sec )
    {
        secList = new ArrayList<SEC>();
        secList.add( sec );
        weight = sec.weight;
        posterior = sec.posterior;
        relativePrior = sec.relativePrior;
        bestMML = sec.bestMML;
        caseInfo = sec.caseInfo;
    }
    
    /** Merge the posterior of secList2 into the current SECList. secList2 is left with no
     *  weight, posterior, prior or SECs */
    public void merge( MMLEC secList2 ) {
        if ( this.caseInfo != secList2.caseInfo ) {
            throw new RuntimeException("Incompatible caseInfo.  SECs cannot be merged.");
        }
        
        // Merge all posterior, weight and prior from secList2 into this SECList.
        this.weight += secList2.weight;  
        secList2.weight = 0;
        this.posterior += secList2.posterior;
        secList2.posterior = 0;
        this.relativePrior += secList2.relativePrior;
        secList2.relativePrior = 0;
        if ( secList2.bestMML < this.bestMML ) {
            this.bestMML = secList2.bestMML;
        }
        secList2.bestMML = 0;
        
        // add all SECs from secList2 to this SECList (and remove them from secList2)
        for ( int i = 0; i < secList2.secList.size(); i++ ) {
            secList.add( secList2.secList.get(i) );
        }
        secList2.secList.clear();
    }
    
    /** Accessor function */
    public SEC getSEC( int n ) 
    {
        return (SEC)secList.get(n);
    }
    
    /** CDMS type used to represent a MMLEC */
    protected static Type.Structured mmlecStructType = 
        new Type.Structured( new Type[] { SECResultsVector.tt, 
                                          Type.CONTINUOUS,
                                          Type.CONTINUOUS,
                                          Type.CONTINUOUS,
                                          Type.CONTINUOUS},
            new String[]{ "SECResultsVector",
                          "posterior",
                          "relativePrior",
                          "bestMML",
                          "weight"});
    
    /** Return a CDMS Vector of SECs */
    public Value.Structured makeSECListStruct() {
        return new MMLECStructure();
    }
    
    /** Value.Structured representation of MMLEC */
    public class MMLECStructure extends Value.Structured {
        
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 8572231087141914140L;

        /** Constructor */
        public MMLECStructure() { super( mmlecStructType); }
        
        /** Return number of cmpnts */
        public int length() { return 5; }
        
        /** Return appropriate cmpnt <br> 
         *  cmpnt(0) = [SEC] <br>
         *  cpmnt(1) = posterior <br>
         *  cmpnt(2) = relative prior <br>
         *  cmpnt(3) = best MML <br>
         *  cmpnt(4) = weight */                
        public Value cmpnt( int i ) { 
            if ( i == 0 ) {
                SEC[] secArray = (SEC[])secList.toArray(new SEC[secList.size()]);
                return new SECResultsVector( secArray );
            }
            else if ( i == 1 ) { return new Value.Continuous( posterior );    }
            else if ( i == 2 ) { return new Value.Continuous( relativePrior ); }
            else if ( i == 3 ) { return new Value.Continuous( bestMML ); }        
            else if ( i == 4 ) { return new Value.Continuous( weight ); }        
            else { throw new RuntimeException("Invalid option selected"); }
        }
    }
    
    /** Sort all MMLECs by their posterior probability.  Highest posterior first */
    public static Comparator<MMLEC> posteriorComparator =  new Comparator<MMLEC>() {
        public int compare( MMLEC a, MMLEC b ) {         
            double posteriorA = a.posterior;
            double posteriorB = b.posterior;
            if ( posteriorA > posteriorB )
                return -1;
            if ( posteriorA < posteriorB )
                return  1;
            if ( posteriorA == posteriorB )
                return 0;
            throw new RuntimeException("Bad Comparison of (" + a + "," + b );
        }
    };
    
    public static final GetRepresentativeDAG getRepresentative = new GetRepresentativeDAG();
    
    /**
     *  (..) | [(..)] | ([(..)]) | [([(..)])] | ([([(..)])]) | [([([(..)])])] -> (m,y) <br>
     * 
     * If a MMLEC, MMLECStructure, SECResultVector, SECStructure, TOMVector
     * or TOMStructure is passed
     * to apply, we "drill down" through MMLEC and SEC equivelence classes to
     * find the best repreentative DAG.  A (Model,Param) pair is returned. <br>
     * 
     */
    public static class GetRepresentativeDAG extends Function {

        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -3073086348161217843L;
        static Type u = new Type.Union(new Type[] {Type.STRUCTURED,Type.VECTOR});
        /**
         */
        public GetRepresentativeDAG() {
            super( new Type.Function(u,Type.STRUCTURED));    
        }

        
        Value.Structured makeMY( Value.Model m, Value.Vector y ) {
            return new Value.DefStructured(new Value[] {m,y},
                                           new String[] {"model","params"} );            
        }

        /** Naive check to see if v is a param vector [("name",[arcs],(m,y))] */
        boolean isParamVec( Value v ) {            
            if ( v instanceof Value.Vector == false) { return false; }
            Value.Vector vec = (Value.Vector)v;
            Value elt0 = vec.elt(0);
            if ( elt0 instanceof Value.Structured == false ) { return false; }
            Value.Structured struct = (Value.Structured)elt0;

            // If it looks like a param vec, return true.
            if (struct.length() == 3 
                && struct.cmpnt(0) instanceof Value.Str
                && struct.cmpnt(1) instanceof Value.Vector
                && struct.cmpnt(2) instanceof Value.Structured) {return true; }
            
            return false;    
        }
        
        /** Attempt to extracy (model,params) pair from BNet parameters.
         *  Sucesfully recognises: <br>
         *  - MSY structs <br>
         *  - Parameter vec (created model) <br>
         *  - MMLEC and SEC vector/structures */
        public Value apply( Value v) {
            Value.Structured my = _apply( v ); 
            if (my == null) {
                throw new RuntimeException("Could not find DAG : " + v);
            }
            return my;
        }
        
        /** @see MMLEC.apply. Returns null if DAG not found. */
        public Value.Structured _apply(Value v) {
            final Value.Structured my;
            
            if ( v instanceof Value.Structured ) {
                Value.Structured  struct = (Value.Structured)v;
                Value cmpnt0 = struct.cmpnt(0); 
                    
                // MY struct 
                if ( cmpnt0 instanceof BNet && isParamVec(struct.cmpnt(1)) ) {
                    my = struct;
                }
                // MSY structure
                else if ( struct.length() == 3 && 
                          cmpnt0 instanceof BNet && isParamVec( struct.cmpnt(2)) ) {  
                    Value.Model m = (Value.Model)struct.cmpnt(0);
                    Value.Vector y = (Value.Vector)struct.cmpnt(2);
                    my = makeMY( m,y );
                }
                // MMLECStructure, SECStructure, etc.
                else if (cmpnt0 instanceof Value.Vector ||
                         (struct.length() == 1 && cmpnt0 instanceof Value.Structured)) {
                    my = _apply(cmpnt0);
                }
                else {                    
                    my = null;
                }
            }
            else if ( v instanceof Value.Vector ) {
                Value.Vector vec = (Value.Vector)v;
                Value elt0 = vec.elt(0);
                if ( elt0 instanceof Value.Structured ) {
                    Value.Structured selt0 = (Value.Structured)elt0;
                    // If first cmpnt of first elt is a string, then 
                    // v is a parameter set (without a model)
                    if (selt0.cmpnt(0) instanceof Value.Str) {
                        throw new RuntimeException(
                                                   "Cannot extract DAG, must select Model & Params together.");
                    }
                    // Possibly a MMLECVec or SECVec
                    else {
                        my = _apply(elt0);
                    }
                }
                // Probably no DAG, try anyway.
                else {
                    my = _apply(elt0);
                }
            }
            // no DAG is specified.
            else { my = null; }
            return my;
        }        
    }
}
