package sim.board;

import java.awt.Graphics;
import javax.swing.JPanel;

/** This class generates the panel used to draw the map and the UAVs movement. */

public class BoardPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	public BoardPanel() {
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		BoardHelper.paintBoard(g, this);
	}
}
