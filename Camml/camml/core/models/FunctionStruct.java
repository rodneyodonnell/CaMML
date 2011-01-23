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
// Wrapper functions to convert a ModelLearner to CDMS Funcitons
//

// File: FunctionStruct.java
// Author: rodo@csse.monash.edu.au

package camml.core.models;

import cdms.core.*;
import camml.core.models.ModelLearner;

/**
 * FunctionsStruct contains curried instances of each ModelLearner function. <br>
 * (parameterizer, sParameterizer, coster, sCoster, msyCoster)
 * 
 * This class defines several subclasses to ge used to allow models to be easily combined through a 
 * standard structure.  The following Value.Functions are defined. <br>
 *
 * paramerizer = (m->) i -> [x] -> [z] -> (m,s,y)       <br>
 * sParameterizer = (m->) s -> (m,s,y)                  <br>
 * coster = (m->) i -> [x] -> [z] -> y -> continuous    <br>
 * sCoster = (m->) s -> y -> conitnuous                 <br>
 * msyCoster = (m,s,y) -> coninuous                     <br>
 * <br>
 *
 * where : <br>
 * m = model, <br>
 * i = initial information, <br>
 * [x] = dependent data, <br>
 * [z] = independent data, <br>
 * y = model parameters <br>
 * s = sufficient statistics (compact summary of x,z,i) <br>
 * <br>
 */
public class FunctionStruct extends Value.Structured
{        
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = -4056229370285605010L;

    /** Return relevant component: (parameterizer, sParameterizer, coster, sCoster, msyCoster)*/
    public Value cmpnt(int i) {
        switch (i) {
        case 0 : return doParameterize;
        case 1 : return doSParameterize;
        case 2 : return doCost;
        case 3 : return doSCost;
        case 4 : return doMSYCost;
        }
        throw new RuntimeException("Requested cmpnt does not exist");
    }
    
    /** Return number of cmpnts. */
    public int length() { return 5; }
    
    /** The Learner all curried functions are based on. */
    protected final ModelLearner learner;
    
    /** Accesor for learner. */
    public ModelLearner getLearner() { return learner; }
    
    /** instance of Parameterizer class.  i -> [x] -> [z] -> (m,s,y) */
    public final Parameterize parameterizer;
    /** instance of SParameterizer class. m -> s -> (m,s,y) */
    public final SParameterize sParameterizer;
    /** instance of Coster class. i -> [x] -> [z] -> y -> continuous  */
    public final Cost coster;
    /** instance of SCoster class. s -> y -> continuous  */
    public final SCost sCoster;
    /** instance of MSYCoster class. (m,s,y) -> continuous  */
    public final MSYCost msyCoster;
    
    /** Value.Function called in Parameterize */
    public Value.Function doParameterize;
    /** Value.Function called in SParameterize */
    public Value.Function doSParameterize; 
    /** Value.Function called in Cost */
    public Value.Function doCost;
    /** Value.Function called in SCost */
    public Value.Function doSCost;        
    /** Value.Function called in MSYCost */
    public Value.Function doMSYCost;
    
    /** Constructor creates parameterizer, sParameterizer, coster, sCoster and msyCoster. */
    public FunctionStruct( ModelLearner learner )
    {
        super( new Type.Structured( new Type[]   {Type.FUNCTION,Type.FUNCTION,Type.FUNCTION,
                                                  Type.FUNCTION,Type.FUNCTION }, 
                new String[] {"parameterize", "sParameterize", 
                              "cost", "sCost", "msyCost"} ) );
        this.learner = learner;
        
        // Create Paramaterization and Costing objects.
        // NOTE: doParameterize, doCost, etc are initialised within these object constructors.
        parameterizer = new Parameterize(  );
        sParameterizer = new SParameterize(  );
        coster = new Cost( );
        sCoster = new SCost( );
        msyCoster = new MSYCost( );
    }
    
