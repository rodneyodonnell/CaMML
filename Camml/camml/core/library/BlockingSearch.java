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
// File contains a blocking version of Search.
//
// 
//
// Source formatted to 100 columns.
// 4567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890

// File: BlockingSearch.java
// Author: ?? joshc@csse.monash.edu.au ??


// package cdms.plugin.search;
package camml.core.library;

import cdms.plugin.search.*;

/** This class does just what the normal CDMS search does, but does it
    in the current thread.  IT DOES NOT RUN THE SEARCH IN A SEPARATE THREAD.
*/
public class BlockingSearch extends Search
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -6238560274322735328L;
    private boolean alive = false;
    //private boolean stepping = false;
    private boolean stop = false;

    public BlockingSearch(Search.SearchObject so)
    {
        super(so);
    }
  

    /**
     * Run search in this thread.
     */
    public void start() 
    {
        // we need to do this (or something similar) to ensure st is garbage collected.
        // st has a reference to it in its threadgroup.  Running start should remove this.
        st.stop = true;
        st.start();
    
        // run search step by step until completion
        if (!alive && !searchObject.isFinished()) {
            stop = false;
            alive = true;
            while(!stop && !searchObject.isFinished()) {          
                doStep();
            }
            alive = false;
            stop = false;
        }
    }

    public void stop()
    {
        if (alive) stop = true; 
    }

    public void step()
    {
        if (!alive && !searchObject.isFinished())
            {
                stepping = true;
                alive = true;
                doStep();
                alive = false;
                stepping = false;
                stop = false;
            }
    }

    public void reset()
    {
        if (!alive)
            {
                epochCount = 0;
                searchObject.reset();

                // Notify listeners.
                for (int i = 0; i < searchListeners.size(); i++)
                    {
                        try
                            {
                                ((SearchListener) searchListeners.get(i)).reset(this);
                            }
                        catch (Exception e) 
                            {
                                System.out.println(e);
                                e.printStackTrace();
                            }
                    }
            }
    }

    public boolean isAlive() 
    { 
        return alive;
    }

    public void doEpoch()
    {
        double metricCost = 0;

        try
            {
                metricCost = searchObject.doEpoch(); 
            }
        catch (Exception e) 
            {
                System.out.println(e);
                e.printStackTrace();
            }

        lastMetric = metricCost;
        epochCount++;   //Must be done after metricCost to be getLastMetric() safe.
    }

    public void doStep()
    {
        for (int i = 0; i < searchListeners.size(); i++)
            {
                try
                    {
                        ((SearchListener) searchListeners.get(i)).beforeEpoch(this);
                    }
                catch (Exception e) 
                    {
                        System.out.println(e);
                        e.printStackTrace();
                    }
            }

        doEpoch();

        for (int i = 0; i < searchListeners.size(); i++)
            {
                try
                    {
                        ((SearchListener) searchListeners.get(i)).afterEpoch(this);
                    }
                catch (Exception e) 
                    {
                        System.out.println(e);
                        e.printStackTrace();
                    }
            }
    }
}
