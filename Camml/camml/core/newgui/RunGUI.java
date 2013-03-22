package camml.core.newgui;

import java.awt.EventQueue;


/**Entry point for running the (new) GUI.
 * No command line arguments etc required.
 * @author Alex Black
 */
public class RunGUI {
	public static void main( String args[] ){
		//Create model object for storing data:
		final GUIModel model = new GUIModel();
		
		//Create GUI object and run CaMML:
		EventQueue.invokeLater( new Runnable(){
			public void run(){
				CaMMLGUI frame = new CaMMLGUI( model );
				frame.setVisible(true);
			}
		});
	}
}
