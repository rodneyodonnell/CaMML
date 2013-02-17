package camml.core.searchDBN;

import java.util.BitSet;

import camml.core.models.ModelLearner;
import camml.core.search.CaseInfo;
import camml.core.search.Node;
import camml.core.search.TOM;
//import camml.core.search.TOM.EditDistance;
import cdms.core.Type;
import cdms.core.Value;
import cdms.core.VectorFN;
import cdms.core.Value.Vector;


/**
 * A TOM is a Totally Ordered Model (see camml.core.search.TOM).
 * A DTOM is a 'Dynamic TOM' - that is, a TOM for Dynamic Bayesian Networks.
 * The intra-slice arcs (that is, the arcs within each time slice) are stored in
 * the TOM class (i.e. DTOM's superclass).
 * Variables and methods for modifying the temporal arcs (i.e. arcs between time
 *  slices in a DBN) are defined here.
 * 
 * @author Alex Black
 *
 */
public class DTOM extends TOM {

	/** Bitset for storing temporal (inter-slice) arcs only.
	 *  If an arc Parent->Child is present, temporalEdge[parent].get(child) == true*/ 
	protected BitSet[] temporalEdge;
	
	/**  Keep track of how many TEMPORAL edges (links) are present in this DTOM   */
    protected int numTemporalEdges;
    
    /** Getter for number of temporal edges in this DTOM */
    public int getNumTemporalEdges(){ return numTemporalEdges; }
    
	public DTOM(CaseInfo caseInfo) {
		super(caseInfo);
		
		numTemporalEdges = 0;	//Initialize count for temporal edges
		
        // Create array for storing temporal arcs/edges:
        temporalEdge = new BitSet[ getNumNodes() ];
        
        for( int i=0; i < getNumNodes(); i++ ){
        	temporalEdge[i] = new BitSet(getNumNodes());
        	//Create DNODE object (overwrite Node objects set by TOM constructor)
            node[i] = new DNode(i);
        }
        //BitSet values are set to false by default, so no explicit initialization is required.
	}

	/** Not Implemented */
	public DTOM(Vector data) {
		super(data);
		throw new RuntimeException("METHOD NOT IMPLEMENTED");
	}

	/** Not Implemented */
	public DTOM(int numNodes) {
		super(numNodes);
		throw new RuntimeException("METHOD NOT IMPLEMENTED");
	}
	
	
	/*--------- Methods for modifying arcs: ----------*/
	///////////////////////////
	// DBN utility functions //
	///////////////////////////
	
	/** set an intraslice arc; x->y (or y->x depending on total ordering of variables)
	 *  Overrides TOM.setArc(...)
	 *  Only change is checking for too many parents - otherwise exactly the same as TOM.setArc()
	 */
	protected void setArc(int x, int y, boolean arcValue)
    {
        if(x == y)
            { return; }
        
        int first, second;
        if(x > y) {
            first = x - 1;
            second = y;
        }
        else {
            first = y - 1;
            second = x;
        }
        boolean oldEdge = edge[first].get(second);    
        edge[first].set(second,arcValue);
        
        int child, parent;
        if ( before(x,y) ) { parent = x; child = y; }
        else { child = x; parent = y; }
        
        if ( !oldEdge && arcValue ) { 
            numEdges ++; 
            //if ( node[child].parent.length >= maxNumParents ) {
            if( node[child].getNumParents() >= maxNumParents ){
                throw new Node.ExcessiveArcsException("MaxParents already reached, cannot add another. (" + node[child].getNumParents() + ")");
            }
            node[child].addParent(parent);
        }
        else if ( oldEdge && !arcValue ) {
            numEdges --; 
            node[child].removeParent(parent);
        }
        else {
            System.out.println("--- Link already present/absent?? ---");
        }       
    }
	
	/** return whether a TEMPORAL arc exists between two variables. x->y  (i.e. x_0 -> y_1)*/
	public boolean isTemporalArc(int x, int y){
		return temporalEdge[x].get(y);
	}
	
