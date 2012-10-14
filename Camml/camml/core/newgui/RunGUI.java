package camml.core.newgui;

import java.awt.EventQueue;


/**Entry point for running GUI
 * 
 * @author Alex Black
 */
public class RunGUI {
	
	public static void main( String args[] ){
		//Create model object for storing data:
		final GUIModel model = new GUIModel();
		
		//Create GUI object and run CaMML:
		EventQueue.invokeLater( new Runnable(){
			public void run(){
				cammlGUI frame = new cammlGUI( model );
				frame.setVisible(true);
			}
		});
	}
}
