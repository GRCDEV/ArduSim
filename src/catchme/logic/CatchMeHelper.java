package catchme.logic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.KeyStroke;

import org.javatuples.Pair;

import api.API;
import api.ProtocolHelper;
import api.pojo.location.Location2DGeo;
import main.sim.gui.MainWindow;
import mbcap.logic.MBCAPParam;

public class CatchMeHelper extends ProtocolHelper {

	@Override
	public void setProtocol() {
		this.protocolString = "Catch Me";
	}

	@Override
	public boolean loadMission() {
		return false;
	}

	@Override
	public JDialog openConfigurationDialog() {
		return null;
	}

	@Override
	public void initializeDataStructures() {
		CatchMeParams.targetLocationPX = new AtomicReference();
		
	}

	@Override
	public String setInitialState() {
		return "Starting";
	}

	@Override
	public void rescaleDataStructures() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void loadResources() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void rescaleShownResources() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void drawResources(Graphics2D graphics, main.sim.board.BoardPanel panel) {
		graphics.setStroke(CatchMeParams.STROKE_POINT);
		int numUAVs = 1;
		Point2D.Double position;
		position = CatchMeParams.targetLocationPX.get();
		if(position != null) {
			int x = (int)position.x;
			int y = (int)position.y;
			graphics.setColor(new Color(255,0,0));
			graphics.drawLine(x - 10, y - 10, x + 10, y + 10);
			graphics.drawLine(x + 10, y - 10, x - 10, y + 10);
		}
	}

	@Override
	public Pair<Location2DGeo, Double>[] setStartingLocation() {
		return new Pair[] {Pair.with(CatchMeParams.startingLocation.getGeoLocation(), 0)};
	}

	@Override
	public boolean sendInitialConfiguration(int numUAV) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void startThreads() {
		ListenerThread listen = new ListenerThread();
		listen.start();
		
	}

	@Override
	public void setupActionPerformed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startExperimentActionPerformed() {
		// TODO Auto-generated method stub
		ActionListener left = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				StartExpWithDraws.move("left");
			}
		};
		ActionListener right = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				StartExpWithDraws.move("right");
			}
		};
		ActionListener up = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				StartExpWithDraws.move("up");
			}
		};
		ActionListener down = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				StartExpWithDraws.move("down");
			}
		};
		
		MainWindow.boardPanel.registerKeyboardAction(left,
				KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		MainWindow.boardPanel.registerKeyboardAction(right,
				KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		MainWindow.boardPanel.registerKeyboardAction(up,
				KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		MainWindow.boardPanel.registerKeyboardAction(down,
				KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		
	}

	@Override
	public void forceExperimentEnd() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getExperimentResults() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getExperimentConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void logData(String folder, String baseFileName, long baseNanoTime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void openPCCompanionDialog(JFrame PCCompanionFrame) {
		// TODO Auto-generated method stub
		
	}

}
