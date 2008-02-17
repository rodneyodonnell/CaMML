//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567


// File: FN.java
// Authors: {leighf,joshc}@csse.monash.edu.au

package cdms.core;

import java.io.*;
import java.net.*;

/** FN contains part of the standard library of user Functions.
*/
public class FN extends Module.StaticFunctionModule
{
  public FN()
  {
    super("Standard",Module.createStandardURL(FN.class),FN.class);
  }

//  public static Value.Function corrupt = new Corrupt();

  /** <code> Continuous -> t -> t </code>  This function takes a
      continuous parameter - the probability of corrupting a data item.
      It returns a function that takes any Value as a parameter and
      either returns it unchanged, or returns a Value of
      the same type flagged as "UNOBSERVED".
      <code> corrupt 0.8 myVal </code> will have an 80% chance of
      returning an "UNOBSERVED" value, and a 20% chance of returning
      <code> myVal </code>. <br>
      The typical use of this function would be to artificially add
      noise to a data set - e.g. <code> map (corrupt 0.1) myVector </code>
      would (on average) replace 10% of the elements of myVector with
      unobserved values.
  */
/*  public static class Corrupt extends Value.Function
  {
    public Corrupt()
    {
      super(new Type.Function(Type.Continuous, Corrupt2.TT));
    }

    public Value apply(Value v)
    {
      return new Corrupt2(((Value.Continuous)v).getContinuous());
    }
  }


Need some polymorphic thing here...
Or a copy method in Value...
Or a big case statement...
Need to make a copy of v and replace its status with S_UNOBSERVED.
But v could be any Value class at all.

  public static class Corrupt2 extends Value.Function
  {
    private double p;
    public static Type.Variable tv = new Type.Variable();
    public static Type.Function TT = new Type.Function(tv, tv);
    private java.util.Random r;

    public Corrupt2(double prob)
    {
      super(TT);
      p = prob;
      r = new java.util.Random();
    }

    public Value apply(Value v)
    {
      if(r.nextDouble() > p) return v;
    }
  }
*/

  public static Value.Function inRange = new InRange();
  
  /** <code> Scalar -> Scalar </code>  This function assumes the scalar parameter
      is cyclic.  It returns the corresponding "in-range" value.  E.g.  Consider
      the discrete cyclic type with the range [-1,1].  Applying this function to a 
      value of this type will return a value within this range.  inRange 2 will 
      return -1.  inRange 3 will return 0.  inRange 4 will return 1.  inRange -1 will 
      return -1.  inRange -2 will return 1.
  */
  public static class InRange extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 6613228792007623204L;

	public InRange()
    {
      super(new Type.Function(Type.SCALAR, Type.SCALAR, false, false));
    }
    
