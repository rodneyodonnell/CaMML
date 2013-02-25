package camml.core.searchDBN;


import camml.core.models.ModelLearner;
import camml.core.search.Node;
import cdms.core.Value;

/** Node class for DBNs.
 * Extends Node class; adds methods etc. for temporal arcs in DBN
 *
 */
public class DNode extends Node {

	/** Create a new DNode (no parents), specifying the variable number */
	public DNode(int var) {
		super(var);	//Sets intraslice parents to be empty
		 this.temporalParent = new int[0];	//Set temporal parents to be empty 
	}

	/** Create a new DNode, with intraslice and interslice parents specified */
	public DNode(int var, int[] parent, int[] temporalParent ) {
		super(var, parent);		//Sets var and parent[]
		this.temporalParent = temporalParent;
	}
	
	/** List of TEMPORAL parents of this node. (i.e. parents from previous time slice) */
    protected int[] temporalParent;
    
    /** Overrides Node.getNumParents(); returns total number of parents (temporal + intraslice) */
    public int getNumParents() { return parent.length + temporalParent.length; }
    
    /** Intraslice parents - same as Node.getNumParents*/
    public int getNumIntrasliceParents(){ return parent.length; }
    
    /** Temporal parents */
    public int getNumTemporalParents() { return temporalParent.length; }
    
    /** Returns a copy of the temporal parents array for this DNode */
    public int[] getTemporalParentCopy() { return temporalParent.clone(); }
    
    
    /** Add a single TEMPORAL parent to this node.*/
    public void addTemporalParent( int node )
    {
    	int[] newTemporalParent = new int[ temporalParent.length + 1 ];	//Allocate space...

    	int i = 0;
    	while( i < temporalParent.length && temporalParent[i] < node ){ //Add in order
    		newTemporalParent[i] = temporalParent[i];
    		i++;
    	}
    	newTemporalParent[i] = node;
    	i++;
    	while( i < newTemporalParent.length ){
    		newTemporalParent[i] = temporalParent[i-1];
    		i++;
    	}
    	temporalParent = newTemporalParent;
    }
    
    /** remove a single TEMPORAL parent from this node. */
    public void removeTemporalParent( int node ){
        int[] newTemporalParent = new int[temporalParent.length - 1];
        int i = 0;
        while ( temporalParent[i] != node ) { 
            newTemporalParent[i] = temporalParent[i]; 
            i++;
        }    
        while ( i < newTemporalParent.length ) { 
            newTemporalParent[i] = temporalParent[i+1]; 
            i++; 
        }
        temporalParent = newTemporalParent;
    }
    
    
    /** This returns a vector of just the dependant variable.
     *  OVERRIDES Node.dependentVector(...)
     *  NOTE: FOR learning DBNs, we only want data from second data point (index 1) to 
     *  end (due to temporal parents, when learning 2nd time slice DBNs...)
     */
    protected Value.Vector dependentVector(Value.Vector data) {
    	Value.Vector dependentData = data.cmpnt(var); 
    	return dependentData.sub(1, dependentData.length()-1 );
    }
    
    /** This returns a view of the data containing only parents of this node.
     *  Overrides Node.parentView(...).
     *  Note: Basically returns a DBNDataWrapper object, which provides the appropriate
     *  offsets for the data (i.e. if a parent is from the previous time slice, the data
     *  should come from the previous time slice...)*/
    protected Value.Vector parentView(Value.Vector data) {
    	//Need to get data(1...length-1) for intraslice arc parents
    	//Also need data(0...length-2) for temporal arc parents
    	return new DBNDataWrapper(data,parent,temporalParent);
    }
    
    /** Make a deep copy of the current DNode. */
    public Object clone() 
    {
        return new DNode( var, (int[])parent.clone(), (int[])temporalParent.clone() );
    }
    
    /** Print ASCII version of DBN Node's intraslice and temporal arcs
     * Format is:<br> NodeNum : <- IntrasliceArc || <- TemporalArc
     * */
    public String toString(){
        String s = "" + var + " : ";
        for (int i = 0; i < parent.length; i++) s += " <- " + parent[i];
        s += "\t||\t";
        for( int j = 0; j < temporalParent.length; j++ ) s += " <- " + temporalParent[j];
        
        return s;
    }
    
    /**
     * Analogous to Node.learnModel(...); Learns the parameters only including intraslice
     * arcs, and not the temporal arcs.
     * Used to learn the first time slice CPDs directly from data. Node.learnModel(...) should
     * be used for learning the second time slice parameters.
     */
    public Value.Structured learnModelT0( ModelLearner modelLearner, Value.Vector data)
    	throws ModelLearner.LearnerException
    {
    	Value.Vector myData = dependentVector(data);
        //Value.Vector parentData = parentView(data);
    	Value.Vector parentData = new DBNDataWrapper(data,parent,new int[0]);
    	
    	Value.Structured msy = modelLearner.parameterize(Value.TRIV,myData,parentData);
    	return msy;
    }
}
