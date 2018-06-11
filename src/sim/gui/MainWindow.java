package sim.gui;

import java.awt.Color;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;

import main.Param;
import main.Text;
import main.ArduSimTools;
import main.Param.SimulatorState;
import sim.board.BoardPanel;
import sim.logic.SimParam;

/** This class generates the main windows of the application. It consists on three elements:
 * <p>A log panel.
 * <p>A panel with buttons to interact with.
 * <p>A panel to show the UAVs moving over a satellite map. */

public class MainWindow {

	public static MainWindow window;
	public static BoardPanel boardPanel;
	public static MainWindowButtonsPanel buttonsPanel;
	public static ProgressDialog progressDialog;
	
	public JFrame mainWindowFrame;

	/**
	 * Create the application.
	 */
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
		int left = Toolkit.getDefaultToolkit().getScreenInsets(config).left;
		int right = Toolkit.getDefaultToolkit().getScreenInsets(config).right;
		int top = Toolkit.getDefaultToolkit().getScreenInsets(config).top;
		int bottom = Toolkit.getDefaultToolkit().getScreenInsets(config).bottom;
		
		int width = config.getBounds().width - left - right;
		int height = config.getBounds().height - top - bottom;
		mainWindowFrame.setSize(width, height);
//		mainWindowFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		mainWindowFrame.setResizable(false);

		mainWindowFrame.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		mainWindowFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				(new Thread(new Runnable() {
					public void run() {
						if (Param.simStatus != SimulatorState.SHUTTING_DOWN
								&& MainWindow.buttonsPanel.exitButton.isEnabled()) {
							ArduSimTools.shutdown();
						}
					}
				})).start();
			}
		});
		mainWindowFrame.setTitle(Text.APP_NAME);
		mainWindowFrame.setVisible(true);
		MainWindow.buttonsPanel = buttonsPanel;
		MainWindow.boardPanel = boardPanel;
		// Store drawing panel dimensions
		SimParam.boardPXHeight = MainWindow.boardPanel.getHeight();
		SimParam.boardPXWidth = MainWindow.boardPanel.getWidth();
	}

}