    public Value apply(Value v)
    {
      return ((Value.Scalar)v).getInRange();
    }
  }
  
  public static Value.Function weight = new Weight();
  
  /** <code> [t] -> [Continuous] </code>  This function takes a vector of values,
      and returns the weights as a vector of continuous values.
  */
  public static class Weight extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 8950130791156818908L;
	
	public static Type.Function TT = new Type.Function(new Type.Vector(new Type.Variable()), new Type.Vector(Type.CONTINUOUS), false, false);
  
    public Weight()
    {
      super(TT);
    }
    
    public Value apply(Value v)
    {
      return new WeightsVector((Value.Vector)v);
    }
    
    private static class WeightsVector extends Value.Vector
    {
      /** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -966221453443841251L;
	private Value.Vector v;
    
      public WeightsVector(Value.Vector v)
      {
        super(new Type.Vector(Type.CONTINUOUS));
        this.v = v;
      }
      
      public int length()
      {
        return v.length();
      }
      
      public Value elt(int i)
      {
        return new Value.Continuous(v.weight(i));
      }
      
      public double doubleAt(int i)
      {
        return v.weight(i);
      }
    }
  }

  public static Value.Function struct = new StructFunction();

  /** <code>t -> (t)</code>  This function simply wraps the value up into a structured.
      e.g. struct 4 will return (4) where (4) is a (single-component) structured value.
  */
  public static class StructFunction extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -3770359721286196955L;
	
	private static final Type.Variable tv = new Type.Variable();
    private static final Type.Structured resultType = new Type.Structured(new Type[]{tv});
    private static final Type.Function TT = new Type.Function(tv, resultType);

    public StructFunction()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      // Returns a structured of the correct type.
      return new Value.DefStructured(new Type.Structured(new Type[]{v.t}), new Value[]{v});
    }
  }


  /** <code>(a, Str, Str) -> Boolean</code> Adds a value (cmpnt[0]) to the environment with 
      name (cmpnt[1]) and comment (cmpnt[2]).  Returns true on success or false on failure.
  */
  public static Value.Function addToEnvironment = new AddToEnvironment();

  /** <code>(a, Str, Str) -> Boolean</code> Adds a value (cmpnt[0]) to the environment with 
      name (cmpnt[1]) and comment (cmpnt[2]).  Returns true on success or false on failure.
  */
  public static class AddToEnvironment extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -3737554306065167145L;

	public AddToEnvironment()
    {
      super(new Type.Function(new Type.Structured(new Type[]{new Type.Variable(), 
                                                             Type.STRING, 
                                                             Type.STRING}, 
                                                 new String[]{"Value","Name","Comment"}, 
                                                 new boolean[]{false, false, false}), 
                              Type.BOOLEAN, false, false));
    }

    public Value apply(Value v)
    {
      try
      {
        if(Environment.env.getObject(((Value.Str)((Value.Structured)v).cmpnt(1)).getString(),
                                     "Standard") != null)
        {
          return Value.FALSE; // Name already exists in the standard module of the environment.  
                              // Function fails.
        }
        // Add the value to the standard module of the environment.
        // Return succss code for the function.
        Environment.env.add(((Value.Str)((Value.Structured)v).cmpnt(1)).getString(), 
                            "Standard", 
                            ((Value.Structured)v).cmpnt(0), 
                            ((Value.Str)((Value.Structured)v).cmpnt(2)).getString());  
        return Value.TRUE; 
      }
      catch(Exception ex)
      {
        return Value.FALSE;  // Catch any exceptions, and return the fail code for the function.
      }
    }
  }

  /** <code>(a, Str, Str, Str) -> Boolean</code> Adds a value (cmpnt[0]) to the environment with 
      name (cmpnt[1]) and comment (cmpnt[2]), in module (cmpnt[3]).  Returns true on success or 
      false on failure.
  */
  public static Value.Function addToModule = new AddToModule();

  /** <code>(a, Str, Str) -> Boolean</code> Adds a value (cmpnt[0]) to the environment with 
      name (cmpnt[1]) and comment (cmpnt[2]).  Returns true on success or false on failure.
  */
  public static class AddToModule extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7845494468470273728L;

	public AddToModule()
    {
      super(new Type.Function(new Type.Structured(new Type[]{new Type.Variable(), 
                                                             Type.STRING, 
                                                             Type.STRING,
                                                             Type.STRING}, 
                                                 new String[]{"Value","Name","Comment","Module"}, 
                                                 new boolean[]{false, false, false, false}), 
                              Type.BOOLEAN, false, false));
    }

    public Value apply(Value v)
    {
      try
      {
        if(Environment.env.getObject(((Value.Str)((Value.Structured)v).cmpnt(1)).getString(),
                                     ((Value.Str)((Value.Structured)v).cmpnt(3)).getString()) != null)
        {
          return Value.FALSE; // Name already exists in the standard module of the environment.  
                              // Function fails.
        }
        // Add the value to the standard module of the environment.
        // Return succss code for the function.
        Environment.env.add(((Value.Str)((Value.Structured)v).cmpnt(1)).getString(), 
                            ((Value.Str)((Value.Structured)v).cmpnt(3)).getString(), 
                            ((Value.Structured)v).cmpnt(0), 
                            ((Value.Str)((Value.Structured)v).cmpnt(2)).getString());  
        return Value.TRUE; 
      }
      catch(Exception ex)
      {
        return Value.FALSE;  // Catch any exceptions, and return the fail code for the function.
      }
    }
  }




  /** <code>Str -> Scalar -> Str</code>Formats a scalar value according to a specified format.
  */
  public static Value.Function format = new Format();

  /** <code>Str -> Scalar -> Str</code> 
      <p>
      Formats a scalar value according to a specified format.
      The format pattern is given as the first parameter.  See the Java documentation on 
      Java.text.DecimalFormat for information on patterns.
  */
  public static class Format extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -6084946142343449610L;
	public static final Type.Function TT = 
      new Type.Function(Type.STRING, Format1.TT);

    public Format()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      return new Format1(((Value.Str)v).getString());
    }
  }

  /** <code>Scalar -> Str</code> 
      <p>
      @see FN.Format
  */
  public static class Format1 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8125047148754413922L;

	public static Type.Function TT = new Type.Function(Type.SCALAR,Type.STRING);

    private final java.text.DecimalFormat valFormat;

    public Format1(String pattern)
    {
      super(TT);
      valFormat = new java.text.DecimalFormat();
    }

    public Value apply(Value v)
    {
      return new Value.Str(valFormat.format(((Value.Scalar)v).getContinuous()));
    }
  }



  /** <code>(a -> Scalar) -> [a] -> a</code> Returns the minimising element of a vector. */
  public static Value.Function argmin = new Argmin();

  /** <code>(a -> Scalar) -> [a] -> a</code> 
      <p>
      Returns the minimising element of a vector.  
      For example <code>argmin (lambda x . snd x) [(1,5),(2,1),(3,2)] evaluates to (2,1)</code>.
  */
  public static class Argmin extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7780030786809579395L;
	public static final Type.Variable A = new Type.Variable();
    public static final Type.Function TT = 
      new Type.Function(new Type.Function(A,Type.SCALAR), Argmin1.TT);

    public Argmin()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      return new Argmin1((Value.Function)v);
    }
  }

  /** <code>[a] -> a</code> 
      <p>
      @see FN.Argmin
  */
  public static class Argmin1 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 9020540855770079931L;
	public static Type.Function TT = new Type.Function(new Type.Vector(Argmin.A), Argmin.A);
    private Value.Function f;

    public Argmin1(Value.Function f)
    {
      super(TT);
      this.f = f;
    }

    public Value apply(Value v)
    {
      Value.Vector vv = (Value.Vector) v;
      double min = Double.MAX_VALUE;
      Value minv = Value.TRIV; 
 
      for (int i = 0; i < vv.length(); i++)
      {
        double x = f.applyDouble(vv.elt(i));
        if (x < min)
        {
          min = x;
          minv = vv.elt(i);
        }
      }
      return minv;
    }
  }

  /** <code>(a -> Scalar) -> [a] -> a</code> Returns the maximising element of a vector. */
  public static Value.Function argmax = new Argmax();

  /** <code>(a -> Scalar) -> [a] -> a</code> 
      <p>
      Returns the maximising element of a vector.  
      For example <code>argmax (lambda x . snd x) [(1,5),(2,1),(3,2)] evaluates to (1,5)</code>.
  */
  public static class Argmax extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5036413301230473496L;
	public static final Type.Variable A = new Type.Variable();
    public static final Type.Function TT = 
      new Type.Function(new Type.Function(A,Type.SCALAR), Argmax1.TT);

    public Argmax()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      return new Argmax1((Value.Function)v);
    }
  }

  /** <code>[a] -> a</code> 
      <p>
      @see FN.Argmax
  */
  public static class Argmax1 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 1204741000819920960L;
	public static Type.Function TT = new Type.Function(new Type.Vector(Argmax.A), Argmax.A);
    private Value.Function f;

    public Argmax1(Value.Function f)
    {
      super(TT);
      this.f = f;
    }

    public Value apply(Value v)
    {
      Value.Vector vv = (Value.Vector) v;
      double max = - Double.MAX_VALUE;
      Value maxv = Value.TRIV; 
 
      for (int i = 0; i < vv.length(); i++)
      {
        double x = f.applyDouble(vv.elt(i));
        if (x > max)
        {
          max = x;
          maxv = vv.elt(i);
        }
      }
      return maxv;
    }
  }

  /** <code>String -> String </code> Loads a text file into a string.  If an error occurs
      then an empty string is returned. */
  public static final LoadText loadText = new LoadText();

  /** <code>String -> String</code>
      <p>
      Loads a text file into a string.  If an error occurs then an empty string is
      returned.
  */
  public static class LoadText extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5360343095291273789L;
	private static final String lineSep = System.getProperty("line.separator");
    public static final Type.Function TT = new Type.Function(Type.STRING,Type.STRING);

    public LoadText()
    {
      super(TT);
    }

    private static String readFile(String filename) throws IOException
    {
      BufferedReader br = new BufferedReader(new FileReader(filename));
      String nextLine = "";
      StringBuffer sb = new StringBuffer();
      while ((nextLine = br.readLine()) != null)
      {
        sb.append(nextLine);
        sb.append(lineSep);
      }
      return sb.toString();
    }

    public Value apply(Value v)
    {
      try
      {
        String s = readFile(((Value.Str) v).getString());
        return new Value.Str(s);
      }
      catch (Exception e)
      {
        return new Value.Str("");
      }
    }
  }

  
  
  /** <code>String -> Boolean </code> Executes the string command in a separate process. */
  public static final Exec exec = new Exec();

  /** <code>String -> Boolean</code>
      <p>
      Executes the string command in a separate process.  Non-blocking, returns true if 
      the process was started correctly.
  */
  public static class Exec extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5907920794332473451L;
	
	public static final Type.Function TT = new Type.Function(Type.STRING,Type.BOOLEAN);

    public Exec()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      try
      {
        Runtime.getRuntime().exec(((Value.Str) v).getString());
        return Value.TRUE;
      }
      catch (Exception e)
      {
        return Value.FALSE;
      }
    }
  }


  /** <code>String -> t -> Boolean </code> Writes a vector of structured values to a CSV file. */
  public static final SaveCSV saveCSV = new SaveCSV();

  /** <code>String -> t -> Boolean</code>
      <p>
      Writes a vector of structured values to a comma separated value (CSV) file. 
      If the structure has component labels then these are included on the first
      line.  If a vector of non-structured values is given then each element
      is written to file.  
  */
  public static class SaveCSV extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7719939440031381220L;
	public static final Type.Function TT = new Type.Function(Type.STRING,SaveCSV2.TT);

    public SaveCSV()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      return new SaveCSV2(((Value.Str) v).getString());
    }
  }

  /** <code>t -> Boolean</code>
      <p>
      @see FN.SaveCSV
  */
  public static class SaveCSV2 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5968845539099375729L;

	public static final Type.Function TT = new Type.Function(Type.VECTOR,Type.BOOLEAN);

    private String fname;

    public SaveCSV2(String fname)
    {
      super(TT);
      this.fname = fname;
    }

    public Value apply(Value v)
    {
      Value.Vector vv = (Value.Vector) v;

      try
      {
        PrintWriter pr = new PrintWriter(new BufferedWriter(new FileWriter(fname)));
 
        if ( ((Type.Vector) vv.t).elt instanceof Type.Structured)
        {
          // Write header.
          Type.Structured eltType = (Type.Structured) ((Type.Vector) vv.t).elt;
          if (eltType.labels != null)
          {
            for (int j = 0; j < eltType.labels.length; j++)
            {
              if (j != 0) pr.print(",");
              if (eltType.labels[j] != null) pr.print(eltType.labels[j]);
            }
            pr.println();
          }

          for (int i = 0; i < vv.length(); i++)
          {
            Value.Structured sv = (Value.Structured) vv.elt(i);
            for (int j = 0; j < sv.length(); j++)
            {
              if (j != 0) pr.print(",");
              pr.print(sv.cmpnt(j).toString());
            }
            pr.println();
          }
        }
        else
        {
          for (int i = 0; i < vv.length(); i++)
            pr.println(vv.elt(i).toString());
        }
        pr.close();
        return Value.TRUE;
      }
      catch (Exception e)
      {
        return Value.FALSE;
      }
    }
  }


  /** <code>Server -> From -> To -> Subject -> t -> Boolean, where Server, From, 
      To, Subject :: String</code> Send an email message. 
  */
  public static final SendMail sendMail = new SendMail();

  /** <code>Server -> From -> To -> Subject -> t -> Boolean, where Server, From, 
      To, Subject :: String</code>
      <p>
      Send an email message.  The value <code>t</code> is converted to a string and 
      sent to the recipient.
  */
  public static class SendMail extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5432339838739206821L;

	public SendMail()
    {
      super(new Type.Function(Type.STRING, SendMail2.TT));
    }

    public Value apply(Value v)
    {
      return new SendMail2(((Value.Str)v).getString());
    }
  }

  /** <code>From -> To -> Subject -> t -> Boolean, where From, To, Subject :: String</code>
      <p>
      @see FN.SendMail
  */
  public static class SendMail2 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 9156791712704701161L;

	public static final Type.Function TT = new Type.Function(Type.STRING, SendMail3.TT);

    private String mailServer;

    public SendMail2(String mailServer)
    {
      super(TT);
      this.mailServer = mailServer;
    }

    public Value apply(Value v)
    {
      return new SendMail3(mailServer, ((Value.Str)v).getString());
    }
  }

  /** <code>To -> Subject -> t -> Boolean, where To, Subject :: String</code>
      <p>
      @see FN.SendMail
  */
  public static class SendMail3 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 969634140400370767L;

	public static final Type.Function TT = new Type.Function(Type.STRING, SendMail4.TT);

    private String mailServer;
    private String fromAddress;

    public SendMail3(String mailServer, String fromAddress)
    {
      super(TT);
      this.mailServer = mailServer;
      this.fromAddress = fromAddress;
    }

    public Value apply(Value v)
    {
      return new SendMail4(mailServer, fromAddress, ((Value.Str)v).getString());
    }
  }

  /** <code>Subject -> t -> Boolean, where Subject :: String</code>
      <p>
      @see FN.SendMail
  */
  public static class SendMail4 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -4753577074310397451L;

	public static final Type.Function TT = new Type.Function(Type.STRING, SendMail5.TT);

    private String mailServer;
    private String fromAddress;
    private String toAddress;

    public SendMail4(String mailServer, String fromAddress, String toAddress)
    {
      super(TT);
      this.mailServer = mailServer;
      this.fromAddress = fromAddress;
      this.toAddress = toAddress;
    }

    public Value apply(Value v)
    {
      return new SendMail5(mailServer, fromAddress, toAddress, ((Value.Str)v).getString());
    }
  }

  /** <code>t -> Boolean</code>
      <p>
      @see FN.SendMail
  */
  public static class SendMail5 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 542504048961411942L;

	public static final Type.Function TT = new Type.Function(new Type.Variable(), Type.BOOLEAN);

    private String mailServer;
    private String fromAddress;
    private String toAddress;
    private String subjectLine;

    public SendMail5(String mailServer, String fromAddress, String toAddress, String subjectLine)
    {
      super(new Type.Function(new Type.Variable(), Type.STRING));
      this.mailServer = mailServer;
      this.fromAddress = fromAddress;
      this.toAddress = toAddress;
      this.subjectLine = subjectLine;
    }

    public Value apply(Value v)
    {
      String body = v.toString();

      try
      {
        Socket s = new Socket(mailServer, 25);
        BufferedReader in = 
          new BufferedReader(new InputStreamReader(s.getInputStream(), "8859_1"));
        BufferedWriter out = 
          new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "8859_1"));

        // here you are supposed to send your username
        sendln(in, out, "Hello from CDMS.");
        // warning : some mail server validate the sender address
        //           in the MAIL FROM command, put your real address here
        sendln(in, out, "MAIL FROM: <"+ fromAddress + ">");
        sendln(in, out, "RCPT TO: <" + toAddress + ">");
        sendln(in, out, "DATA");
        sendln(out, "MIME-Version: 1.0");
        sendln(out, "Subject: " + subjectLine);
     
        // Send the body
        sendln(out, "Content-Type: text/plain; charset=\"us-ascii\"\r\n");
        sendln(out, body);     
        sendln(in, out,".");
        sendln(in, out, "QUIT");
        s.close();
        return Value.TRUE;
      }
      catch (Exception e)
      {
        return Value.FALSE;
      }
    }
   
    private static void sendln(BufferedReader in, BufferedWriter out, String s) throws IOException
    {
      out.write(s + "\r\n");
      out.flush();
      s = in.readLine();
    }

    private static void sendln(BufferedWriter out, String s) throws IOException
    {
      out.write(s + "\r\n");
      out.flush();
    }
  }


  /** <code>[Scalar] -> Continuous</code> Computes the arithmetic average of a vector of scalars. */
  public static final Average average = new Average();

  /** <code>[Scalar] -> Continuous</code>
      <p>
      Computes the arithmetic average of a vector of scalars. 
  */
  public static class Average extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -1875028199286539993L;
	
	public static final Type.Function TT = 
      new Type.Function(new Type.Vector(Type.SCALAR),Type.CONTINUOUS);

    public Average()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      return new Value.Continuous(applyDouble(v));
    }

    public double applyDouble(Value v)
    {
      Value.Vector vv = (Value.Vector) v;
      Value s = cdms.plugin.model.Normal.normal.getSufficient(vv,null);
      Value.Structured ml = 
        (Value.Structured) cdms.plugin.ml.Normal.normalEstimator.apply(s); 
      return ml.doubleCmpnt(0); 
    }
  }


  /** <code>[String] -> String</code>The sconcat function concatenates a vector of strings to
      produce a single string.
  */
  public static Value.Function sconcat = new Sconcat();

  /** <code>[String] -> String</code><P>The sconcat function concatenates a vector of strings to
      produce a single string.
  */
  public static class Sconcat extends Value.Function 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -3056775173919129018L;
	public static final Type.Function thisType = new Type.Function(
      new Type.Vector(new Type.Variable(), Type.STRING, false, false, false, false), Type.STRING);

    public Sconcat()
    {
      super(thisType);
    }

    public Value apply(Value v)
    {
      Value.Vector vv = (Value.Vector)v;
      String result = "";
      for(int count = 0; count < vv.length(); count++)
      {
	  Value elt = vv.elt(count);
	  if ( elt instanceof Value.Str ) {
	      result = result + ((Value.Str)elt).getString();
	  }
	  else {
	      result = result + elt.toString();
	  }
      }
      return new Value.Str(result);
    }
  }

  /** <code>[(...)] -> (...)</code>The merge function joins a vector of structured values
      together to form a single structured value.  <code>merge [(a,b),(c),(d,e,f)]</code> 
      will return <code>(a,b,c,d,e,f)</code>. <br> 
      Related functions include {@link FN.sconcat} (for Strings) and {@link VectorFN.concat}
      (for Vectors).
  */
  public static Value.Function merge = new MergeStructured();

  /** <code>[(...)] -> (...)</code>The MergeStructured function joins a vector of 
      structured values together to form a single structured value.  
      <code>merge [(a,b),(c),(d,e,f)]</code> will return <code>(a,b,c,d,e,f)</code>.  
      FN.merge is the static instance of this class, and is the only instance that 
      should ever (need to) be created. <br>
      Related functions include {@link FN.sconcat} (for Strings) and {@link VectorFN.concat}
      (for Vectors).
  */
  public static class MergeStructured extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 3992115870959392198L;
	private static Type.Function thisType = new Type.Function(new Type.Vector(Type.STRUCTURED),Type.STRUCTURED);
 
    public MergeStructured()
    {
      super(thisType);
    }

    public Value apply(Value v)
    {
      Value.Vector vv = (Value.Vector)v;
      int count, count2, count3;
      for(count = 0, count2 = 0; count < vv.length(); count++)
      {
        count2 += ((Value.Structured)vv.elt(count)).length();
      }
      Value[] components = new Value[count2];
      Type[] types = new Type[count2];
      String[] names = new String[count2];
      boolean[] care = new boolean[count2];
      Value.Structured tmp;
      Type.Structured tmpType;
      for(count = 0, count3 = 0; count < vv.length(); count++)
      {
        tmp = (Value.Structured)vv.elt(count);
        tmpType = (Type.Structured)tmp.t;
        for(count2 = 0; count2 < tmp.length(); count2++, count3++)
        {
          components[count3] = tmp.cmpnt(count2);
          types[count3] = tmpType.cmpnts[count2];
          if(tmpType.labels != null)
          {
            names[count3] = tmpType.labels[count2];
          }
          care[count3] = tmpType.checkCmpntsNames[count2];
        }     
      }
      return new Value.DefStructured(new Type.Structured(types, names, care), components);
    }
  }

  /** <code>[t] -> Discrete -> t</code> Get the element of a vector.  */
  public static Value.Function elt = new Elt();

  /** <code>[t] -> Discrete -> t</code> 
      <p>
      Get the element of a vector.  For example <code>elt 3 [0,1,2,3,4,5,6] evaluates to 3</code>.
  */
  public static class Elt extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 8605353133143563207L;
	public static final Type.Variable eltType = new Type.Variable();
    public static final Type.Vector thisParamType = new Type.Vector(eltType);
    public static final Type.Function thisType = new Type.Function(thisParamType, Elt1.thisType);

    public Elt()
    {
      super(thisType);
    }

    public Value apply(Value v)
    {
      return new Elt1((Value.Vector)v);
    }
  }

  /** <code>Discrete -> t</code> 
      <p>
      @see FN.Elt
  */
  public static class Elt1 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -3533481626218761142L;
	public static Type.Function thisType = new Type.Function(Type.DISCRETE, Elt.eltType);
    private Value.Vector vv;

    public Elt1(Value.Vector vv)
    {
      super(thisType);
      this.vv = vv;
    }

    public Value apply(Value v)
    {
      return vv.elt(((Value.Scalar)v).getDiscrete());
    }
  }


  /** <code>[t]|(...) -> Discrete</code> Length of a vector or structure. */
  public static Value.Function length = new Length();

  /** <code>[t]|(...) -> Discrete</code> 
      <p>
      Length of a vector or structure. 
  */
  public static class Length extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 3251813399651094387L;
	public static final Type.Function thisType = 
      new Type.Function(new Type.Union(new Type[] { Type.VECTOR, Type.STRUCTURED }), Type.DISCRETE);

    public Length()
    {
      super(thisType);
    }

    public Value apply(Value v)
    {
      if (v instanceof Value.Structured) return new Value.Discrete(((Value.Structured)v).length());
        else return new Value.Discrete(((Value.Vector)v).length());
    }
  }


  /** <code>Scalar -> Scalar -> Scalar</code>Min */
  public static Value.Function min = new Min();

  /** <code>Scalar -> Scalar -> Scalar</code> 
      <p>
      Min 
  */
  public static class Min extends Value.Function 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 905370317283418989L;
	public static final Type.Function TT = new Type.Function(Type.SCALAR,Min2.TT);

    public Min()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      return new Min2((Value.Scalar)v);
    }
  }

  /** <code>Scalar -> Scalar</code> 
      <p>
      Min 
  */
  public static class Min2 extends Value.Function 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 2274009136305458063L;

	public static final Type.Function TT = new Type.Function(Type.SCALAR,Type.SCALAR);

    private Value.Scalar v1;

    public Min2(Value.Scalar v1)
    {
      super(TT);
      this.v1 = v1;
    }

    public Value apply(Value v)
    {
      if (v instanceof Value.Continuous || v1 instanceof Value.Continuous)
        return new Value.Continuous(Math.min(((Value.Scalar)v).getContinuous(),v1.getContinuous()));
      else return new Value.Discrete((int)
                        Math.min(((Value.Scalar)v).getContinuous(),v1.getContinuous()));
    }
  }


  /** <code>Scalar -> Scalar -> Scalar</code>Min */
  public static Value.Function max = new Max();

  /** <code>Scalar -> Scalar -> Scalar</code> 
      <p>
      Max 
  */
  public static class Max extends Value.Function 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -879207966521037898L;
	
	public static final Type.Function TT = new Type.Function(Type.SCALAR,Max2.TT);

    public Max()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      return new Max2((Value.Scalar)v);
    }
  }

  /** <code>Scalar -> Scalar</code> 
      <p>
      Max 
  */
  public static class Max2 extends Value.Function 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -1404849080194028963L;

	public static final Type.Function TT = new Type.Function(Type.SCALAR,Type.SCALAR);

    private Value.Scalar v1;

    public Max2(Value.Scalar v1)
    {
      super(TT);
      this.v1 = v1;
    }

    public Value apply(Value v)
    {
      if (v instanceof Value.Continuous || v1 instanceof Value.Continuous)
        return new Value.Continuous(Math.max(((Value.Scalar)v).getContinuous(),v1.getContinuous()));
      else return new Value.Discrete((int)
                        Math.max(((Value.Scalar)v).getContinuous(),v1.getContinuous()));
    }
  }


  /** <code>Continuous -> Continuous</code> Sin */
  public static Value.Function sin = new Sin();

  /** <code>Continuous -> Continuous</code> 
      <p>
      Sin 
  */
  public static class Sin extends Value.Function 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -1079868134018946112L;
	
	public static final Type.Function thisType = 
      new Type.Function(Type.CONTINUOUS,Type.CONTINUOUS);

    public Sin()
    {
      super(thisType);
    }

    public double applyDouble(Value v)
    {
      return Math.sin(((Value.Scalar)v).getContinuous());
    }

    public Value apply(Value v)
    {
      return new Value.Continuous(applyDouble(v));
    }
  }


  /** <code>Continuous -> Continuous</code> Cos */
  public static Value.Function cos = new Cos();

  /** <code>Continuous -> Continuous</code> 
      <p>
      Cos 
  */
  public static class Cos extends Value.Function 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 3148965117263918057L;
	public static final Type.Function thisType = 
      new Type.Function(Type.CONTINUOUS,Type.CONTINUOUS);

    public Cos()
    {
      super(thisType);
    }

    public double applyDouble(Value v)
    {
      return Math.cos(((Value.Scalar)v).getContinuous());
    }

    public Value apply(Value v)
    {
      return new Value.Continuous(applyDouble(v));
    }
  }

  /** <code>Continuous -> Discrete</code> Floor */
  public static Value.Function floor = new Floor();

  /** <code>Continuous -> Discrete</code> 
      <p>
      Floor 
  */
  public static class Floor extends Value.Function 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5336630706632906813L;
	public static final Type.Function thisType = 
      new Type.Function(Type.CONTINUOUS,Type.DISCRETE);

    public Floor()
    {
      super(thisType);
    }

    public int applyInt(Value v)
    {
      return (int) Math.floor(((Value.Scalar)v).getContinuous());
    }

    public Value apply(Value v)
    {
      return new Value.Discrete(applyInt(v));
    }
  }


  /** <code>Continuous -> Continuous</code> Abs */
  public static Value.Function abs = new Abs();

  /** <code>Continuous -> Continuous</code> 
      <p>
      Abs 
  */
  public static class Abs extends Value.Function 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -17026800083548181L;
	public static final Type.Function thisType = 
      new Type.Function(Type.CONTINUOUS,Type.CONTINUOUS);

    public Abs()
    {
      super(thisType);
    }

    public double applyDouble(Value v)
    {
      return Math.abs(((Value.Scalar)v).getContinuous());
    }

    public Value apply(Value v)
    {
      return new Value.Continuous(applyDouble(v));
    }
  }

  /** <code>Continuous -> Continuous</code> Exp */
  public static Value.Function exp = new Exp();

  /** <code>Continuous -> Continuous</code> 
      <p>
      Exp 
  */
  public static class Exp extends Value.Function 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 7920039879620233466L;
	public static final Type.Function thisType = 
      new Type.Function(Type.CONTINUOUS,Type.CONTINUOUS);

    public Exp()
    {
      super(thisType);
    }

    public double applyDouble(Value v)
    {
      return Math.exp(((Value.Scalar)v).getContinuous());
    }

    public Value apply(Value v)
    {
      return new Value.Continuous(applyDouble(v));
    }
  }

  /** <code>Continuous -> Continuous</code> Log */
  public static Value.Function log = new Log();

  /** <code>Continuous -> Continuous</code> 
      <p>
      Log 
  */
  public static class Log extends Value.Function 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 7048397664383504495L;
	public static final Type.Function thisType = 
      new Type.Function(Type.CONTINUOUS,Type.CONTINUOUS);

    public Log()
    {
      super(thisType);
    }

    public double applyDouble(Value v)
    {
      return Math.log(((Value.Scalar)v).getContinuous());
    }

    public Value apply(Value v)
    {
      return new Value.Continuous(applyDouble(v));
    }
  }

  /** <code>Continuous -> Continuous</code> Sqrt */
  public static Value.Function sqrt = new Sqrt();

  /** <code>Continuous -> Continuous</code> 
      <p>
      Sqrt 
  */
  public static class Sqrt extends Value.Function 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -3657006466277323190L;
	public static final Type.Function thisType = 
      new Type.Function(Type.CONTINUOUS,Type.CONTINUOUS);

    public Sqrt()
    {
      super(thisType);
    }

    public double applyDouble(Value v)
    {
      return Math.sqrt(((Value.Scalar)v).getContinuous());
    }

    public Value apply(Value v)
    {
      return new Value.Continuous(applyDouble(v));
    }
  }

  /** <code>Scalar -> Scalar -> Scalar</code> Pow */
  public static Value.Function pow = new Pow();

  /** <code>Scalar -> Scalar -> Scalar</code> 
      <p>
      Power function.  <code>pow y 5 is equivalent to y*y*y*y*y</code> 
  */
  public static class Pow extends Value.Function 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 1468164583623239825L;
	
	public static final Type.Function thisType =   
      new Type.Function(Type.CONTINUOUS,Pow1.thisType);

    public Pow()
    {
      super(thisType);
    }

    public Value apply(Value v)
    {
      return new Pow1(((Value.Scalar) v).getContinuous());
    }
  }

  /** <code>Scalar -> Scalar</code> 
      <p>
      @see FN.Pow
  */
  public static class Pow1 extends Value.Function 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5487854637651257628L;

	public static final Type.Function thisType = 
      new Type.Function(Type.CONTINUOUS,Type.CONTINUOUS);

    private double base;

    public Pow1(double base)
    {
      super(thisType);
      this.base = base;
    }

    public double applyDouble(Value v)
    {
      return Math.pow(base,((Value.Scalar)v).getContinuous());
    }

    public Value apply(Value v)
    {
      return new Value.Continuous(applyDouble(v));
    }
  }


  /** <code>Discrete -> (a,b,c,...) -> t</code> Get a component from a tuple. */
  public static Value.Function cmpnt = new Cmpnt();

  /** <code>(a,b,c,...) -> t</code> Get the first component of a tuple. */
  public static Value.Function fst = (Value.Function) cmpnt.apply(new Value.Discrete(0));

  /** <code>(a,b,c,...) -> t</code> Get the second component of a tuple. */
  public static Value.Function snd = (Value.Function) cmpnt.apply(new Value.Discrete(1));

  /** A curried function to get the nth element of a tuple. */
  /** <code>Discrete -> (a,b,c,...) -> t</code> 
      <p>
      Get a component from a tuple.  For example <code>cmpnt 2 (0,"hello",1.34,[1,2,3]) 
      evaluates to 1.34</code>. 
  */
  public static class Cmpnt extends Value.Function 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -4992308677489708972L;
	public static Type.Function curriedCmpntType = new Type.Function(Type.STRUCTURED,Type.TYPE);  
    public static final Type.Function TT = new Type.Function(Type.DISCRETE,Cmpnt2.TT);

    public Cmpnt()
    {
      super(TT);
    }

    public Value apply(Value v)
    {
      return new Cmpnt2(((Value.Discrete) v).getDiscrete());
    }
  }

  /** <code>(a,b,c,...) -> t</code> 
      <p>
      @see FN.Cmpnt
  */
  public static class Cmpnt2 extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -4022187408176608092L;

	public static final Type.Function TT = new Type.Function(Type.STRUCTURED,Type.TYPE);  

    private int n;

    public Cmpnt2(int n)
    {
      super(TT);
      this.n = n;
    }

    public Value apply(Value v)
    {
      return ((Value.Structured) v).cmpnt(n);
    }
  }


  /** <code>t -> String</code> Returns the string representation of a value. */
  public static Value.Function toString = new ToString();

  /** <code>t -> String</code> 
      <p>
      Returns the string representation of a value. 
  */
  public static class ToString extends Value.Function 
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8049871394615574455L;

	public ToString()
    {
      super(new Type.Function(Type.TYPE,Type.STRING));
    }

    public Value apply(Value v)
    {
      return new Value.Str(v.toString());
    }
  }

  /** <code>String -> t</code> CreateValue (cv) creates a value instance 
      from a Java class. */
  public static CV cv = new CV();

  /** <code>String -> t</code> 
      <p>
      CreateValue (cv) creates a value instance from a Java class. 
  */
  public static class CV extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -2079259386742341192L;
	public static final Type.Function TT = new Type.Function(Type.STRING,Type.TYPE);

    public CV()
    {
      super(TT);
    }

    public Value apply(Value p)
    {
      try
      {
        return (Value) Class.forName( ((Value.Str)p).getString() ).newInstance();
      }
      catch (Exception e)
      {
        System.out.println("Unable to create value: " + ((Value.Str)p).getString());
        return Value.TRIV;
      }
    }
  }


  /** <code>String -> Obj</code> CreateObject (co) creates a value object instance 
      from a Java class. */
  public static CO co = new CO();

  /** <code>String -> Obj</code> 
      <p>
      CreateObject (co) creates a value object instance from a Java class. 
  */
  public static class CO extends Value.Function
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 3718831722512474259L;
	public static final Type.Function TT = new Type.Function(Type.STRING,Type.OBJECT);

    public CO()
    {
      super(TT);
    }

    public Value apply(Value p)
    {
      try
      {
        return new Value.Obj(new Type.Obj( ((Value.Str)p).getString()));
      }
      catch (Exception e)
      {
        System.out.println("Unable to create object: " + ((Value.Str)p).getString());
        return Value.TRIV;
      }
    }
  }

  /** <code>Discrete -> Continuous</code> Log factorial. */
  public static LogFactorial logFactorial = new LogFactorial();
  
  /** <code>Discrete -> Continuous</code>
   * <p>
   * Log factorial. 
   */
  public static class LogFactorial extends Value.Function
  {	
	  /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	  private static final long serialVersionUID = -5622789634295591089L;
	  
	  /** Fast lookup table for n less than 2000 */
	  public static final int lookupSize = 2000;
	  
	  /** Fast lookup table for n less than 2000 */
	  protected static double[] lookup;
	  
	  /** Use stirlings approximation when n > lookupSize. default = true. */
	  static boolean useStirling = true;
	  
	  public static final Type.Function TT = new Type.Function(Type.DISCRETE, Type.CONTINUOUS);
	  
	  public LogFactorial()
	  {
		  super(TT);
	  }
	  
	  
	  public double applyDouble(Value v)
	  {
		  return logFactorial(((Value.Discrete)v).getDiscrete());
	  }
	  
	  public Value apply(Value v)
	  {
		  return new Value.Continuous(applyDouble(v));
	  }	
	  
	  
	  public static double logFactorial(int n)
	  {
		  if (lookup == null) {
			  lookup = new double[lookupSize];
			  lookup[0] = 0;
			  lookup[1] = 0;
			  for (int i = 2; i < lookup.length; i++) {
				  lookup[i] = lookup[i-1] + Math.log(i);
			  } 	      
		  }	  
		  
		  if (n < lookup.length ) {
			  return lookup[n];
		  }
		  
		  // If not in lookup table, use stirlings approximation.
		  // at n=2000, the approximation error is ~= 4.16E-5
		  if (useStirling) {
			  return n * Math.log(n) - n + .5*Math.log(2.0*Math.PI*n) + 1.0/(12.0*n);
		  }
		  
		  // If n > lookup.length we can still use partially calculated results.
		  
		  // Highest lookup table value < m
		  int bestN = lookup.length-1;
		  // partialResult = lookup[bestN]
		  double partialResult = lookup[bestN];
		  
		  double logResult = partialResult;
		  for (int count = bestN+1; count <= n; count++) {
			  logResult += Math.log(count);	  
		  }
		  
		  if ( n < lookup.length )
			  lookup[n] = logResult;
		  
		  return logResult;
	  }
	  
	  
  }

  /** <code>t -> t</code> Identity function (polymorphic). */
  public static Value.Function identity = new Identity();

  /** <code>t -> t</code> 
      <p>
      Identity function (polymorphic).
  */
  public static class Identity extends Value.Function
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -5845257376050571284L;
	public static final Type.Variable T = new Type.Variable();
    public static final Type.Function TT = new Type.Function(T,T);

    public Identity()
    { 
      super(TT);
    }

    public Value apply(Value param) 
    { 
      return param; 
    }
  }

}

// End of file.
