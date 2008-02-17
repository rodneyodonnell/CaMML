package cdms.plugin.c5;

import cdms.core.*;
import java.io.*;

/** The C5 class is a module that (currently) adds two functions to the environment:<BR>
    <UL>
    <LI> The C5-CDMS data loader ({@link #c5loader}): a function to load C5 data into CDMS.
    <LI> The CDMS-C5 data saver ({@link #c5saver}): a function to save CDMS data into the C5 .data
    and .names format.
    </UL>
*/
public class C5 extends Module
{
  public static java.net.URL helpURL = Module.createStandardURL(C5.class);

  public String getModuleName() { return "C5"; }
  public java.net.URL getHelp() { return helpURL; }

  public void install(Value params) throws Exception
  {
    Environment.env.add("loadC5file","C5", c5loader,
                        "Load a file in C5 format");

    Environment.env.add("saveC5file","C5", c5saver,
                        "Save data to C5 file format");

    Environment.env.add("saveC5files","C5", c5testAndDataSaver,
                        "Save data to C5 file format");
  }

//************************************************************** C5 Data Loader ********************

  /** This is a static instance of the {@link C5.C5Loader} class. */
  public static Value.Function c5loader = new C5Loader();

  /** This function takes the filename of a C5 .data, .test or .names file, and returns
      the data as a CDMS vector.  It expects the .data and .names files to be
      in the same directory.<BR>
      <B>Please note:</B><BR>
      <UL>
      <LI>It does not yet support the C5 date, time, or timestamp formats,
      and treats these values as strings.  
      <LI>Nor does it support derived attributes, specified in C5 .names files as <BR>
      attName := exp<BR>
      where exp is some expression containing one or more (non-derived) attributes.
      Such derived attributes will be skipped completely (as they do not actually exist
      in the .data file).
      <LI>C5 attributes given as 'ignore' or 'label' attributes are treated as strings.
      This ensures no data is lost from the .data file when it is loaded into CDMS.
      </UL><BR>
      The table below shows the mapping from C5 types to CDMS types.<BR>
      <TABLE BORDER="1">
      <TR><TD ROWSPAN="2">C5 TYPE</TD><TD COLSPAN="2">CDMS TYPE</TD></TR>
      <TR><TD>BASE TYPE</TD><TD>TYPE ATTRIBUTES</TD></TR>
      <TR><TD>continuous</TD><TD>CONTINUOUS</TD><TD>-</TD></TR>
      <TR><TD>nominal</TD><TD>SYMBOLIC</TD><TD>UNORDERED</TD></TR>
      <TR><TD>nominal [ordered]</TD><TD>SYMBOLIC</TD><TD>ORDERED</TD></TR>
      <TR><TD>discrete</TD><TD>DISCRETE</TD><TD>-</TD></TR>
      <TR><TD>label</TD><TD>STRING</TD><TD>-</TD></TR>
      <TR><TD>time</TD><TD>STRING</TD><TD>-</TD></TR>
      <TR><TD>date</TD><TD>STRING</TD><TD>-</TD></TR>
      <TR><TD>timestamp</TD><TD>STRING</TD><TD>-</TD></TR>
      <TR><TD>ignore</TD><TD>STRING</TD><TD>-</TD></TR>
      </TABLE>
  */
  public static class C5Loader extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 7814773792646316079L;

	public C5Loader()
    {
      super(new Type.Function(new Type.Structured(new Type[]{Type.STRING, new Type.Variable()}, 
                                                  new String[]{"Filename", "Anything"}, 
                                                  new boolean[]{false, false}), 
                              new Type.Vector(new Type.Variable(), Type.STRUCTURED, false, false, false, false), 
            false, false));
    }

    private static Value.Vector defaultReturnValue = new VectorFN.EmptyVector(new Type.Vector(new Type.Variable(), Type.STRUCTURED, false, false, false, false));

