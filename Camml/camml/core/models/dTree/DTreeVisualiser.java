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
// Decision Visualiser class
//

// File: BNetVisualiser.java
// Author: rodo@csse.monash.edu.au


package camml.core.models.dTree;

import cdms.core.*;
import camml.core.models.dual.DualVisualiser;

import java.awt.*;
import javax.swing.*;

public class DTreeVisualiser 
    extends JPanel //Container
            //    implements camml.CDMSUtils.visualiser.Visualiser
{
    
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 610338537012481501L;

    public static final FN visualiseDTree = new FN();
    
    /** [paretNames] -> childName -> model -> params -> Object(JComponent) */
    public static class FN extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 543272157261363917L;
        /** Initialise function Type to params -> Object(Jpanel)  */
        public static final Type.Function tt = new Type.Function( new Type.Vector(Type.STRING),
                                                                  FN2.tt );
        
        public FN( ) { super(tt); }
        
        /** [paretNames] -> childName -> model -> params -> Object(JComponent) */
        public Value apply( Value v ) 
        {
            // extract a string[] from vector.
            Value.Vector vec = (Value.Vector)v;
            String[] parentName = new String[ vec.length() ];
            for ( int i = 0; i < parentName.length; i++ ) {
                parentName[i] = ((Value.Str)vec.elt(i)).getString();
            }
            return new FN2( parentName );
        }
    }
    
    /** childName -> model -> params -> Object(JComponent) */
    public static class FN2 extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -2650010815249591934L;

        /** Initialise function Type to params -> Object(Jpanel)  */
        public static final Type.Function tt = new Type.Function( Type.STRING, FN3.tt );
        
        String[] parentName;
        public FN2( String[] parentName ) { super(tt); this.parentName = parentName;}
        
        /** childName -> model -> params -> Object(JComponent) */
        public Value apply( Value v ) 
        {
            return new FN3( parentName, ((Value.Str)v).getString() );
        }
    }
    
    /** model -> params -> Object(JComponent) */
    public static class FN3 extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 8396169299830370287L;

        /** Initialise function Type to params -> Object(Jpanel)  */
        public static final Type.Function tt = new Type.Function( Type.MODEL, FN4.tt );
        
        String[] parentName;
        String childName;
        public FN3( String[] parentName, String childName ) { 
            super(tt); 
            this.parentName = parentName;
            this.childName = childName;
        }
        
        /** model -> params -> Object(JComponent) */
        public Value apply( Value v ) 
        {
            return new FN4( parentName, childName, (DTree)v );
        }
    }
    
    /**  params -> Object(JComponent) */
    public static class FN4 extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 452039670198743979L;

        /** Initialise function Type to params -> Object(Jpanel)  */
        public static final Type.Function tt = new Type.Function( Type.TYPE,
                                                                  new Type.Obj("JComponent") );
        
        String[] parentName;
        String childName;
        DTree m;
        public FN4( String[] parentName, String childName, DTree m ) { 
            super(tt); 
            this.parentName = parentName;
            this.childName = childName;
            this.m = m;
        }
        
        /**  params -> Object(JComponent) */
        public Value apply( Value v ) 
        {
            
            DTreeVisualiser vis = new DTreeVisualiser( m, parentName, childName, null );
            vis.updateParams(v);
            return new Value.Obj( vis );
        }
    }
    
    
    
    /** The uparameterised version of the model to be displayed. */
    protected DTree dTree;
    
    /** The current parameters being displayed */
    protected Value params;
    
    /** name of each variable */
    protected String[] parentName;
    
    /** name of the variable being modeled */
    protected String varName;
    
    /** 
     * The next level of visualisation above this one.  Should be null if this is the root of     
     * the DTree.
     */
    protected DTreeVisualiser parentVisualiser;
    
    protected Color getColour( int i )
    {
        switch (i % 2) {
        case 0 : return Color.blue;
            //     case 1 : return Color.yellow;
            //     case 2 : return Color.orange;        
        case 1 : return Color.red;
        }
        throw new RuntimeException("Bad colour?");
    }
    
    
    boolean topLevel = false;
    /** Set the current parameters to a given value. */
    public void updateParams( Value newParams ) 
    {
        if ( this.params == newParams ) return;
        
        this.params = newParams;
        Value.Structured params = (Value.Structured)this.params;// save us doing repeated typecasts.
        int numLeaves =  findNumLeaves( params );
        int depth = findTreeDepth( params )+1;
        
        setLayout( new GridLayout( numLeaves, depth ) );
        
        int currentDepth = 0; // Keep track of how deep into the tree we are
        int currentLeaf = 0;  // Keep track of how many leaf noded have been placed so far.
        Component[][] component = new Component[depth][numLeaves];
        
        
        // Use a stack to keep track of parameters not yet drawn (could also be done recursively.)
        java.util.Stack paramStack = new java.util.Stack();
        paramStack.add(params);
        while( true ) {
            Value.Structured currentParams = (Value.Structured)paramStack.pop();
            int splitAttribute = currentParams.intCmpnt(0);
            
            if ( splitAttribute == -1 ) {
                Value.Structured subModelParams = (Value.Structured)currentParams.cmpnt(2);
                Value.Model subModel = (Value.Model)subModelParams.cmpnt(0);
                Value subParams = subModelParams.cmpnt(1);
                Component leaf = 
                    DualVisualiser.makeComponent(varName, parentName, subModel, subParams );
                
                component[currentDepth][currentLeaf] = leaf;
                
                currentLeaf++;
                
                if ( currentLeaf < numLeaves ) {
                    while ( component[currentDepth-1][currentLeaf] == null ) { currentDepth --; }
                }
                
            }
            else { 
                Value.Vector paramVector = (Value.Vector)currentParams.cmpnt(2);        
                for ( int i = 0; i < paramVector.length(); i++ ) {
                    Value.Structured elt = (Value.Structured)paramVector.elt(paramVector.length()-i-1);
                    paramStack.push( elt );            
                }
                
                
                int x = currentLeaf;
                for ( int value = 0; value < paramVector.length(); value ++ ) {
                    Value.Structured elt = (Value.Structured)paramVector.elt(value);            
                    int subLeaves = findNumLeaves( elt );
                    Color colour = getColour(value);
                    for ( int j = 0; j < subLeaves; j++ ) {
                        if ( component[currentDepth][x] != null )  { throw new RuntimeException("SHouldn't be overwriting! ["+currentDepth+","+x+"]");}
                        if ( j == 0 )
                            component[currentDepth][x] = new JLabel( currentParams.cmpnt(0).toString() 
                                                                     + " = " + value ); 
                        else component[currentDepth][x] = new JLabel("");
                        component[currentDepth][x].setBackground( colour );
                        ((JComponent)component[currentDepth][x]).setOpaque(true);
                        x++;
                    }
                }
                
                currentDepth ++;
            }
            if ( currentLeaf == numLeaves ) break;
        }
        
        for ( int i = 0; i < numLeaves; i++ ) {
            for ( int j = 0; j < depth; j++ ) {        
                //         if ( component[j][i] == null ) component[j][i] = new JLabel("("+j+","+i+")");
                if ( component[j][i] == null ) component[j][i] = new JLabel("");
                this.add( component[j][i] );
            }
        }
    }
    
    
    
    /** Do a quick traversal of the dTree to find the maximum depth reached.*/
    public int findTreeDepth( Value.Structured params )
    {
        int splitAttribute = params.intCmpnt(0);
        
        int maxDepth = 0;
        
        // we have hit a leaf.
        if ( splitAttribute == -1 ) { return 0;}
        else {
            Value.Vector paramVector = (Value.Vector)params.cmpnt(2);
            for ( int i = 0; i < paramVector.length(); i++ ) {
                int newDepth = findTreeDepth((Value.Structured)paramVector.elt(i));
                if ( newDepth > maxDepth ) { maxDepth = newDepth; }
            }
        }
        return maxDepth + 1;
    }
    
    /** Do a quick traversal of the dTree to find the number of leaves on the tree.*/
    public int findNumLeaves( Value.Structured params )
    {
        int splitAttribute = params.intCmpnt(0);
        
        int numLeaves = 0;
        
        // we have hit a leaf.
        if ( splitAttribute == -1 ) { numLeaves = 1;}
        else {
            Value.Vector paramVector = (Value.Vector)params.cmpnt(2);
            for ( int i = 0; i < paramVector.length(); i++ ) {
                numLeaves += findNumLeaves( (Value.Structured)paramVector.elt(i));
            }
        }
        
        return numLeaves;
    }
    
    /**
     * As well as normal parameters, parentVisualiser must also be passed to DTreeVisualiser <br>
     * Visualising a DTree is a recursive process, so each may gain something from knowing it's
     * parent.  If this is the root of the DTree, simply pass null for this parameter.
     **/
    public DTreeVisualiser( DTree dTree, 
                            String[] parentName, 
                            String varName, 
                            DTreeVisualiser parentVisualiser )
    {
        //    super( new GridLayout(1,1) );
        this.dTree = dTree;
        this.parentName = parentName;
        this.varName = varName;           
        this.parentVisualiser = parentVisualiser;
        // this.setMinimumSize( new Dimension(10000,10000) );
        
        
    }
    
}

