//
// Functions for working with Tetrad IV in CDMS.
//
// Copyright (C) 2004 Rodney O'Donnell.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: Tetrad4.java
// Author: rodo@dgs.monash.edu.au

package camml.plugin.tetrad4;


import cdms.core.*;
import camml.core.search.TOM;

import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.*;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

// Included for makeValidNeticaNames()
import camml.plugin.netica.NeticaFn;

/**
 * Class contains various functions for working with Tetrad IV in the CDMS environment,
 *
 * @author Rodney O'Donnell <rodo@dgs.monash.edu.au>
 * @version $Revision: 1.14 $ $Date: 2006/11/13 14:01:11 $
 * $Source: /u/csse/public/bai/bepi/cvs/CAMML/Camml/camml/plugin/tetrad4/Tetrad4.java,v $
 */
public class Tetrad4 {
	
	/** Make tetrad names, uses Netica naming conventions, but uses numbers if 
	 * TetradLearner.useVariableNames == false */
	static String[] makeTetNames( String s[], boolean overwrite ) {
		String[] labels = NeticaFn.makeValidNeticaNames( s, overwrite );
		if (TetradLearner.useVariableNames == false ) {
			labels = new String[labels.length];
			for (int i = 0; i < labels.length; i++) { labels[i] = ""+(i+1); }
		}
		return labels;
	}
	
	/** Convert a discrete CDMS vector into a Tetrad IV style DiscreteDataSet */
	public static RectangularDataSet cdms2TetradDiscrete( Value.Vector vec ) {

		// Convert from CDMS to Tetrad IV data.
		StringBuffer sb = new StringBuffer();
		

		// Print out header info.
		sb.append( "/discretevars\n" );		
		Type.Structured eltType = ((Type.Structured)(((Type.Vector)vec.t).elt));
		Type[] cmpnts = eltType.cmpnts;
		// copy valid versions of name labels to a new array	
		String[] labels = makeTetNames( eltType.labels, false );
		
		for ( int i = 0; i < cmpnts.length; i++ ) {
			sb.append( labels[i]+":" );
			Type.Discrete t = (Type.Discrete)cmpnts[i];
			int arity = (int)(t.UPB - t.LWB)+1;
			for ( int j = 0; j < arity; j++ ) {				
				sb.append( ""+j+"="+j );
				if ( j != arity -1 ) sb.append( "," );
			}
			sb.append("\n");
		}

		// print out data
		sb.append( "\n/discretedata\n" );
		for ( int i = 0; i < labels.length; i++ ) {
			sb.append( labels[i] + "\t" );
		}
		sb.append("\n");

		for ( int i = 0; i < vec.length(); i++ ) {
			Value.Structured elt = (Value.Structured)vec.elt(i);
			for ( int j = 0; j < elt.length(); j++ ) {
				// sb.append( "" + sb.intCmpnt(j) );
				sb.append( "" + elt.intCmpnt(j) + "\t" );
			}
			sb.append( "\n" );
		}


		try {
			File tempDataFile = File.createTempFile("TetradData",null);
			FileWriter fw = new FileWriter(tempDataFile);
			fw.write(sb.toString());
			//File fileString = new null;//sb.toString();
			fw.flush();
			
			RectangularDataSet tetData;
			tetData = DataLoaders.loadDiscreteData(tempDataFile,
												   DelimiterType.WHITESPACE_OR_COMMA, 
												   "//",null);

			System.out.println("# variables = " + tetData.getNumColumns()
							   + ", # cases = " + tetData.getNumRows());

			return tetData;
		} catch ( Exception e ) { throw new RuntimeException(e); }
	}

	/** Convert a Tetrad IV Dag into a CaMML TOM */
	public static TOM dagToTOM( Value.Vector data, Dag dag ) {
		TOM tom = new TOM(data);
		
		// generate a total valid ordering in terms of nodes.
		List order = dag.getTierOrdering();

		// copy order[] to a format recognisable by a TOM
		int[] intOrder = new int[order.size()];
		String[] name = ((Type.Structured)((Type.Vector)data.t).elt).labels;
		// ensure names are valid tetrad names (netica converter used as it has similar rules)
		name = makeTetNames( name, false );
		for ( int i = 0; i < intOrder.length; i++ ) {
			intOrder[ order.indexOf( dag.getNode(name[i]) ) ] = i;
		}

		// Set the total ordering of the TOM
		tom.setOrder( intOrder );


		// Add arcs.
		List nodes = dag.getNodes();
		List edges = dag.getEdges();
		for ( int i = 0; i < edges.size(); i++ ) {
			Edge edge = (Edge)edges.get(i);
			tom.addArc( nodes.indexOf( edge.getNode1() ), 
						nodes.indexOf( edge.getNode2() ) );
		}
		
		return tom;
	}