    private static boolean commentLine(String line)
    {
//      char c;
//      int count;
//System.out.println("COMMENT? :" + line);
      String l = line.trim();
//System.out.println("TRIM? :" + line);
      if(l == null) return true;
      if(l.length() > 0)
      {
//System.out.println("CHAR AT 0? :" + l.charAt(0));
        if((l.charAt(0) == '|')||(l.charAt(0) == '\n')) 
        {
          return true;
        }
        // Check if there is a ":=" in the line, and that it doesn't come after a comment symbol "|"...
        if(l.indexOf('|') != -1)
        {
          if((l.indexOf(":=") != -1) && (l.indexOf(":=") < l.indexOf('|'))) return true;  //  CDMS does not (yet) support derived attributes in C5 files.
        }
        else
        {
          if(l.indexOf(":=") != -1) return true;     //  CDMS does not (yet) support derived attributes in C5 files.
        }
        return false;
      }
      return true;
    }

    private static String chewUpWhiteSpace(String line)
    {
      int count, count2, len;
      char[] result;
      for(count = 0, len = 0; count < line.length(); count++)
      {
        if(line.charAt(count) == '|') break;
        if((line.charAt(count) != ' ')&&(line.charAt(count) != '\t'))
          len++;
      }
      result = new char[len];
      for(count = 0, count2 = 0; ((count < line.length())&&(count2 < len)); count++)
      {
        if((line.charAt(count) != ' ')&&(line.charAt(count) != '\t'))
        {
          result[count2] = line.charAt(count);
          count2++;
        }
      }
      if(len > 0)
        return new String(result);
      else 
        return null;
    }

    private static class TypeNamePair
    {
      public Type t;
      public String name;

      public TypeNamePair(Type t, String name)
      {
        this.t = t;
        this.name = name;
      }
    }

    private static int numberOfCommas(String line)
    {
      int i = line.indexOf(',');
      int result = 0;
      while(i != -1)
      {
        result++;
        i = line.indexOf(',', i+1);
      }
      return result;
    }

    private static TypeNamePair lineToTypeNamePair(String line)
    {
      String attributeName = "attribute";
      Type t = Type.TRIV;
      if(commentLine(line)) return null;
      String l = chewUpWhiteSpace(line);
//System.out.println("Line: " + line);
//System.out.println("Chewed Line: " + l);
      String remainder;
      int colonIndex = l.indexOf(':');
      if(colonIndex != -1)
      {
        attributeName = l.substring(0,colonIndex);
        int periodIndex = l.lastIndexOf('.');
        if(periodIndex != -1)
          remainder = l.substring(colonIndex+1, periodIndex);
        else
          remainder = l.substring(colonIndex+1);
        remainder = remainder.trim(); // Get rid of any white space between the colon and where the info starts.  e.g.  "attName: continuous."
//System.out.println("Type string: " + remainder);
        if(remainder.equals("continuous"))
        {
//System.out.println("Continuous Type Found");
          t = Type.CONTINUOUS;
        }
        else
        {
          if(remainder.startsWith("discrete"))
          {
//System.out.println("Discrete Type Found");
            t = Type.DISCRETE;
          }
          else
          {
            if((remainder.equals("label"))||
               (remainder.equals("date"))||
               (remainder.equals("time"))||
               (remainder.equals("timestamp"))||
               (remainder.equals("ignore")))
            {
//System.out.println("Unsupported Type Found - treating as string");
              t = Type.STRING;
            }
            else
            {
              t = getSymbolicType(remainder);
            }
          }
        }
      }
      return new TypeNamePair(t, attributeName);
    }

    private static Type.Symbolic getSymbolicType(String line)
    {
      boolean ordered = false;
      if(line.startsWith("[ordered]"))
      {
        ordered = true;
        line = line.substring(9); // "[ordered]" is 9 characters long.
      }
//System.out.println("Symbolic Type Found");
      int count, i, j;
      String[] labels = new String[numberOfCommas(line) + 1];
      for(count = 0, i = -1; count < labels.length; count++)
      {
        j = line.indexOf(',', i+1);
        if(j != -1)
          labels[count] = line.substring(i+1,j);
        else
          labels[count] = line.substring(i+1);
//System.out.println("labels[" + count + "] = " + labels[count]);
        i = j;
      }
      return new Type.Symbolic(false, false, true, ordered, labels);
    }

