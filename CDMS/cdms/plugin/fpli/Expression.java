//
// CDMS
//
// Copyright (C) 1997-2001 Leigh Fitzgibbon, Josh Comley and Lloyd Allison.  All Rights Reserved.
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

package cdms.plugin.fpli;

//import java.lang.reflect.*;
import cdms.core.*;

// class Expression defines the abstract-syntax
// (i.e. parse tree) of expressions in the language.


public abstract class Expression implements java.io.Serializable
{
  public abstract Value eval(Environment r); // EVALuate an Expression.

  public abstract void  appendSB(StringBuffer sb); // printing - efficiency!


  /** Checks if a predefined entity and looks up identity in environment. */
  public static class Ident extends Expression                        // Ident
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 9181089070439580027L;

	public class BeanFN extends Value.Function
    {
      /** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = -2449942561053083788L;
	Value.Obj o;
      String m;

      public BeanFN(Value.Obj o, String m)
      {
        super(new Type.Function(Type.TYPE,Type.TYPE,false,false));
        this.o = o;
        this.m = m; 
      }

      public Value apply(Value p)
      {
        return o.invoke(m,p);
      }

    } 

    public final String id;

    public Ident(String id) { this.id = id; }

    public Value eval(Environment r) 
    { 
      if (id.equals("cv")) 
      {
        return FN.cv;
      }
      else if (id.equals("co")) 
      {
        return FN.co;
      }
      else
      {
        int idx = id.indexOf(".");
        if (id.indexOf(".") == -1)
        {
          Value val = (Value) r.getObject(id);
          if (val == null) throw new RuntimeException(id + " does not exist in environment.");
          return val;
        }
        else
        {                                   // Method call or Module.variable
          try
          {
            Object o = r.getObject(id);
            if (o != null && o instanceof Value) return (Value) o;

            Value.Obj val = (Value.Obj) r.getObject(id.substring(0,idx));
            if (val == null) throw new RuntimeException(id + " does not exist in environment.");
            return new BeanFN(val,id.substring(idx+1));
          }
          catch (Exception e)
          {
            throw new RuntimeException(id + " " + e.toString());
          }
        }
      }
    }

    public void appendSB(StringBuffer sb) { sb.append(id); }
  }

  public static class IntCon extends Expression                      // Discrete
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 550889747898749231L;
	public final int n;
    public IntCon(int n) { this.n = n; }
    public Value eval(Environment r) { return new Value.Discrete(n); }
    public void appendSB(StringBuffer sb) { sb.append(String.valueOf(n)); }
  }

  public static class DoubleCon extends Expression                 // Continuous
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 7198937194524261272L;
	public final double x;
    public DoubleCon(double x) { this.x = x; }
    public Value eval(Environment r) { return new Value.Continuous(x); }
    public void appendSB(StringBuffer sb) { sb.append(String.valueOf(x)); }
  }

  public static final Expression ttrue = new Expression()             // TRUE
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8643343068177192312L;
	public Value eval(Environment r) { return Value.TRUE; }
    public void appendSB(StringBuffer sb)
    { 
      sb.append( Lexical.Symbol[Lexical.trueSy] ); 
    }
  };

  public static final Expression ffalse = new Expression()           // FALSE
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 5942383552099855343L;
	public Value eval(Environment r) { return Value.FALSE; }
    public void appendSB(StringBuffer sb)
    { 
      sb.append( Lexical.Symbol[Lexical.falseSy] ); 
    }
  };

  public static class Str extends Expression                        // String 
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 1241710823802951964L;
	public final String s;
    public Str(String s) { this.s = s; }
    public Value eval(Environment r) { return new Value.Str(s); }
    public void appendSB(StringBuffer sb)
    { 
      sb.append( "\"" + s + "\"" ); 
    }
  }

  public static final Expression       triv = new Expression()         // TRIV 
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -936908334556822261L;
	public Value eval(Environment r) { return Value.TRIV; }
    public void appendSB(StringBuffer sb)
    {  
      sb.append( Lexical.Symbol[Lexical.triv] ); 
    }
  };


  public static final Expression nilCon = new Expression()           // nilCon
  {                                                                  // []
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 4559334010047365962L;
	public Value eval(Environment r) { return Value.TRIV; }
    public void appendSB(StringBuffer sb)
    { 
      sb.append( Lexical.Symbol[Lexical.nilSy] ); 
    }
  };


  public static class LambdaExp extends Expression                // LambdaExp
  { 

    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -3328241393636711308L;

	public class LambdaFn extends Value.Function
    {
      /** Serial ID required to evolve class while maintaining serialisation compatibility. */
		private static final long serialVersionUID = 3397076600090198417L;
	LambdaExp code;
      Environment r;

