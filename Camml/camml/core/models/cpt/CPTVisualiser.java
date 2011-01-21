//
// CPT Visualiser class
//
// Copyright (C) 2003 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

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