	/** Enumerate all DAGs consistent with a given Tetrad graph. */
	public static TOM[] enumerateDAGs( Value.Vector data, Graph graph ) {
		List list = enumerate( graph );

		TOM tomArray[] = new TOM[list.size()];
		for ( int i = 0; i < tomArray.length; i++ ) {
			tomArray[i] = dagToTOM( data, (Dag)list.get(i) );
		}
		if ( tomArray.length == 0 ) {
			System.out.println( graph );
			System.out.println( graph.getClass() );
			System.out.println( "graph.existsDirectedCycle() = true" );


 			throw new RuntimeException("Length == 0!!\n" + graph );
		}
		return tomArray;
	}


	/**
	 * Covered returns true if node1, node2 and a third node form a fully
	 * connected structure.  Arc directions are ignored.
	 */
	public static boolean covered( Graph graph, Node node1, Node node2 ) {

		List edge1 = graph.getEdges(node1);
		List edge2 = graph.getEdges(node2);

		// nodeX contains all nodes edge1 connects to
		ArrayList<Node> nodeX = new ArrayList<Node>();
		for (int i = 0; i < edge1.size(); i++) {
			Edge e = (Edge)edge1.get(i);
			if ( e.getNode1() == node1 ) { nodeX.add( e.getNode2() ); }
			else { nodeX.add( e.getNode1() ); }
		}

		// Endure node1 & node2 are connected
		if (!nodeX.contains(node2)) { return false; }

		// Search through edge2 to see if any entries from edge1 are present.
		for (int i = 0; i < edge2.size(); i++) {
			Edge e = (Edge)edge2.get(i);
			Node temp;
			if ( e.getNode1() == node2 ) { temp = e.getNode2(); }
			else { temp = e.getNode1(); }

			if (nodeX.contains(temp)) { return true; }
		}

		return false;
	}

	public static Graph lastBroken = null;
	
	/** 
	 *  Recursively enumerate a tetrad graph into a list of tetrad Dags <br>
	 *  The graph is treated as a SEC for the purpose of enumeration.
	 *  As such only DAGs consisent with the SEC are returned, eg.
	 *  a--b--c  => a->b->c, a<-b->c, a<-b<-c, but not a->b<-c as it is
	 *  not in the same SEC. Graphs which are not consistent with any SECs,
	 *  eg. a->b--c return an empty list.
	 */
	public static List<Dag> enumerate( Graph graph ) {
		List<Dag> list = new ArrayList<Dag>();
		if (graph.existsDirectedCycle()) {
			return list;
		}

		// We require an EndpointMetrixGraph.  If a different
		// graph type is used edges.get(i) may be in a different
		// order.
		if ( !(graph instanceof EndpointMatrixGraph) ) {
			graph = new EndpointMatrixGraph(graph);
		}


		List edges = graph.getEdges();
		for ( int i = 0; i < edges.size(); i++ ) {
			Edge edge = (Edge)edges.get(i);

			// If the edge is not of type --> or <--
			// (ie. <->, <-o, o->, o-o, ---)
			// Then choose to direct it and recur.
			if( !Edges.isDirectedEdge(edge) ) {

				// We can only direct an edge if:
				// - It is the only arc pointing into a node
				// - Arc is "covered"
				if ( graph.getIndegree( edge.getNode1() ) == 0 ||
					 covered(graph, edge.getNode1(), edge.getNode2()) 
					 || Edges.isBidirectedEdge(edge)) {
					//System.out.println("Graph(1) = " + graph );
					//System.out.println("Edge = " + edge );
					if ( Edges.isBidirectedEdge(edge)) { lastBroken = new EndpointMatrixGraph(graph);}
					
					Graph clone1 = new EndpointMatrixGraph(graph);
					clone1.removeEdge( (Edge)clone1.getEdges().get(i) );
					clone1.addDirectedEdge( edge.getNode2(), edge.getNode1() );
					list.addAll( enumerate(clone1) );
					//System.out.println("list.size() = " + list.size() );
				}
				if ( graph.getIndegree( edge.getNode2() ) == 0 ||
					 covered(graph, edge.getNode1(), edge.getNode2()) 
					 || Edges.isBidirectedEdge(edge)) {
					//System.out.println("Graph(2) = " + graph );
					//System.out.println("Edge = " + edge );
					Graph clone2 = new EndpointMatrixGraph(graph);
					clone2.removeEdge( (Edge)clone2.getEdges().get(i) );
					clone2.addDirectedEdge( edge.getNode1(), edge.getNode2() );
					list.addAll( enumerate(clone2) );
					//System.out.println("list.size() = " + list.size() );
				}

				return list;
			}
		}

		// If we make it to here, then only a single DAG is consistent with the graph.
		list.add( new Dag(graph) );

		return list;
	}


