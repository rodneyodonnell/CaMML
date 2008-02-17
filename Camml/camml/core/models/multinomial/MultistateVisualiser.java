//
// Bayesian Network Visualiser class
//
// Copyright (C) 2003 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: MuktistateVisualiser.java
// Author: rodo@csse.monash.edu.au


package camml.core.models.multinomial;

import cdms.core.*;

import java.awt.*;
import javax.swing.*;

import cdms.plugin.desktop.VisualizerFN.MultistateVisualizer3;

public class MultistateVisualiser 
extends JPanel
//    implements camml.CDMSUtils.visualiser.Visualiser
{
	
	/** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -2192716039885309287L;

	/** The current parameters being displayed */
	protected Value params;
	
	/** name of the variable being modeled */
	final protected String varName;
	
	/** the panel the scrollpane views. */
	protected JPanel view;
	
	protected static final MultistateVisualizer3 multistateVisualizer = 
		new MultistateVisualizer3( 0 );
	
	Component picture;
	Component label;
	
	/** Set the current parameters to a given value. */
	public void updateParams( Value newParams ) 
	{
		if ( this.params != newParams ) {
			params = newParams;
			if ( picture != null ) {		
				remove( picture );
			}
			picture = multistateVisualizer._apply( newParams );
			add( picture );
			doLayout();
		}
		
	}
	
	public MultistateVisualiser( final String varName )
	{
		super( new FlowLayout() );
		this.varName = varName;       	
		label = new JLabel( "Multistate : " + varName );
		add( label );
		label.setVisible(true);
		this.setVisible(true);
		this.setBackground( Color.green );
	}
	
	
}
