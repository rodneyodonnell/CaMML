/*
 *  [The "BSD license"]
 *  Copyright (c) 2002-2011, Lucas Hope, Rodney O'Donnell
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
// JUnit test routine for TOM model for CDMS
//

// File: TestTOM.java
// Author: lhope@csse.monash.edu.au
// Modified : rodo@dgs.monash.edu.au

package camml.test.core.search;

import junit.framework.*;

import cdms.core.*;
import camml.core.search.*;

// import camml.core.models.*;
// import camml.ModelPackage.*;

/**
 * Some simple tests.
 *
 */
public class TestTOM extends TestCase
{
    Value.Vector data;
    
    public TestTOM(String name) 
    {
        super(name);
    }
    
    protected void setUp() 
    {
        data = SearchDataCreator.generateWallaceKorbStyleDataset( new java.util.Random(123),
                                                                  1000, 2,2,2 );
    }
    
    public static Test suite() 
    {
        return new TestSuite(TestTOM.class);
    }
    
    
    public void testConstructor() 
    {
        TOM newTOM = new TOM(data);
        
        assertEquals(8, newTOM.getNumNodes());  // tests getNumNodes too.
        
        assertEquals(data, newTOM.getData());   // tests getData too.
    }
    
    
    public void testArcOperations()
    {
        TOM newTOM = new TOM(data);
        //isArc
        for(int i = 0; i < newTOM.getNumNodes(); i ++) {
            for(int j = 0; j < newTOM.getNumNodes(); j ++) {
                assertEquals(false, newTOM.isArc(i, j));
            }
        }
        
        
        //addArc
        for(int i = 0; i < newTOM.getNumNodes(); i ++) {
            for(int j = 0; j <= i; j ++) 
                { assertEquals((i == j) ? false : true, newTOM.addArc(i, j)); }
        }
        
        for(int i = 0; i < newTOM.getNumNodes(); i ++) {
            for(int j = 0; j < newTOM.getNumNodes(); j ++) {
                if(i == j)
                    { assertEquals(false, newTOM.isArc(i, j)); }
                else
                    { assertEquals(true, newTOM.isArc(i, j)); }
                
            }
        }
        
        //removeArc
        for(int i = 0; i < newTOM.getNumNodes(); i ++) {
            for(int j = 0; j <= i; j ++) 
                { assertEquals((i == j) ? false : true, newTOM.removeArc(i, j)); }
        }
        
        for(int i = 0; i < newTOM.getNumNodes(); i ++) {
            for(int j = 0; j < newTOM.getNumNodes(); j ++)
                { assertEquals(false, newTOM.isArc(i, j)); }
        }
    }
    
    
    public void testIsAncestor()
    {
        // build TOM
        TOM newTOM = new TOM(data);
        // order starts ascending.
        newTOM.addArc(0, 2);
        newTOM.addArc(1, 2);
        newTOM.addArc(2, 3);
        
        assertEquals(true, newTOM.isAncestor(0, 2));
        assertEquals(false, newTOM.isAncestor(0, 1));
        assertEquals(false, newTOM.isAncestor(2, 1));
        assertEquals(true, newTOM.isAncestor(1, 3));
        assertEquals(false, newTOM.isAncestor(2, 2));
        
    }
    
    public void testIsDescendant()
    {
        // build TOM
        TOM newTOM = new TOM(data);
        // order starts ascending.
        newTOM.addArc(0, 2);
        newTOM.addArc(1, 2);
        newTOM.addArc(2, 3);
        
        assertEquals(true, newTOM.isDescendant(2, 0));
        assertEquals(false, newTOM.isDescendant(1, 0));
        assertEquals(false, newTOM.isDescendant(1, 2));
        assertEquals(true, newTOM.isDescendant(3, 1));
        assertEquals(false, newTOM.isDescendant(2, 2));
        
        
    }
    
    public void testIsCorrelated()
    {
        // build TOM
        TOM tom = new TOM(data);
        // order starts ascending.
        tom.addArc(0, 2); 
        tom.addArc(0, 3);
        tom.addArc(1, 2); 
        tom.addArc(1, 4);
        tom.addArc(2, 7);
        tom.addArc(4, 5);
        tom.addArc(5, 6);
                
        for ( int i = 0; i < 8; i++) {
            for ( int j = 0; j < 8; j++) {
                if (i != j && (tom.isDescendant(i,j) || tom.isAncestor(i,j))) {
                    assertTrue( tom.isCorrelated(i,j));
                    assertTrue( tom.isCorrelated(j,i));
                }
            }
        }

        assertTrue( tom.isCorrelated(3,2) );
        assertTrue( tom.isCorrelated(3,7) );        
        assertTrue( tom.isCorrelated(6,7) );
        
        assertFalse( tom.isCorrelated(3,6) );
        assertFalse( tom.isCorrelated(3,1) );
    }
    
