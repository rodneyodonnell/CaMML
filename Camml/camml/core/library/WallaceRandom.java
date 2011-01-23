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
// File contains a translation of c7randi.c to java (a random number generator by Chris Wallace)
//

// File: WallaceRandom.java
// Author: rodo@csse.monash.edu.au (original csw@csse.monash.edu.au)


package camml.core.library;

import java.util.Random;

/*

  A random number generator called as a function by
  c7rand (iseed)    or    irandm (iseed)

  IMPORTANT     I M P O R T A N T    !!!!!!!!

  This algorithm only works if the arithmetic is done as signed 32-bit
  2-s complement integers.   Please set the "define Word" to define
  either int or long as appropriate
*/



/**
   The parameter should be a pointer to a 2-element Word vector.
   The first call gives a double uniform in 0 .. 1.
   The second gives an Word integer uniform in 0 .. 2**31-1
   Both update iseed[] in exactly the same way.
   iseed[] must be a 2-element Word vector.
   The initial value of iseed[1] may be any 32-bit integer.
   The initial value of iseed[0] may be any 32-bit integer except -1.

   The period of the random sequence is 2**32 * (2**32-1)

   This is an implementation in C of the algorithm described in
   Technical Report "A Long-Period Pseudo-Random Generator"
   TR89/123, Computer Science, Monash University,
   Clayton, Vic 3168 AUSTRALIA
   by

   C.S.Wallace     csw@cs.monash.edu.au

   The table mt[0:127] is defined by mt[i] = 69069 ** (128-i)


   NOTE: Only nextDouble() and nextInt() are implemented.
*/
public class WallaceRandom extends java.util.Random
{
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 8323728949824870871L;

    /** seed of generator */
    protected int seed[] = new int[2];

    public int numCalls = 0;

    protected boolean verbose = false;
    public void setVerbose( boolean verbose )
    {
        this.verbose = verbose;
    }

    public int[] getSeed() { return seed; }
    public void setSeed( int[] seed ) { 
        if (seed.length == 2) {
            this.seed = seed;
        }  
        else {
            throw new RuntimeException("Invalid seed length");
        }
    }

    /** Create a WallaceRandom using java.util.Random to generate initial seed.*/
    public WallaceRandom() {
        Random r = new Random();
        this.seed[0] = r.nextInt();
        this.seed[1] = r.nextInt();
    }
    
    /** seed should be an integer array containing two seed values. */
    public WallaceRandom( int[] seed )
    {
        if ( seed.length != 2) {
            throw new IllegalArgumentException("Invalid seed value in WallaceRandom,");
        }
        this.seed[0] = seed[0];
        this.seed[1] = seed[1];
    }

    /** generate a double on the interval (0,1), update seed */
    public double nextDouble()
    {
        double x = c7rand( seed );
        if ( verbose ) { System.out.println("double::"+x);} numCalls++;
        return x;
    }
    
    /** 
     * return an integer. <br>
     * NOTE: unlike java.util.Random the range returned is (0,UPB) (inclusive??);
     */
    public int nextInt()
    {
        int x = irandm(seed);
        if ( verbose ) { System.out.println("int::"+x);}
        return x;
    }


    /** NOT IMPLEMENTED */
    public boolean nextBoolean() 
    {
        throw new RuntimeException("function not implemented");
    }

    /** NOT IMPLEMENTED */
    public void nextBytes( byte[] bytes) 
    {
        throw new RuntimeException("function not implemented");
    }

    /** NOT IMPLEMENTED */
    public float nextFloat() 
    {
        throw new RuntimeException("function not implemented");
    }

    
    /**
     * return nextInt() % n. <br>
     * NOTE: This is bias for values of n which are not factors of 2^32, larger values of n produce
     *       a higher bias.  This should be fixed.
     */
    public int nextInt(int n) {
        if (n<=0)
            throw new IllegalArgumentException("n must be positive");
    
        return nextInt() % n;
    }

    /** NOT IMPLEMENTED */
    public double nextGaussian() 
    {
        throw new RuntimeException("function not implemented");
    }

    /** NOT IMPLEMENTED */
    public long nextLong() 
    {
        long x = ((long)nextInt() << 32) + (long)nextInt();
        if ( verbose ) { System.out.println("long::"+x); }
        return x;
    }

    /**  */
    public void setSeed( long seed ) 
    {
        setSeed( new int[] {(int)(seed >> 32), (int)(seed & 0xFFFFFFFF)} );
    }


