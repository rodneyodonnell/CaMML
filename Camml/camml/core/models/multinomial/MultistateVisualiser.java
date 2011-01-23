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