	/** Recursively enumerate a tetrad graph into a list of tetrad Dags 
	 *  This function doesn't work correctly as all link combinations are used,
	 *   not just those within the correct SEC
	 */
	public static List enumerate2( Graph graph ) {
		List<Dag> list = new ArrayList<Dag>();
		if (graph.existsDirectedCycle()) {
			return list;
		}

		// We require an EndpointMetrixGraph.  If a different
		// graph type is used edges.get(i) may be in a different
		// order.
		if ( !(graph instanceof EndpointMatrixGraph) ) {
			graph = new EndpointMatrixGraph(graph);
		}

		List edges = graph.getEdges();
		for ( int i = 0; i < edges.size(); i++ ) {
			Edge edge = (Edge)edges.get(i);

			// If the edge is not of type --> or <--
			// (ie. <->, <-o, o->, o-o, ---)
			// Then choose a direct it and recur.
			if( !Edges.isDirectedEdge(edge) ) {
				Graph clone1 = new EndpointMatrixGraph(graph);
				Graph clone2 = new EndpointMatrixGraph(graph);
				
				clone1.removeEdge( (Edge)clone1.getEdges().get(i) );
				clone2.removeEdge( (Edge)clone2.getEdges().get(i) );

				clone1.addDirectedEdge( edge.getNode1(), edge.getNode2() );
				clone2.addDirectedEdge( edge.getNode2(), edge.getNode1() );

				list.addAll( enumerate(clone1) );
				list.addAll( enumerate(clone2) );
				return list;
			}
		}

		// If we make it to here, then only directed arcs are present in graph.
		list.add( new Dag(graph) );
		return list;
	}

	/** Takes a cyclic graph and returns a repaired acyclic graph. 
	 *  The repaired graph will have the same undirected skeleton
	 *   and the arc reversals are chosen to minimise the number
	 *   of V structure changes.
	 *
	 *  If the graph supplied does not contain a directed cycle, 
	 *   it is returned unaltered.
	 *
	 *  NOTE: Currently only works when a single reversal is required
	 *        and does not attempt to minimise V structure changes.
	 */
	public static Graph repair( Graph g ) {
		Graph g2;
		if ( g.existsDirectedCycle() ) {
			// Make a copy of g to attempt repair operations on.
			g2 = new EdgeListGraph(g);

			// Extract list of edges from g2
			List edges = g.getEdges();

			
			// Loop through all edges.
			// If removing an edge makes the graph acyclic, 
			// we can add an edge in the opposite direction 
			// which is consistent with an acyclic graph.
			ArrayList<Graph> validList = new ArrayList<Graph>();
			for ( int i = 0; i < edges.size(); i++ ) {
				Edge edge = (Edge)edges.get(i);
				g2.removeEdge( edge );				
				if ( !g2.existsDirectedCycle() ) {
					// Add a copy of g2 (with an edge reversed) to validList
					Edge edge2 = new Edge( edge.getNode1(), edge.getNode2(),
										   edge.getEndpoint2(), edge.getEndpoint1() );
					g2.addEdge( edge2 );
					validList.add( new EdgeListGraph(g2) );
					g2.removeEdge( edge2 );
				}
				g2.addEdge( edge );
			}

			// validList not contains all acyclic graphs created by removing a single edge.
			// We choose the first graph in validList as our repaired graph.
			g2 = validList.get(0);

			System.err.println( "Repairing : " + g.getEdges() + "\n       as : " + g2.getEdges() );
		} else {
			System.out.println("No repair required.");
			g2 = g; // return original graph
		}
		

		return g2;
	}

}
