//
// Camml plugin for CDMS
//
// Author        : Rodney O'Donnell
// Last Modifies : 1-6-02
//
// Source formatted to 100 columns.
// 1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567

// File: CammlRuntimeException.java
// General runtime exception to be throws by all Value.Function.apply() under camml. 

package camml.plugin.scripter;

/** This exception should be thrown in the apply method of a Value.Function.  It is required as
 *  a apply cannot throw non checked Exceptions (IE. mainly Exceptions which are not 
 *  RuntimeExceptions) this fixes that problem by throwing them as a RuntimeException.
 */
public class CammlRuntimeException extends RuntimeException {
    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
    private static final long serialVersionUID = 3901348368810252494L;

    CammlRuntimeException( ) {
        super ("Camml Runtime Exception");
    }
    
    CammlRuntimeException( String s ) {
        super ("Camml Runtime Exception : " + s);
    }
}








