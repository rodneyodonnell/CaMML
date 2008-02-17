
package cdms.plugin.bean;

/**
 * Pop up a (modal) error dialog and wait for a user to press "continue".
 */

import java.awt.*;

public class ErrorDialog extends MessageDialog {

    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -7835093757872944414L;

	public ErrorDialog(Frame frame, String message) {
	super(frame, "Error", message);
    }

}
