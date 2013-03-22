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
package camml.plugin.dbn;

import norsys.netica.Environ;
import norsys.netica.Net;
import norsys.netica.NeticaException;
import norsys.netica.Node;
import norsys.netica.NodeList;
import norsys.netica.Streamer;

/**Class for flexibly calculating the edit distance between two DBNs.
 * Edit distances are decomposed - i.e. it is possible to find just the number of
 * reversed intraslice arcs in the first time slice, or the number of missing temporal
 * arcs etc.<br>
 * Edit distances are reported as counts - it is possible to multiply these counts by whatever
 * cost you want (usually 1.0) to arrive at a (possibly weighted) edit distance score.
 * @author Alex Black
 */
public class DBNEditDistance {
	
	public Environ env;
	private boolean finalizeEnv = false;
	
	private String t0_suffix;
	private String t1_suffix;
	
	private int numIntrasliceMissingT0;
	private int numIntrasliceSpuriousT0;
	private int numIntrasliceReversedT0;
	private int numIntrasliceMissingT1;
	private int numIntrasliceSpuriousT1;
	private int numIntrasliceReversedT1;
	private int numTemporalMissing;
	private int numTemporalSpurious;
	private int numTemporalReversed;
	
	/** Alternate constructor.
	 * 
	 * @param file1 File path for original file
	 * @param file2 File path for learned file
	 * @param t0_suffix Suffix for variables in first time slice - i.e. "_0" or "0" etc
	 * @param t1_suffix Suffix for variables in second time slice - i.e. "_1" or "1" etc
	 * @throws Exception
	 */
	public DBNEditDistance( String file1, String file2, String t0_suffix, String t1_suffix ) throws Exception
	{
		this.env = new Environ(null);
		finalizeEnv = true;
		
		this.t0_suffix = t0_suffix;
		this.t1_suffix = t1_suffix;
		
		int[] EDs = CalcDistanceArray( file1, file2 );
		
		numIntrasliceMissingT0 = EDs[3];
		numIntrasliceSpuriousT0  = EDs[4];
		numIntrasliceReversedT0  = EDs[5];
		numIntrasliceMissingT1  = EDs[6];
		numIntrasliceSpuriousT1  = EDs[7];
		numIntrasliceReversedT1  = EDs[8];
		numTemporalMissing  = EDs[9];
		numTemporalSpurious  = EDs[10];
		numTemporalReversed  = EDs[11];
	}
	
	/**Constructor.
	 * 
	 * @param env Netica Environ object. Can be shared if multiple Netica-based classes are used at once
	 *  (i.e. pass the same Environ object to all DBNEditDistance objects, for tests run in parallel)
	 * @param file1 File path for original file
	 * @param file2 File path for learned file
	 * @param t0_suffix Suffix for variables in first time slice - i.e. "_0" or "0" etc
	 * @param t1_suffix Suffix for variables in second time slice - i.e. "_1" or "1" etc
	 * @throws Exception
	 */
	public DBNEditDistance( Environ env, String file1, String file2, String t0_suffix, String t1_suffix ) throws Exception
	{
		this.env = env;
		
		this.t0_suffix = t0_suffix;
		this.t1_suffix = t1_suffix;
		
		//{numMissing, numSpurious, numReversed, numIntrasliceMissingT0, numIntrasliceSpuriousT0, numIntrasliceReversedT0,
		//numIntrasliceMissingT1, numIntrasliceSpuriousT1, numIntrasliceReversedT1, numTemporalMissing, numTemporalSpurious, numTemporalReversed };
		int[] EDs = CalcDistanceArray( file1, file2 );
		
		numIntrasliceMissingT0 = EDs[3];
		numIntrasliceSpuriousT0  = EDs[4];
		numIntrasliceReversedT0  = EDs[5];
		numIntrasliceMissingT1  = EDs[6];
		numIntrasliceSpuriousT1  = EDs[7];
		numIntrasliceReversedT1  = EDs[8];
		numTemporalMissing  = EDs[9];
		numTemporalSpurious  = EDs[10];
		numTemporalReversed  = EDs[11];
	}
	