    /** Search for x and y in order and swap there positions. */
    protected void swap( int[] order, int x, int y ) {
        int xPos = -1;
        int yPos = -1;
        for ( int i = 0; i < order.length; i++ ) {
            if ( order[i] == x ) xPos = i;
            if ( order[i] == y ) yPos = i;
        }
        int tmp = order[xPos];
        order[xPos] = order[yPos];
        order[yPos] = tmp;
    }
    
    
    /** Test swapOrder with and without updateNodes */
    public void testSwapOrder()
    {
        
        // Initialise test TOMs
        TOM emptyTOM = new TOM(data);
        TOM fullTOM = new TOM(data);
        TOM partialTOM = new TOM(data);
        
        partialTOM.addArc(1,5);
        partialTOM.addArc(4,5);
        partialTOM.addArc(6,7);
        partialTOM.addArc(3,1);
        
        for ( int i = 0; i < fullTOM.getNumNodes(); i++ ) {
            for ( int j = i + 1; j < fullTOM.getNumNodes(); j++ ){
                fullTOM.addArc(i,j);
            }
        }
        
        
        
        
        // Create a fully ordered set.
        int[] order = new int[ emptyTOM.getNumNodes() ];
        for ( int i = 0; i < order.length; i++ ) { order[i] = i; }
        
        // Randomly permute order 100 times testing for errors.
        java.util.Random rand = new java.util.Random(123);
        for ( int i = 0; i < 100; i ++ ) {
            int x = rand.nextInt( order.length );
            int y = rand.nextInt( order.length );
            
            emptyTOM.swapOrder( x, y, true );
            fullTOM.swapOrder( x, y, true );
            partialTOM.swapOrder( x, y, true );
            swap( order, x, y );
            
            for ( int j = 0; j < emptyTOM.getNumNodes(); j++ ) {
                assertEquals( order[j], emptyTOM.nodeAt(j) );
                assertEquals( order[j], fullTOM.nodeAt(j) );
                assertEquals( order[j], partialTOM.nodeAt(j) );
                
                assertEquals( j, emptyTOM.nodeAt( emptyTOM.getNodePos(j) ) );
                assertEquals( j, fullTOM.nodeAt( fullTOM.getNodePos(j) ) );
                assertEquals( j, partialTOM.nodeAt( partialTOM.getNodePos(j) ) );
                
            }
        }
        
    }
    
    public void testOrder()
    {
    }

    
    /**
     *  test TOM.clone() function <br>
     *  - Check arc[][] and order[] are done as a deep copy. <br>
     */
    public void testClone()
    {
        // build TOM
        TOM newTOM = new TOM(data);
        // order starts ascending.
        newTOM.addArc( 0, 2 );
        newTOM.addArc( 1, 2 );
        newTOM.addArc( 2, 3 );
        
        // New order should be {2,1,0,3}
        newTOM.swapOrder( newTOM.nodeAt(0), newTOM.nodeAt(2), false );
        
        
        // clone TOM.  Node & data should be a shallow copy, order & arc should be a deep copy.
        TOM clonedTOM = (TOM)newTOM.clone();
        
        // Check everything starts out equal
        assertEquals( newTOM.isArc(2,0), clonedTOM.isArc(2,0) );
        assertEquals( newTOM.isArc(0,3), clonedTOM.isArc(0,3) );
        assertEquals( newTOM.nodeAt(0), newTOM.nodeAt(0) );
        assertEquals( newTOM.nodeAt(3), newTOM.nodeAt(3) );
        assertEquals( newTOM.getData(), clonedTOM.getData() );    
        
        // Make some modifications and check deep copies remain unchanged.
        clonedTOM.addArc(0,3);
        clonedTOM.swapOrder(clonedTOM.nodeAt(0),clonedTOM.nodeAt(1),false);  // new Order {1,2,0,3}
        
        assertEquals( newTOM.isArc(2,0), clonedTOM.isArc(2,0) );
        assertEquals( false, newTOM.isArc(0,3) );
        assertEquals( true, clonedTOM.isArc(0,3) );
        
        assertEquals( 2, newTOM.nodeAt(0) );
        assertEquals( 1, clonedTOM.nodeAt(0) );
        assertEquals( newTOM.nodeAt(3), newTOM.nodeAt(3) );
        
        
    }

    public void testSetStructure()
    {
        // build TOM
        TOM tom = new TOM(data);        

        // randomise order
        java.util.Random rand = new java.util.Random(123);
        tom.randomOrder( rand );        

        // add some arcs
        tom.addArc( 0, 2 );
        tom.addArc( 0, 4 );
        tom.addArc( 2, 3 );
        tom.addArc( 1, 0 );
        tom.addArc( 5, 2 );
        tom.addArc( 0, 4 );
        
        // make a backup of tom
        TOM cloneTOM = (TOM)tom.clone();
        assertTrue( tom.equals( cloneTOM) );

        // Parameterise
        Value.Vector params;
        try { params = tom.makeParameters( SearchPackage.mmlCPTLearner  ); }
        catch ( camml.core.models.ModelLearner.LearnerException e ) {
            throw new RuntimeException(e);    }
        assertTrue( tom.equals( cloneTOM) );


        // Modify ordering of original TOM
        tom.randomOrder( rand );
        assertFalse( tom.equals( cloneTOM) );

        // Set structure based on original TOM
        tom.setStructure( params );
        assertTrue( tom.equals( cloneTOM) );


        // Modify ordering & add arcs to original TOM
        tom.randomOrder( rand );
        tom.addArc( 4, 2 );
        tom.addArc( 0, 7 );
        assertFalse( tom.equals( cloneTOM) );

        // Set structure based on original TOM
        tom.setStructure( params );
        assertTrue( tom.equals( cloneTOM) );
    }

}
