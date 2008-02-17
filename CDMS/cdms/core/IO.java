//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: IO.java
// Authors: {leighf,joshc}@csse.monash.edu.au

package cdms.core;

import java.io.*;

/** Standard library of IO functions. */ 
public class IO extends Module.StaticFunctionModule
{
  public IO()
  {
    super("IO",Module.createStandardURL(IO.class),IO.class);
  }
  
  /** <code>(Str,Str -> Str -> Str -> Str -> Str -> Str -> Str -> Boolean -> Boolean) -> t</code> 
      A function to load a delimited text file.  The parameters are: the filename, a serialized 
      string representing column types; unobserved string; irrelevant string; start delimiter; 
      end delimiter; word delimiter; quote character; consume consecutive delimiters; 
      has title line.  The easy was to create the parameters for this function is to use 
      the LoadDelimitedFileGui in the Desktop package. 
  */
  public static final LoadDelimitedFile loadDelimitedFile = new LoadDelimitedFile();

  /** <code>(Str,Str -> Str -> Str -> Str -> Str -> Str -> Str -> Boolean -> Boolean) -> t</code> 
      <p>
      A function to load a delimited text file.  The parameters are: the filename, a serialized 
      string representing column types; unobserved string; irrelevant string; start delimiter; 
      end delimiter; word delimiter; quote character; consume consecutive delimiters; 
      has title line.  The easy was to create the parameters for this function is to use 
      the LoadDelimitedFileGui in the Desktop package. 
      @see cdms.plugin.dialog.DelimitedFile
  */
  public static class LoadDelimitedFile extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -4255869947213026238L;

	public static final Type.Structured partParamType =
      new Type.Structured(new Type[] { Type.STRING, Type.STRING, 
                                       Type.STRING, Type.STRING, Type.STRING,
                                       Type.STRING, Type.STRING, Type.BOOLEAN, Type.BOOLEAN },
                          new String[] { "Column types", "Unobserved String", "Irrelevant String", 
                                         "Start delim.", "End delim.", "Word delim.", 
                                         "Quote char.", "Consume consecutive delim.", 
                                         "Has Title line" } );

    public static final Type.Structured paramType = 
      new Type.Structured(new Type[] { Type.STRING, partParamType },
                          new String[] { "Filepath", "Params" } );

    public static final Type.Function TT = new Type.Function(paramType, Type.TYPE);

    public LoadDelimitedFile()
    {
       super(TT); 
    }

