//
// JUnit test routine for Tetrad4 interface to CDMS
//
// Copyright (C) 2005 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: TestTetrad4.java
// Author: rodo@dgs.monash.edu.au

package camml.test.plugin.tetrad4;

import junit.framework.*;

import camml.plugin.tetrad4.*;
import edu.cmu.tetrad.graph.*;

import java.util.List;


/**
 * Some simple tests.
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision $ $Date: 2006/08/22 03:13:42 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/test/plugin/tetrad4/TestTetrad4.java,v $
 */
public class TestTetrad4 extends TestCase
{

	public TestTetrad4(String name) 
	{
		super(name);
	}
	
	protected void setUp() 
	{
	}
	
	public static Test suite() 
	{
		return new TestSuite(TestTetrad4.class);
	}
	

	/** Test SEC enumeration function. */
	public static void testEnumerate() {

		// Create a test graph (a)---(b)---(c) 
		Graph g = new EdgeListGraph();
		GraphNode[] node = new GraphNode[6];
		for ( int i = 0; i < node.length; i++ ) {
			node[i] = new GraphNode("Node_"+i);
			g.addNode( node[i] ); 
		}
		g.addUndirectedEdge( node[0], node[1] );
		g.addUndirectedEdge( node[1], node[2] );


		List list = null;

		// There should be 3 dags in the equivelence class.
		list = Tetrad4.enumerate(g);
		assertTrue( list.size() == 3 );

		// Fully connected 3 node DAG
		g.addUndirectedEdge( node[0], node[2] );
		list = Tetrad4.enumerate(g);
		assertTrue( list.size() == 6 );

		// As above, with extra link to fourth node
		g.addUndirectedEdge( node[0], node[3] );
		list = Tetrad4.enumerate(g);
		assertTrue( list.size() == 8 );

		// As above with single disocnected pair added.
		g.addUndirectedEdge( node[4], node[5] );
		list = Tetrad4.enumerate(g);
		assertTrue( list.size() == 16 );


		// Empty graph
		g.removeEdges( g.getEdges() );
		list = Tetrad4.enumerate(g);
		assertTrue( list.size() == 1 );

		// Invalid SEC.
		// No combination of V structures could ever produce this undirected DAG.
		g.addUndirectedEdge( node[0], node[1] );
		g.addUndirectedEdge( node[0], node[2] );
		g.addUndirectedEdge( node[1], node[3] );
		g.addUndirectedEdge( node[2], node[3] );
		list = Tetrad4.enumerate(g);
		assertTrue( list.size() == 0 );


		// As above with v structures added.
		g.addUndirectedEdge( node[1], node[2] );
		list = Tetrad4.enumerate(g);
		assertTrue( list.size() == 18 );
	}

}