	/**  set a TEMPORAL arc.  x -> y  */
	protected void setTemporalArc(int x, int y, boolean arcValue){
		boolean oldEdge = temporalEdge[x].get(y);
		temporalEdge[x].set(y, arcValue);
		
		//Update DNodes to reflect changes
		//If temporal arc changed from false to true:
		if( !oldEdge && arcValue ){
			if( node[y].getNumParents() >= maxNumParents ){
				throw new Node.ExcessiveArcsException("MaxParents already reached, cannot add another (temporal). node[" + y + "].getNumParents()=" + node[y].getNumParents() );
			}
			((DNode)node[y]).addTemporalParent(x);		//i.e. node[child].addTemporalParent(parent)
			numTemporalEdges++;
			return;
		}
		//If temporal arc changed from true to false:
		if( oldEdge && !arcValue ){
			((DNode)node[y]).removeTemporalParent(x);
			numTemporalEdges--;
			return;
		}
		System.out.println("Error: Tried to add (remove) arc that was already present (absent)");
	}
	
	/**	Add a TEMPORAL arc by index. x -> y
	*   Returns true if an operation was performed.
	*/
	public boolean addTemporalArc(int x, int y){
		if( isTemporalArc(x,y) ){
			return false;
		} else{
			setTemporalArc(x,y,true);
			return true;
		}
	}
	
	/**Remove a TEMPORAL arc by index. x -> y
	*  Returns true if an operation was performed.
	*/
	public boolean removeTemporalArc(int x, int y){
		if( !isTemporalArc(x,y) ){
			return false;
		} else{
			setTemporalArc(x,y,false);
			return true;
		}
	}
	
	
	/**
	 * Add every possible edge in this TOM to a maximum of maxParents...
	 * Adds both temporal AND intraslice parents
	 */
	public void fillArcs( int maxParents ){
		
		//Do maxParents/2 on intraslice arcs
		//Do maxParents - maxParents/2 on temporal arcs
		
		//Intraslice arcs: Add up to maxParents/2 arcs starting with nodes just before current node in total ordering.
		//Loop taken from TOM.fillArcs(...), with some modifications
        for ( int child = 0; child < node.length; child++ ) {
            //for ( int parentIndex = getNodePos(child)-1; (parentIndex >= 0) && 
            //          (node[child].parent.length < maxNumParents); parentIndex-- ) {
        	for ( int parentIndex = getNodePos(child)-1; (parentIndex >= 0) && 
                  (node[child].getParentCopy().length < maxNumParents/2); parentIndex-- ) {
                addArc( nodeAt(parentIndex), child );
                
                // If adding this arc makes the cost infinite, then don't add it.
                // Removing this can leave the tom in a nasty state of having an infinite cost.
                // This most commonly occurs when we try and build a CPT with way to many states
                // and an exception is thrown upstream.
                
                if ( caseInfo.nodeCache != null && 
                     Double.isInfinite( caseInfo.nodeCache.getMMLCost( node[child] ) ) ) {
                    removeArc( nodeAt(parentIndex), child );
                }
            }
        }
		
        //Temporal arcs: Add up to maxParents - maxParents/2
        for( int child=0; child < node.length; child++ ){
        	//for( int i=0; ((DNode)node[child]).temporalParent.length + node[child].getParentCopy().length < maxNumParents-1; i++ ){
        	for( int i=0; node[child].getNumParents() <= maxNumParents-1 && i < getNumNodes(); i++ ){
        		//Add node from child to itself (probably a likely arc in DBNs)
        		if( i==0 ){
        			addTemporalArc( child, child );
        			if ( caseInfo.nodeCache != null && 
                            Double.isInfinite( caseInfo.nodeCache.getMMLCost( node[child] ) ) ) {
                           removeTemporalArc( child, child );
                   }
        		} else{
        			//TODO: Find a better way to do this... Ideally, would do it randomly
        			// (But can't - no RNG passed or accessible...)
        			int parentIndex = getNodePos(child)-1-i;
        			if( parentIndex < 0 ) parentIndex += this.getNumNodes();
        			
        			
        			addTemporalArc( nodeAt(parentIndex), child );
        			if ( caseInfo.nodeCache != null && 
                            Double.isInfinite( caseInfo.nodeCache.getMMLCost( node[child] ) ) ) {
                           removeTemporalArc( nodeAt(parentIndex), child );
                   }
        		}
        	}
        }
	}
	
	
	/**
     * Add every possible TEMPORAL edge in this TOM to a maximum of maxParents. <br>
     * If the addition of extra nodes would give the node an infinite MML cost then it is not added.
     */
    public void fillTemporalArcs( int maxParents )
    {
        throw new RuntimeException("METHOD NOT IMPLEMENTED");
    }
    
    /** remove ALL arcs (intraslice AND temporal) from DTOM
     *  Overrides TOM.clearArcs()
     */
    public void clearArcs(){
    	this.clearIntrasliceArcs();
    	this.clearTemporalArcs();
    }
    
