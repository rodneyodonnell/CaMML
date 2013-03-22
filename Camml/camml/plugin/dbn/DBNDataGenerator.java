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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Random;


import norsys.netica.Environ;
import norsys.netica.Net;
import norsys.netica.NeticaException;
import norsys.netica.Node;
import norsys.netica.NodeList;
import norsys.netica.Streamer;

/**Class for generating a time series from a DBN, and then exporting the data as an .arff file.
 * 
 * Methods make a number of assumptions:<br>
 * - Network is in Netica format (.dne or .neta)<br>
 * - There are exactly two time slices<br>
 * - There are exactly the same number of variables in each time slice<br>
 * - The causal structure of the variables in each time slice is identical<br>
 * - The variables in each time slice are named identically, except for their suffixes, such as "_0" and "_1"<br>
 * - The variable suffixes are exactly the same for each variable in that timeslice<br>
 * Note: Not all of these assumptions are explicitly checked!
 * 
 * @author Alex Black
 *
 */
public class DBNDataGenerator {

	protected Random rand;
	
	protected String filepath;
	
	protected boolean finalizeEnviron;
	protected Environ env;	//For Netica...
	protected Net net;						//The actual network
	protected Node[][] orderedNodeArray = null;
	
	//Details regarding the network:
	int numNodes;
	String t0_suffix;
	String t1_suffix;
	
	/** Pass a DBN file path */
	public DBNDataGenerator( String filepath, String t0_suffix, String t1_suffix ) throws Exception	{
		this( new Environ(null), filepath, t0_suffix, t1_suffix );
		finalizeEnviron = true;
	}
	
	/** Pass a DBN file path, and a Netica Environ object. Note that if multiple DBNDataGenerator objects are created at
	 * once (i.e. during multi-threaded testing) then it is necessary to pass the same Environ object to all of them.
	 * (In short, Netica will throw an exception otherwise...)
	 * @param env Netica Environ object. May be shared between multiple DBNDataGenerator objects.
	 * @param filepath The location of the Netica BN to open.
	 * @param t0_suffix The suffix of the variable names for the first time slice. i.e. if name is "NodeX0" then suffix is "0"
	 * @param t1_suffix The suffix of the variable names for the second time slice. i.e. if name is "NodeX_1" then suffix is "1"
	 * @throws Exception For IO problems, Netica etc.
	 */
	public DBNDataGenerator( Environ env, String filepath, String t0_suffix, String t1_suffix ) throws Exception	{
		this.filepath = filepath;
		this.env = env;
		
		try{
			net = new Net( new Streamer( filepath ) );
		} catch( NeticaException e ){
			throw new Exception( "Problem opening file. " + e);
		}
		
		//First: Do some basic checks to see if it is a DBN of the required format...
		numNodes = net.getNodes().toArray().length;
		if( numNodes % 2 != 0 ) throw new Exception("Error: Odd # nodes in DBN (numNodes =" + numNodes + ")" );
		
		rand = new Random();
		this.t0_suffix = t0_suffix;
		this.t1_suffix = t1_suffix;
		if( !checkSuffix() ) throw new Exception("Error in name suffixes!");
		finalizeEnviron = false;
	}

	/**Generate a set of data (as a String array) from the DBN passed to the constructor. <br>
	 * exportARFF and exportCAS functions will probably be more useful. 
	 * @param dataLength Number of elements in the time series (i.e. length of data set)
	 */
	public String[][] generateDataString( int dataLength ) throws Exception	{
		
		if( this.orderedNodeArray == null ) orderedNodeArray = getOrderedNodeList();
		
		net.compile();
		
		try{
			if( !checkSuffix() ) throw new Exception("Error in supplied suffixes relative to variable names");
		} catch( NeticaException e ){
			throw new Exception(e);
		}
		
		int[] t0Assignment = new int[numNodes/2];
		int[] t1Assignment = new int[numNodes/2];
		
		//Remove all findings (observations) from the BN:
		net.retractFindings();
		
		String[][] str = new String[dataLength][numNodes/2];
		
		//Generate the very first set of observations, for time step t0:
		Node n;
		for( int i=0; i<numNodes/2; i++ ){
			n = orderedNodeArray[0][i];
			t0Assignment[i] = randomFromDistribution( n.getBeliefs() );
			n.finding().setState( t0Assignment[i] );
			str[0][i] = n.state( t0Assignment[i] ).toString();
		}
		
		//Generate the remainder of the data...
		for( int d=1; d<dataLength; d++ ){
			for( int i=0; i<numNodes/2; i++ ){
				n = orderedNodeArray[1][i];
				t1Assignment[i] = randomFromDistribution( n.getBeliefs() );
				n.finding().setState( t1Assignment[i] );
				str[d][i] = n.state( t1Assignment[i] ).toString();
			}

			//Now, remove the previous findings and apply t1 findings to t0:
			net.uncompile();
			net.setAutoUpdate( 0 );	//Saves computation while changing things...
			
			for( int i=0; i<numNodes/2; i++ ){
				//Set each node in t0 to whatever the equivalent node in t1 is:
				Node n0 = orderedNodeArray[0][i];
				Node n1 = orderedNodeArray[1][i];
				
				n0.finding().setState( n1.finding().getState() );
				n1.finding().clear();
			}
			
			t0Assignment = t1Assignment;
			t1Assignment = new int[numNodes/2];
			
			net.setAutoUpdate(Net.BELIEF_UPDATE);
			net.compile();
		}

		System.out.println("Done");
		
		return str;
	}
	
