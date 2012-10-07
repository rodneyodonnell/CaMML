package camml.core.newgui;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.Arrays;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.event.MouseInputAdapter;

import norsys.netica.Environ;
import norsys.netica.Net;
import norsys.netica.NeticaException;
import norsys.netica.Node;
import norsys.netica.NodeList;
import norsys.netica.Streamer;
import norsys.netica.Value;
import norsys.netica.gui.NetPanel;
import norsys.netica.gui.NodePanel;
import norsys.netica.gui.NodePanel_BeliefBars;
import norsys.netica.gui.NodePanel_BeliefBarsRow;
import javax.swing.JFrame;


/**Bayesian Network Viewer using Netica Libraries 
 * Displays a Bayesian network parameterized by CaMML, allows user to reposition nodes,
 * and run inference on that network.
 * 
 * Note: May not function if Netica library files are not in a location that is
 * 	part of the OS PATH environment variable.
 * 
 * @author Alex Black
 * 
 */
public class BNetViewer extends JFrame {
	private static final String defaultWindowName = "CaMML - Network Viewer";
	protected NetPanel netPanel;
	protected static Environ env = null;
	private Net net;
	
	private static final int windowWidth = 800;
	private static final int windowHeight = 600;
	
	//Graph layout constants:
	private static final int nodeWidth = 180;
	private static final int nodeHeight = 120;
	private static final int nodeSeparation = 50;
	
	/**
	 * 
	 * @param network Bayesian Network - the entire network as a String (if isFileName == false) or the path of a network file
	 * @param isFileName True: Load from file (network is file path). False: network specified by string 
	 * @param windowTitle Title of display window
	 * @throws Exception
	 */
	public BNetViewer( String network, boolean isFileName, String windowTitle ) throws Exception {
		

		if( env == null ) env = new Environ(null);
		
		//Network path passed:
		if( isFileName ){
			net = new Net( new Streamer( network ) );
		} else { //Assume actual network is passed as a string...
			//Create from string: (A bit of a hackish work-around, but the String needs to be some sort of InputStream...)
			net = new Net( new Streamer(new ByteArrayInputStream( network.getBytes("UTF-8")), "StringReader", env ) );
		}
		
		//Lay out the graph: (By default, all nodes are at (0,0))
		layoutGraph();
			
		net.compile(); // optional
		
		// Create a NetPanel for 'net'
		netPanel = new NetPanel(net, NodePanel.NODE_STYLE_AUTO_SELECT);


		// Make all the components listen for mouse clicks
		netPanel.addListenerToAllComponents(new Viewer_MouseInputAdapter( this ));

		// Add the panel to the application's content pane
		getContentPane().add(new JScrollPane(netPanel));

		//Set the title for the window
		this.setTitle( windowTitle );
		
		// Close the window (but not program) when the user clicks 'X'
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Set the frame (i.e. window) size and show the frame
		setSize(windowWidth, windowHeight);
		setVisible(true);
	}
	
	public BNetViewer( String network, boolean isFileName ) throws Exception {
		this( network, isFileName, defaultWindowName );
	}
	
	
	/**Lays the graph out in a basic grid arrangement.
	 * No doubt there are MUCH better methods of doing such a layout...
	 * 
	 * Presently implemented to use a basic grid layout, layed out top left to
	 * bottom right according to the order given by net.getNodes()
	 */
	public void layoutGraph(){
		//First: Get all Nodes...
		NodeList list;
		try{
			list = net.getNodes();
		} catch( NeticaException e ){
			return;
		}
		
		int numPerRow = (windowWidth - nodeWidth) / (nodeWidth);
		
		for( int i = 0; i < list.size(); i++ ){
			Node n = (Node)list.get(i);													//Current node
			int rowNum = i*(nodeWidth + nodeSeparation) / (windowWidth - nodeWidth);	//Determine row number for this node
			int posY = rowNum * (nodeHeight + nodeSeparation);
			int posX = nodeSeparation + i % numPerRow * ( nodeWidth + nodeSeparation );
			try{
				n.visual().setPosition(posX, posY);
			} catch( NeticaException e ){
				//Do nothing?
			}
		}
	}
	

}

/**Class to deal with mouse events: i.e. click, click+drag on BN nodes
 * Required for inference and repositioning nodes.
 * @author Alex Black
 */
class Viewer_MouseInputAdapter extends MouseInputAdapter {
	
	int mouseDownStartX = 0;
	int mouseDownStartY = 0;
	Component lastClicked;
	
	BNetViewer viewer;
	
	public Viewer_MouseInputAdapter( BNetViewer viewer ){
		this.viewer = viewer;
	}
	
	//User clicks mouse:
	public void mouseClicked(MouseEvent me) {
		try {
			// Find out which component got clicked
			Component comp = me.getComponent();
			
			// If a belief bar was clicked...
			if (comp instanceof NodePanel_BeliefBarsRow) {
				NodePanel_BeliefBarsRow row = (NodePanel_BeliefBarsRow) comp;

				// Find the state index of the belief bar
				int clickedState = row.getState().getIndex();
				
				// Get the node that owns this belief bar, and get that node's current finding
				Value finding    = row.getState().getNode().finding();
				
				// If the node finding is what was clicked, clear the finding
				if (finding.getState() == clickedState) {
					finding.clear();
				// Otherwise, set the finding
				} else {
					finding.setState(clickedState);
				}
				
				// Compile net and refresh the display
				viewer.netPanel.getNet().compile();
				viewer.netPanel.refreshDataDisplayed();
			}
		} catch (NeticaException e) { e.printStackTrace(); }
	}
	
	//User clicks and holds mouse button down:
	public void mousePressed(MouseEvent me){
		//Store the location (+component) where the click+drag started, for later use:
		mouseDownStartX = me.getX();
		mouseDownStartY = me.getY();
		lastClicked = me.getComponent();
	}
	
	//User releases mouse after holding down:
	public void mouseReleased(MouseEvent me){
		if( lastClicked == null ) return;
		
		Component toMove;
		
		if( lastClicked instanceof NodePanel ){
			toMove = lastClicked;
		} else if( lastClicked instanceof NodePanel_BeliefBarsRow ){
			toMove = ((NodePanel_BeliefBarsRow)lastClicked).getParent().getParent();
		} else if( lastClicked instanceof NodePanel_BeliefBars ){
			toMove = ((NodePanel_BeliefBars)lastClicked).getParent();
		} else if( lastClicked instanceof JLabel ){
			toMove = ((JLabel)lastClicked).getParent();
		} else{
			//Unknown component: Don't know how to deal with it...
			return;
		}
		
		if( toMove == null ) return;	//Should never happen...
		
		int newX = toMove.getX() + me.getX() - mouseDownStartX;
		int newY = toMove.getY() + me.getY() - mouseDownStartY;
		
		
		
		//Make sure it cannot be moved outside of window:
		if( newX < 0 ) newX = 0;
		if( newY < 0 ) newY = 0;
		if( newX > viewer.getWidth() - toMove.getWidth()  ) newX = viewer.getWidth() - toMove.getWidth();
		if( newY > viewer.getHeight() - toMove.getHeight() ) newY = viewer.getHeight() - toMove.getHeight();
		
		toMove.setLocation( newX, newY);
		
		// Compile net and refresh the display
		try{
			viewer.netPanel.getNet().compile();
			viewer.netPanel.refreshDataDisplayed();
		} catch( NeticaException e){ 
			//e.printStackTrace();
		}
		
	}
	

}