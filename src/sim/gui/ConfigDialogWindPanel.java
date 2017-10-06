package sim.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import api.GUIHelper;
import main.Param;
import main.Text;
import sim.logic.SimParam;

/** This class generates a panel to show the wind direction as an arrow, on the general configuration dialog. */

public class ConfigDialogWindPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	public ConfigDialogWindPanel() {

		setMinimumSize(new Dimension(SimParam.ARROW_PANEL_SIZE, SimParam.ARROW_PANEL_SIZE));
		setMaximumSize(new Dimension(SimParam.ARROW_PANEL_SIZE, SimParam.ARROW_PANEL_SIZE));
		setPreferredSize(new Dimension(SimParam.ARROW_PANEL_SIZE, SimParam.ARROW_PANEL_SIZE));

		// Load and scale the image to be drawn
		URL url = MainWindow.class.getResource(SimParam.ARROW_IMAGE_PATH);
		try {
			BufferedImage dummyImage = ImageIO.read(url);
			SimParam.arrowImage = new BufferedImage(SimParam.ARROW_PANEL_SIZE,
					SimParam.ARROW_PANEL_SIZE, dummyImage.getType());
			Graphics2D g = SimParam.arrowImage.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(dummyImage, 0, 0, SimParam.ARROW_PANEL_SIZE, SimParam.ARROW_PANEL_SIZE,
					0, 0, dummyImage.getWidth(), dummyImage.getHeight(), null);
			g.dispose();
			this.repaint();
		} catch (IOException e) {
			GUIHelper.exit(Text.ARROW_IMAGE_LOAD_ERROR);
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
				RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2.setRenderingHint( RenderingHints.  KEY_STROKE_CONTROL,
				RenderingHints.VALUE_STROKE_PURE);

		AffineTransform trans = new AffineTransform();
		trans.translate(SimParam.arrowImage.getWidth()/2.0, SimParam.arrowImage.getHeight()/2.0);
		trans.rotate(Param.windDirection*Math.PI/180.0 + Math.PI/2.0);
		trans.translate(-SimParam.arrowImage.getWidth()/2.0, -SimParam.arrowImage.getHeight()/2.0);
		g2.drawImage(SimParam.arrowImage, trans, this);
	}

}
