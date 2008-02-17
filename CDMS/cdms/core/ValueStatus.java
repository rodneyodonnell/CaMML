//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: ValueStatus.java
// Authors: {lloyd,leighf,joshc}@csse.monash.edu.au

package cdms.core;

import java.io.ObjectStreamException;


/** Represents the status of a value. */
public class ValueStatus implements java.io.Serializable
{
  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 1268901769520129025L;

private boolean defnLoaded = false;

  String description;

  public ValueStatus(String description)
  {
    this.description = description;
  }

  public String toString()
  {
    if (!defnLoaded) 
    {
      // Load a description list into a hash table from a file.
      // Internationalization !!!
    }

    return description;
  }

    /**
     * This .equals() function required for serialisation. 
     * Post serialisation (S_PROPER == S_PROPER) returns false
     *  but S_PROPER.equals( S_PROPER ) should return true.
     */
    public boolean equals( Object obj ) {
	return ((ValueStatus)obj).description.equals( description );
    }
    
	/**
	 * The standard CDMS ValueStatus values must be serialised properly
	 *  to allow for the "==" method to be used after serialisation or RMI operations
	 *  have been performed. <br>
	 * Added by Rodney O'Donnell, 4/11/05
	 * see: http://java.sun.com/j2se/1.4.2/docs/api/java/io/Serializable.html <br>
	 */
	public Object readResolve() throws ObjectStreamException {
		// This should probably be done in a more elegant manner, but it works.
		if (this.equals(Value.S_PROPER)) {return Value.S_PROPER;}
		if (this.equals(Value.S_NA)) {return Value.S_NA;}
		if (this.equals(Value.S_INVALID)) {return Value.S_INVALID;}
		if (this.equals(Value.S_UNOBSERVED)) {return Value.S_UNOBSERVED;}
		if (this.equals(Value.S_IRRELEVANT)) {return Value.S_IRRELEVANT;}
		if (this.equals(Value.S_INTERVENTION)) {return Value.S_INTERVENTION;}
		return this;
	}
}

// End of file.