    /////////////////////////////////////////////////////////////////////////////////////////////
    //                   Functions taken directly from original code.                          //
    /////////////////////////////////////////////////////////////////////////////////////////////

    /** generate a double on interval (0,1) */
    public static double c7rand ( int[] is)
    {
        int it, leh;
    
        it = is [0];
        leh = is [1];
        /*    Do a 7-place right cyclic shift of it  */
        it = ((it >> 7) & 0x01FFFFFF) + (it << 25);
        if (it >= 0) it = it ^ MASK;
        leh = leh * mt[it & 127] + it;
        is [0] = it;    is [1] = leh;
        if (leh < 0) leh = ~leh;
        return (SCALE * ((int) (leh | 1)));
    }
 
    /** generate an integer */
    public static int irandm (int[] is)
    {
        int it, leh;
    
        it = is [0];
        leh = is [1];
        /*    Do a 7-place right cyclic shift of it  */
        it = ((it >> 7) & 0x01FFFFFF) + (it << 25);
        if (it >= 0) it = it ^ MASK;
        leh = leh * mt[it & 127] + it;
        is [0] = it;    is [1] = leh;
        if (leh < 0) leh = ~leh;
        return (leh);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    //                     Below are various constants used in generations.                    //
    /////////////////////////////////////////////////////////////////////////////////////////////
    
    /** or in decimal, 316492066  */
    private static final int MASK = 0x12DD4922;

    /**    i.e. 2 to power -31    */
    private static final double SCALE = (1.0 / (1024.0 * 1024.0 * 1024.0 * 2.0));

    private static final int mt[] = new int[] {    
        902906369,    2030498053,    -473499623,    1640834941,    723406961,    1993558325,
        -257162999,    -1627724755,    913952737,    278845029,    1327502073,    -1261253155,
        981676113,    -1785280363,    1700077033,    366908557,    -1514479167,    -682799163,
        141955545,    -830150595,    317871153,    1542036469,    -946413879,    -1950779155,
        985397153,    626515237,    530871481,    783087261,    -1512358895,    1031357269,
        -2007710807,    -1652747955,    -1867214463,    928251525,    1243003801,    -2132510467,
        1874683889,    -717013323,    218254473,    -1628774995,    -2064896159,    69678053,
        281568889,    -2104168611,    -165128239,    1536495125,    -39650967,    546594317,
        -725987007,    1392966981,    1044706649,    687331773,    -2051306575,    1544302965,
        -758494647,    -1243934099,    -75073759,    293132965,    -1935153095,    118929437,
        807830417,    -1416222507,    -1550074071,    -84903219,    1355292929,    -380482555,
        -1818444007,    -204797315,    170442609,    -1636797387,    868931593,    -623503571,
        1711722209,    381210981,    -161547783,    -272740131,    -1450066095,    2116588437,
        1100682473,    358442893,    -1529216831,    2116152005,    -776333095,    1265240893,
        -482278607,    1067190005,    333444553,    86502381,    753481377,    39000101,
        1779014585,    219658653,    -920253679,    2029538901,    1207761577,    -1515772851,
        -236195711,    442620293,    423166617,    -1763648515,    -398436623,    -1749358155,
        -538598519,    -652439379,    430550625,    -1481396507,    2093206905,    -1934691747,
        -962631983,    1454463253,    -1877118871,    -291917555,    -1711673279,    201201733,
        -474645415,    -96764739,    -1587365199,    1945705589,    1303896393,    1744831853,
        381957665,    2135332261,    -55996615,    -1190135011,    1790562961,    -1493191723,
        475559465,    69069
    };



    public String toString()
    {
        return "Wallace random generator";
    }
    
}

 
//   // Random seeds set.  No particular reasons for choosing 7 and 17.
//   SetSeed( 7, 17);


// // Random Float and Int generated using c7randi
// double LOOKUP::RandomDouble( void )
// {
//   return c7rand( Seed );
// }

// int LOOKUP::RandomInt( void )
// {
//   return irandm( Seed );
// }

// void LOOKUP::SetSeed( int a, int b )
// {
//   Seed[0] = a;
//   Seed[1] = b;
// }

// int  LOOKUP::GetSeed( int Num )
// {
//   if ( (Num != 0) && (Num != 1) )
//     throw ERROR("Invalid Seed requested", Num);
//   return Seed[Num];
// }






