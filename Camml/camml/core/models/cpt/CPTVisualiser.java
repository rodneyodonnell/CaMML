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
// CPT Visualiser class
//

// File: CPTVisualiser.java
// Author: rodo@csse.monash.edu.au


package camml.core.models.cpt;

import cdms.core.*;

import java.awt.*;
import javax.swing.*;

import camml.core.models.dual.DualVisualiser;

public class CPTVisualiser 
    extends JPanel
            //    implements camml.CDMSUtils.visualiser.Visualiser
{
    
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -110119595095099460L;

    /** The uparameterised version of the model to be displayed. */
    protected CPT cpt;
    
    /** The current parameters being displayed */
    protected Value.Vector params;
    
    /** name of each variable */
    protected String[] parentName;
    
    /** name of the variable being modeled */
    protected String varName;
    
    /** subModel describing individual variables  */
    protected Value.Model[] subModel;
    
    /** parameters describing each subModel*/
    protected Value[] subParam;
    
    /** Each variable is represented by a JPanel (which is a child of the main JPanel) */
    protected JPanel[] subPanel;
    
    /** Scroll Pane so that all sub components may be seen.*/
    protected JScrollPane scrollPane;
    
    /** the panel the scrollpane views. */
    protected JPanel view;
    
    /** Set the current parameters to a given value. */
    public void updateParams( Value newParams ) 
    {
        if ( this.params != (Value.Vector)newParams ) {
            this.params = (Value.Vector)newParams;
            
            this.setBackground( Color.yellow );
            
            add( new JLabel("CPT : " ) );
            add( new JLabel(varName) );
            for ( int i = 0; i < cpt.numCombinations; i++ ) {
                
                String str = "";
                int[] parent = cpt.encodeParents(i);
                for ( int j = 0; j < parent.length; j++ ) {
                    str += parentName[j] + " = " + parent[j] + "\t";
                }
                
                JLabel label = new JLabel("state["+i+"] : " + str);
                add( label );
                
                
                Value.Structured elt = (Value.Structured)params.elt(i);
                
                Value.Model subModel = (Value.Model)elt.cmpnt(0);
                Value.Structured subParams = (Value.Structured)elt.cmpnt(1);
                
                add( DualVisualiser.makeComponent( varName,
                                                   parentName, 
                                                   subModel, 
                                                   subParams ));
            }
            
        }
    }
    
    /** Override the paintComponent method to do stuff... */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        //int height = getHeight();
        //int width = getWidth();
    }
    
    public CPTVisualiser( CPT cpt, String[] parentName, String varName )
    {
        super( new GridLayout(cpt.numCombinations + 1,2) );
        this.cpt = cpt;
        this.parentName = parentName;
        this.varName = varName;           
    }
    
}