	public int getNumIntrasliceMissingT0(){ return numIntrasliceMissingT0; }
	public int getNumIntrasliceSpuriousT0(){ return numIntrasliceSpuriousT0; }
	public int getNumIntrasliceReversedT0(){ return numIntrasliceReversedT0; }
	public int getNumIntrasliceMissingT1(){ return numIntrasliceMissingT1; }
	public int getNumIntrasliceSpuriousT1(){ return numIntrasliceSpuriousT1; }
	public int getNumIntrasliceReversedT1(){ return numIntrasliceReversedT1; }
	public int getNumTemporalMissing(){ return numTemporalMissing; }
	public int getNumTemporalSpurious(){ return numTemporalSpurious; }
	public int getNumTemporalReversed(){ return numTemporalReversed; }
	
	
	private int[] CalcDistanceArray( String file1, String file2 ) throws Exception
	{
		if( t0_suffix == null || t1_suffix == null || t0_suffix.equals(t1_suffix) ) throw new Exception("Error in timeslice variable name suffixes passed!");
			
		
		Net net1;
		Net net2;
		int numNodes1;
		int numNodes2;
		
		//Try to open two files:
		
		try{
			net1 = new Net( new Streamer( file1 ) );
		} catch( NeticaException e ){
			throw new Exception("Problem opening file1 - " + file1 + "\n" + e );
		}
		try{
			net2 = new Net( new Streamer( file2 ) );
		} catch( NeticaException e ){
			throw new Exception("Problem opening file2 - " + file2 + "\n" + e );
		}
		
		//Get the number of nodes in each network:
		try{
			numNodes1 = net1.getNodes().toArray().length;
			numNodes2 = net2.getNodes().toArray().length;
		} catch( NeticaException e ){
			throw new Exception("Problem with net.getNodes() " + e );
		}
		
		if( numNodes1 != numNodes2 || numNodes1 == 0 || numNodes2 == 0 ){
			throw new Exception("Error in number of nodes in one/both networks:  (file1=" + numNodes1 +"; file2=" + numNodes2  + ")");
		}
		
		//Get names for nodes in each network:
		NodeList nodeList1 = net1.getNodes();
		NodeList nodeList2 = net2.getNodes();
		Object[] temp1 = nodeList1.toArray();
		Object[] temp2 = nodeList2.toArray();
		Node[] nodes1 = new Node[ temp1.length ];
		Node[] nodes2 = new Node[ temp2.length ];
		String[] names1 = new String[numNodes1];
		String[] names2 = new String[numNodes2];
		
		
		for( int i=0; i < temp1.length; i++ ){
			nodes1[i] = (Node)temp1[i];
			nodes2[i] = (Node)temp2[i];
			names1[i] = nodes1[i].getName();
			names2[i] = nodes2[i].getName();
		}
		
		
		//Now, check that the names are the same; build a mapping from nodes1 to nodes2
		// such that nodes1[i] == nodes2[ map[i] ]
		int[] map = new int[numNodes1];
		for( int i=0; i<numNodes1; i++ ){
			boolean n1Found = false;
			for( int j=0; j<numNodes1; j++ ){
				if( names1[i].equals(names2[j]) ){
					n1Found = true;
					map[i]=j;
					break;
				}
			}
			if( n1Found == false ) throw new Exception("Error: Names in networks are different!");
		}

		
		
		//At this point: Can start to actually calculate the edit distance...
		System.out.println("Networks OK... Calculating edit distance");
		int numMissing = 0;
		int numSpurious = 0;
		int numReversed = 0;
		int numIntrasliceMissingT0 = 0;
		int numIntrasliceSpuriousT0 = 0;
		int numIntrasliceReversedT0 = 0;
		int numIntrasliceMissingT1 = 0;
		int numIntrasliceSpuriousT1 = 0;
		int numIntrasliceReversedT1 = 0;
		int numTemporalMissing = 0;
		int numTemporalSpurious = 0;
		int numTemporalReversed = 0;
		
		//Loop through each node...
		for( int i=0; i<numNodes1; i++ ){
			Node[] np1 = nodeListToNodeArray( nodes1[i].getParents() );
			Node[] np2 = nodeListToNodeArray( nodes2[map[i]].getParents() );
			
			//MISSING ARCS: Find parents that network 1 node has that network 2 node does not have:
			for( int a=0; a<np1.length; a++ ){	//Loop over node1 parents
				boolean found = false;
				for( int b=0; b<np2.length; b++ ){	//Loop over node2 parents
					//if( np1[a].equals(np2[b]) ){
					if( np1[a].getName().equals( np2[b].getName() )){
						found = true;
						break;
					}
				}
				if( found == false ){	//Missing! Arc in net1 not present in net2; np1[a]->nodes1[i] not present!
					if( nodes1[i].getName().endsWith(t0_suffix) && np1[a].getName().endsWith(t0_suffix) ){	//Intraslice, t0
						//Is the arc missing, or just reversed?
						//if( !isArc(nodes1[i],np1[a]) ){
						if( isArc( net2, nodes1[i].getName(), np1[a].getName() ) ){		//Is opposite present in net2?
							numIntrasliceReversedT0++;	//If so: Reversed
							numReversed++;
						}else{
							numIntrasliceMissingT0++;	//If not: Missing
							numMissing++;
						}
					} else if( nodes1[i].getName().endsWith(t1_suffix) && np1[a].getName().endsWith(t1_suffix) ){
						//Node np1[a]->nodes1[i] possibly missing. If reversed, then nodes2[map[i]]->np1[a] present
						//if( isArc(nodes1[i],np1[a]) ){	// arc in n2, child -> parent?
						//if( isArc(nodes2[map[i]],np1[a]) ){	// arc in n2, child -> parent?
						if( isArc(net2,nodes2[map[i]].getName(), np1[a].getName() ) ){
							numIntrasliceReversedT1++;	//Reversed
						}else{
							numIntrasliceMissingT1++;	//Missing
							numMissing++;
							//System.out.println( "MISSING T1++: " + nodes1[i].getName() + " <- " + np1[a].getName() );
							//System.out.println( "NO ARC in net2: " + nodes2[map[i]].getName() + "->" + np1[a] );
						}
					} else if( nodes1[i].getName().endsWith(t1_suffix) && np1[a].getName().endsWith(t0_suffix) ){
						//Parent in t0, child in t1
						//if( !isArc(nodes1[i],np1[a]) ){
						if( isArc(net2, nodes1[i].getName(), np1[a].getName() ) ){	//Is opposite present in net2?
							numTemporalReversed++;	//If so: Reversed
						}else{
							numTemporalMissing++;	//If not: Missing
							numMissing++;
						}
					} else if( nodes1[i].getName().endsWith(t0_suffix) && np1[a].getName().endsWith(t1_suffix) ){
						System.out.println("ERROR: Network 1 has temporal arc in wrong direction: " + nodes1[i].getName() + " <- " + np1[a].getName() );
					}
				} else{
					//System.out.println( "Arc in both: " + nodes1[i].getName() + " <- " + np1[a].getName() );
				}
			}
			
			//SPURIOUS ARCS (or reversed): Find parents that network 2 node has that network 1 node does not have:
			for( int a=0; a<np2.length; a++ ){	//Loop over node2 parents
				boolean found = false;
				for( int b=0; b<np1.length; b++ ){	//Loop over node1 parents
					if( np2[a].getName().equals( np1[b].getName() )){
						found = true;
						break;
					}
				}
				if( found == false ){	//Arc in net2 that is not in net1.
					if( nodes2[map[i]].getName().endsWith(t0_suffix) && np2[a].getName().endsWith(t0_suffix) ){
						//Might be reversed, might be spurious...
						if( isArc( net1, nodes2[map[i]].getName(), np2[a].getName() ) ){
							//Reversed, but already counted earlier...
						} else {
							numIntrasliceSpuriousT0++;
						}
					}else if( nodes2[map[i]].getName().endsWith(t1_suffix) && np2[a].getName().endsWith(t1_suffix) ){
						//Could be reversed, could be spurious
						if( isArc(net1, nodes2[map[i]].getName(), np2[a].getName() ) ){		//Reverse arc in net1 present?
							//Is reversed, but would have already been counted (as present in net1, not in net2)
							//System.out.println( "REVERSED T1++: " + nodes2[map[i]].getName() + " <- " + np2[a].getName() );
						} else {	//If it's not reversed, then it must be spurious
							numIntrasliceSpuriousT1++;
							//System.out.println( "SPURIOUS T1++: " + nodes2[map[i]].getName() + " <- " + np2[a].getName() );
						}
					}else if( nodes2[map[i]].getName().endsWith(t1_suffix) && np2[a].getName().endsWith(t0_suffix) ){
						//Parent in t0, child in t1
						numTemporalSpurious++;		//Can't be reversed, t1 -> t0
					}else if( nodes2[map[i]].getName().endsWith(t1_suffix) && np2[a].getName().endsWith(t0_suffix) ){
						System.out.println("ERROR: Network 2 has temporal arc in wrong direction: " + nodes2[map[i]].getName() + " <- " + np2[a].getName() );
						//if( !isArc(nodes2[map[i]],np2[a]) ){
						//Is the arc present in node1, but reversed?
						if( !isArc( net1, nodes2[map[i]].getName(), np2[a].getName() ) ){
							numTemporalSpurious++;	//Spurious
							numSpurious++;
						}else{
							numIntrasliceReversedT1++;	//Reversed
						}
					}
					numSpurious++;
				} else{
					//System.out.println( "Arc in both: " + nodes1[i].getName() + " <- " + np1[a].getName() );
				}
			}
			
			//System.out.println("-------------------");
		}
		
		/*
		System.out.println("Arcs missing: " + numMissing );
		System.out.println("Arcs spurious: " + numSpurious );
		System.out.println("Arcs reversed: " + numReversed );
		System.out.println(" --- Intraslice Arcs: --- ");
		System.out.println("Intraslice arcs missing (t0): " + numIntrasliceMissingT0 );
		System.out.println("Intraslice arcs spurious (t0): " + numIntrasliceSpuriousT0 );
		System.out.println("Intraslice arcs reversed (t0): " + numIntrasliceReversedT0 );
		System.out.println("Intraslice arcs missing (t1): " + numIntrasliceMissingT1 );
		System.out.println("Intraslice arcs spurious (t1): " + numIntrasliceSpuriousT1 );
		System.out.println("Intraslice arcs reversed (t1): " + numIntrasliceReversedT1 );
		System.out.println(" --- Temporal Arcs: --- ");
		System.out.println("Temporal arcs missing: " + numTemporalMissing );
		System.out.println("Temporal arcs spurious: " + numTemporalSpurious );
		System.out.println("Temporal arcs reversed: " + numTemporalReversed );
		*/
		if( finalizeEnv ){
			env.finalize();
			env = null;
		}
		
		return new int[]{numMissing, numSpurious, numReversed, numIntrasliceMissingT0, numIntrasliceSpuriousT0, numIntrasliceReversedT0,
				numIntrasliceMissingT1, numIntrasliceSpuriousT1, numIntrasliceReversedT1, numTemporalMissing, numTemporalSpurious, numTemporalReversed };
		
	}
	
	//Check if there exists an arc from n1 -> n2
	/*
	private static boolean isArc( Node n1, Node n2 ) throws NeticaException
	{
		Node[] n2parents = nodeListToNodeArray( n2.getParents() );
		//Loop through list of parents, to see if n1 == n2parents[i]
		for( Node pa : n2parents ){
			if( n1.getName().equals(pa.getName()) ) return true;
			//TESTING CODE:
			System.out.println("NOT EQUAL: " + n1.getName() + " - " + pa.getName() );
		}
		return false;
	}*/
	
	private static boolean isArc( Net net, String from, String to ) throws NeticaException
	{
		Node child = net.getNode(to);
		Node[] parents = nodeListToNodeArray( child.getParents() );
		for( Node pa : parents ){
			if( pa.getName().equals(from) ) return true;
		}
		return false;
	}
	
	private static Node[] nodeListToNodeArray( NodeList nodeList ){
		Object[] temp = nodeList.toArray();
		Node[] nodes = new Node[ temp.length ];
		for( int i=0; i < temp.length; i++ ) nodes[i] = (Node)temp[i];
		return nodes;
	}

}
