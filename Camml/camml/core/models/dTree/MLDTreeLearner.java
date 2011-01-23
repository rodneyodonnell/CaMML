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
// Functions to learn DTrees's from raw data.
//

// File: ForcedSplitDTreeGlue.java
// Author: rodo@csse.monash.edu.au

package camml.core.models.dTree;


import camml.core.library.DTreeSelectedVector;
import camml.core.models.FunctionStruct;
import camml.core.models.ModelLearner;
import camml.core.models.multinomial.MLMultinomialLearner;
import cdms.core.Type;
import cdms.core.Value;
import cdms.core.VectorFN;

/**
 * MLDtreeLearner calculates the Maximum Liklihood score of a dataset.  This
 * task is often performed by a MLCPTLearner, however using a DTree allows
 * larger parent sets to accurately costed.  If there are too many parents
 * a CPT learner will simply throw a LearnerException.  The DTree representation
 * removes this problem by only splitting the data as thinly as required.
 * 
 * This code is based on ForcedSplitDTreeLearner.java and as such uses a 
 * similar data structures.  The primary difference is here a ML instead
 * of MML subcoster is used in the leaves and stating of a structure costs
 * 0 bits.  The search has been modified to keep splitting while leafCost > 0
 */
public class MLDTreeLearner extends ModelLearner.DefaultImplementation {

    private static final long serialVersionUID = -1088647975068729058L;

    protected static final ModelLearner defaultLeafLearner = 
        MLMultinomialLearner.mlMultinomialLearner;
    
    /** Static instance of DTree using Multistates in the leaves */
    public static MLDTreeLearner mlDTreeLearner = new MLDTreeLearner();
    
    public String getName() {
        return "MLDTreeLearner";
    }
    
    /** ModelLearner used to cost leaves of the tree. */
    ModelLearner leafLearner;
    
    /**
     * Default constructor same as
     * ForcedSplitDTreeLearner(MultinomialLearner.multinomialLearner)
     */
    public MLDTreeLearner() {
        super(makeModelType(), Type.TRIV);
        this.leafLearner = defaultLeafLearner;
    }
    
    public MLDTreeLearner(ModelLearner leafLearner) {
        super(makeModelType(), Type.TRIV);
        this.leafLearner = leafLearner;
    }
    
    protected static Type.Model makeModelType() {
        //Type.Model subModelType = Type.MODEL;
        Type dataSpace = Type.DISCRETE;
        Type paramSpace = Type.VECTOR;
        Type sharedSpace = Type.STRUCTURED;
        Type sufficientSpace = new Type.Structured(new Type[] { dataSpace,
                                                                sharedSpace });
        
        return new Type.Model(dataSpace, paramSpace, sharedSpace,
                              sufficientSpace);        
    }
    
    /**
     * Parameterize tree. Return structure of (m,s,y)
     */
    public Value.Structured parameterize(Value initialInfo, Value.Vector x,
                                         Value.Vector z) throws LearnerException {
                
        if (x.length() != z.length()) {
            throw new RuntimeException(
                                       "Length mismatch in DTreeLearner.parameterize");
        }
        
        // Find the number of splits which must be tested from this node.
        Type.Structured inputType = (Type.Structured) ((Type.Vector) z.t).elt;
        int numVars = inputType.cmpnts.length;
        
        int[] parent = new int[numVars];
        for (int i = 0; i < parent.length; i++) {
            parent[i] = i;
        }
        
        // Create a simple leaf node.
        TreeNode rootNode = new Leaf(new DTreeSelectedVector(x), new DTreeSelectedVector(z), parent, null);
        
        boolean[] splitUsed = new boolean[parent.length];
        
        java.util.ArrayList<TreeNode> leafList = new java.util.ArrayList<TreeNode>();
        leafList.add(rootNode);
        
        // expand the tree until further expansions do not decreace MML cost
        for (int i = 0; i < leafList.size(); i++) {
            Leaf leaf = (Leaf) leafList.get(i);
            double leafCost = leaf.getCost();
            double[] splitCost = leaf.findAllSplitCosts();
            
            double bestCost = Double.POSITIVE_INFINITY;
            int bestIndex = -1;
            for (int j = 0; j < splitCost.length; j++) {
                if (splitCost[j] < bestCost) {
                    bestCost = splitCost[j];
                    bestIndex = leaf.availableParent[j];
                }
            }
            
            
            
            //if (bestCost < leafCost) {
            if ( bestIndex != -1 && leafCost > 0) {
                Split split = new Split(leaf);
                split.setSplit(bestIndex);
                splitUsed[bestIndex] = true;
                for (int j = 0; j < split.splitArray.length; j++) {
                    leafList.add(split.splitArray[j]);
                }
                // remove unneeded leaf from the list. we must also decrement i
                // so the correct
                // item is wtill pointed to.
                leafList.remove(i);
                i--;
                
                if (leaf.parentNode == null) {
                    rootNode = split;
                } else {
                    leaf.parentNode.replaceChild(leaf, split);
                }
            }
        }                
        
        Value treeParams = rootNode.getParams();
        return new Value.DefStructured(new Value[] { DTree.dTree,
                                                     DTree.dTree.getSufficient(x, z), treeParams }, new String[] {
                "DTree", "Stats", "Params" });
        
    }

