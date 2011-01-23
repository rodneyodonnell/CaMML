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
// Bayesian Network Visualiser class
//

// File: BNetVisualiser.java
// Author: rodo@csse.monash.edu.au


package camml.core.models.bNet;

import cdms.core.*;
import cdms.plugin.desktop.DesktopFrame;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import camml.core.models.dual.DualVisualiser;

public class BNetVisualiser 
    extends JPanel 
            //implements camml.CDMSUtils.visualiser.Visualiser
{
    
    
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -9028523089300550496L;

    public static final FN visualiseBNet = new FN();
    
    
    /** model -> params -> Object(JComponent)*/
    public static class FN extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -3392092070618811403L;
        /** Initialise function Type to params -> Object(Jpanel)  */
        public static final Type.Function tt = new Type.Function( Type.MODEL, FN2.tt );
        
        public FN( ) { super(tt); }
        
        /** model -> params -> Object(BNetVisualiser) */
        public Value apply( Value v ) 
        {
            return new FN2( (BNet)v );
        }
    }
    
    /** params -> Object(JComponent)*/
    public static class FN2 extends Value.Function
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -3778153975246424216L;

        /** Initialise function Type to params -> Object(Jpanel)  */
        public static final Type.Function tt = new Type.Function( new Type.Variable(),
                                                                  new Type.Obj("JComponent") );
        
        BNet bNet;
        public FN2( BNet bNet ) { super(tt); this.bNet = bNet;}
        
        /** params -> Object(BNetVisualiser) */
        public Value apply( Value v ) 
        {
            BNetVisualiser vis = new BNetVisualiser( bNet );
            vis.updateParams( v );
            //        return cdms.plugin.desktop.Desktop.defaultVisualizer(  ); 
            return new Value.Obj( vis );
        }
    }
    
    
    
    /** The uparameterised version of the model to be displayed. */
    protected BNet bNet;
    
    /** The current parameters being displayed */
    protected Value.Vector params;
    
    /** name of each variable */
    protected String[] name;
    
    /** list of connections */
    protected int[][] parent;
    
    /** list of parent names. */
    String[][] parentName;
    
    /** subModel describing individual variables  */
    protected Value.Model[] subModel;
    
    /** parameters describing each subModel*/
    protected Value[] subParam;
    
    /** Each variable is represented by a JComponent (which is a child of the main JComponent) */
    protected Component[] subPanel;
    
    /** (X,Y) location relative to an area of size (0,0,1,1) */
    protected double location[][];
    
    
    /** Set the current parameters to a given value. */
    public void updateParams( Value newParams ) 
    {    
        
        if ( this.params != (Value.Vector)newParams ) {
            this.params = (Value.Vector)newParams;
            
            // delete previous subpanels.
            if ( subPanel != null ) {
                for ( int i = 0; i < subPanel.length; i++ ) {
                    subPanel[i].removeNotify();
                }
                subPanel = null;
            }
            
            
            // extract various details from params
            name = bNet.makeNameList( params );
            parent = bNet.makeParentList( params );
            subModel = bNet.makeSubModelList( params );
            subParam = bNet.makeSubParamList( params );        
            int numVariables = params.length();
            
            parentName = new String[numVariables][];
            for ( int i = 0; i < parentName.length; i++ ) {
                parentName[i] = new String[ parent[i].length ];
                for ( int j = 0; j < parentName[i].length; j++ ) {
                    parentName[i][j] = name[ parent[i][j] ];
                }
            }
            
            // Work out which variables should be placed on which tiers.
            int[][] tier = makeTiers( );
            
            
            // Set each location on the appropriate (x,y) positioon
            // This is done by dividing the height into a number of teirs, and placing
            //  each variable on its respective tier.
            location = new double[numVariables][2];
            for ( int i = 0; i < tier.length; i++ ) {
                double y = (double)i / (tier.length - 1.0);
                for ( int j = 0; j < tier[i].length; j++ ) {
                    double x;
                    
                    x = (double)(j+0.5)/( tier[i].length );
                    
                    location[ tier[i][j] ][ 0 ] = y;
                    location[ tier[i][j] ][ 1 ] = x;
                }
            }
            
            
            
            // Create a new JComponent for each variable.
            subPanel = new Component[ name.length ];
            for ( int i = 0; i < subPanel.length; i++ ) {
                
                subPanel[i] = DualVisualiser.makeComponent( name[i], parentName[i], subModel[i], subParam[i] );
                this.add( subPanel[i] );        
            }        
            
            repaint(10);
        }
    }
    
    /**
     * Work out the "optimal" tier for each variable to be placed on. <br>
     *  "Optimal" is defined as a configuration in which no variable is placed before it's 
     *  ancestors.  There may also be some effort to reduce the number of arcs crossing
     *  on the drawn DAG, and also move smoe variables further down in the total ordering
     *  so the graph looks pretty.
     */
    protected int[][] makeTiers( )
    {
        // Store which tier each node is on.  The highest (root cause) is on tier 0.
        int[] tier = new int[parent.length];
        
        for ( int i = 0; i < tier.length; i++ ) {
            tier[i] = 0;
        }
        
        boolean changesMade = true;
        int currentTier = -1;
        int maxTier = 0;
        while ( changesMade == true ) {
            changesMade = false;
            currentTier ++;
            
            for ( int i = 0; i < tier.length; i++ ) {
                if ( tier[i] >= currentTier ) {
                    for ( int j = 0; j < parent[i].length; j++ ) {
                        if ( tier[parent[i][j]] >= tier[i] ) {
                            tier[i] = tier[parent[i][j]] + 1;
                            changesMade = true;
                            if ( tier[i] > maxTier ) {
                                maxTier = tier[i];
                            }
                        }            
                    }
                }
            }
        }
        
        
        // Count the number of entries on each tier.
        int[] tierLength = new int[ maxTier+1 ];
        for ( int i = 0; i < tier.length; i++ ) {
            tierLength[ tier[i] ] ++;
        }
        
        // Allocate memory to store the variable on each tier.
        int[][] tierArray = new int[ maxTier+1 ][];
        for( int i = 0; i < tierArray.length; i++ ) {
            tierArray[i] = new int[ tierLength[i] ];
        }
        
        // current index into tierArray
        int tierCount[] = new int[tierArray.length];
        for ( int i = 0; i < tier.length; i++ ) {        
            tierArray[ tier[i] ][ tierCount[ tier[i]] ] = i;
            tierCount[ tier[i] ] ++;
        }
        
        
        System.out.println();
        int[][] newTiers =  rearrangeTiers( tierArray );
        
        return newTiers;
        //    return tierArray;
    }
    
    
    /**
     *  rearrangeTiers swaps the ordering of variables on a given tier to reduce the number of
     *   arcs crossing.  This uses a simple algorithm finding all offending crosses and attempint
     *   to reverse the nodes.
     */
    protected int[][] rearrangeTiers( final int[][] messyTier )
    {
        // Allocate memory to store the neatened up tiers into.
        int[][] neatTier = new int[messyTier.length][];
        
        // Store the x coordinate of each variable.
        double[] pos = new double[parent.length];
        
        // The average position of the parents of this node.
        double[] parentPos = new double[parent.length];
        
        for ( int i = 0; i < messyTier.length; i++ ) { // loop throgh all tiers
            int[] tier = (int[])messyTier[i].clone();
            
            
            // First pass through we find the average parent value of each variable on 
            // this tier.  Save this to parentPos[]
            for ( int j = 0; j < tier.length; j++ ) {  // loop through all variables on this tier
                // the current variable
                int var = tier[j];  
                
                // parents of the current variable
                int[] parentArray = parent[var]; 
                
                // work out the average value for a parent of the current node.
                double total = 0;
                for ( int k = 0; k < parentArray.length; k++ ) {
                    total += pos[parentArray[k]];
                }
                if (parentArray.length != 0) {
                    parentPos[var] = total / parentArray.length;
                }
                else { parentPos[var] = 0.5; }
            }        
            
            // Perform a bubble sort to rearrange entries on this tier.  The sorting key is the
            // average value of their parents.
            boolean changesMade = true;
            while ( changesMade ) {
                changesMade = false;
                
                for ( int j = 0; j < tier.length - 1; j++ ) {
                    
                    if (  parentPos[tier[j]] > parentPos[tier[j+1]] ) {
                        int temp = tier[j];
                        tier[j] = tier[j+1];
                        tier[j+1] = temp;
                        changesMade = true;
                    }
                }
            }
            
            
            
            // Now save the current x coordinate into pos[]
            for ( int j = 0; j < tier.length; j++ ) {
                pos[ tier[j] ] = (j + 1.0) / (tier.length + 1.0);
            }
            
            // Save the newly created tier so it may be returned later.
            neatTier[i] = tier;
        }
        
        
        
        // Return the result being computed.
        return neatTier;
    }
    
    
    
    /** Override the paintComponent method to do stuff... */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        double height = getHeight();
        double width = getWidth();
        
        double xScale = width * 0.9; 
        double yScale = height * 0.9; 
        double xOffset = width * 0.05; 
        double yOffset = height * 0.05; 
        
        for ( int i = 0; i < parent.length; i++ ) {
            for ( int j = 0; j < parent[i].length; j++ ) {
                int xStart = (int)(location[parent[i][j]][1] * xScale + xOffset );
                int yStart = (int)(location[parent[i][j]][0] * yScale + yOffset 
                                   + subPanel[j].getHeight()/2);
                
                int xFinal = (int)(location[i][1] * xScale + xOffset);
                int yFinal = (int)(location[i][0] * yScale + yOffset 
                                   - subPanel[i].getHeight()/2);
                
                int dX = xFinal - xStart;
                int dY = yFinal - yStart;
                
                g.drawLine ( xStart, yStart, xFinal, yFinal );
                
                // void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle)
                int arrowLength = 10;
                int arrowDegrees = 30;
                g.fillArc(xFinal - arrowLength, yFinal - arrowLength,
                          2 * arrowLength, 2 * arrowLength,
                          (int)(Math.atan2(dY,-dX)*180.0/Math.PI)-arrowDegrees/2, arrowDegrees  );
            }
        }
        
        for ( int i = 0; i < subPanel.length; i++ ) {
            subPanel[i].setLocation( (int)(location[i][1]*xScale + xOffset 
                                           - subPanel[i].getWidth()/2), 
                                     (int)(location[i][0]*yScale + yOffset 
                                           - subPanel[i].getHeight()/2)  );
        }
        
    }
    
    /** Create a new visualiser.  No listener is created. */
    public BNetVisualiser( BNet bNet )
    {
        super( null );  // no layout manager.
        this.bNet = bNet;
        
        setSize(640,480);
        
        // need to check if what is clicked on is this (BNetVisualiser) in mouselistener.
        // not sure how to do this more elegently.  ("this" refers to the mouseadapter when used
        // inside the anonymous class below.)
        //final BNetVisualiser bNetViz = this;
        
        this.addMouseListener( new MouseAdapter() {
                public void mouseClicked( MouseEvent e ) {
                    super.mouseClicked(e);
                
                    // we only care about a double-click.
                    if ( e.getButton() == 1 && e.getClickCount() == 2 ) {            
                        System.out.println("BNet : clicked : " + 
                                           "\tnumClicks = " + e.getClickCount() + 
                                           "\tbutton = " + e.getButton()
                                           );
                    
                        Component child = findComponentAt( e.getX(), e.getY());
                    
                        for ( int i = 0; i < subPanel.length; i++ ) {
                        
                        
                            // if ( child == subPanel[i] ) {
                            // if ( ((java.awt.Container)subPanel[i]).isAncestor(child) ) {
                            if ( subPanel[i] == child ||
                                 ((java.awt.Container)subPanel[i]).isAncestorOf( child ) ) {
                                //Value.Function fn = cdms.plugin.desktop.Desktop.show;
                                //cdms.plugin.desktop.DesktopFrame dtFrame;
                                //dtFrame = cdms.plugin.desktop.Desktop.desktop;
                                Component newChild = DualVisualiser.makeComponent( name[i],
                                                                                   parentName[i],
                                                                                   subModel[i], 
                                                                                   subParam[i] );
                            
                                DesktopFrame.makeWindow( child.toString(), newChild );
                            }
                        
                        }
                    
                    }
                }
            } );
    }
    
}






