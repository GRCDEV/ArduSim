package main.sim.board;

import java.awt.Graphics;
import javax.swing.JPanel;

/** This class generates the panel used to draw the map and the UAVs movement.
 * <p>Developed by: Francisco Jos&eacute; Fabra Collado, from GRC research group in Universitat Polit&egrave;cnica de Val&egrave;ncia (Valencia, Spain).</p> */

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