    /** remove all INTRASLICE arcs (only) from this DTOM
     */
    public void clearIntrasliceArcs(){
    	super.clearArcs();
    }
    
    
    /** remove all TEMPORAL arcs (only) from this DTOM */
    public void clearTemporalArcs() {
    	for( int i = 0; i < getNumNodes(); i++ ){
    		for( int j : ((DNode)node[i]).temporalParent ){
    			removeTemporalArc(j,i);		//i.e. removeTemporalArc parent->child
    		}
    	}
    }
    
    /**Set all TEMPORAL arcs randomly (p=0.5)*/
    public void randomTemporalArcs(java.util.Random rand) {
        randomTemporalArcs(rand,0.5);
    }
    
    /** Set all TEMPORAL arcs randomly (p prob of an arc)*/
    public void randomTemporalArcs(java.util.Random generator, double p) {
    	throw new RuntimeException("METHOD NOT IMPLEMENTED");
    }

    
    /**
     *  Two DBN DAGs are considered equal if:
     *  - They have identical intra-slice arcs, and those arcs are in the same direction.
	 *  - They have identical temporal arcs
     */
    public boolean equals( Object o ){
    	if( !(o instanceof DTOM) ) return false;
    	
    	//Check intra-slice arcs:
    	if( !super.equals(o) ) return false;
    	
        DTOM dtom1 = this;
        DTOM dtom2 = (DTOM)o;
        
        //Do the two DTOMs have the same number of variables?
        if( dtom1.temporalEdge.length != dtom2.temporalEdge.length ) return false;
        
        //Check temporal edges, one by one:
        for( int i = 0; i < getNumNodes(); i++ ){
        	for( int j = 0; j < getNumNodes(); j++ ){
        		if( dtom1.isTemporalArc(i, j) != dtom2.isTemporalArc(i, j) ){
        			return false;
        		}
        	}
        }
        return true;
    }
    
    /** OVERRIDES TOM.makeParameters(...). Note: Parameters are for SECOND TIME SLICE ONLY.
     *  Using the current list of connections, create the parameter list required to interact with
     *  a BNet model.  <br>
     *  OLD FORMAT: The format is : [ ( [parents], (submodel,subparams) ) ]
     *  NEW FORMAT: [ ( [intraslice parents], [temporal parents], (submodel,subparams) ) ]
     *  Much of code is derived from TOM.makeParameters(...)
     */
    public Value.Vector makeParameters( ModelLearner modelLearner ) throws ModelLearner.LearnerException
    {
    	int numVars = getNumNodes();
        
        // Create arrays to hold initial structures.
        String name[] = new String[ numVars ];
        Value.Vector subParents[] = new Value.Vector[numVars];
        Value.Vector subParentsTemporal[] = new Value.Vector[numVars];		//TEMPORAL parents
        Value.Model[] subModel = new Value.Model[ numVars ];
        Value subModelParam[] = new Value[ numVars ];
        Value.Structured subParam[] = new Value.Structured[ numVars ];
        Value.Structured localStructure[] = new Value.Structured[ numVars ];
        
        // initialise name
        Type.Structured dataType = (Type.Structured)((Type.Vector)data.t).elt;
        for ( int i = 0; i < name.length; i++ ) {
            if ( dataType.labels != null ) {
                name[i] = dataType.labels[i];
            }
            else {
                name[i] = "var(" + i + ")";
            }
        }
        
        // set value of parents.
        for ( int i = 0; i < subParents.length; i++ ) {
            //subParents[i] = new VectorFN.FastDiscreteVector( node[i].parent.clone() );
        	subParents[i] = new VectorFN.FastDiscreteVector( node[i].getParentCopy() );
        	subParentsTemporal[i] = new VectorFN.FastDiscreteVector( ((DNode)node[i]).getTemporalParentCopy() );
        }               
        
        // set CPT models and parameters for nodes.
        for ( int i = 0; i < subModel.length; i++ ) {
            Value.Structured msy = node[i].learnModel( modelLearner, data );
            subModel[i] = (Value.Model)msy.cmpnt(0);
            subModelParam[i] = msy.cmpnt(2);
        }    
        
        // ( subModel, subParam )
        for ( int i = 0; i < subParam.length; i++ ) {
            subParam[i] = new Value.DefStructured( new Value[] {subModel[i], subModelParam[i]} ); 
        }
        
        
        // ( [parants], ( subModel, subParam ) )
        for ( int i = 0; i < localStructure.length; i++ ) {
            localStructure[i] = new Value.DefStructured( new Value[] {new Value.Str(name[i]), 
                                                                      subParents[i],
                                                                      subParentsTemporal[i],
                                                                      subParam[i]} );
        }
        
        return new VectorFN.FatVector( localStructure  );
    }
    
    
    /**
     *  Clone the DTOM.
     *  - Deep copy of Node[] node <br>
     *  - Deep copy of int[] totalOrder
     *  - Deep copy of int[][] edge
     *  - Shallow copy of Value.Vector data
     *  - Deep copy of int[][] temporalEdge
     */
    public Object clone(){
        //throw new RuntimeException("METHOD NOT IMPLEMENTED");
        
    	DTOM tempTOM = new DTOM( caseInfo );
        
        tempTOM.totalOrder = new int[totalOrder.length];
        tempTOM.variablePlace = new int[variablePlace.length];
        for (int i = 0; i < totalOrder.length; i++) {
            tempTOM.totalOrder[i] = totalOrder[i];        
            tempTOM.variablePlace[i] = variablePlace[i];
        }
        
        tempTOM.node = new DNode[node.length];
        
        for (int i = 0; i < node.length; i++) {
            tempTOM.node[i] = (DNode)node[i].clone();
        }
        
        tempTOM.edge = new BitSet[edge.length];
        for (int i = 0; i < edge.length; i++) {
            tempTOM.edge[i] = (BitSet)edge[i].clone();            
        }
        
        tempTOM.numEdges = numEdges;
        tempTOM.numTemporalEdges = numTemporalEdges;
        
        //Clone temporal arcs:
        tempTOM.temporalEdge = new BitSet[ temporalEdge.length ];
        for( int i=0; i< temporalEdge.length; i++ ){
        	tempTOM.temporalEdge[i] = (BitSet)temporalEdge[i].clone();
        }
        
        return tempTOM;
    }
    

