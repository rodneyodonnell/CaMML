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
// Test Simple Search for CaMML
//

// File: TestUltraSimpleSearch.java
// Author: rodo@dgs.monash.edu.au


package camml.test.core.search;

import junit.framework.*;

import cdms.core.*;
import cdms.plugin.search.*;
import camml.core.library.BlockingSearch;
import camml.core.search.*;

import javax.swing.*;



public class TestUltraSimpleSearch extends TestCase
{
    protected Value.Vector commonCauseDataset;
    
    public TestUltraSimpleSearch(String name) 
    {
        super(name);
    }
    
    // Create some data sets to test with.
    protected void setUp() 
    {
        commonCauseDataset = 
            SearchDataCreator.generateCommonCauseDataset( new java.util.Random(123), 200 );
    }
    
    public static Test suite() 
    {
        return new TestSuite(TestUltraSimpleSearch.class);
    }
    
    public void testSearchInterface()
    {
        System.out.println("Mem : " + getClass() + "\t" + camml.test.core.search.TestCases.mem() );

        JFrame f = new JFrame("Simple Search Object");
        Value.Vector dataset = 
            SearchDataCreator.generateWallaceKorbStyleDataset(new java.util.Random(123),100,2,2,3);
        Search s = new BlockingSearch( new UltraSimpleSearch( new java.util.Random(12345), dataset ));
        f.getContentPane().add(new SearchControl(s,new JLabel("UltraSimpleSearch")));
        f.pack();
        //f.setVisible(true);
        
        s.start();
    }
    
    
    
    /** Test main function. */
    public static void main( String args[] )
    {
        if (args.length != 2)
            throw new IllegalArgumentException("Correct Syntax java <prog> seed samples");
        
        java.util.Random rand = new java.util.Random( (Integer.parseInt(args[0])+1)*777 );
        Value.Vector data = SearchDataCreator.generateCommonCauseDataset(
                                                                         new java.util.Random(Integer.parseInt(args[0])),
                                                                         Integer.parseInt(args[1]));
        
        
        JFrame f = new JFrame("Sample Search Object");
        Search s = new Search(new UltraSimpleSearch( rand, data ));
        f.getContentPane().add(new SearchControl(s,new JLabel("Hello")));
        f.pack();
        f.setVisible(true);
    }
    
    
}