      public LambdaFn(LambdaExp code, Environment r)
      {
        super(Type.FUNCTION);
        this.code = code;
        this.r = r; 
      }

      public Value apply(Value aparam)                              // apply
      { 
        if( code.fparam == Expression.triv ) // type check!
        {
          if( aparam == Value.TRIV ) 
            return code.body.eval(r);
          else 
          { 
            throw new RuntimeException("Error: (L ().e) exp"); 
          }
        }
        else // ((L x.e) exp
        { 
          Expression.Ident fparam = (Expression.Ident) code.fparam;
          Environment newenv = new Environment(r);  
          newenv.add(fparam.id,aparam);
          return code.body.eval( newenv );
        }
      }

    }

    public final Expression fparam, body; // lambda x.e  or  lambda ().e

    public LambdaExp(Expression fparam, Expression body)
    { 
      this.fparam = fparam;  
      this.body = body; 
    }

    public Value eval(Environment r)
    { 
      return new LambdaFn(this, r); 
    }

    public void appendSB(StringBuffer sb)
    { 
      sb.append(Lexical.Symbol[Lexical.lambdaSy] + " ");
      fparam.appendSB(sb);  
      sb.append( ".");
      body.appendSB(sb);
    }
  }

  public static class Application extends Expression            // Application
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -4356855789870747723L;
	public final Expression fun, aparam;

    public Application(Expression fun, Expression aparam)
    { 
      this.fun = fun; 
      this.aparam = aparam; 
    }

    public Value eval(Environment r)
    { 
      Value evald = fun.eval(r);
      try
      {
        if (evald instanceof Value.Function) 
          return ((Value.Function) evald).apply( aparam.eval(r) );
      }
      catch (RuntimeException e)
      {
        StringBuffer sb = new StringBuffer();
        fun.appendSB(sb);
        sb.append(" to ");
        aparam.appendSB(sb);
 
	// e.printStackTrace();
	//        throw new RuntimeException("Attempted application of: " + sb + " is invalid. " + e.getMessage());
	throw e;
      }
      StringBuffer sb = new StringBuffer();
      fun.appendSB(sb);
      throw new RuntimeException("Attempted application of: " + sb + " is invalid.");
    }

    public void appendSB(StringBuffer sb)
    { 
      sb.append("("); 
      fun.appendSB(sb);  
      sb.append( " " );
      aparam.appendSB(sb); 
      sb.append(")");
    }
  }//Application e.g. f(x)


  public static class Unary extends Expression                        // Unary
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8946409202416861942L;
	public final int opr;  
    public final Expression e;

    public Unary(int opr, Expression e)  
    { 
      this.e = e; 
      this.opr = opr; 
    }

    // NB. all the unary operators are strict
    public Value eval(Environment r) 
    { 
      return Expression.U( opr, e.eval(r) ); 
    }

    public void appendSB(StringBuffer sb)
    { 
      sb.append( "(" + Lexical.Symbol[opr] + " " );
      e.appendSB(sb);  
      sb.append( ")" );
    }
  }//Unary


  public static class Binary extends Expression                      // Binary
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 1340296243333692642L;
	public final int opr; 
    public final Expression lft, rgt;

    public Binary(int opr, Expression lft, Expression rgt)
    { 
      this.opr = opr; 
      this.lft = lft; 
      this.rgt = rgt; 
    }

    public Value eval(Environment r)
    { 
      if( opr == Lexical.consSy ) 
        return new Cell(r, lft, rgt); // cons
      else
      {
        if( opr == Lexical.andSy || opr == Lexical.orSy ) 
          return Expression.O(opr, lft.eval(r), rgt.eval(r));
        else // others strict
          return Expression.O(opr, lft.eval(r), rgt.eval(r));
      } 
    }

    public void appendSB(StringBuffer sb)
    { 
      sb.append( "(" );
      lft.appendSB(sb);  
      sb.append( " " + Lexical.Symbol[opr] + " " );
      rgt.appendSB(sb);  
      sb.append( ")" );
    }
  }


  public static class Tuple extends Expression                        // Tuple
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 1699247810657738798L;
	java.util.Vector es;

    public Tuple(java.util.Vector es)
    {
      this.es = es;
    }

    public Value eval(Environment r)
    { 
      String n[] = new String[es.size()];
      Type t[] = new Type[es.size()];
      Value cmpnts[] = new Value[es.size()];

      for (int i = 0; i < cmpnts.length; i++)
      {
        // Set the cmpnt labels by checking if the cmpnt is a 
        // binary assignment, e.g.  ("name"="Leigh Fitzgibbon", "dob"="2/8/75").
        // This needs to be modified, it currently has to evaluate the left expression.
        Expression expi = (Expression) es.elementAt(i);
        if (expi instanceof Binary && ((Binary) expi).opr == Lexical.eq)
        {
          Value lftv = ((Binary) expi).lft.eval(r);
          if (lftv instanceof Value.Str) 
          {
            n[i] = ((Value.Str) lftv).getString();
            Value v = ((Binary) expi).rgt.eval(r);
            cmpnts[i] = v;
            t[i] = v.t;
          }
        }
        else 
        {
          n[i] = null;
          Value v = expi.eval(r);
          cmpnts[i] = v;
          t[i] = v.t;
        }
      }

      return new Value.DefStructured(new Type.Structured(t,n),cmpnts);
    }

    public void appendSB(StringBuffer sb)
    { 
      sb.append( "(" );
      for (int i = 0; i < es.size(); i++)
      {
        if (i != 0) sb.append(",");
        ((Expression) es.elementAt(i)).appendSB(sb);  
      }
      sb.append( ")" );
    }
  }

  public static class Vector extends Expression                        // Vector
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 7136485635452850392L;
	java.util.Vector v;

    public Vector(java.util.Vector v)
    {
      this.v = v;
    }

    public Value eval(Environment r)
    { 
      if(v.size() > 0)
      {
        Value values[] = new Value[v.size()];
        for (int i = 0; i < values.length; i++)
          values[i] = ((Expression) v.elementAt(i)).eval(r);
        return new cdms.core.VectorFN.FatVector(values);
      }
      return new cdms.core.VectorFN.EmptyVector();
    }

    public void appendSB(StringBuffer sb)
    { 
      sb.append( "[" );
      for (int i = 0; i < v.size(); i++)
      {
        if (i != 0) sb.append(",");
        ((Expression) v.elementAt(i)).appendSB(sb);  
      }
      sb.append( "]" );
    }
  }


  public static class IfExp extends Expression                        // IfExp
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = 2195911140210175206L;
	public final Expression e1, e2, e3;

    public IfExp(Expression e1, Expression e2, Expression e3)
    { 
      this.e1 = e1; 
      this.e2 = e2; 
      this.e3 = e3; 
    }

    public Value eval(Environment r)
    { 
      return (e1.eval(r) == Value.TRUE ? e2 : e3).eval(r); 
    }

    public void appendSB(StringBuffer sb)
    { 
      sb.append( Lexical.Symbol[Lexical.ifSy] + " " );
      e1.appendSB(sb);
      sb.append( " " + Lexical.Symbol[Lexical.thenSy] + " " );
      e2.appendSB(sb);
      sb.append( " " + Lexical.Symbol[Lexical.elseSy] + " " );
      e3.appendSB(sb);
    }
  }//IfExp


  public static class Block extends Expression                        // Block
  { 
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -6460229720814367782L;
	public final Declaration d; 
    public final Expression e;

    public Block(Declaration d, Expression e) 
    {
      this.d = d; 
      this.e = e; 
    }

    public Value eval(Environment r)
    { 
      // Create the environment.
      boolean needNew = false;        // 1st block uses global environment.
      Environment be = r;

      if (r.parent == null)
        needNew = true;
      else be = new Environment(r);

      Declaration dec = d;
      while (dec != null)
      {
        be.add(dec.id,dec.e.eval(be));
        dec = dec.next;
      }

      if (needNew) be = new Environment(r);

      return e.eval( be );  // Evaluate e in new environment.
    }

    public void appendSB(StringBuffer sb)
    { 
      sb.append(Lexical.Symbol[Lexical.letSy]+ " ");
      d.appendSB(sb);
      sb.append("\n" + Lexical.Symbol[Lexical.inSy] + " ");
      e.appendSB(sb);
    }
  }//Block  e.g. let x1=e1, x2=e2, ... in e2


  // U : Value -> Value   i.e. Unary Operators
  protected static Value U(int opr, Value v)                                // U
  { 
    switch( opr )
    { 
      case Lexical.minus:                                               // -
           if (v instanceof Value.Discrete) 
              return new Value.Discrete( - ((Value.Discrete)v).getDiscrete() );
           else return new Value.Continuous( - ((Value.Continuous)v).getContinuous() );
      case Lexical.notSy:                                             // not
           if( v == Value.TRUE ) return Value.FALSE;
           if( v == Value.FALSE ) return Value.TRUE;
         /* else */ error("'not' applied to a non boolean Value");
      case Lexical.hdSy:                                               // hd
           return ((Value.Structured)v).cmpnt(0);
      case Lexical.tlSy:                                               // tl
           return ((Value.Structured)v).cmpnt(1); 
      case Lexical.nullSy:                                           // null
           return v == Value.TRIV ? Value.TRUE : Value.FALSE;
      default: error("no " + opr + " case in U()");
    }

    return null; // keep javac happy!

  }

  public static class Cell extends Value.Structured    // a cons cell
  {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -2327061566988147294L;
	public static final Type.Structured tp = 
      new Type.Structured(new Type[] { Type.TYPE, Type.TYPE }, new boolean[] { false, false} );
    Expression lft, rgt;
    Environment r;

    public Cell(Environment r, Expression lft, Expression rgt)
    {
      super(tp);
      this.lft = lft;
      this.rgt = rgt;
      this.r = r;
    }

    public int length() 
    { 
      return 2;
    }

    public Value cmpnt(int i)  
    { 
      if (i == 0) return lft.eval(r);
        else return rgt.eval(r);
    }
 
    public String toString()
    {
      return "(" + cmpnt(0).toString() + " , " + cmpnt(1).toString() + ")";
    }
  } 


  // O : Value x Value -> Value   i.e. Binary Operators
  protected static Value O(int opr, Value lft, Value rgt)                   // O
  { 
    switch( opr )
    { 
      case Lexical.plus: case Lexical.minus:
      case Lexical.times: case Lexical.over:                         // +-*/
        double i1 = ((Value.Scalar)lft).getContinuous();
        double i2 = ((Value.Scalar)rgt).getContinuous();
        if (lft.t instanceof Type.Continuous || 
            rgt.t instanceof Type.Continuous) 
        {
          switch( opr )
          { 
            case Lexical.plus:  return new Value.Continuous(i1+i2);
            case Lexical.minus: return new Value.Continuous(i1-i2);
            case Lexical.times: return new Value.Continuous(i1*i2);
            case Lexical.over:  return new Value.Continuous(i1/i2);
          }
        }
        else
        {
          switch( opr )
          { 
            case Lexical.plus:  return new Value.Discrete((int)(i1+i2));
            case Lexical.minus: return new Value.Discrete((int)(i1-i2));
            case Lexical.times: return new Value.Discrete((int)(i1*i2));
            case Lexical.over:  return new Value.Discrete((int)(i1/i2));
          }
        }

      case Lexical.eq: case Lexical.ne: case Lexical.lt:
      case Lexical.le: case Lexical.gt: case Lexical.ge:       // comparison
        if( lft.getClass() != rgt.getClass() )
          error("comparison of different types: " + lft.getClass().getName() + " != " + rgt.getClass().getName());
        /* else */
        double abs1 = 0, abs2 = 0;
        if (lft == Value.TRUE || lft == Value.FALSE )     // bool:bool
        { 
          abs1 = lft == Value.TRUE ? 0 : 1;
          abs2 = rgt == Value.TRUE ? 0 : 1;
        }
        else if( lft instanceof Value.Discrete )               //  int:int
        { 
          abs1 = ((Value.Discrete)lft).getDiscrete(); 
          abs2 = ((Value.Discrete)rgt).getDiscrete(); 
        }
        else if( lft instanceof Value.Continuous )             //  double:double
        { 
          abs1 = ((Value.Continuous)lft).getContinuous(); 
          abs2 = ((Value.Continuous)rgt).getContinuous(); 
        }
        else if( lft instanceof Value.Str )                  // char:char
        { 
          abs1 = 0;
          abs2 = ((Value.Str)lft).getString().compareTo( ((Value.Str)rgt).getString() );
        }
        else error("comparison undefined on operands");

        boolean ans = false;
        switch( opr )
        { 
          case Lexical.eq: ans = abs1 == abs2; break;
          case Lexical.ne: ans = abs1 != abs2; break;
          case Lexical.gt: ans = abs1 >  abs2; break;
          case Lexical.ge: ans = abs1 >= abs2; break;
          case Lexical.lt: ans = abs1 <  abs2; break;
          case Lexical.le: ans = abs1 <= abs2; break;
        }

        return ans ? Value.TRUE : Value.FALSE;

      case Lexical.andSy: case Lexical.orSy:                       // and or
        boolean b1 = lft == Value.TRUE;
        switch( opr ) // short-cut evaluation...
        { 
          case Lexical.andSy:
	    return b1 ? rgt : Value.FALSE;
          case Lexical.orSy:
	    return b1 ? Value.TRUE : rgt;
        }

      default: error("no " + opr + " case in O()");
    }

    return null; // javac

  }


  // the use of a StringBuffer gives linear rather than quadratic complexity.
  public String toString()
  { 
    StringBuffer sb = new StringBuffer(); // efficiency!
    this.appendSB(sb);
    return sb.toString();
  }


  public static void error(String msg)
  { 
    throw new RuntimeException("\nError: " + msg);
  }

}


