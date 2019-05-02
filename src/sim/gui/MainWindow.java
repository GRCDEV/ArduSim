package sim.gui;

import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import main.ArduSimTools;
import main.Param;
import main.Param.SimulatorState;
import main.Text;
import sim.board.BoardPanel;
import sim.logic.SimParam;

/** This class generates the main windows of the application. It consists on three elements: A log panel, a panel with buttons to interact with, and a panel to show the UAVs moving over a satellite map.</p>
 * <p>Developed by: Francisco José Fabra Collado, from GRC research group in Universitat Politècnica de València (Valencia, Spain).</p> */

public class MainWindow {

	public static MainWindow window;
	public static BoardPanel boardPanel;
	public static MainWindowButtonsPanel buttonsPanel;
	
	public JFrame mainWindowFrame;
	
	public final static Object CLOSE_SEMAPHORE = new Object();

	public MainWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the main frame.
	 */
	private void initialize() {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		GraphicsConfiguration config = gd.getDefaultConfiguration();
		mainWindowFrame = new JFrame(config);

		// User interaction panel
		MainWindowButtonsPanel buttonsPanel = new MainWindowButtonsPanel(mainWindowFrame);
		buttonsPanel.setName(Text.CONFIGURATION_PANEL);
		buttonsPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));

		// Draws a panel to show the simulated UAVs
		BoardPanel boardPanel = new BoardPanel();
		boardPanel.setName(Text.DRAWING_PANEL);

		BoxLayout boxlayout = new BoxLayout(mainWindowFrame.getContentPane(), BoxLayout.Y_AXIS);
		mainWindowFrame.getContentPane().setLayout(boxlayout);

		mainWindowFrame.getContentPane().add(buttonsPanel);
		mainWindowFrame.getContentPane().add(boardPanel);

		if (Param.runningOperatingSystem != Param.OS_WINDOWS) {
			mainWindowFrame.setUndecorated(true);
		}
		mainWindowFrame.pack();

		//  Adapting the window to the screen size
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(config);
		int left = insets.left;
		int right = insets.right;
		int top = insets.top;
		int bottom = insets.bottom;
		
		int width = config.getBounds().width - left - right;
		int height = config.getBounds().height - top - bottom;
		mainWindowFrame.setSize(width, height);
//		mainWindowFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		mainWindowFrame.setResizable(false);

		mainWindowFrame.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		mainWindowFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				MainWindow.window.closeArduSim();
			}
		});
		mainWindowFrame.setTitle(Text.APP_NAME);
		ActionListener escListener = new ActionListener() {

	        @Override
	        public void actionPerformed(ActionEvent e) {
	        	MainWindow.window.closeArduSim();
	        }
	    };
	    mainWindowFrame.getRootPane().registerKeyboardAction(escListener,
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
	            JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		
		mainWindowFrame.setVisible(true);
		MainWindow.buttonsPanel = buttonsPanel;
		MainWindow.boardPanel = boardPanel;
		// Store drawing panel dimensions
		SimParam.boardPXHeight = MainWindow.boardPanel.getHeight();
		SimParam.boardPXWidth = MainWindow.boardPanel.getWidth();
	}
	
	/** Request to close ArduSim simulator. */
	public void closeArduSim() {
		(new Thread(new Runnable() {
			@Override
			public void run() {
				synchronized(MainWindow.CLOSE_SEMAPHORE) {
					if (Param.simStatus != SimulatorState.SHUTTING_DOWN
							&& MainWindow.buttonsPanel.exitButton.isEnabled()) {
						ArduSimTools.shutdown();
					}
				}
			}
		})).start();
	}
	
}