	private int randomFromDistribution( float[] distribution ){
		float p = rand.nextFloat();
		
		float sum = 0;
		for( int i=0; i<distribution.length; i++ ){
			sum += distribution[i];
			if( p <= sum ) return i;
		}
		throw new RuntimeException("Error - problem with distribution?");	//Should never happen
	}
	
	private boolean checkSuffix() throws NeticaException
	{
		if( t0_suffix == null || t1_suffix == null || t0_suffix.equals("") || t1_suffix.equals("") || t0_suffix.equals(t1_suffix) ){
			return false;
		}
		
		//First: Get the variable names...
		NodeList nodeList = getNodeList();
		
		Object[] temp = nodeList.toArray();
		Node[] nodes = new Node[ temp.length ];
		for( int i=0; i < temp.length; i++ ) nodes[i] = (Node)temp[i];
		
		int t0_count = 0;
		int t1_count = 0;
		
		for( Node n : nodes ){
			//System.out.println(n);
			//System.out.println(n.getParents());
			if( n.getName().endsWith( t0_suffix ) ) t0_count++;
			else if( n.getName().endsWith( t1_suffix ) ) t1_count++;
			else return false;
				//throw new Exception("Node not ending in either suffix!");
		}
		
		if( t0_count != t1_count || t0_count != numNodes / 2 ) return false;
			//throw new Exception("Error in number of nodes with suffixes (" + t0_suffix + "," + t1_suffix + ")");
		 
		return true;
	}
	
	private NodeList getNodeList(){
		try {
			return net.getNodes();
		} catch (NeticaException e) {
			e.printStackTrace();
		}
		throw new RuntimeException("Cannot generate NodeList");
	}
	
	private Node[][] getOrderedNodeList( ) throws NeticaException
	{
		Node[] nodes = nodeListToNodeArray( getNodeList() );
		
		Node[][] temp = new Node[2][numNodes/2];
		
		//First, find the nodes that don't have any parents
		int count = 0;
		boolean[] added = new boolean[ nodes.length ];
		
		while( count < numNodes/2 ){
			for( int i=0; i< numNodes; i++ ){
				if( added[i] || !nodes[i].getName().endsWith(t0_suffix) ) continue;
				//Get current node:
				Node n = nodes[i];
				//Get parents of node:
				Node[] parents = nodeListToNodeArray( n.getParents() );
				if( parents.length == 0 ){	//No parents, not yet added. Can add next!
					temp[0][count++] = n;
					added[i] = true;
				} else {	//Have all of this nodes parents been added? If so, we can add it next...
					//Do a linear search for each parent through the list:
					//boolean allParentsFound = true;
					for( Node p : parents ){
						boolean thisParentFound = false;
						for( int j=0; j<count; j++ ){
							if( nodes[j] == p ) thisParentFound = true;
						}
						if( thisParentFound == false ){
							//allParentsFound = false;
							break;
						}
					}
					
					//If all parents found, we can add to the list:
					temp[0][count++] = n;
					added[i] = true;
				}
			}
		}
		
		
		//Now: Build the second part of the list.
		count = 0;
		while( count < numNodes/2 ){
			Node toMatch = temp[0][count]; 
			String origName = toMatch.getName();
			String toMatchName = origName.substring(0, origName.length()-t0_suffix.length() );
			
			//loop through the remaining nodes, find the appropriate one:
			for( int i=0; i< numNodes; i++ ){
				if( added[i] || !nodes[i].getName().endsWith(t1_suffix) ) continue;
				
				String thisNameOrig = nodes[i].getName();
				String nameNoSuffix = thisNameOrig.substring(0, thisNameOrig.length()-t1_suffix.length() );
				
				if( toMatchName.equals( nameNoSuffix ) ){	//Add this one next...
					temp[1][count++] = nodes[i];
					added[i] = true;
				}
			}
		}
		
		return temp;
	}
	
