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
// Dual Model Visualiser class
//

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
                
            //                new JPanel() {
            //                /** Override the paintComponent method to do stuff... */
            //                public void paintComponent(Graphics g) {
            //                    super.paintComponent(g);
            //                    this.setBackground( Color.red );
            //                    
            //                    int height = getHeight();
            //                    int width = getWidth();
            //                    
            //                    g.drawString( model.toString(), width / 10, height * 3 / 4);
            //                    g.drawString( varName, width / 10, height / 2);
            //                }
            //                
            //            };
        }
        
        component.setSize( 160, 80 );    
        
        return component;
    }
    
}
