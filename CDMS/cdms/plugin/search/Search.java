//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: Search.java
// Author: {leighf,joshc}@csse.monash.edu.au

package cdms.plugin.search;

import javax.swing.*;

/** Search module. */
public class Search implements java.io.Serializable
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -883636347557227816L;
	protected SearchObject searchObject;
    protected SearchRunThread st;
    protected int epochCount = 0;
    protected double lastMetric;
    protected boolean stepping = false;

    protected java.util.Vector searchListeners = new java.util.Vector();

    /** Some example code. */
    public static void main(String argv[])
    {
	JFrame f = new JFrame("Sample Search Object");
	Search s = new Search(new SampleSearchObject());
	f.getContentPane().add(new SearchControl(s,new JLabel("Hello")));
	f.pack();
	f.setVisible(true);
    }

    /** A simple SearchObject that counts. */
    public static class SampleSearchObject implements SearchObject
    {
	int counter = 0;

	public void reset() 
	{ 
	    counter = 0; 
	}

	public double doEpoch() 
	{
	    counter++;
	    for (int i = 0; i < counter * 1000; i++) ;
	    return counter;
	}

	public boolean isFinished()
	{
	    return counter == 100;
	}

	public double getPercentage()
	{
	    return counter / 100.0;
	}
    }


    public static interface SearchObject
    {
	public void reset();
	public double doEpoch();        // Returns the error associated with the epoch.
	public boolean isFinished();
	public double getPercentage();  // Returns approximate fraction completed or negative if inappropriate.
    }

    public static interface SearchListener
    {
	public void reset(Search sender);
	public void beforeEpoch(Search sender);
	public void afterEpoch(Search sender);
	public void onCompletion(Search sender);
    }

    public Search(SearchObject so)
    {
	this.searchObject = so;
	st = new SearchRunThread();
	reset();
    }

    public void start() 
    {
	if (!st.isAlive() && !searchObject.isFinished())
	    {
		st = new SearchRunThread();
		st.stop = false;
		st.start();
	    }
	else
	    {
		System.out.println("Cannot start search - st.isAlive() = " + st.isAlive() + ", searchObject.isFinished() = " + searchObject.isFinished());
		System.out.flush();
	    }
    }

    public void stop()
    {
	if (st.isAlive())
	    {
		st.stop = true; 
	    }
    }

    public void reset()
    {
	if (!st.isAlive())
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

    public void step()
    {
	if (!st.isAlive() && !searchObject.isFinished())
	    {
		st = new SearchStepThread();
		st.stop = false;
		st.start();
	    }
    }

    /** Returns the search object. */
    public SearchObject getSearchObject() { return searchObject; }

    /** Returns true if the search thread is alive. */
    public boolean isAlive() { return st.isAlive(); }

    /** Returns true if stepping. */
    public boolean isStepping() { return stepping; }

    /** Returns the curent epoch number. */
    public int epoch() { return epochCount; }

    /** Returns last metric (error) value. */
    public double getLastMetric() { return lastMetric; }

    public void addSearchListener(SearchListener sel)
    {
	searchListeners.add(sel);
    }

    protected class SearchRunThread extends Thread
    {
	public boolean stop = false;

	public synchronized void doEpoch()
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

	public synchronized void step()
	{
	    for (int i = 0; i < searchListeners.size(); i++)
		{
		    try
			{
			    ((SearchListener) searchListeners.get(i)).beforeEpoch(Search.this);
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
			    ((SearchListener) searchListeners.get(i)).afterEpoch(Search.this);
			}
		    catch (Exception e) 
			{
			    System.out.println(e);
			    e.printStackTrace();
			}
		}
	}

	public synchronized void run()
	{
	    while (!stop && !searchObject.isFinished()) {
		step();
	    }
	    // Fire the onCompletion method of each listener if the search has finished 
	    // (wasn't forced to stop before the end)...
	    if(searchObject.isFinished()) {		
		for (int i = 0; i < searchListeners.size(); i++)
		    {
			try
			    {
				((SearchListener) searchListeners.get(i)).onCompletion(Search.this);
			    }
			catch (Exception e) 
			    {
				System.out.println(e);
				e.printStackTrace();
			    }
		    }
	    }
	}
    }


    private class SearchStepThread extends SearchRunThread
    {
	public synchronized void run()
	{
	    stepping = true;
	    stop = false;
	    this.step();
	    stepping = false;
	    stop = true;

	    if(searchObject.isFinished())                                     // Fire the onCompletion method of each listener if the search
		{                                                                 // has finished (wasn't forced to stop before the end)...
		    for (int i = 0; i < searchListeners.size(); i++)
			{
			    try
				{
				    ((SearchListener) searchListeners.get(i)).onCompletion(Search.this);
				}
			    catch (Exception e) 
				{
				    System.out.println(e);
				    e.printStackTrace();
				}
			}
		}
	}
    }
}

