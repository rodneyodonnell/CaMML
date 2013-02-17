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
// Node Cache for CaMML
//

// File:   NodeCache.java
// Author: rodo@dgs.monash.edu.au


package camml.core.search;

import java.io.ObjectStreamException;
import java.io.Serializable;

import cdms.core.*;
import camml.core.models.ModelLearner;

/**
 *  NodeCache contains numerous lazy functors dealing with caching the cost of a Node. <br>
 *  Any time a getXXX(node) functor is called, if the value has been previously calculated
 *   then that result is returned, if not the value will ba calculated and stored for later
 *   reuse (ie. Lazy Evaluation) <br>
 *  MML and ML costs for each node are cached, as well as the clean version of ML and MML where
 *   cleaning is defined as the removal of arcs which do not improve some score. <br>
 *  
 *  For clarity, hash refers to a 63 bit value representing a node/parent combination,
 *  index represents an index into the cache.  Usually (index == hash % cache.length)
 */
public class NodeCache implements Serializable
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 10616871348338916L;

    /**
     * How many hits should be attempted before giving up? 
     * If after this time no cache entries are found an old entry is removed.
     */
    public final static int maxCacheAttempts = 100;
    
    /** length of all arrays in the cache. */
    public final int cacheSize;
    
    /** The number of entries in the cache so far. */
    protected int numCacheEntries;
    
    /** number of nodes in the dataset */
    public final int numNodes;
    
    /** Dataset all values are calculated in relation to. */
    public final Value.Vector data;
    
    /** The ModelLearner function used for Maximum Likelyhood estimates. */
    public final ModelLearner mlLearner;
    
    /** The ModelLearner function used for MML extimates */
    public final ModelLearner mmlLearner;
    
    /** Details about this case (dataset & search instance) */
    public CaseInfo caseInfo;
    
    /** parentHash and childHash are arrays of random doubles used to generate node hashes. */
    protected long[] parentHashArray, childHashArray;
    
    ///////////////////////////////
    // Data stored in hash table //
    ///////////////////////////////
    
    /** cached MML estimate of nodes. */
    protected final double[] mmlCost;
    
    /** cached ML estimate of nodes. */
    protected final double[] mlCost;
        
    /** cacheHash[] stores hash of each cache entry with corresponding index. 
     *  cacheHash[] is stored to ensure no cache mismatches occur.
     *  Ideally (cacheHash[i] % cacheIndex.length == i) but this will differ due to cache clashes*/
    protected final long[] hashLookup;
    
    /** current value used in entryNumber[] */
    //protected int currentEntryNumber;
    
    ///////////////////////////////////////////////////////
    // Variables used to keep track of cache statistics. //
    ///////////////////////////////////////////////////////
    /** Variables to monitor cache, not essential. long is used as int is not big enough */
    public long getIndexCalled = 0, newHash = 0, hashFailed = 0, hashSucceed = 0,
        recalculations = 0,    cleanCalled = 0, outOfRange = 0;
    
    /** Keep track of cache statistics */
    public final int[] costings;
    /** Keep track of cache statistics */
    public int totalCostings = 0;
    /** Keep track of cache statistics */
    public final int[] learnings;
    /** Keep track of cache statistics */
    public int totalLearnings = 0;
    
    /** Keep track of cache statistics */
    public int totalInfiniteLearnings = 0;
    /** Keep track of cache statistics */
    public int totalInfiniteCostings = 0;
    
    ////////////////////////
    // Accessor functions //
    ////////////////////////
    /** accessor for number of nodes. */
    public int getNumNodes() { return numNodes; }
    
    /** accessor for data */ 
    public Value.Vector getData() { return data; }
    
    /** accessor for mmlLearner */
    public ModelLearner getMMLLearner() { return mmlLearner; }
    
    /** accessor for mlLearner */
    public ModelLearner getMLLearner() { return mlLearner; }
    
    /** Accessor function for numCacheEntries */
    public int getNumCacheEntries()
    {
        return numCacheEntries;
    }

    double mbPerNode;
    
    /** Convenience constructor for NodeCache */
    public NodeCache( Value.Vector data, ModelLearner mmlLearner, ModelLearner mlLearner ){
        this(data,mmlLearner,mlLearner,2.0);
    }
    
    /** Default maxumum cache size of 1gb.*/
    public static double maxCacheSizeMB = 500;
    
    /**
     *  Constructor for NodeCache <br>
     *  - Store all values passed in to local copies <br>
     *  - Allocate memory for mmlCost, mlCost etc    <br>
     *  
     *  mbPerNode is the number of MB of data allocated per node (default is 2.0)
     *  WARNING: Using too low a value for mbPerNode may significantly effect performance.
     */
    public NodeCache( Value.Vector data, ModelLearner mmlLearner, ModelLearner mlLearner, double mbPerNode )
    {
        // Save values passed in
        this.data = data;
        this.mmlLearner = mmlLearner;
        this.mlLearner = mlLearner;
        this.mbPerNode = mbPerNode;
        
        // Work out various contants needed for choosing a cache size.
        numNodes = ((Type.Structured)((Type.Vector)data.t).elt).cmpnts.length;    
        
        // used to gather statistics about nodeCache 
        costings = new int[numNodes];
        learnings = new int[numNodes];
        
        // Allocate some big slabs of memory to cache various things in.
        try {
            // Try to keep cache size linear compared to numNodes.  2 MegeBytes per node?
            double cacheSizeMB = numNodes * mbPerNode;
            
            // Maximum cache size of 1gb.
            if (cacheSizeMB > maxCacheSizeMB) cacheSizeMB = maxCacheSizeMB;
            
            // each cache entry is approx 24 bytes, multiply by 42000 to get MB
            cacheSize = (int)cacheSizeMB * 42000 + 7; // add 7 to make it a prime... maybe
            
            numCacheEntries = 0;
            
            // Allocate memory for cache entries.  48 bytes/entry
            // If an object was used for each entry instead, this would take 48+8+8=64 bytes/obj
            // Overhead is from extra java objects (8 bytes/obj) and reference array (8 bytes/obj).
            mmlCost = new double[cacheSize];
            mlCost = new double[cacheSize];
            hashLookup = new long[cacheSize];
            
            // Initialise cache values.
            for ( int i = 0; i < cacheSize; i++ ) { 
                mmlCost[i] = -1; 
                mlCost[i] = -1; 
                hashLookup[i] = -1;         
            }
        } catch (java.lang.OutOfMemoryError e) {
            System.err.println("Could not allocate memory for nodeCache.");
            throw e;
        }
        
    }           
    
    /**
     * Print statistics about cache useage. <br>
     * Level 0 = no printing <br>
     * Level 1 = Summary <br>
     * Level 2 = Full details 
     */
    public void printStats( int level )
    {
        if ( level <= 0 ) {}
        else if ( level == 1 ) {
            // Print out line of general cache info.
            System.out.print( "Cache hits : " + (100.0 * (getIndexCalled - newHash) / getIndexCalled)+"\t");
            System.out.print( "Models Learned : " + newHash + "\t" );
            System.out.print( "Learning failed : " + totalInfiniteLearnings + "\n" );
        } 
        else {
            System.out.println( "--- Cache Statistics ---" );
            System.out.println( "cacheSize =      " + cacheSize + " entries." );
            System.out.println( "getIndexCalled = " + getIndexCalled );
            System.out.println( "newHash    =     " + newHash );
            System.out.println( "hashFailed =     " + hashFailed );
            System.out.println( "hashSucceed =    " + hashSucceed );
            System.out.println( "recalculations = " + recalculations );
            System.out.println( "cleanCalled =    " + cleanCalled );
            System.out.println( "outOfRange =     " + outOfRange );
            System.out.println( "infiniteLearnings = " + totalInfiniteLearnings );
            System.out.println( "infiniteCostings  = " + totalInfiniteCostings );
            System.out.println();
            System.out.println( "#parents\tLearn\tCosting" );
            for ( int i = 0; i < learnings.length; i++ ) {
                System.out.println( i + "\t\t" + learnings[i] + "\t" + costings[i] );
                if ( learnings[i] == 0 && costings[i] == 0 ) { break; }
            }
            System.out.println( "total\t\t"+totalLearnings+"\t"+totalCostings );
            System.out.println( "------------------------" );
        }
    }
    
    /**
     * overwrite the entry of index[hash] with a new entry.  The cacheIndex of the new entry
     *  is specified by longHash.
     */
    protected void overwriteCacheEntry( int index, long hash )
    {
        // clear all cached information at index[hash]
        mmlCost[index] = -1;
        mlCost[index] = -1;
        
        // index[hash] now represents the node with long hash nodeID
        hashLookup[index] = hash;
        
        newHash ++;
    }
    
    /** Generate an index into the cache */
    protected int getIndex( Node node ) {
        getIndexCalled ++;
        
        // if not allocated, create parentHash and childHash
        if ( parentHashArray == null ) {
            java.util.Random rand = new java.util.Random(123);
            parentHashArray = new long[numNodes];
            childHashArray = new long[numNodes];
            for ( int i = 0; i < parentHashArray.length; i++ ) {
                parentHashArray[i] = rand.nextLong();
                childHashArray[i] = rand.nextLong();
            }
        }
        
        // create hash for node.  hash is a random 63 bit value representing each parent[]->child
        // combinations.  It is possible (though remarkably unlikely) for two nodes to hash to the
        // same value.  No checks are in place as this will probably never happen.
        long nodeHash = childHashArray[ node.var ];
        for ( int i = 0; i < node.parent.length; i++ ) {
            nodeHash += parentHashArray[node.parent[i]];
        }
        // we use a 63 bit value so nodeHash is always positive.
        nodeHash &= 0x7FFFFFFFFFFFFFFFl;
        
        // Using nodeHash create an index into arrays.
        int nodeIndex = (int)(nodeHash % cacheSize);
        
        // use quadratic probing to find a free entry in the hashtable.  This has better behaviour
        // than linear probe (although it is not guarenteed to fill the entire table.)
        for ( int i = 0; i < maxCacheAttempts; i++ ) {
            
            // Quadratic probing, each failure we lookahead an additional 'i' spaces.
            nodeIndex = (nodeIndex + i) % cacheSize;
            
            // If the nodeHash is found, return nodeIndex.
            if ( hashLookup[nodeIndex] == nodeHash ) {
                hashSucceed ++;
                
                return nodeIndex;
            }
            
            // If an empty entry is found, assign nodeHash to this nodeIndex
            if ( hashLookup[ nodeIndex ] == -1 ) {
                hashLookup[ nodeIndex ] = nodeHash;
                
                newHash ++;
                numCacheEntries ++;
                return nodeIndex;
            }
            
            // current cache entry already used by another node, so try again with new index.
            hashFailed ++;
        }    
                
        // We are forced to overwrite a value.
        nodeIndex = (int)(nodeHash % cacheSize);
        overwriteCacheEntry( nodeIndex, nodeHash );
        
        return nodeIndex;
    }
    
    /** Return unclean ML costs from default dataset */
    public double getMLCost( Node node )       
    {
        int hash = getIndex(node);
        if ( mlCost[hash] == -1 ) {
            mlCost[hash] = node.cost( mlLearner, data ); 
        }
        return mlCost[hash];
    }
    
    /** Return unclean MML costs from default dataset */
    public double getMMLCost( Node node )      
    { 
        int index = getIndex(node);
        
        if ( mmlCost[index] == -1 ) {
            mmlCost[index] = node.cost( mmlLearner, data ); 
            
            learnings[ node.parent.length ] ++;
            totalLearnings ++;
            
            if ( Double.isInfinite( mmlCost[index] ) ) {
                totalInfiniteLearnings ++;
            }
        }
        
        costings[ node.parent.length ]++;
        totalCostings ++;
        
        if ( Double.isInfinite( mmlCost[index] ) ) {
            totalInfiniteCostings ++;
        }
        
        return mmlCost[index];
    }
        
    /** return cost using a given modelLearner on data. */
    public double getCost( Node node, ModelLearner modelLearner ) 
    {
        if ( modelLearner == mmlLearner ) { return getMMLCost(node); }
        if ( modelLearner == mlLearner )  { return getMLCost(node); }    
        throw new RuntimeException( "unknown modelLearner = " + modelLearner );
    }
    
    /**
     * Do not serialise the actual cache when NodeCache object is 
     * serialised as it is often very large.  It can be recalculated
     * as needed.
     */
    public Object writeReplace() throws ObjectStreamException
    {
        // Create a cache of size 0
        NodeCache nc = new NodeCache(data,mmlLearner,mlLearner,0.0);
        // Trick the serialised cache into thinking it is larger.
        // mbPerNode flags to our unserialise method the desired size.
        nc.mbPerNode = mbPerNode;
        return nc;
    }

    /**
     * Unserialise NodeCache and create cache of the correct size.
     */
    public Object readResolve() throws ObjectStreamException {
        // When unserialising mpPerNode is used to determine cache size
        // instead of the actual size which writeReplace sets to 0.0
        NodeCache nc = new NodeCache(data,mmlLearner,mlLearner,mbPerNode);
        return nc;
    }

    
}