	private static Node[] nodeListToNodeArray( NodeList nodeList ){
		Object[] temp = nodeList.toArray();
		Node[] nodes = new Node[ temp.length ];
		for( int i=0; i < temp.length; i++ ) nodes[i] = (Node)temp[i];
		return nodes;
	}
	
	/*
	private void printNodesAndBeliefs( ) throws NeticaException
	{
		//if( this.orderedNodeArray == null ) orderedNodeArray = getOrderedNodeList(t0_suffix,t1_suffix);
		
		System.out.println("\n----- Nodes and beliefs: -----");
		
		for( Node[] array : orderedNodeArray ){
			for( Node n : array ){
				float[] temp = n.getBeliefs();
				String beliefs = " [";
				for( float t : temp ) beliefs += t + ", ";
				beliefs += "]";
				System.out.println(n + "" + beliefs);
			}
		}
	}*/
	
	/**Generate a time series from the DBN passed to the constructor, and export as a .CAS file.
	 * @param filepath Filename and path to export the time series to.
	 * @param dataLength Length of time series
	 * @throws Exception IO errors, Netica errors etc.
	 */
	public void exportCAS( String filepath, int dataLength ) throws Exception
	{
		String[][] data = generateDataString(dataLength);
		String[] names = getNamesNoSuffix();
		
		//Try to create a new file:
		BufferedWriter outputWriter;
		try{
			outputWriter = new BufferedWriter( new FileWriter( filepath ));
			
			outputWriter.write( " IDnum" );
			for( String name : names ){
				outputWriter.write("\t" + name);
			}
			
			for( int i=1; i<=dataLength; i++ ){
				outputWriter.newLine();
				outputWriter.write( (new Integer(i)).toString() );
				for( String s : data[i-1] ){
					outputWriter.write("\t" + s);
				}
			}
			
			outputWriter.flush();
			outputWriter.close();
			
		} catch( Exception e ){
			System.out.println("Error writing to file - " + filepath );
		}
		
	}
	
	/**Generate a time series from the DBN passed to the constructor, and export as a .ARFF file.
	 * @param filepath Filename and path to export the time series to.
	 * @param dataLength Length of time series
	 * @throws Exception IO errors, Netica errors etc.
	 */
	public void exportARFF( String filepath, int dataLength ) throws Exception {
		
		if( !filepath.endsWith(".arff") ) throw new Exception("Path must end with .arff!");
		System.out.println("Generating data: " + numNodes/2 + " unique variables, " + dataLength + " lines");
		
		if( env == null ) env = new Environ(null);
		
		String filename = "";
		//Scan the file path to find the file name...
		for( int i=filepath.length()-1; i >=0; i-- ){
			if( filepath.charAt(i) == '\\' ){
				filename = filepath.substring(i, filepath.length()-5 );
				break;
			}
		}
		
		String[][] data = generateDataString(dataLength);
		String[] names = getNamesNoSuffix();
		
		//Try to create a new file:
		BufferedWriter outputWriter;
		try{
			outputWriter = new BufferedWriter( new FileWriter( filepath ));
			
			outputWriter.write( "@relation " + filename +"\n\n" );
			//for( String name : names ){
			for( int i=0; i<numNodes/2; i++ ){
				outputWriter.write("@attribute " + names[i] + " {" );
				
				Node n = orderedNodeArray[0][i];
				int numStates = n.getNumStates();
				
				for( int j=0; j<numStates; j++ ){
					outputWriter.write(n.state(j).toString());
					if( j < numStates-1 ) outputWriter.write(",");
				}
				
				outputWriter.write("}\n");
				
			}
			
			outputWriter.write("\n@data\n");
			
			for( int i=1; i<=dataLength; i++ ){
				for( int j=0; j< data[i-1].length; j++ ){
					outputWriter.write(data[i-1][j]);
					if( j < numNodes-1 ) outputWriter.write(",");
				}
				outputWriter.write("\n");
			}
			
			outputWriter.flush();
			outputWriter.close();
			
		} catch( Exception e ){
			e.printStackTrace();
			System.out.println("Error writing to file - " + filepath );
			
			if( finalizeEnviron ){
				env.finalize();
				env = null;
			}
		}
		if( finalizeEnviron ){
			env.finalize();
			env = null;
		}
	}
	
	private String[] getNamesNoSuffix( ) throws NeticaException
	{
		if( env == null ) env = new Environ(null);
		
		String[] names = new String[numNodes/2];
		
		for( int i=0; i<numNodes/2; i++ ){
			Node n = orderedNodeArray[0][i];
			names[i] = n.getName().substring(0, n.getName().length() - t0_suffix.length());
		}
		
		return names;
	}
}