    /** Install functions into the cdms environment. */
    public void install( String name ) throws Exception
    {
        if ( name == null ) { name = learner.getName(); } 
        Environment env = Environment.env;
        env.add( "parameterize", name, doParameterize, "Parameterize model i->x->z->(m,s,y)" );
        env.add( "sParameterize", name, doSParameterize, "Parameterize model s -> (m,s,y)" );
        env.add( "cost", name, doCost, "Cost model i->x->z->y->continuous" );
        env.add( "sCost", name, doSCost, "Cost model s->y->continuous" );
        env.add( "msyCost", name, doMSYCost, "Cost model (m,s,y)->continuous" );
        env.add( "learnerStruct", name, this, "ModelLearner function struct" );
    }
    
    
    
    /**********************************************************************************
     *                                                                                *
     *                            CURRIED FUNCTIONS                                   *
     *                                                                                *
     **********************************************************************************/
    
    /** 
     * This class contains the curried version of parameterize.  The function :
     * ModelLearner.parameterize(i,x,y) is abstract and must be written for any parameterize 
     * function. <br>
     * coster = (m->) i -> [x] -> [z] -> continuous    <br>
     */
    protected class Parameterize implements java.io.Serializable
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 6198276894191279671L;
        /** Types of parameters used as curried functions */
        private Type.Function param1Type;
        private Type.Function param2Type;
        private Type.Function param3Type;
        
        /** Constructor saves types. */
        public Parameterize( )
        {
            Type.Model mType = learner.getModelType();
            Type iType = learner.getIType();
            // Create all the types.  This only has to be done once.
            param3Type = 
                new Type.Function( new Type.Vector(mType.sharedSpace),
                                   new Type.Structured( new Type[] 
                                       {mType, mType.sufficientSpace, mType.paramSpace}) );
            param2Type = new Type.Function( new Type.Vector(mType.dataSpace), param3Type );
            param1Type = new Type.Function( iType, param2Type );
            
            doParameterize = new Parameterize1();
        }
        
        /** First curried function.  i -> [x] -> [z] -> (m,s,y) */
        public class Parameterize1 extends Value.Function
        {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = -2536789083422935257L;

            Parameterize1()    { 
                super(param1Type); 
            }
            
            public Value apply( Value v ) { 
                return new Parameterize2(v); 
            }
        }
        
        /** Second curried function.  [x] -> [z] -> (m,s,y) */
        public class Parameterize2 extends Value.Function
        {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = -3137033702176731358L;
            
            protected Value i;
            
            Parameterize2( Value i ) {        
                super(param2Type);
                this.i = i;
            }
            
            public Value apply( Value v ) {
                return new Parameterize3(i, (Value.Vector)v);
            }
        }
        
        /** 
         * Third curried function.  [z] -> (m,s,y) <br>
         * parameterize(i,x,z) is called here. 
         */
        public class Parameterize3 extends Value.Function
        {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = -6444972039260689733L;
            
            protected Value i;
            protected Value.Vector x;
            
            Parameterize3( Value i, Value.Vector x) {
                super(param3Type);
                this.i = i;
                this.x = x;
            }
            