    /** Print DTOM as ASCII */
    public String toString(){
    	String[] name = new String[node.length];
        Type.Structured dataType = (Type.Structured)((Type.Vector)data.t).elt;
        for ( int i = 0; i < name.length; i++ ) {
            if ( dataType.labels != null ) {
                name[i] = dataType.labels[i];
            }
            else {
                name[i] = "var(" + i + ")";
            }
        }
    	
        int temporalEdgeCount = 0;
        String RS = "---Temporal Arcs:---";
        
        for( int to=0; to<getNumNodes(); to++ ){	//
        	RS += "\n" + name[to] + "_1 : ";
        	for( int from=0; from<getNumNodes(); from++ ){
        		if( temporalEdge[from].get(to) ){
        			RS += " <- " + name[from] + "_0";
        			temporalEdgeCount++;
        		}
        	}
        }

    	RS = RS + "\n# Temporal arcs = " + temporalEdgeCount;
    	return RS + "\n---Intraslice Arcs:---\n" + super.toString();    	
    }
    
    /** Calculate cost of DTOM. */
    public double getCost(){
        throw new RuntimeException("METHOD NOT IMPLEMENTED");
    }
    
    
    /** Return Edit Distance between two TOMs. ED indexed by EditDistance enum. */
    public int[] editDistance( DTOM t2) {
    	throw new RuntimeException("METHOD NOT IMPLEMENTED");
    }
    
    /** Set the current node ordering and edges to those of tom2 
     *  Overrides TOM.setStructure
     * */
    public void setStructure( TOM tom2 ){
    	if( tom2.getClass() != DTOM.class ){
    		throw new RuntimeException("Must pass DTOM as argument; passed TOM?");
    	}
    	
    	//Clear temporal arcs: (Must be done first to avoid having too many parents...)
    	this.clearTemporalArcs();
    	
    	//Set the intraslice arcs using TOM class:
    	super.setStructure( tom2 );
    	
    	DTOM dtom2 = (DTOM)tom2;
        
        for( int i = 0; i < dtom2.node.length; i++ ){
        	for( int j : ((DNode)dtom2.getNode(i)).temporalParent ){
        		this.addTemporalArc(j, i); //add arc from parent J to child I
        	}
        }
        this.numTemporalEdges = ((DTOM)tom2).numTemporalEdges;
        
    }
    
    /**Returns a copy of the total order; i.e. if the network is <br>
     * (2) -> (1) -> (3) -> (0) -> (4), then return value is: <br>
     * totalOrder    = { 2, 1, 3, 0, 4 } <br>
     */
    public int[] getTotalOrderCopy(){
    	return totalOrder.clone();
    }

}
