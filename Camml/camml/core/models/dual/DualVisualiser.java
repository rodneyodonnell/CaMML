//
// Dual Model Visualiser class
//
// Copyright (C) 2003 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: DualVisualiser.java
// Author: rodo@csse.monash.edu.au


package camml.core.models.dual;

import cdms.core.*;
import java.awt.*;
import javax.swing.*;
import camml.core.models.cpt.*;
import camml.core.models.dTree.*;
import camml.core.models.multinomial.*;

public class DualVisualiser
{
	public static Component makeComponent( String name, String[] parentName, 
			final Value.Model model, Value params )
	{
		Component component;
		
		// If we have a CPT, use CPT Visualiser
		if ( model instanceof CPT ) {
			CPTVisualiser temp = new CPTVisualiser( (CPT)model, parentName, name );
			temp.updateParams( params );
			component = temp;
		}
		// If we have a DTree, use DTree Visualiser
		else if ( model instanceof DTree ) {
			DTreeVisualiser temp  = new DTreeVisualiser( (DTree)model, parentName, name, null );
			temp.updateParams( params );
			component = temp;
		}
		// if we have a multistate, use Multistate visualiser.
		else if ( model instanceof cdms.plugin.model.Multinomial ) {	    
			MultistateVisualiser temp = new MultistateVisualiser( name );
			temp.updateParams( params );
			component = temp;
		}
		// If we do not have a Visualiser for this model type, draw a red box with the
		// variable name in it.
		else {
			//final String varName = name;
			component = new JTextArea("Model = " + model.toString() + "\nparams = " + params.toString()); 
				
//				new JPanel() {
//				/** Override the paintComponent method to do stuff... */
//				public void paintComponent(Graphics g) {
//					super.paintComponent(g);
//					this.setBackground( Color.red );
//					
//					int height = getHeight();
//					int width = getWidth();
//					
//					g.drawString( model.toString(), width / 10, height * 3 / 4);
//					g.drawString( varName, width / 10, height / 2);
//				}
//				
//			};
		}
		
		component.setSize( 160, 80 );	
		
		return component;
	}
	
}
