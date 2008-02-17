
package cdms.plugin.bean;

// Support for a PropertyEditor that uses text.

import java.awt.*;
import java.awt.event.*;
import java.beans.*;

class PropertyText extends TextField implements KeyListener, FocusListener {

    /** Serial ID required to evolve class while maintaining serialisation compatibility. */
	private static final long serialVersionUID = -8432117484999110736L;

	PropertyText(PropertyEditor pe) {
	super(pe.getAsText());
	editor = pe;
	addKeyListener(this);
	addFocusListener(this);
    }

    public void repaint() {
	setText(editor.getAsText());
    }

    protected void updateEditor() {
	try {
	    editor.setAsText(getText());
	} catch (IllegalArgumentException ex) {
	    // Quietly ignore.
	}
    }
    
    //----------------------------------------------------------------------
    // Focus listener methods.

    public void focusGained(FocusEvent e) {
    }

    public void focusLost(FocusEvent e) {
    	updateEditor();
    }
    
    //----------------------------------------------------------------------
    // Keyboard listener methods.

    public void keyReleased(KeyEvent e) {
 	if (e.getKeyCode() == KeyEvent.VK_ENTER) {
	    updateEditor();
	}
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    //----------------------------------------------------------------------
    private PropertyEditor editor;
}