    public Value apply(Value param)
    {
      Value.Structured sparams = (Value.Structured) ((Value.Structured) param).cmpnt(1);
      String fname = ((Value.Str) ((Value.Structured) param).cmpnt(0)).getString();

      // Serialization used for types. Need type parser.
      try
      {
        String ctypesStr = ((Value.Str) sparams.cmpnt(0)).getString();
        ByteArrayInputStream is = new ByteArrayInputStream(ctypesStr.getBytes());
        ObjectInputStream p = new ObjectInputStream(is);
        Type.Structured ctypes = (Type.Structured) p.readObject();
        is.close();

        String unobservedStr = ((Value.Str) sparams.cmpnt(1)).getString();
        String irrelevantStr = ((Value.Str) sparams.cmpnt(2)).getString();
        String startDelim = ((Value.Str) sparams.cmpnt(3)).getString();
        String endDelim = ((Value.Str) sparams.cmpnt(4)).getString();
        String wordDelim = ((Value.Str) sparams.cmpnt(5)).getString();
        String quoteChar = ((Value.Str) sparams.cmpnt(6)).getString();
        boolean consume = ((Value.Discrete) sparams.cmpnt(7)).getDiscrete() == 
                           Value.TRUE.getDiscrete();
        boolean titleLine = ((Value.Discrete) sparams.cmpnt(8)).getDiscrete() == 
                             Value.TRUE.getDiscrete();

        // Load data and return vector of structured Values.
        return new IO.DataVector(fname,ctypes,unobservedStr,irrelevantStr, startDelim, endDelim, 
                                 wordDelim, quoteChar, consume, titleLine);
      }
      catch (Exception e)
      {
        System.out.println("Error serializing types.");
        e.printStackTrace();
        return Value.TRIV;
      }
    }
  }

  private static class DataVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 4062894543159682172L;
	public String fname;
    public Type.Structured ctypes;
    public String unobservedStr;
    public String irrelevantStr;
    public String startDelim;
    public String endDelim;
    public String wordDelim;
    public boolean consume;
    public boolean titleLine;
    public String quoteChar;
    public Value.Structured[] vals;

    private static Type.Structured makeEltType(Type.Structured ctypes, 
                                               String fname, 
                                               String startDelim,
                                               String endDelim,
                                               String wordDelim,
                                               String quoteChar,
                                               boolean consume, 
                                               boolean titleLine)
    {
      if(!titleLine) return ctypes;
      try
      {
        FileReader fr = new FileReader(fname.toString());
        BufferedReader reader = new BufferedReader(fr);
        String ln = reader.readLine();
        reader.close();
        String[] labels = new String[ctypes.cmpnts.length];
        boolean quoted = (!quoteChar.equals(""));
        labels = DataStructured.getCmpnts(ln, wordDelim, consume, quoted, quoteChar, ctypes.cmpnts.length);
        return new Type.Structured(ctypes.cmpnts, labels, ctypes.checkCmpntsNames);
      }
      catch(Exception e)
      {
        e.printStackTrace();
        return ctypes;
      }
    }

    public DataVector(String fname, Type.Structured ctypes,
                      String unobservedStr, String irrelevantStr,
                      String startDelim,
                      String endDelim, String wordDelim,
                      String quoteChar, boolean consume, boolean titleLine)
    {
      super(new Type.Vector(Type.DISCRETE,
                            makeEltType(ctypes, fname, startDelim, endDelim, wordDelim, quoteChar, consume, titleLine),
                            false,
                            false,
                            false,
                            false));

      this.fname = fname;
      this.ctypes = ctypes;
      this.unobservedStr = unobservedStr;
      this.irrelevantStr = irrelevantStr;
      this.startDelim = startDelim;
      this.endDelim = endDelim;
      this.wordDelim = wordDelim;
      this.quoteChar = quoteChar;
      this.consume = consume;
      this.titleLine = titleLine;

      // Load the data.
      loadData();
    }

    public int length()
    {
      return vals.length;
    }

    public Value elt(int i)
    {
      return vals[i];
    }

    private void loadData()
    {
      try
      {
        java.util.Vector dv = new java.util.Vector();
        FileReader fr = new FileReader(fname.toString());
        BufferedReader reader = new BufferedReader(fr);
        String ln = reader.readLine();
        if (titleLine) ln = reader.readLine();             // Skip the title line if there is one...
        while (ln != null)
        {
          if(!this.startDelim.equals(""))
          {
            /* Use startDelim and endDelim to extract the relevant part of ln */
            if(ln.indexOf(this.startDelim) != -1)
            {
              ln = ln.substring(ln.indexOf(this.startDelim)+this.startDelim.length());
            }
          }
          if(!this.endDelim.equals(""))
          {
            if(ln.indexOf(this.endDelim) != -1)
            {
              ln = ln.substring(0, ln.indexOf(this.endDelim));
            }
          }
          dv.add(ln);
          ln = reader.readLine();
        }
        vals = new Value.Structured[dv.size()];
        reader.close();
 
        for(int count = 0; count < dv.size(); count++)
        {
          vals[count] = new DataStructured((String)dv.elementAt(count), this);
        }

        System.out.println("Found " + vals.length + " lines in " + fname + ".");
      }
      catch (Exception e)
      {
        System.out.println("\nError parsing file: " + e);
      }
    }
  }

  /** This class provides a method to retrieve a field (component of a Value.Structured)
      from an element of a DataVector.  NB  DataStructured can only
      handle Discrete, Continuous, String, Triv, SymbolicNames, and Prob - and 
      Vectors or Structures of these.  If any components are of Type Function, 
      Model or Obj, they will be returned as Value.TRIV by the cmpnt() method.
      Any field to be interpreted as a structure should have the
      form:  "(cmpnt1,cmpnt2,...,cmpntN)".
      Any field to be interpreted as a vector should have the form:  "[elt1,elt2, ...,eltN]".

      @see IO.DataVector
  */
  public static class DataStructured extends Value.Structured
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5647190860656775854L;
	
	private String s;
    private Value[] vals = null;
    private DataVector dv;
    
    public DataStructured(String s, DataVector dv)
    {
      super(dv.ctypes);
      this.s = s;
      this.dv = dv;
    }
    
    public int length()
    {
      return ((Type.Structured)t).cmpnts.length;
    }
    
    public Value cmpnt(int i)
    {
      if(vals == null)
      {
        try
        {
          vals = new Value[length()];
          boolean quoted = (!dv.quoteChar.equals(""));
          String[] cmpntStrings = getCmpnts(s, dv.wordDelim, dv.consume, quoted, dv.quoteChar, length());
          int count;
          for(count = 0; count < vals.length; count++)
          {
            vals[count] = getValueFromString(dv.ctypes.cmpnts[count], cmpntStrings[count]);
          }
          s = null;
          dv = null;
        }
        catch(Exception e)
        {
          vals = null;
          return Value.TRIV;
        }
      }
      return vals[i];
    }

    private Value getValueFromString(Type cmpntType, String s)
    {
      /* Check which Type component is of, and return a new value of that Type. */
      if(cmpntType.getClass() == Type.Discrete.class)
      {
        return getDiscreteFromString(s, cmpntType);
      }
      else
      {
        if(cmpntType.getClass() == Type.Continuous.class)
        {
          return getContinuousFromString(s, cmpntType);
        }
        else
        {
          if(cmpntType.getClass() == Type.Str.class)
          {
            return getStrFromString(s, cmpntType);
          }
          else
          {
            if(cmpntType.getClass() == Type.Triv.class)
            {
              return getTrivFromString(s, cmpntType);
            }
            else
            {
              if(cmpntType.getClass() == Type.Symbolic.class)
              {
                return getSymbolicNamesFromString(s, (Type.Symbolic) cmpntType);
              }
              else
              {
                return Value.TRIV;
              }
            }
          }
        }
      }
    }

    /* This method checks whether component is unobserved or irrelevant... */
    private ValueStatus getValueStatus(String s)
    {
      if(s.equals(dv.unobservedStr))
      {
        return Value.S_UNOBSERVED;
      }
      else
      {
        if(s.equals(dv.irrelevantStr))
        {
          return Value.S_IRRELEVANT;
        }
        else
        {
          return Value.S_PROPER;
        }
      }
    }

    private Value.Discrete getDiscreteFromString(String s, Type cmpntType)
    {
      ValueStatus vs = getValueStatus(s);
      if(vs == Value.S_PROPER)
        return new Value.Discrete((Type.Discrete)cmpntType, vs, Integer.parseInt(s));
      else
        return new Value.Discrete((Type.Discrete)cmpntType, vs, 0);
    }

    private Value.Continuous getContinuousFromString(String s, Type cmpntType)
    {
      ValueStatus vs = getValueStatus(s);
      if(vs == Value.S_PROPER)
        return new Value.Continuous((Type.Continuous)cmpntType, vs, Double.parseDouble(s)); 
      else
        return new Value.Continuous((Type.Continuous)cmpntType, vs, 0); 
    }

    private Value.Str getStrFromString(String s, Type cmpntType)
    {
      ValueStatus vs = getValueStatus(s);
      return new Value.Str((Type.Str)cmpntType, vs, s);
    }

    private Value getTrivFromString(String s, Type cmpntType)
    {
      return Value.TRIV;
    }

    private Value.Discrete getSymbolicNamesFromString(String s, Type.Symbolic cmpntType)
    {
      ValueStatus vs = getValueStatus(s);
      if(vs == Value.S_PROPER)
        return new Value.Discrete((Type.Symbolic)cmpntType, vs, cmpntType.string2int(s));
      else
        return new Value.Discrete((Type.Symbolic)cmpntType, vs, 0);
    }

    /** This method takes 's' as a parameter which is a string of words
        delimited by 'delimiter'.  It returns an array of Strings, where
        each element of the array is a word.
    */
    public static String[] getCmpnts(String str, String delimiter, boolean consume, boolean quoted, String quoteChar, int nWords)
    {
      int pos1 = 0;
      int pos2;
      int quotePos;
      int count;
      String s = str.substring(0);
      String[] result = new String[nWords];

      if(consume)
      {
        if(quoted) s = checkAndRemoveConsecutiveDelimitersWithQuotes(s, delimiter, quoteChar);
        else s = checkAndRemoveConsecutiveDelimiters(s, delimiter);
      }

      pos2 = s.indexOf(delimiter, 0);
      if(pos2 == 0) pos1 = delimiter.length();
      else pos1 = 0;
      
      // pos1 is now where we start from...

      for(count = 0; count < nWords-1; count++)
      {
        pos2 = s.indexOf(delimiter, pos1);

        if(quoted)
        {
          quotePos = s.indexOf(quoteChar, pos1);
          if(quotePos == pos1)
          {
            quotePos = s.indexOf(quoteChar, quotePos + quoteChar.length());
            if(quotePos != -1)
            {
              pos2 = s.indexOf(delimiter, quotePos + quoteChar.length());
            }
            else
            {
              throw new RuntimeException("Error - unmatched quote character:" + s);
            }
          }
        }
        result[count] = s.substring(pos1, pos2);
        pos1 = pos2+delimiter.length();
      }
      
      // last word...
      
      pos2 = s.indexOf(delimiter, pos1);
      if(quoted)
      {
        quotePos = s.indexOf(quoteChar, pos1);
        if(quotePos == pos1)
        {
          quotePos = s.indexOf(quoteChar, quotePos + quoteChar.length());
          if(quotePos != -1)
          {
            pos2 = s.indexOf(delimiter, quotePos + quoteChar.length());
          }
          else
          {
            throw new RuntimeException("Error - unmatched quote character:" + s);
          }
        }
      }
      if(pos2 != -1)
      {
        result[count] = s.substring(pos1, pos2);
      }
      else
      {  
        result[count] = s.substring(pos1);
      }
      
      if(quoted)  // remove quotes...
      {
        for(count = 0; count < nWords; count++)
        {
          pos1 = result[count].indexOf(quoteChar);
          if(pos1 == 0)
          {
            pos2 = result[count].indexOf(quoteChar, pos1+quoteChar.length());
            if(pos2 == result[count].length() - 1)
            {
              result[count] = result[count].substring(1, result[count].length() - 1);
            }
          }
        }
      }

      return result;
    }
    
    public static String checkAndRemoveConsecutiveDelimiters(String s, String delimiter)
    {
      int firstDelim, secondDelim;
      
      firstDelim = s.indexOf(delimiter, 0);
      
      while(firstDelim != -1)
      {
        secondDelim = s.indexOf(delimiter, firstDelim + delimiter.length());
        if(secondDelim == firstDelim + delimiter.length())
        {
          s = s.substring(0, firstDelim).concat(s.substring(secondDelim));
        }
        else
        {
          firstDelim = secondDelim;
        }
      }
      return s;
    }
  
    public static String checkAndRemoveConsecutiveDelimitersWithQuotes(String s, String delimiter, String quoteChar)
    {
      int firstDelim, secondDelim, quotePos;
      
      firstDelim = s.indexOf(delimiter, 0);
      quotePos = s.indexOf(quoteChar, 0);
      if((quotePos != -1)&&(quotePos < firstDelim))
      {
        quotePos = s.indexOf(quoteChar, quotePos + quoteChar.length());
        if(quotePos != -1)
        {
          firstDelim = s.indexOf(delimiter, quotePos + quoteChar.length());
        }
        else
        {
          throw new RuntimeException("Error - unmatched quote character: " + s);
        }
      }
      
      while(firstDelim != -1)
      {
        secondDelim = s.indexOf(delimiter, firstDelim + delimiter.length());
        if(secondDelim == firstDelim + delimiter.length())
        {
          s = s.substring(0, firstDelim).concat(s.substring(secondDelim));
        }
        else
        {
          quotePos = s.indexOf(quoteChar, firstDelim + delimiter.length());
          if((quotePos != -1)&&(quotePos < secondDelim))
          {
            quotePos = s.indexOf(quoteChar, quotePos + quoteChar.length());
            if(quotePos != -1)
            {
              secondDelim = s.indexOf(delimiter, quotePos + quoteChar.length());
            }
            else
            {
              throw new RuntimeException("Error - unmatched quote character: " + s);
            }
          }
          firstDelim = secondDelim;
        }
      }
      return s;
    }

    /** This method is just for the automatic detection of quote and delimiter characters. */
    public static boolean quotedCmpntExists(String s, String delim, int cmpntNumber, boolean consume, String quoteChar)
    {
      try
      {
        String[] allWordsUpToCmpntNumber = getCmpnts(s, delim, consume, true, quoteChar, cmpntNumber+1);
        String tmp = allWordsUpToCmpntNumber[cmpntNumber];
        return (tmp.length() > 0);
      }
      catch(Exception e)
      {
        return false;
      }
    }

    /** This method is just for the automatic detection of quote and delimiter characters. */
    public static boolean cmpntExists(String s, String delim, int cmpntNumber, boolean consume)
    {
      try
      {
        String[] allWordsUpToCmpntNumber = getCmpnts(s, delim, consume, false, "no quotes", cmpntNumber+1);
        String tmp = allWordsUpToCmpntNumber[cmpntNumber];
        return (tmp.length() > 0);
      }
      catch(Exception e)
      {
        return false;
      }
    }

    /** This method is just for the automatic detection of quote and delimiter characters. */
    public static String getCmpnt(String s, String delim, int cmpnt, boolean consume)
    {
      return getCmpnts(s, delim, consume, false, "no quotes", cmpnt+1)[cmpnt];
    }

    /** This method is just for the automatic detection of quote and delimiter characters. */
    public static String getQuotedCmpnt(String s, String delim, int cmpnt, boolean consume, String quoteChar)
    {
      return getCmpnts(s, delim, consume, true, quoteChar, cmpnt+1)[cmpnt];
    }
  }

  /** <code>Str -> [Symbolic(a,c,g,t)]</code> A GeneBank DNA file loader.  The only
      parameter is the filename.
  */
  public static final LoadGeneBankFile loadGeneBankFile = new LoadGeneBankFile();

  /** <code>Str -> [Symbolic(a,c,g,t)]</code>
      <p>
      A GeneBank DNA file loader.  The only parameter is the filename.
  */
  public static class LoadGeneBankFile extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5222861559953722825L;
	public static final Type.Function TT = new Type.Function(Type.STRING,SymbolicStreamVector.TT);

    public LoadGeneBankFile()
    {
      super(TT);
    }

    public Value apply(Value param)
    {
      // Get params: filename. 
      String fname = ((Value.Str) param).getString();

      FileInputStream fis = null;
      try
      {
        fis = new FileInputStream(fname);
      }
      catch (Exception e)
      {
        System.out.println("Error opening Genebank file.  " + e);
      }

      return new SymbolicStreamVector(fis,"ORIGIN","");
    }
  }

  /** A generic class for parsing and loading a vector of symbolic values from
      a text file. 
  */
  public static class SymbolicStreamVector extends Value.Vector
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 2638465137032243542L;

	public static final Type.Vector TT = new Type.Vector(Type.DNA);

    public InputStream istr;
    public String origin, fin;

    // The Java Vector class is not used because it stores *expensive* Objects.
    // Instead we use a fast and efficient bucket system to store the symbolic int.
    // The next free position is data[nextI][nextII]
    private static final int log2BucketSize = 8;  // 256 elements.
    private static final int bucketSize = 1 << log2BucketSize;
    private static final int modBucketSize = bucketSize - 1;
    private static final int divBucketSize = log2BucketSize;

    private int bucketCount = 128;
    private int[][] data = new int[bucketCount][];
    private int length = 0;

    public SymbolicStreamVector(InputStream istr, String origin, String fin)
    {
      super(TT);

      this.istr = istr;
      this.origin = origin;
      this.fin = fin;

      // Initialize first bucket.
      data[0] = new int[bucketSize];

      loadData();
    }

    public int length()
    {
      return length;
    }

    public Value elt(int i)
    {
      return new Value.Discrete(Type.DNA,intAt(i));
    }

    public int intAt(int i)
    {
      return data[i >> divBucketSize][i & modBucketSize];
    }

    private void loadData()
    {
      try
      {
        boolean originFound = false;
        boolean finFound = false;

        BufferedReader in = new BufferedReader(new InputStreamReader(istr));

        // If origin string is empty or null then assume no start code.
        if (origin == null)
        {
          originFound = true;
        }
        else
        {
          if (origin.equals("")) originFound = true;
        }

        int intVal = 0;
        boolean match = false;
        String ln = in.readLine();
        while (ln != null && !finFound)
        {
          if (!originFound)
          {
            int originIndex = ln.indexOf(origin);
            if (originIndex >= 0)
            {
              originFound = true;
              if (originIndex + origin.length() < ln.length())
              {
                ln = ln.substring(originIndex + origin.length(),ln.length() - 1);
              }
              else
              {
                ln = new String("");
              }
            }
          }
          if (originFound)
          {
            // Check for fin token.
            if (fin != null && !fin.equals(""))
            {
              int finIndex = ln.indexOf(fin);
              if (finIndex >= 0)
              {
                finFound = true;
                if (finIndex != 0)
                {
                  ln = ln.substring(0,finIndex - 1);
                }
                else
                {
                  ln = new String("");
                }
              }
            }

            for (int i = 0; i < ln.length(); i++)
            {
              match = false;
              try
              {
                intVal = Type.DNA.string2int(String.valueOf(ln.charAt(i)));
                match = true;
              }
              catch (Exception e) { ; }

              if (match)
              {
                // Check if enought bucket space available.
                int reqBucketCount = length + 1 + modBucketSize >> divBucketSize;
                if (reqBucketCount > 0 && reqBucketCount > bucketCount)
                {
                  int newData[][] = new int[reqBucketCount][];
                  System.arraycopy(data,0,newData,0,bucketCount);
                  data = newData;
                  bucketCount = reqBucketCount;
                }

                int indexI = length >> divBucketSize;
                if (data[indexI] == null) data[indexI] = new int[bucketSize];
                data[length >> divBucketSize][length & modBucketSize] = intVal;
                length++;
              }
            }
          }
          ln = in.readLine();
        }

        System.out.println(length + " DNA symbols read.");
      }
      catch (Exception e)
      {
        System.out.println("\nError parsing stream: " + e);
      }
    }
  }


}

