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

package camml.core.library.extensionCounter;

import java.util.BitSet;

public class ExtensionCounterLib {
    
    /** Mask for each element in arcMatrix. nodeMask[i] = 1<<(i-1) <br>
     *  This code is essentially useless but may clarify some areas of the code. */
    public final static long[] nodeMask = new long[64];
    static { for (int i = 0; i < 64; i++) {nodeMask[i] = 1l << (i); } }
    
    /** count the number of bits set in x */
    public static int countBits( long x ) {
        // This algorithm uses the nice little trick that
        // x & (x-1) == x with its lowest bit set to false.
        // It makes sense if you think about it for a while.
        int count = 0;
        while (x != 0) { x = x & (x-1); count++;}
    
        return count;
    }
    
    public static int[] getNodeArray(long x)
    {
        int[] array = new int[ExtensionCounterLib.countBits(x)];
        int arrayIndex = 0;
        int bitIndex = 0;
        while (arrayIndex < array.length && bitIndex < 64) {
            if ((x & nodeMask[bitIndex]) != 0) {
                array[arrayIndex] = bitIndex;
                arrayIndex ++;
            }
            bitIndex++;
        }
        assert( arrayIndex == array.length );
        return array;
    }

    public static int[] getNodeArray(BitSet bitSet)
    {
        int[] array = new int[bitSet.cardinality()];
        int arrayIndex = 0;
        for( int bit = bitSet.nextSetBit(0); bit >= 0; bit = bitSet.nextSetBit(bit+1) ) {
            try{
                array[arrayIndex] = bit;
            }
            catch (Exception e) {
                System.out.println("Broken!");
            }
            arrayIndex ++;
        }
        assert( arrayIndex == array.length );
        return array;
    }
    
    public static long toLong(BitSet bitSet) {
        long x = 0;        
        for( int bit = bitSet.nextSetBit(0); bit >= 0; bit = bitSet.nextSetBit(bit+1) ) {
            x = x | nodeMask[bit];
        }
        return x;
    }

    public static BitSet toBitSet(long x) {
        BitSet bitSet = new BitSet();
        int[] array = getNodeArray(x);
        for (int i = 0; i < array.length; i++) {
            bitSet.set(array[i]);
        }
        return bitSet;
    }
    
    public static long removeBit( long x, int bit ) {
        // Create masks used for removing/shifting out of parent/child list.
        long mask1 = -1l << bit;
        long mask2 = -1l >>> 64-(bit);
    
        // We need a special check for boundary conditions.
        if (bit == 0) {mask2 = 0;}
        if (bit == 63) {mask1 = 0;}
    
        return ((x >>> 1) & mask1) | (x & mask2);
    }
    
    public static BitSet removeBit( BitSet bitSet, int bitPos, boolean overwrite ) {
        BitSet lower = bitSet;
        if (!overwrite) {lower = (BitSet)bitSet.clone(); }
        
        // Get all bits above bitPos, shift them left by 1
        BitSet upper = bitSet.get(1,bitSet.size());
        if (bitPos > 0) { upper.clear(0,bitPos); }
        
        lower.clear(bitPos, bitSet.size());
        lower.or(upper);
        return lower;
    }
}