      /** (string,*) -> [(STRUCT)]*/
    public Value apply(Value v)
    {
      String filename = ((Value.Structured)v).cmpnt(0).toString();
      int lastIndex = filename.lastIndexOf(".");
      String stem = filename.substring(1,lastIndex);
//System.out.println("STEM:" + stem);
//String stem = "/home/joshc/DATA/genetics";
      Type.Vector vectorType = Type.VECTOR;
      String line = "| <- that's the comment character.";
// Read in the .names file, and setup all the type information...
      try
      {
        String namesFile = stem + ".names";
        BufferedReader namesIn = new BufferedReader(new FileReader(namesFile)); 
// Determine the number of non-comment lines in the file.
        int numberOfNonCommentLines = 0;
        while((line = namesIn.readLine()) != null)
        {
          if(!commentLine(line)) numberOfNonCommentLines++;
        }
        namesIn.close();
        namesIn = new BufferedReader(new FileReader(namesFile)); 
// Get rid of all comment lines at the start.
        line = "| <- that's the comment character.";
        while (commentLine(line)) 
        {
          line = namesIn.readLine();
          if(line == null)
          {
            System.out.println("ERROR - empty .names file.");
            return defaultReturnValue;
          }
        }
// Get the line that defines the class attribute.
//System.out.println("CLASS LINE: " + line);
        String classLine = chewUpWhiteSpace(line).trim();
//System.out.println("CHEWED CLASS LINE: " + classLine);
//        classLine.trim(); // shouldn't be necessary if chewUpWhiteSpace works!
        int count = 0;
        TypeNamePair[] typesAndNames;
        int periodIndex = classLine.lastIndexOf('.');
        if(periodIndex != -1)
          classLine = classLine.substring(0, periodIndex);
        if(classLine.indexOf(',') != -1)
        {
          typesAndNames = new TypeNamePair[numberOfNonCommentLines];
          typesAndNames[numberOfNonCommentLines - 1] = new TypeNamePair(getSymbolicType(classLine), "Class");
        }
        else // the class has a name and is defined somewhere below...
        {
          typesAndNames = new TypeNamePair[numberOfNonCommentLines - 1];
        }
        while((line = namesIn.readLine()) != null)
        {
          if(!commentLine(line))
          {
            typesAndNames[count] = lineToTypeNamePair(line);
            count++;
          }
        }
        namesIn.close();
        Type[] typeArray = new Type[typesAndNames.length];
        String[] stringArray = new String[typesAndNames.length];
        boolean[] falseArray = new boolean[typesAndNames.length];
        for(count = 0; count < typeArray.length; count++)
        {
          typeArray[count] = typesAndNames[count].t;
          stringArray[count] = typesAndNames[count].name;
          falseArray[count] = false;
        }
        vectorType = new Type.Vector(new Type.Variable(), 
                                     new Type.Structured(typeArray, stringArray, falseArray), 
                                     false, false, false, false);
      }
      catch(IOException e)
      {
        System.out.println("ERROR - exception processing .names file.");
        e.printStackTrace();
        return defaultReturnValue;
      }
// Now read in the data...
      try
      {
        String dataFile = filename.substring(1,filename.length()-1); //THis used to be        stem + ".data";        but this doesn't allow loading of .test files.
System.out.println("dataFile: " + dataFile);
        BufferedReader dataIn = new BufferedReader(new FileReader(dataFile));
        int attCount, nAtts, pos1, pos2;
        nAtts = ((Type.Structured)vectorType.elt).cmpnts.length;
        String valS;
        Value[] val;
        Type tmpType;
        Value.Structured structuredValue;
        java.util.Vector valueVector = new java.util.Vector();
        line = "| <- that's the comment character.";
        while((line != null)&&((line = dataIn.readLine())!= null))
        {
          while((line != null)&&(commentLine(line)))
          {
//System.out.println("COMMENT LINE");
            line = dataIn.readLine();
          }
          if(line == null) break;
//System.out.println("DATA LINE");
          val = new Value[nAtts];
          line = chewUpWhiteSpace(line);
          pos1 = -1;
          for(attCount = 0; attCount < nAtts; attCount++)
          {
            pos2 = line.indexOf(',',pos1+1);
            if(pos2 != -1)
              valS = line.substring(pos1+1, pos2);
            else
              valS = line.substring(pos1+1);
            tmpType = ((Type.Structured)vectorType.elt).cmpnts[attCount];

            if(tmpType instanceof Type.Str)                                 // String
            {
              val[attCount] = string2str(valS);
            }
            else
            {
              if(tmpType instanceof Type.Continuous)                       // Continuous
              {
                val[attCount] = string2continuous(valS);
              }
              else
              {
                if(tmpType instanceof Type.Symbolic)                       // Symbolic
                {
                  val[attCount] = string2symbolic(valS, tmpType);
                }
                else                                                       // Discrete
                {
                  val[attCount] = string2discrete(valS);
                }
              }
            }
            pos1 = pos2;
          }
          structuredValue = new Value.DefStructured((Type.Structured)vectorType.elt, val);
//System.out.println("Adding value: " + structuredValue);
          valueVector.add(structuredValue);
        }
        dataIn.close();
//System.out.println("Array: " + valueVector.toArray());
        Value[] array = new Value[valueVector.size()];
        for(int count = 0; count < array.length; count++)
        {
          array[count] = (Value.Structured)valueVector.elementAt(count);
        }
        return new VectorFN.FatVector(array, vectorType);
      }
      catch(IOException e)
      {
        System.out.println("ERROR - exception processing .data file.");
        e.printStackTrace();
        return new VectorFN.EmptyVector(vectorType);
      }
    }
  }

  private static Value.Str string2str(String s)
  {
    return new Value.Str(string2valueStatus(s), s);
  }

  private static Value.Continuous string2continuous(String s)
  {
     ValueStatus vs = string2valueStatus(s);
     if(vs == Value.S_PROPER)
       return new Value.Continuous(Double.parseDouble(s));
     else
       return new Value.Continuous(vs, 0);
  }

  private static Value.Discrete string2discrete(String s)
  {
    ValueStatus vs = string2valueStatus(s);
    if(vs == Value.S_PROPER)
      return new Value.Discrete(Integer.parseInt(s));
    else
      return new Value.Discrete(vs, 0);
  }

  private static Value.Discrete string2symbolic(String s, Type t)
  {
    ValueStatus vs = string2valueStatus(s);
    if(vs == Value.S_PROPER)
      return new Value.Discrete((Type.Symbolic)t, ((Type.Symbolic)t).string2int(s));
    else
      return new Value.Discrete((Type.Symbolic)t, vs, 0);
  }

  private static ValueStatus string2valueStatus(String s)
  {
    if(s.equals("?"))
    {
      return Value.S_UNOBSERVED;
    }
    if(s.equals("N/A"))
    {
//System.out.println("Irrelevant value found!");
      return Value.S_IRRELEVANT;
    }
    return Value.S_PROPER;
  }

