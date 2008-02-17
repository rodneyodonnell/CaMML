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
