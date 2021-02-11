package com.setup.sim.gui;

import es.upv.grc.mapper.DrawableScreenAnchor;
import es.upv.grc.mapper.GUIMapPanel;
import es.upv.grc.mapper.GUIMapPanelNotReadyException;
import es.upv.grc.mapper.Mapper;
import com.api.ArduSimTools;
import com.setup.Main;
import com.setup.Param;
import com.setup.Param.SimulatorState;
import com.setup.Text;
import com.setup.sim.logic.SimParam;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/** This class generates the main windows of the application. It consists on three elements: A log panel, a panel with buttons to interact with, and a panel to show the UAVs moving over a satellite map.</p>
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

public class MainWindow {

	public static MainWindow window;
	public static GUIMapPanel boardPanel;
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
		MainWindowButtonsPanel buttonsPanel = new MainWindowButtonsPanel();
		buttonsPanel.setName(Text.CONFIGURATION_PANEL);
		buttonsPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
		
		BoxLayout boxlayout = new BoxLayout(mainWindowFrame.getContentPane(), BoxLayout.Y_AXIS);
		mainWindowFrame.getContentPane().setLayout(boxlayout);

		// Draws a panel to show the simulated UAVs
		int initialMap = Mapper.BING_AERIAL;
		if (SimParam.bingKey == null) {
			initialMap = Mapper.OPEN_STREET_MAPS;
		}
		GUIMapPanel boardPanel = Mapper.getMapPanel(Main.class, (JPanel)mainWindowFrame.getContentPane(),
				SimParam.failedMapDownloadCheckPeriod, initialMap, SimParam.bingKey,
				SimParam.screenUpdatePeriod, SimParam.minScaleUpdatePeriod);
		boardPanel.setName(Text.DRAWING_PANEL);

		mainWindowFrame.getContentPane().add(buttonsPanel);
		mainWindowFrame.getContentPane().add(boardPanel);

		mainWindowFrame.pack();
		
		mainWindowFrame.setSize(mainWindowFrame.getWidth(), buttonsPanel.getHeight() + 500);
		mainWindowFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);

		mainWindowFrame.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		mainWindowFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				MainWindow.window.closeArduSim();
			}
		});
		mainWindowFrame.setTitle(Text.APP_NAME);
		ActionListener escListener = e -> MainWindow.window.closeArduSim();
	    mainWindowFrame.getRootPane().registerKeyboardAction(escListener,
	            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
	            JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		
		mainWindowFrame.setVisible(true);
		MainWindow.buttonsPanel = buttonsPanel;
		MainWindow.boardPanel = boardPanel;
	}
	
	/** Request to close ArduSim simulator. */
	public void closeArduSim() {
		(new Thread(() -> {
			synchronized(MainWindow.CLOSE_SEMAPHORE) {
				if (Param.simStatus != SimulatorState.SHUTTING_DOWN
						&& MainWindow.buttonsPanel.exitButton.isEnabled()) {
					ArduSimTools.shutdown();
				}
			}
		})).start();
	}
	
	/** Builds the wind image (arrow) shown in the drawing panel. */
	public void buildWindImage() {
		int width = SimParam.arrowImage.getWidth();
		int height = SimParam.arrowImage.getHeight();
		BufferedImage arrowImageRotated = GraphicsEnvironment.getLocalGraphicsEnvironment().
				getDefaultScreenDevice().getDefaultConfiguration().
				createCompatibleImage(width, height, Transparency.TRANSLUCENT);
		Graphics2D g2 = arrowImageRotated.createGraphics();
		AffineTransform trans = new AffineTransform();
		trans.translate(width * 0.5, height * 0.5);
		trans.rotate(Param.windDirection * Math.PI/180.0 + Math.PI/2.0);
		trans.translate(-width * 0.5, - height * 0.5);
		g2.drawImage(SimParam.arrowImage, trans, null);
		g2.dispose();
		try {
			Mapper.Drawables.addImageScreen(SimParam.WIND_LEVEL, DrawableScreenAnchor.UP_RIGHT_CORNER,
					new Point(-width / 2, height /2), 0, arrowImageRotated, Math.max(width, height));
		} catch (GUIMapPanelNotReadyException e) {
			e.printStackTrace();
		}
	}
	
}