    /** Return true if array[] contains any false values. */
    boolean containsFalse(boolean[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == false) { return true; }
        }
        return false;
    }
    
    
    /**
     * A TreeNode may either be Split or a leaf.
     */
    protected abstract class TreeNode implements java.io.Serializable {
        /**
         * Saves everything as appropriate. <br>
         * AvailableInput and unplitCost are generated.
         */
        public TreeNode(DTreeSelectedVector output, DTreeSelectedVector input,
                        int[] availableParent, Split parentNode) {
            this.allInput = input;
            this.output = output;
            this.availableParent = availableParent;
            this.availableInput = new DTreeSelectedVector(allInput, null,
                                                          availableParent);
            this.parentNode = parentNode;
            
            allInputType = ((Type.Structured) ((Type.Vector) input.t).elt).cmpnts;
            allInputLabel = ((Type.Structured) ((Type.Vector) input.t).elt).labels;
        }
        
        /** parents which have NOT been split on yet. */
        final int[] availableParent;
        
        /** Single column of output data available to this leaf */
        final DTreeSelectedVector output;
        
        /** input data corresponding to output. All input variables are included */
        final DTreeSelectedVector allInput;
        
        /** A list of types of each input */
        final Type[] allInputType;
        
        /** the labels of each input */
        final String[] allInputLabel;
        
        /**
         * input data corresponding to output. Only attributes which may be
         * split on are included. <br>
         * availableInput = new DTreeSelectedVector( allInput, null, availableParent );
         */
        final DTreeSelectedVector availableInput;
        
        /**
         * The Split in the tree directly above this node. null implies root
         * node.
         */
        final Split parentNode;
        
        /** cost of this node */
        abstract double getCost();
        
        /** return a CDMS value containing parameters for this model */
        abstract Value getParams() throws LearnerException;


        public String toString() {
            return "(" + ((Type.Vector) availableInput.t).elt + ")";
        }
    }
    
    /**
     * A leaf structure in a decision tree.
     */
    protected class Leaf extends TreeNode {

        private static final long serialVersionUID = 5906128365379782354L;

        /**
         * Saves everything as appropriate. <br>
         * AvailableInput and unplitCost are generated.
         */
        public Leaf(DTreeSelectedVector output, DTreeSelectedVector input,
                    int[] availableParent, Split parentNode)
            throws LearnerException {
            super(output, input, availableParent, parentNode);
            
            // find the cost of this leaf.
            leafCost = leafLearner.parameterizeAndCost(Value.TRIV, output,
                                                       input);
        }
        
        /** alternate constructor (makes it easier to convert from leaf <->split ) */
        Leaf(TreeNode treeNode) throws LearnerException {
            this(treeNode.output, treeNode.allInput, treeNode.availableParent,
                 treeNode.parentNode);
        }
        
        /** Cost of expressing data in this leaf. */
        final double leafCost;
        
        /** return leafCost (+ one bit to state this is a leaf.) */
        public double getCost() {
            return leafCost;
        }
        
        /** return parameters of a multistate distribution. */
        public Value getParams() throws LearnerException {
            // calculate leaf parameters.
            Value.Structured msy = leafLearner.parameterize(Value.TRIV, output,
                                                            allInput);
            Value.Structured leafParams = new Value.DefStructured(new Value[] {
                    msy.cmpnt(0), msy.cmpnt(2) });
            
            // if the top level of out tree hapens to be a leaf node, we must
            // wrap the multinomial
            // up to make it look like a tree.
            Value treeParams = makeDTreeParamStruct(-1, getCost(), leafParams);
            return treeParams;
        }
        
        /**
         * provate array storing the result of turning this node into a split
         * node.
         */
        private double[] splitCostArray = null;
        
        /** calculate the cost of splitting on each variable. */
        double[] findAllSplitCosts() throws LearnerException {
            // computed lazily.
            if (splitCostArray != null) {
                return splitCostArray;
            }
            
            splitCostArray = new double[availableParent.length];
            Split splitNode = new Split(this);
            for (int i = 0; i < splitCostArray.length; i++) {
                int currentSplit = availableParent[i];
                splitNode.setSplit(currentSplit);
                splitCostArray[i] = splitNode.getCost();
            }
            
            return splitCostArray;
        }
    }
    
    /** returns the structure (split,cost,params) */
    public Value.Structured makeDTreeParamStruct(int split, double cost,
                                                 Value params) {
        Value.Discrete splitVal = new Value.Discrete(split);
        Value.Continuous costVal = new Value.Continuous(cost);
        return new Value.DefStructured(
                                       new Value[] { splitVal, costVal, params }, new String[] {
                                           "splitAttribute", "cost", "params" });
    }
    
    /** returns the structure ( DTree, DTree.getSufficient(x,z), params ) */
    public Value.Structured makeDTreeMSY(Value.Vector x, Value.Vector z,
                                         Value params) {
        return new Value.DefStructured(new Value[] { DTree.dTree,
                                                     DTree.dTree.getSufficient(x, z), params }, new String[] {
                "DTree", "Stats", "Params" });
        
    }
    
    /**
     * a split in the network.
     */
    protected class Split extends TreeNode {
        
        private static final long serialVersionUID = 7903040951216660771L;

        /** Split constructor. setSplit must be called independently */
        Split(DTreeSelectedVector output, DTreeSelectedVector input,
              int[] availableParent, Split parentNode) {
            super(output, input, availableParent, parentNode);
        }
        
        /** alternate constructor (makes it easier to convert from leaf <->split ) */
        Split(TreeNode treeNode) {
            this(treeNode.output, treeNode.allInput, treeNode.availableParent,
                 treeNode.parentNode);
        }
        
        /**
         * Change the variable being split upon in this node.
         */
        void setSplit(int splitVar) throws LearnerException {
            
            // quick check to make sure can actually split on the suggested
            // variable.
            boolean splitAvailable = false;
            for (int i = 0; i < availableParent.length; i++) {
                if (availableParent[i] == splitVar) {
                    splitAvailable = true;
                    break;
                }
            }
            if (!splitAvailable) {
                throw new RuntimeException("unavailable split selected : "
                                           + splitVar);
            }
            
            // set splitVar to its new value.
            this.splitVar = splitVar;
            this.splitArity = (int) ((Type.Discrete) allInputType[splitVar]).UPB
                - (int) ((Type.Discrete) allInputType[splitVar]).LWB + 1;
            this.splitArray = new TreeNode[splitArity];
            
            // split input and output as appropriate.
            DTreeSelectedVector[] splitInput = DTreeSelectedVector.d_splitVector(allInput,
                                                                                 splitVar, false);
            DTreeSelectedVector[] splitOutput = new DTreeSelectedVector[splitInput.length];
            for (int i = 0; i < splitOutput.length; i++) {
                splitOutput[i] = splitInput[i].d_copyRowSplit(output);
            }
            
            int[] reducedAvailableParent = new int[availableParent.length - 1];
            int j = 0;
            for (int i = 0; i < availableParent.length; i++) {
                if (availableParent[i] != splitVar) {
                    reducedAvailableParent[j] = availableParent[i];
                    j++;
                }
            }
            
            for (int i = 0; i < splitArray.length; i++) {
                splitArray[i] = new Leaf(splitOutput[i], splitInput[i],
                                         reducedAvailableParent, this);
            }
        }
        
        int splitVar;
        
        int splitArity;
        
        TreeNode[] splitArray;
        
        /**
         * return the cost of all child nodes plus the structure cost of this
         * split.
         */
        double getCost() {
            // cost of each node below this split.
            double subCost = 0;
            for (int i = 0; i < splitArray.length; i++) {
                subCost += splitArray[i].getCost();
            }
                    
            return subCost;
        }
                
        /** replace the childe oldNode with the childe newNode */
        public void replaceChild(TreeNode oldNode, TreeNode newNode) {
            for (int i = 0; i < splitArray.length; i++) {
                if (splitArray[i] == oldNode) {
                    splitArray[i] = newNode;
                    return;
                }
            }
            throw new RuntimeException("oldNode not found in replaceChild : "
                                       + oldNode);
        }
        
        /** recursively find and return parameters. */
        public Value getParams() throws LearnerException {
            // DTree split parameters are determined in terms of which possible
            // parent the split
            // is on, not which of all inputs to the full DTree the split is.
            int relativeSplitVar = -2;
            for (int i = 0; i < availableParent.length; i++) {
                if (availableParent[i] == splitVar) {
                    relativeSplitVar = i;
                    break;
                }
            }
            if (relativeSplitVar == -2) {
                throw new RuntimeException("Invalid split specified : "
                                           + splitVar);
            }
            
            final String splitName = allInputLabel[splitVar];
            Value.Discrete splitAttribute = new RenamedDiscrete(relativeSplitVar,splitName);

            Value.Continuous cost = new Value.Continuous(getCost());
            Value[] paramVec = new Value[splitArray.length];
            for (int i = 0; i < paramVec.length; i++) {
                paramVec[i] = splitArray[i].getParams();
            }
            Value.Vector subParams = new VectorFN.FatVector(paramVec);
            
            return new Value.DefStructured(new Value[] { splitAttribute, cost,
                                                         subParams }, new String[] { "splitAttribute", "cost",
                                                                                     "paramVector" });
        }
        
    }
    
    /** RenamedDiscrete is a regular Value.Discrete with its .toString() method
     *  overridden to return name. <br>
     *  Note: name.intern() is called on name so this function should only be called
     *        on common strings (such as variable names).
     */
    public static class RenamedDiscrete extends Value.Discrete {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -8197542930711721103L;
        public final String name;
        public RenamedDiscrete(int var, String name) { 
            super(var);
            this.name = name.intern();
        }
        public String toString() { return name; }
    }

    /** Parameterize and return (m,s,y) */
    public Value.Structured sParameterize(Value.Model model, Value s)
        throws LearnerException {
        return parameterize(Value.TRIV, (Value.Vector) ((Value.Structured) s)
                            .cmpnt(0), (Value.Vector) ((Value.Structured) s).cmpnt(1));
    }
    
    /**
     * return cost. This is read directly out of parameters. Ideally it should
     * be calculated using parameters and data as currently it entirely ignores
     * data.
     */
    public double cost(Value.Model m, Value initialInfo, Value.Vector x,
                       Value.Vector z, Value y) throws LearnerException {
        if (m instanceof DTree) {
            Value.Structured params = (Value.Structured) y;
            double cost = params.doubleCmpnt(1);
            return cost;
        } else {
            return leafLearner.cost(m, initialInfo, x, z, y);
        }
    }
    
    /**
     * This function ignores the parameters given and costs using the costing
     * method from Camml Classic discrete. <br>
     * 
     *  
     */
    public double sCost(Value.Model m, Value s, Value y)
        throws LearnerException {
        if (m instanceof DTree) {
            Value.Structured params = (Value.Structured) y;
            double cost = params.doubleCmpnt(1);
            return cost;
        } else {
            return leafLearner.sCost(m, s, y);
        }
    }
    
    public String toString() {
        return "ForcedSplitDTreeLearner(" + leafLearner + ")";
    }
    
    /** Default implementation of makeForcedSplitDTreeLearnerStruct */
    public static final MakeForcedSplitDTreeLearnerStruct makeForcedSplitDTreeLearnerStruct = new MakeForcedSplitDTreeLearnerStruct();
    
    /**
     * MakeForcedSplitDTreeLearnerStruct returns a ForcedSplitDTreeLearner given
     * a "leafLearner" in its options.
     */
    public static class MakeForcedSplitDTreeLearnerStruct extends
                                                              Value.Function {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 2003275199831383979L;

        public MakeForcedSplitDTreeLearnerStruct() {
            super(new Type.Function(new Type.Vector(new Type.Structured(
                                                                        new Type[] { Type.STRING, Type.TYPE }, new String[] {
                                                                            "name", "value" })), Type.STRUCTURED));
        }
        
        /** Shortcut apply method */
        public ModelLearner _apply(String[] option, Value[] optionVal) {
            
            // Set default values.
            ModelLearner leafLearner = defaultLeafLearner;
            
            // Search options for overrides.
            for (int i = 0; i < option.length; i++) {
                if (option[i].equals("leafLearner")) {
                    leafLearner = ((FunctionStruct) optionVal[i]).getLearner();
                } else {
                    throw new RuntimeException("Unknown option : " + option[i]);
                }
            }
            
            return new ForcedSplitDTreeLearner(leafLearner);
        }
        
        /**
         * return a ForcedSplitDTreeLearner using specified options <br>[
         * ("option", optionValue) ]<br>
         * Valid options :<br>
         * "leafLearner" -> (FunctionStruct)
         */
        public Value apply(Value v) {
            
            Value.Vector vec = (Value.Vector) v;
            String[] option = new String[vec.length()];
            Value[] optionVal = new Value[option.length];
            
            for (int i = 0; i < option.length; i++) {
                Value.Structured elt = (Value.Structured) vec.elt(i);
                if (elt.length() != 2) {
                    throw new RuntimeException(
                                               "Invalid Option in makeLearnerStruct");
                }
                option[i] = ((Value.Str) elt.cmpnt(0)).getString();
                optionVal[i] = elt.cmpnt(1);
            }
            
            return _apply(option, optionVal).getFunctionStruct();
        }
    }
    
}