//************************************************************************************************** C5 Data Saver ********************

  /** This is a static instance of the {@link C5.C5Saver} class. */
  public static Value.Function c5testAndDataSaver = new C5testAndDataSaver();

  /** This is a wrapper function for the c5saver function.  Instead of the parameters
      <code>(Str file_path_and_stem_name, Vector data, Discrete classIndex)</code>, this 
      function uses
      <code>(Str file_path_and_stem_name, (Vector train_data, Vector test_data), Discrete classIndex)</code>. 
      This function creates the .names, .data and .test files, rather than just the .names and .data files.
  */
  public static class C5testAndDataSaver extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8161745765186931213L;

	public C5testAndDataSaver()
    {
      super(new Type.Function(new Type.Structured(new Type[]{Type.STRING, 
                                                             new Type.Structured(new Type[]{
                                                                                            new Type.Vector(new Type.Variable(), 
                                                                                            Type.STRUCTURED, 
                                                                                            false, false, false, false),
                                                                                            new Type.Vector(new Type.Variable(), 
                                                                                            Type.STRUCTURED, 
                                                                                            false, false, false, false)},
                                                                                 new String[]{"Training Data","Testing Data"},
                                                                                 new boolean[]{false, false}),
                                                             Type.DISCRETE}, 
                                                  new String[]{"File path and stem name", "(Training data, Testing data)", "Class Index"}, 
                                                  new boolean[]{false, false, false}), 
                              new Type.Structured(new Type[]{Type.STRING, Type.STRING},
                                                  new String[]{"Save training data result","Save testing data result"}, 
                                                  new boolean[]{false, false}),
                              false, false));
    } 

    public Value apply(Value v)
    {
      Value.Structured vs = (Value.Structured)v;
      Value dataResult = c5saver.apply(new Value.DefStructured(new Value[]{vs.cmpnt(0), 
                                                                           ((Value.Structured)vs.cmpnt(1)).cmpnt(0),  
                                                                           vs.cmpnt(2)}),
                                       ".data");
      Value testResult = c5saver.apply(new Value.DefStructured(new Value[]{vs.cmpnt(0), 
                                                                           ((Value.Structured)vs.cmpnt(1)).cmpnt(1),  
                                                                           vs.cmpnt(2)}),
                                       ".test");
      return new Value.DefStructured(new Value[]{dataResult, testResult});
    }
  }

  /** This is a static instance of the {@link C5.C5Saver} class. */
  public static C5Saver c5saver = new C5Saver();

  /** This function saves a vector of structured data in C5 format.  It takes 
      a structured (Str file_path_and_stem_name, Vector data, Discrete classIndex),
      and writes the data in 'data' to the files "file_path_and_stem_name.names" and 
      "file_path_and_stem_name.data".  'data' should be a vector of structured values, 
      and 'classIndex' indicates which column of data C5 should treat as the class 
      (starting at 0 and going to numberOfColumns-1).<BR>Here are a list of mappings
       from CDMS types to C5 types...<BR>
      <TABLE BORDER="1">
      <TR><TD COLSPAN="2">CDMS TYPE</TD><TD ROWSPAN="2">C5 TYPE</TD></TR>
      <TR><TD>BASE TYPE</TD><TD>TYPE ATTRIBUTES</TD></TR>
      <TR><TD>CONTINUOUS</TD><TD>-</TD><TD>continuous</TD></TR>
      <TR><TD>SYMBOLIC</TD><TD>ORDERED</TD><TD>nominal [ordered]</TD></TR>
      <TR><TD>SYMBOLIC</TD><TD>UNORDERED</TD><TD>nominal</TD></TR>
      <TR><TD>DISCRETE</TD><TD>ORDERED, BOUNDED</TD><TD>nominal [ordered]</TD></TR>
      <TR><TD>DISCRETE</TD><TD>UNORDERED, BOUNDED</TD><TD>nominal</TD></TR>
      <TR><TD>DISCRETE</TD><TD>ORDERED, UNBOUNDED</TD><TD>continuous</TD></TR>
     <TR><TD>DISCRETE</TD><TD>UNORDERED, UNBOUNDED</TD><TD>discrete 999999</TD></TR>
      <TR><TD>OTHER</TD><TD>-</TD><TD>ignore</TD></TR>
      </TABLE><BR>
     Perhaps in the future more complex CDMS values - the values containing
     other values - (e.g. Vector, Structured, Model) could be (recursively) "flattened out"
     to form one or more simple attributes (e.g. Discrete, Continuous, Symbolic).  For
     intance, a CDMS structured value of (discrete, discrete) could make two C5 discrete 
     attributes, rather than being ignored (as it is currently).  Or a structured value
     of (discrete, (discrete, discrete)) could be recursively flattened out to form three
     discrete C5 attributes.  The variable length of CDMS Vector values would cause
     problemos, as we could not guarantee every resultant C5 record would have the same
     number of attributes.  Also CDMS types such as the general Type.STRUCTURED do not  
     indicate how many components they may contain, and the same variable-length problem
     could occur.  In fact, it's easy to see why we currently ignore complex values...
  */
  public static class C5Saver extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -6150897058683584593L;

	public C5Saver()
    {
      super(new Type.Function(new Type.Structured(new Type[]{Type.STRING, 
                                                             new Type.Vector(new Type.Variable(), 
                                                                             Type.STRUCTURED, 
                                                                             false, false, false, false),
                                                             Type.DISCRETE}, 
                                                  new String[]{"File path and stem name", "Data", "Class Index"}, 
                                                  new boolean[]{false, false, false}), 
                              Type.STRING, false, false));
    }

    /** This method makes sure that labels itself is not null,
    and that none of its elements are null.  Any null element
    at index i is replaced with "Att_i".*/
    private static String[] fixLabels(String[] origLabels, int length)
    {
      int count;
      String[] labels = new String[length];      
      if(origLabels == null)
      {
        for(count = 0; count < labels.length; count++)
        {
          labels[count] = "Att_"+count;
        }
      }
      else
      {
        for(count = 0; count < labels.length; count++)
        {
          if(origLabels[count] == null)
            labels[count] = "Att_"+count;
          else
            labels[count] = origLabels[count];
        }
      }
      return labels;
    }

    public Value apply(Value v)
    {
      return apply(v, ".data");
    }

    public Value apply(Value v, String suffix)
    {
      String fileStem = ((Value.Structured)v).cmpnt(0).toString();
      fileStem = fileStem.substring(1,fileStem.length()-1);
      Value.Vector origData = (Value.Vector)((Value.Structured)v).cmpnt(1);
      String[] labels = ((Type.Structured)((Type.Vector)origData.t).elt).labels;
      Type[] types = ((Type.Structured)((Type.Vector)origData.t).elt).cmpnts;
      int count, count2;

      //------------------------------------------Make sure every attribute has a name.
      labels = fixLabels(labels, types.length);

      //-------------------------------------------Write comments to start of file.
      try
      {
        //--------------------------------------------------------------------------Now for the .data file.
        PrintWriter dataOut = new PrintWriter(new FileWriter(new File(fileStem + suffix)));

        //-------------------------------------------Write comments to start of file.
        dataOut.println("|");
        dataOut.println("|   This " + suffix + " file has been automatically generated by CDMS.");
        dataOut.println("|");
     
        //-------------------------------------------Write the data.
        Value.Structured tmpStructured;
        for(count = 0; count < origData.length(); count++)
        {
          tmpStructured = (Value.Structured)origData.elt(count);
          for(count2 = 0; count2 < tmpStructured.length() - 1; count2++)
          {
            dataOut.print(getValueAsString(tmpStructured.cmpnt(count2)) + ",");
          }
          dataOut.println(getValueAsString(tmpStructured.cmpnt(tmpStructured.length() - 1)));
        }
        dataOut.close();
        return new Value.Str("Data saved.");
      }
      catch(Exception e)
      {
        System.out.println("ERROR - problems encountered writing to files " + fileStem + ".names" + " and " + fileStem + suffix);
        e.printStackTrace();
        return new Value.Str("Error saving data.");
      }
    }

    private String getValueAsString(Value v)
    {
      if(v.status() == Value.S_UNOBSERVED)
      {
        return "?";
      }
      if(v.status() == Value.S_IRRELEVANT)
      {
        return "N/A";
      }
      if(v instanceof Value.Continuous)
      {
        return ((Value.Continuous)v).getContinuous() + "";
      }
      else
      {
        if(v instanceof Value.Discrete)
        {
          if(v.t instanceof Type.Symbolic)
          {
            return ((Type.Symbolic)v.t).int2string(((Value.Discrete)v).getDiscrete());
          }
          else
          {
            return (((Value.Discrete)v).getDiscrete() + "");
          }
        }
        else
        {
          return v.toString();
        }
      }
    }
  }
}