            public Value apply( Value v ) {
                try {
                    return learner.parameterize( i, x, (Value.Vector)v );
                } catch ( ModelLearner.LearnerException e) { throw new RuntimeException(e); }
            }
        }
    }
    
    
    /** 
     * This class contains the curried version of parameterize.  The function :
     * ModelLearner.sParameterize(s) is abstract and must be written for any parameterize 
     * function. <br>
     * coster = m-> s -> continuous    <br>
     */
    protected class SParameterize implements java.io.Serializable
    {    
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -8824989456486394578L;
        
        private Type.Function param1Type;
        private Type.Function param2Type;
        
        /** Constructor saves types. */
        public SParameterize(  )
        {
            Type.Model mType = learner.getModelType();
            //Type iType = learner.getIType();
            
            param2Type = 
                new Type.Function( mType.sufficientSpace,
                                   new Type.Structured( new Type[] 
                                       {mType, mType.sufficientSpace, mType.paramSpace}) );
            
            param1Type = new Type.Function( mType, param2Type );
            
            doSParameterize = new SParameterize1();
        }
        
        /**
         * Only one parameter so extra currying is not required.  s -> continuous <br>
         * sParameterize(s) is called here.
         */
        public class SParameterize1 extends Value.Function
        {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = 8214668298636080197L;

            SParameterize1()
            {
                super(param1Type);
            }
            
            public Value apply( Value v )
            {
                return new SParameterize2((Value.Model) v);
            }
        }
        
        public class SParameterize2 extends Value.Function
        {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = 2990857305039589335L;
            
            Value.Model m;
            SParameterize2( Value.Model m )
            {
                super(param2Type);
                this.m = m;
            }
            
            public Value apply( Value v )
            {
                try {
                    return learner.sParameterize( m, v );
                } catch ( ModelLearner.LearnerException e) { throw new RuntimeException(e); }
            }
            
            
        }
    }
    
    
    /** 
     * This class contains the curried version of cost.  The function :
     * ModelLearner.cost(i,x,z,y) is abstract and must be written for any parameterize function.
     * coster = m-> i -> [x] -> [z] -> y -> continuous    <br>
     */
    public class Cost implements java.io.Serializable
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 5822374562701588998L;
        
        /** Types of parameters used as curried functions */
        private final Type.Function param1Type;
        private final Type.Function param2Type;
        private final Type.Function param3Type;
        private final Type.Function param4Type;
        private final Type.Function param5Type;
        
        /** Constructor saves types. */
        public Cost( )
        {
            Type.Model mType = learner.getModelType();
            Type iType = learner.getIType();
            
            // Creata all the types.  This only has to be done once.
            param5Type = new Type.Function( mType.paramSpace, Type.CONTINUOUS );
            param4Type = new Type.Function( new Type.Vector(mType.sharedSpace), param5Type );
            param3Type = new Type.Function( new Type.Vector(mType.dataSpace), param4Type );
            param2Type = new Type.Function( iType, param3Type );
            param1Type = new Type.Function( mType, param2Type );
            
            doCost = new Cost1();
        }
        
        /** First curried function.  m -> i -> [x] -> [z] -> y -> cost */
        public class Cost1 extends Value.Function
        {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = -1548082958758664010L;

            Cost1()
            {
                super(param1Type);
            }
            
            public Value apply( Value v )
            {
                return new Cost2((Value.Model)v);
            }
        }
        
        /** First curried function.  i -> [x] -> [z] -> y -> cost */
        public class Cost2 extends Value.Function
        {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = 7357897011261095797L;
            
            protected Value.Model m;
            
            Cost2( Value.Model m)
            {
                super(param2Type);
                this.m = m;
            }
            
            public Value apply( Value v )
            {
                return new Cost3(m,v);
            }
        }
        
        /** Second curried function.  [x] -> [z] -> y -> cost */
        public class Cost3 extends Value.Function
        {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = -3632914304443394019L;
            
            protected Value.Model m;
            protected Value i;
            
            Cost3( Value.Model m, Value i )
            {
                super(param3Type);
                this.m = m;
                this.i = i;
            }
            
            public Value apply( Value v )
            {
                return new Cost4(m, i, (Value.Vector)v);
            }
        }
        
        /** Third curried function.  [z] -> y -> cost */
        public class Cost4 extends Value.Function
        {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = 4095788302835228491L;
            
            protected Value.Model m;
            protected Value i;
            protected Value.Vector x;
            
            Cost4( Value.Model m, Value i, Value.Vector x )
            {
                super(param4Type);
                this.m = m;
                this.i = i;
                this.x = x;
            }
            
            public Value apply( Value v )
            {
                return new Cost5(m, i, x, (Value.Vector)v);
            }
        }
        
        /** 
         * Fourth curried function.  y -> cost <br>
         * cost(m,i,x,z,y) is called here/
         */
        public class Cost5 extends Value.Function
        {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = -137501819509991450L;
            
            protected Value.Model m;
            protected Value i;
            protected Value.Vector x;
            protected Value.Vector z;
            
            Cost5( Value.Model m, Value i, Value.Vector x, Value.Vector z)
            {
                super(param5Type);
                this.m = m;
                this.i = i;
                this.x = x;
                this.z = z;
            }
            
            public Value apply( Value v )
            {
                return new Value.Continuous( applyDouble(v) );
            }
            
            public double applyDouble( Value v )
            {
                try {
                    return learner.cost( m, i, x, z, v );
                } catch ( ModelLearner.LearnerException e) { throw new RuntimeException(e); }
            }
        }
    }
    
    /**
     * This class contains the curried version of sCost.  The function :
     * ModelLearner.sCost(s,y) is abstract and must be written for any parameterize function.
     * coster = (m->) s -> y -> continuous    <br>
     */
    public class SCost implements java.io.Serializable
    {    
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = -555515501423989582L;
        
        /** Types of parameters used as curried functions */
        private final Type.Function param1Type;
        private final Type.Function param2Type;
        private final Type.Function param3Type;
        
        /** Constructor saves types. */
        public SCost( )
        {
            Type.Model mType = learner.getModelType();
            //Type iType = learner.getIType();
            
            // Create all the types.  This only has to be done once.
            param3Type = new Type.Function( mType.paramSpace, Type.CONTINUOUS );
            param2Type = new Type.Function( mType.sufficientSpace, param3Type );
            param1Type = new Type.Function( mType, param2Type );
            
            doSCost = new SCost1();
        }
        
        /** First curried function.  m -> s -> y -> cost */
        public class SCost1 extends Value.Function
        {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = -5062367398235233513L;

            SCost1()
            {
                super(param1Type);
            }
            
            public Value apply( Value v )
            {
                return new SCost2((Value.Model)v);
            }
        }
        
        /** First curried function.  s -> y -> cost */
        public class SCost2 extends Value.Function
        {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = 4860342178462953744L;
            
            protected Value.Model m;
            SCost2( Value.Model m )
            {
                super(param2Type);
                this.m = m;
            }
            
            public Value apply( Value v )
            {
                return new SCost3(m, v);
            }
        }
        
        /** 
         * Second curried function.  y -> cost <br>
         * cost(s,y) is called here.
         */
        public class SCost3 extends Value.Function
        {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = -8318899827427159362L;
            
            protected Value.Model m;
            protected Value stats;
            
            SCost3( Value.Model m, Value stats )
            {
                super(param3Type);
                this.m = m;
                this.stats = stats;
            }
            
            public Value apply( Value v )
            {
                return new Value.Continuous( applyDouble(v) );
            }
            
            public double applyDouble( Value v )
            {
                try {
                    return learner.sCost( m, stats, v );
                } catch ( ModelLearner.LearnerException e) { throw new RuntimeException(e); }
            }
        }
    }
    
    /** 
     * This class contains the curried version of msyCost.  The function :
     * ModelLearner.msyCost(msy) is abstract and must be written for any parameterize function.
     * coster = (m,s,y)-> continuous    <br>
     */
    public class MSYCost implements java.io.Serializable
    {
        /** Serial ID required to evolve class while maintaining serialisation compatibility. */
        private static final long serialVersionUID = 4340127853901478247L;
        private Type.Function paramType;
        
        /** Constructor saves types. */
        public MSYCost( )
        {
            Type.Model mType = learner.getModelType();
            //Type iType = learner.getIType();
            
            paramType = 
                new Type.Function( new Type.Structured(new Type[] {mType,
                                                                   mType.sufficientSpace,
                                                                   mType.paramSpace }) , 
                    Type.CONTINUOUS );
            doMSYCost = new MSYCost1();
        }
        
        /**
         * Only one parameter so no more currying is required.  s -> continuous <br>
         * msyCost( msy ) is called here.
         */
        public class MSYCost1 extends Value.Function
        {
            /** Serial ID required to evolve class while maintaining serialisation compatibility. */
            private static final long serialVersionUID = 2957730252125891111L;

            MSYCost1()
            {
                super(paramType);
            }
            
            public double applyDouble( Value v )
            {
                try {
                    return learner.msyCost( (Value.Structured)v );
                } catch ( ModelLearner.LearnerException e) { throw new RuntimeException(e); }
            }
            
            public Value apply( Value v )
            {
                return new Value.Continuous( applyDouble(v) );
            }
        }
    }
    
    
    
    /**     
     * A Value.Model implementing GetNumParams can return the number of continuous parameters
     * required to state a model.  This may be useful for implementing no MML metrics, (and is
     * useful when you hava a numParams column in a paper...)
     */
    public interface GetNumParams {
        public int getNumParams( Value params );
    }
    
}
